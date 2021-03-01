package com.francisbailey.hive.sessionagent.sms

import java.time.LocalTime

class SmsDateTimeFormatter {

    companion object {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mma")
        fun formatTime(time: LocalTime) = time.format(formatter)
        fun parseTime(time: String) = LocalTime.parse(time, formatter)
    }
}

