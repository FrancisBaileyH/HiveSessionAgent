package com.francisbailey.hive.common

import java.time.LocalDate

interface HiveBookingClient: AutoCloseable {
    fun getBookingAvailability(date: LocalDate, location: HiveLocation): List<ScheduleEntry>
}


enum class HiveLocation(val fullName: String) {
    POCO("PoCo"),
    SURREY("Surrey"),
    VANCOUVER("Vancouver"),
    NORTH_VANCOUVER("North Vancouver")
}