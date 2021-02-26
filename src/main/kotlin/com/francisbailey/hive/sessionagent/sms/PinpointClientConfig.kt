package com.francisbailey.hive.sessionagent.sms

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.pinpoint.PinpointClient

class PinpointClientConfig {
    val pinpointAppId: String = System.getenv("SESSION_AGENT_PINPOINT_APP_ID")

    val pinpointOriginNumber: String = System.getenv("SESSION_AGENT_PINPOINT_DEDICATED_NUMBER")

    val pinpointKeyword: String = System.getenv("SESSION_AGENT_PINPOINT_KEYWORD")

    val pinpointRegion: Region = Region.of(System.getenv("SESSION_AGENT_PINPOINT_REGION"))

    fun buildClient(): PinpointClient = PinpointClient.builder()
        .region(pinpointRegion)
        .build()
}