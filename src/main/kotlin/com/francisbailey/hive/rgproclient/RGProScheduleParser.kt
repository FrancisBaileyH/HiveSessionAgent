package com.francisbailey.hive.rgproclient

import com.francisbailey.hive.common.ScheduleAvailability
import com.francisbailey.hive.common.ScheduleEntry
import com.francisbailey.hive.common.SessionPeriod
import java.lang.Exception
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.jsoup.Jsoup


interface RGProScheduleParser {
    fun parse(text: String, scheduleDate: LocalDate): List<ScheduleEntry>
}

class RGProScheduleParseException(override val cause: Throwable?, override val message: String?): RuntimeException()

/**
 * <table id='offering-page-select-events-table'">
 *     <tr>
 *         <td class='offering-page-schedule-list-time-column'>Sun, February 14, 11 AM to  1 PM</td>
 *         <td><strong>Availability</strong><br><div class='offering-page-event-is-full'>Full.&nbsp;Please make a different selection.</div></td>
 *         <td></td>
 *         <td></td>
 *     </tr>
 *     <tr>
 *         <td class='offering-page-schedule-list-time-column'>Sun, February 14, 12 PM to  2 PM</td>
 *         <td><strong>Availability</strong><br><div class='offering-page-event-is-full'>Full.&nbsp;Please make a different selection.</div></td>
 *         <td></td>
 *         <td></td>
 *     </tr>
 * </table>
 */
class DefaultRGProScheduleParser: RGProScheduleParser {

    override fun parse(text: String, scheduleDate: LocalDate): List<ScheduleEntry> = try {
        val document = Jsoup.parse(text)

        val htmlScheduleTable = document.select("table")[0]
        val rows = htmlScheduleTable.select("tr")

        rows.map { row ->
            val columns = row.select("td")

            val timeSlot = columns[0].text()
            val availabilityString = columns[1].text()
            val sessionPeriod = parseTimePeriod(timeSlot)
            val cantBeBookedYet = columns[3].text().contains("NOT AVAILABLE YET")

            val availability = when {
                cantBeBookedYet -> ScheduleAvailability.NOT_AVAILABLE_YET
                availabilityString.contains("Availability Full") -> ScheduleAvailability.FULL
                availabilityString.run { contains("Availability Available") || contains("space") } -> ScheduleAvailability.AVAILABLE
                else -> ScheduleAvailability.UNKNOWN
            }

            val totalSpaces = if (availabilityString.contains("space")) {
                availabilityString.split(" ")[1].toLong()
            } else {
                null
            }

            ScheduleEntry(
                sessionPeriod = SessionPeriod(
                    startTime = LocalDateTime.of(scheduleDate, sessionPeriod.first),
                    endTime = LocalDateTime.of(scheduleDate, sessionPeriod.second)
                ),
                availability = availability,
                spaces = totalSpaces
            )
        }
    } catch (e: Exception) {
        throw RGProScheduleParseException(e.cause, "Failed to parse value: $text")
    }

    /**
     * Parse time values like:
     * Sun, February 14, 11:45 AM to 1:15 PM
     * Sun, February 14, 3 PM to 5 PM
     */
    private fun parseTimePeriod(timeValue: String): Pair<LocalTime, LocalTime> {
        val timeComponent = timeValue.split(",").last()
        val periods = timeComponent.split("to")
        val startTime = parseHour(periods.first().trim())
        val endTime = parseHour(periods.last().trim())

        return startTime to endTime
    }

    /**
     * Parse strings such as:
     * 11 AM
     * 1 PM
     * 11:15 AM
     */
    private fun parseHour(hourValue: String): LocalTime {
        val hourValueComponents = hourValue.split(" ")
        val hourAndMinutes = hourValueComponents.first().split(":")
        val twelveHourClockIndicator = hourValueComponents.last()

        val hour = hourAndMinutes.first().toInt()
        val minute = hourAndMinutes.elementAtOrElse(1) { "0" }.toInt()

        val hourToTwentyFour = when {
            twelveHourClockIndicator.contains("PM") -> when (hour) {
                12 -> hour
                else -> hour + 12
            }
            else -> when(hour) {
                12 -> 0
                else -> hour
            }
        }

        return LocalTime.of(hourToTwentyFour, minute)
    }

}