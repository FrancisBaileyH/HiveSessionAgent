package com.francisbailey.hive.sessionagent.event

import kotlinx.serialization.Serializable

@Serializable
data class PinpointSMSEvent(
    val originationNumber: String,
    val messageBody: String,
    val inboundMessageId: String,
    val previousPublishedMessageId: String?,
    val messageKeyword: String,
    val destinationNumber: String
)