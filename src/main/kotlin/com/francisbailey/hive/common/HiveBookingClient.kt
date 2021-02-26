package com.francisbailey.hive.common

import java.time.LocalDate
import java.time.ZoneId

interface HiveBookingClient: AutoCloseable {
    fun getBookingAvailability(date: LocalDate, location: HiveLocation): List<ScheduleEntry>
}

enum class HiveLocation(
    val fullName: String,
    val zoneId: ZoneId = ZoneId.of("America/Vancouver")
) {
    POCO("PoCo"),
    SURREY("Surrey"),
    VANCOUVER("Vancouver"),
    NORTH_VANCOUVER("North Vancouver")
}