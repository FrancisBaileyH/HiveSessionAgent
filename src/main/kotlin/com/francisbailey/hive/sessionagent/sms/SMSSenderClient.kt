package com.francisbailey.hive.sessionagent.sms

import java.lang.Exception
import software.amazon.awssdk.services.pinpoint.model.AddressConfiguration
import software.amazon.awssdk.services.pinpoint.model.ChannelType
import software.amazon.awssdk.services.pinpoint.model.DeliveryStatus
import software.amazon.awssdk.services.pinpoint.model.DirectMessageConfiguration
import software.amazon.awssdk.services.pinpoint.model.MessageRequest
import software.amazon.awssdk.services.pinpoint.model.MessageType
import software.amazon.awssdk.services.pinpoint.model.SMSMessage
import software.amazon.awssdk.services.pinpoint.model.SendMessagesRequest

interface SMSSenderClient {
    fun sendMessage(message: String, phoneNumber: String)
}

class SMSSendFailure(message: String): Exception(message)


class PinpointSMSSenderClient(
    private val pinpointClientConfig: PinpointClientConfig
): SMSSenderClient {

    private val client = pinpointClientConfig.buildClient()

    override fun sendMessage(message: String, phoneNumber: String) {
        val response = client.sendMessages(
            SendMessagesRequest.builder()
            .applicationId(pinpointClientConfig.pinpointAppId)
            .messageRequest(
                MessageRequest.builder()
                .addresses(mapOf(
                    message to AddressConfiguration.builder()
                        .channelType(ChannelType.SMS)
                        .build()
                ))
                .messageConfiguration(
                    DirectMessageConfiguration.builder()
                    .smsMessage(
                        SMSMessage.builder()
                        .body(message)
                        .messageType(MessageType.TRANSACTIONAL)
                        .originationNumber(pinpointClientConfig.pinpointOriginNumber)
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .build()
        ).messageResponse()

        val status = response.result()[phoneNumber]?.deliveryStatus()

        if (status != DeliveryStatus.SUCCESSFUL) {
            throw SMSSendFailure("Failed to send SMS to: $phoneNumber. Reason: $status")
        }
    }


}