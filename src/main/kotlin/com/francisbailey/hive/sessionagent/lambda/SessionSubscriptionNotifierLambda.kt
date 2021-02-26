package com.francisbailey.hive.sessionagent.lambda

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEvent
import com.francisbailey.hive.sessionagent.sms.PinpointClientConfig
import com.francisbailey.hive.sessionagent.sms.PinpointSMSSenderClient
import com.francisbailey.hive.sessionagent.sms.SMSSenderClient
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierDAO
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


class SessionSubscriptionNotifierLambda: RequestHandler<SNSEvent, Unit> {

    private val dynamoDBClient = AmazonDynamoDBClient.builder().build()

    private val pinpointClientConfig = PinpointClientConfig()

    private val smsSenderClient = PinpointSMSSenderClient(pinpointClientConfig)

    private val sessionAvailabilityNotifierDAO = SessionAvailabilityNotifierDAO(dynamoDBClient)

    private val sessionSubscriptionCheckerHandler = SessionSubscriptionNotifierHandler(sessionAvailabilityNotifierDAO, smsSenderClient)

    override fun handleRequest(input: SNSEvent, context: Context) {
        log.info { "Processing: ${input.records.size} event(s)" }

        input.records.forEach { payload ->
            val event = Json.decodeFromString<SessionAvailabilityEvent>(payload.sns.message)
            log.debug { "Received the following event: ${payload.sns.message}" }
            sessionSubscriptionCheckerHandler.handleRequest(event)
        }
    }

}

internal class SessionSubscriptionNotifierHandler(
    private val sessionAvailabilityNotifierDAO: SessionAvailabilityNotifierDAO,
    private val smsSenderClient: SMSSenderClient
) {

    fun handleRequest(event: SessionAvailabilityEvent) {
        val subscribers = sessionAvailabilityNotifierDAO.getNotificationSubscriptions(event.sessionDate, event.location)
        log.info { "Found ${subscribers.size} subscriber(s) for ${event.location.fullName} on: ${event.sessionDate}" }

        subscribers
            .filterNot { it.hasBeenNotified }
            .sortedBy { it.requestTimestamp }
            .forEach { subscriber ->
                log.info { "Checking matching sessions for: ${subscriber.phoneNumber}" }

                val matchingSessions = event.availableSessions.filter {
                    it.period.startTime >= LocalDateTime.parse(subscriber.sessionStartDateTime)
                            && it.period.endTime <= LocalDateTime.parse(subscriber.sessionEndDateTime)
                }

                if (matchingSessions.isNotEmpty()) {
                    log.info { "Found openings, sending message to user: ${subscriber.phoneNumber}" }
                    smsSenderClient.sendMessage(
                        message = "An opening at the ${event.location.fullName} location has just appeared for: ${event.sessionDate}. Available session(s): ${matchingSessions.joinToString { "${it.period.startTime.toLocalTime()} - ${it.period.endTime.toLocalTime()}" }}",
                        phoneNumber = subscriber.phoneNumber
                    )

                    subscriber.hasBeenNotified = true
                    sessionAvailabilityNotifierDAO.save(subscriber)
                }
            }
    }
}