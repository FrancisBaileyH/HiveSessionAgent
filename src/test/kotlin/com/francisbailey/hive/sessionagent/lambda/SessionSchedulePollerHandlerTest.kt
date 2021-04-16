package com.francisbailey.hive.sessionagent.lambda

import com.francisbailey.hive.common.HiveBookingClient
import com.francisbailey.hive.common.HiveLocation
import com.francisbailey.hive.common.ScheduleAvailability
import com.francisbailey.hive.common.ScheduleEntry
import com.francisbailey.hive.common.SessionPeriod
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEntry
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEvent
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityPeriod
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

class SessionSchedulePollerHandlerTest {

    private val hiveClient = mock<HiveBookingClient>()

    private val snsClient = mock<SnsClient>()

    private val cloudWatchClient = mock<CloudWatchClient>()

    private val sessionTopicArn = "arn:aws:sns:us-east-2:123412341234:SessionScheduleAvailability"

    private val sessionSchedulePollerHandler = SessionSchedulePollerHandler(snsClient, sessionTopicArn, cloudWatchClient)

    init {
        val lookAheadRange = (0..6L)
        HiveLocation.values().forEach { location ->
            val startDate = LocalDate.now(Clock.system(location.zoneId))
            lookAheadRange.forEach { lookAheadDay ->
                val date = startDate.plusDays(lookAheadDay)
                whenever(runBlocking { hiveClient.getBookingAvailability(date, location) }).thenReturn(listOf())
            }
        }
    }

    @Test
    fun `should publish event for every date that has availability`() {
        val scheduleAvailabilityEvent = ScheduleEntry(
            sessionPeriod = SessionPeriod(startTime = LocalDateTime.now(), endTime = LocalDateTime.now()),
            spaces = 3,
            availability = ScheduleAvailability.AVAILABLE
        )

        val expectedEvent = SessionAvailabilityEvent(
            location = HiveLocation.POCO,
            sessionDate = LocalDate.now(),
            availableSessions = listOf(SessionAvailabilityEntry(
                spaces = 3,
                period = SessionAvailabilityPeriod(
                    startTime = scheduleAvailabilityEvent.sessionPeriod.startTime,
                    endTime = scheduleAvailabilityEvent.sessionPeriod.endTime
                )
            ))
        )

        whenever(runBlocking { hiveClient.getBookingAvailability(LocalDate.now(), HiveLocation.POCO) }).thenReturn(listOf(scheduleAvailabilityEvent))
        sessionSchedulePollerHandler.handleRequest(hiveClient)
        verify(snsClient).publish(PublishRequest.builder()
            .topicArn(sessionTopicArn)
            .message(Json.encodeToString(expectedEvent))
            .build())
    }

}