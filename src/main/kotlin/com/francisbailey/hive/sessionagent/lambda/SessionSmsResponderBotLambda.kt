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
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierDAO
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging


private val log = KotlinLogging.logger {}


class SmsResponderBotLambda: RequestHandler<SNSEvent, Unit> {

    private val pinpointClientConfig = PinpointClientConfig()

    private val smsSenderClient = PinpointSMSSenderClient(pinpointClientConfig)

    private val dynamoDBClient = AmazonDynamoDBClient.builder().build()

    private val sessionAvailabilityNotifierDAO = SessionAvailabilityNotifierDAO(dynamoDBClient)

    private val smsChatBotHandler = SmsResponderBotHandler(smsSenderClient, sessionAvailabilityNotifierDAO)

    override fun handleRequest(input: SNSEvent, context: Context) {
        val snsMessage = input.records.first().sns.message
        val smsEvent = Json.decodeFromString<PinpointSMSEvent>(snsMessage)

        smsChatBotHandler.handleRequest(smsEvent.originationNumber, smsEvent.messageBody)
    }
}


class SmsResponderBotHandler(
    private val smsSenderClient: SMSSenderClient,
    private val sessionAvailabilityNotifierDAO: SessionAvailabilityNotifierDAO
) {
    fun handleRequest(origin: String, smsMessageBody: String) {
        when {
            smsMessageBody.startsWith("CreateAlert") -> handleCreateAlert(origin, smsMessageBody)
            else -> smsSenderClient.sendMessage("Unknown command. $HELP_MESSAGE", origin)
        }
    }

    private fun handleCreateAlert(origin: String, message: String) = try {
        val messageComponents = message.split(" ")

        val location = HiveLocation.valueOf(messageComponents[2])
        val date = LocalDate.parse(messageComponents[4])
        val startTime = LocalTime.parse(messageComponents[5].toUpperCase(), formatter)
        val endTime = LocalTime.parse(messageComponents[7].toUpperCase(), formatter)

        val startDateTime = LocalDateTime.of(date, startTime)
        val endDateTime = LocalDateTime.of(date, endTime)

        sessionAvailabilityNotifierDAO.create(location, startDateTime, endDateTime, origin)
        smsSenderClient.sendMessage("Successfully registered alert for: ${location.fullName} on: $date from $startTime to $endTime", origin)
    } catch (e: Exception) {
        smsSenderClient.sendMessage("Failed to register alert. Please try again.", origin)
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("hha")
        private val HELP_MESSAGE = "To create an alert type: CreateAlert for <Location> on <Date> from <Start Time> to <End Time>"
    }
}