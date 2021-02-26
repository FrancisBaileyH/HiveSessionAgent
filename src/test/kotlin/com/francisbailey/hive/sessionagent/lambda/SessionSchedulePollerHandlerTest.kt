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
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

class SessionSchedulePollerHandlerTest {

    private val hiveClient = mock<HiveBookingClient> {
        this.onBlocking { mock.getBookingAvailability(LocalDate.now(), HiveLocation.POCO) }.thenReturn(listOf(ScheduleEntry(
            sessionPeriod = SessionPeriod(startTime = LocalDateTime.now(), endTime = LocalDateTime.now()),
            spaces = 3,
            availability = ScheduleAvailability.AVAILABLE
        )))
    }

    private val snsClient = mock<SnsClient>()

    private val sessionTopicArn = "arn:aws:sns:us-east-2:123412341234:SessionScheduleAvailability"

    private val sessionSchedulePollerHandler = SessionSchedulePollerHandler(snsClient, sessionTopicArn)

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

        whenever(hiveClient.getBookingAvailability(LocalDate.now(), HiveLocation.POCO)).thenReturn(listOf(scheduleAvailabilityEvent))
        sessionSchedulePollerHandler.handleRequest(hiveClient)
        verify(snsClient).publish(PublishRequest.builder()
            .topicArn(sessionTopicArn)
            .message(Json.encodeToString(expectedEvent))
            .build())
    }

}