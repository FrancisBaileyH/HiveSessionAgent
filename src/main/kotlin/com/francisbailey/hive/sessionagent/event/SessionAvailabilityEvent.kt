package com.francisbailey.hive.sessionagent.event

import com.francisbailey.hive.common.RGProLocation
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class SessionAvailabilityEvent(
    val location: RGProLocation,
    @Serializable(with = LocalDateSerializer::class)
    val sessionDate: LocalDate,
    val availableSessions: List<SessionAvailabilityEntry>
)

@Serializable
data class SessionAvailabilityEntry(
    val spaces: Long?,
    val period: SessionAvailabilityPeriod
)

@Serializable
data class SessionAvailabilityPeriod(
    @Serializable(with = LocalDateTimeSerializer::class)
    val startTime: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val endTime: LocalDateTime
) {
    fun sessionDuration(): Duration = Duration.between(startTime, endTime)
}

object LocalDateSerializer: KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor get() = buildClassSerialDescriptor("LocalDate")
    override fun deserialize(decoder: Decoder) = LocalDate.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
}

object LocalDateTimeSerializer: KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor get() = buildClassSerialDescriptor("LocalDateTime")
    override fun deserialize(decoder: Decoder) = LocalDateTime.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
}
