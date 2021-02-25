package com.francisbailey.hive.sessionagent.sms

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.pinpoint.PinpointClient

class PinpointClientConfig {
    val pinpointAppId = System.getenv("SESSION_AGENT_PINPOINT_APP_ID")

    val pinpointOriginNumber = System.getenv("SESSION_AGENT_PINPOINT_DEDICATED_NUMBER")

    val pinpointRegion = Region.of(System.getenv("SESSION_AGENT_PINPOINT_REGION"))

    fun buildClient() = PinpointClient.builder()
        .region(pinpointRegion)
        .build()
}