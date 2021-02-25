package com.francisbailey.hive.common

import java.time.Duration
import java.time.LocalDateTime


data class ScheduleEntry(
    val sessionPeriod: SessionPeriod,
    val availability: ScheduleAvailability,
    val spaces: Long?
)

enum class ScheduleAvailability {
    AVAILABLE,
    FULL,
    UNKNOWN,
    NOT_AVAILABLE_YET
}

data class SessionPeriod(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
) {
    fun sessionDuration() = Duration.between(startTime, endTime)
}