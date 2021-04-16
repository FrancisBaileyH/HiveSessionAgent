package com.francisbailey.hive.sessionagent.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.francisbailey.hive.common.CloudWatchOperationTimer
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
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import mu.KotlinLogging
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest


private val log = KotlinLogging.logger {}


class SessionSchedulePollerLambda: RequestHandler<Unit, Unit> {

    private val sessionTopicArn: String = System.getenv("SESSION_AGENT_TOPIC_ARN")

    private val snsClient = SnsClient.builder().build()

    private val cloudWatchClient = CloudWatchClient.builder().build()

    private val rgProHiveClientConfig = RGProHiveClientConfig()

    private val sessionSchedulePollerHandler = SessionSchedulePollerHandler(snsClient, sessionTopicArn, cloudWatchClient)

    override fun handleRequest(input: Unit?, context: Context) {
        RGProHiveClient(rgProHiveClientConfig, DefaultRGProScheduleParser()).use {
            sessionSchedulePollerHandler.handleRequest(it)
        }
    }
}

internal class SessionSchedulePollerHandler(
    private val sns: SnsClient,
    private val sessionTopicArn: String,
    private val cloudWatchClient: CloudWatchClient
) {
    private val maxConcurrentRequestLimiter = Semaphore(MAX_CONCURRENT_REQUESTS)

    fun handleRequest(hiveBookingClient: HiveBookingClient) = runBlocking {
        val lookAheadRange = (0..6L)

        HiveLocation.values().forEach { location ->
            val startDate = LocalDate.now(Clock.system(location.zoneId))
            lookAheadRange.forEach { lookAheadDay ->
                launch {
                    maxConcurrentRequestLimiter.withPermit {
                        val date = startDate.plusDays(lookAheadDay)
                        fetchAndPublishScheduleInfo(hiveBookingClient, date, location)
                    }
                }
            }
        }
    }

    private suspend fun fetchAndPublishScheduleInfo(hiveBookingClient: HiveBookingClient, date: LocalDate, location: HiveLocation) {
        try {
            log.info { "Fetching schedule data for: ${location.fullName} on: $date" }

            val scheduleEntries = CloudWatchOperationTimer(cloudWatchClient, service = "HiveBookingClient", operation = "getBookingAvailability").use {
                hiveBookingClient.getBookingAvailability(date, location).filter {
                    it.availability == ScheduleAvailability.AVAILABLE
                }
            }
            log.info { "Successfully fetched schedule data for: ${location.fullName} on: $date" }

            if (scheduleEntries.isNotEmpty()) {
                log.info { "Found availability" }
                publishEvent(scheduleEntries, date, location)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch and publish schedule info for: $date at $location"}
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

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 10
    }
}


