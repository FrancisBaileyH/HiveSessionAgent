package com.francisbailey.hive.common

import java.lang.Exception
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.ZoneId

interface RGProBookingClient: AutoCloseable {
    suspend fun getBookingAvailability(date: LocalDate, location: RGProLocation): List<ScheduleEntry>
}

class UnknownRGProLocationException(override val message: String): Exception()

enum class RGProLocation(
    val fullName: String,
    val zoneId: ZoneId = ZoneId.of("America/Vancouver"),
    val guid: String
) {
    HIVE_POCO("Hive-Poco", guid = "b405d11ff01346e8bce172d854720c3d"),
    HIVE_SURREY("Hive-Surrey",guid = "b41f7158c38e43f5adb1ee5b003e4bd5"),
    HIVE_VANCOUVER("Hive-Vancouver", guid = "484c1a7ca09145419ef258eeb894c38f"),
    HIVE_NORTH_VANCOUVER("Hive-North-Vancouver", guid = "6fa9139cc3584fc0a5662a5c36d68958"),
    BASE5_NORTH_VANCOUVER("Base5-North-Vancouver", guid = "c00b143f9b9744c1920af1dceeac57d4"),
    BASE5_COQUITLAM("Base5-Coquitlam", guid = "030ad9b4f2c74404af906fbe7d475e7e");

    companion object {
        fun fromString(location: String) = try {
            valueOf(location.replace("-", "_").toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw UnknownRGProLocationException(location)
        }
    }
}