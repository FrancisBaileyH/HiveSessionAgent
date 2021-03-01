package com.francisbailey.hive.sessionagent.lambda

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.francisbailey.hive.common.HiveLocation
import com.francisbailey.hive.sessionagent.event.PinpointSMSEvent
import com.francisbailey.hive.sessionagent.sms.PinpointClientConfig
import com.francisbailey.hive.sessionagent.sms.PinpointSMSSenderClient
import com.francisbailey.hive.sessionagent.sms.SMSSenderClient
import com.francisbailey.hive.sessionagent.sms.SmsDateTimeFormatter
import com.francisbailey.hive.sessionagent.store.SMSAllowListDAO
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierDAO
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


class SessionSmsResponderLambda: RequestHandler<SNSEvent, Unit> {

    private val allowListEnabled: Boolean = System.getenv("SESSION_AGENT_ALLOW_LIST_ENABLED")?.toBoolean() ?: true

    private val pinpointClientConfig = PinpointClientConfig()

    private val smsSenderClient = PinpointSMSSenderClient(pinpointClientConfig)

    private val dynamoDBClient = AmazonDynamoDBClient.builder().build()

    private val sessionAvailabilityNotifierDAO = SessionAvailabilityNotifierDAO(dynamoDBClient)

    private val smsAllowListDAO = SMSAllowListDAO(dynamoDBClient)

    private val smsChatBotHandler = SessionSmsResponderHandler(smsSenderClient, sessionAvailabilityNotifierDAO, smsAllowListDAO, allowListEnabled)

    override fun handleRequest(input: SNSEvent, context: Context) {
        val snsMessage = input.records.first().sns.message
        val smsEvent = Json.decodeFromString<PinpointSMSEvent>(snsMessage)

        smsChatBotHandler.handleRequest(smsEvent.originationNumber, smsEvent.messageBody)
    }
}


class SessionSmsResponderHandler(
    private val smsSenderClient: SMSSenderClient,
    private val sessionAvailabilityNotifierDAO: SessionAvailabilityNotifierDAO,
    private val smsAllowListDAO: SMSAllowListDAO,
    private val allowListEnabled: Boolean
) {
    fun handleRequest(origin: String, smsMessageBody: String) {
        log.info { "Received message: $smsMessageBody from: $origin" }

        if (allowListEnabled && !smsAllowListDAO.isAllowed(origin)) {
            log.warn { "$origin is not enabled for this resource" }
            smsSenderClient.sendMessage("Sorry you are not registered to use this service. Cannot complete request.", origin)
        }

        when {
            smsMessageBody.startsWith("CreateAlert") -> handleCreateAlert(origin, smsMessageBody)
            else -> smsSenderClient.sendMessage("Unknown command. $HELP_MESSAGE", origin)
        }
    }

    // CreateAlert for <Location> on <date> from <time> to <time>
    private fun handleCreateAlert(origin: String, message: String) = try {
        val messageComponents = message.split(" ")

        val location = HiveLocation.valueOf(messageComponents[2].toUpperCase())
        val date = LocalDate.parse(messageComponents[4])
        val startTime = SmsDateTimeFormatter.parseTime(messageComponents[6].toUpperCase())
        val endTime = SmsDateTimeFormatter.parseTime(messageComponents[8].toUpperCase())

        val startDateTime = LocalDateTime.of(date, startTime)
        val endDateTime = LocalDateTime.of(date, endTime)

        sessionAvailabilityNotifierDAO.create(location, startDateTime, endDateTime, origin)
        smsSenderClient.sendMessage("Successfully registered alert for: ${location.fullName} on: $date from ${SmsDateTimeFormatter.formatTime(startTime)} to ${SmsDateTimeFormatter.formatTime(endTime)}", origin)
    } catch (e: Exception) {
        log.error(e) { "Failed to process message" }
        smsSenderClient.sendMessage("Failed to register alert. Please try again", origin)
    }

    companion object {
        private val HELP_MESSAGE = "To create an alert type: CreateAlert for <Location> on <Date> from <Start Time> to <End Time>"
    }
}