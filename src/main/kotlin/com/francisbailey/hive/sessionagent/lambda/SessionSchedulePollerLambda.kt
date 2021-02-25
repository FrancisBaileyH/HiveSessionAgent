package com.francisbailey.hive.sessionagent.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.francisbailey.hive.common.HiveBookingClient
import com.francisbailey.hive.common.HiveLocation
import com.francisbailey.hive.common.ScheduleAvailability
import com.francisbailey.hive.common.ScheduleEntry
import com.francisbailey.hive.rgproclient.DefaultRGProScheduleParser
import com.francisbailey.hive.rgproclient.RGProHiveClient
import com.francisbailey.hive.rgproclient.RGProHiveClientConfig
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEvent
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityPeriod
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEntry
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest


private val log = KotlinLogging.logger {}


class SessionSchedulePollerLambda: RequestHandler<Unit, Unit> {

    private val sessionTopicArn: String = System.getenv("SESSION_AGENT_TOPIC_ARN")

    private val snsClient = SnsClient.builder().build()

    private val rgProHiveClientConfig = RGProHiveClientConfig()

    private val sessionSchedulePollerHandler = SessionSchedulePollerHandler(snsClient, sessionTopicArn)

    override fun handleRequest(input: Unit?, context: Context) {
        RGProHiveClient(rgProHiveClientConfig, DefaultRGProScheduleParser()).use {
            sessionSchedulePollerHandler.handleRequest(it)
        }
    }
}

internal class SessionSchedulePollerHandler(
    private val sns: SnsClient,
    private val sessionTopicArn: String
) {

    fun handleRequest(hiveBookingClient: HiveBookingClient) {
        val currentDate = LocalDate.now()
        val dateRange = (0..6L).map { currentDate.plusDays(it) }

        HiveLocation.values().forEach { location ->
            dateRange.forEach { date ->
                log.info { "Fetching schedule data for: ${location.fullName} on: $date" }
                val scheduleEntries = hiveBookingClient.getBookingAvailability(date, location).filter {
                    it.availability == ScheduleAvailability.AVAILABLE
                }

                if (scheduleEntries.isNotEmpty()) {
                    log.info { "Found availability" }
                    publishEvent(scheduleEntries, date, location)
                }
            }
        }
    }

    private fun publishEvent(scheduleEntries: List<ScheduleEntry>, date: LocalDate, location: HiveLocation) {
        val sessionAvailabilityEvent = SessionAvailabilityEvent(
            location = location,
            sessionDate = date,
            availableSessions = scheduleEntries.map {
                SessionAvailabilityEntry(
                    spaces = it.spaces,
                    period = SessionAvailabilityPeriod(
                        startTime = it.sessionPeriod.startTime,
                        endTime = it.sessionPeriod.endTime
                    )
                )
            }
        )
        sns.publish(PublishRequest.builder()
            .message(Json.encodeToString(sessionAvailabilityEvent))
            .topicArn(sessionTopicArn)
            .build()
        )
        log.info { "Published event: $sessionAvailabilityEvent" }
    }
}


