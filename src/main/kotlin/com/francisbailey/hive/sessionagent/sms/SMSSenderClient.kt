package com.francisbailey.hive.sessionagent.sms

import java.lang.Exception
import mu.KotlinLogging
import software.amazon.awssdk.services.pinpoint.model.AddressConfiguration
import software.amazon.awssdk.services.pinpoint.model.ChannelType
import software.amazon.awssdk.services.pinpoint.model.DeliveryStatus
import software.amazon.awssdk.services.pinpoint.model.DirectMessageConfiguration
import software.amazon.awssdk.services.pinpoint.model.MessageRequest
import software.amazon.awssdk.services.pinpoint.model.MessageType
import software.amazon.awssdk.services.pinpoint.model.SMSMessage
import software.amazon.awssdk.services.pinpoint.model.SendMessagesRequest

private val log = KotlinLogging.logger {}

interface SMSSenderClient {
    fun sendMessage(message: String, phoneNumber: String)
}

class SMSSendFailure(message: String): Exception(message)


class PinpointSMSSenderClient(
    private val pinpointClientConfig: PinpointClientConfig
): SMSSenderClient {

    private val client = pinpointClientConfig.buildClient()

    override fun sendMessage(message: String, phoneNumber: String) {
        val request = SendMessagesRequest.builder()
            .applicationId(pinpointClientConfig.pinpointAppId)
            .messageRequest(MessageRequest.builder()
                .addresses(mapOf(
                    phoneNumber to AddressConfiguration.builder()
                        .channelType(ChannelType.SMS)
                        .build()
                ))
                .messageConfiguration(DirectMessageConfiguration.builder()
                    .smsMessage(SMSMessage.builder()
                        .body(message)
                        .messageType(MessageType.TRANSACTIONAL)
                        .originationNumber(pinpointClientConfig.pinpointOriginNumber)
                        .keyword(pinpointClientConfig.pinpointKeyword)
                        .build()
                    ).build()
                ).build()
            ).build()
        log.debug { "Request is: $request" }
        val response = client.sendMessages(request).messageResponse()
        log.info { response }

        val status = response.result()[phoneNumber]?.deliveryStatus()

        if (status != DeliveryStatus.SUCCESSFUL) {
            throw SMSSendFailure("Failed to send SMS to: $phoneNumber. Reason: $status")
        }
    }

}