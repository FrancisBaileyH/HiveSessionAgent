package com.francisbailey.hive.sessionagent.lambda

import com.francisbailey.hive.common.RGProLocation
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEntry
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEvent
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityPeriod
import com.francisbailey.hive.sessionagent.sms.SMSSenderClient
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierDAO
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

class SessionSubscriptionNotifierHandlerTest {

    private val sessionAvailabilityNotifierDAO = mock<SessionAvailabilityNotifierDAO>()

    private val smsSenderClient = mock<SMSSenderClient>()

    private val sessionSubscriptionNotifierHandler = SessionSubscriptionNotifierHandler(sessionAvailabilityNotifierDAO, smsSenderClient)

    private val defaultEvent = SessionAvailabilityEvent(
        sessionDate = LocalDate.parse("2021-02-01"),
        location = RGProLocation.HIVE_POCO,
        availableSessions = listOf(
            SessionAvailabilityEntry(
                spaces = 3,
                period = SessionAvailabilityPeriod(
                    startTime = LocalDateTime.parse("2021-02-01T10:00:00.000"),
                    endTime = LocalDateTime.parse("2021-02-01T12:00:00.000")
                )
            )
        )
    )

    @Test
    fun `publishes event for any matching session notification item`() {
        val item = SessionAvailabilityNotifierItem(
            sessionDateAndLocation = "TEST",
            sessionEndDateTime = LocalDateTime.parse("2021-02-01T12:00:00.000").toString(),
            sessionStartDateTime = LocalDateTime.parse("2021-02-01T10:00:00.000").toString(),
            phoneNumber = "+1231231234",
            requestTimestamp = 1,
            hasBeenNotified = false
        )

        whenever(sessionAvailabilityNotifierDAO.getNotificationSubscriptions(any(), any())).thenReturn(listOf(item))
        sessionSubscriptionNotifierHandler.handleRequest(defaultEvent)
        verify(smsSenderClient).sendMessage("An opening at the Hive-Poco location has just appeared for: 2021-02-01. Available session(s): 10:00AM - 12:00PM", item.phoneNumber)
        verify(sessionAvailabilityNotifierDAO).save(eq(item.apply { this.hasBeenNotified = true }))
    }

    @Test
    fun `does not publish if no matching sessions are found`() {
        val item = SessionAvailabilityNotifierItem(
            sessionDateAndLocation = "TEST",
            sessionEndDateTime = LocalDateTime.parse("2021-02-01T10:00:00.000").toString(),
            sessionStartDateTime = LocalDateTime.parse("2021-02-01T08:00:00.000").toString(),
            phoneNumber = "+1231231234",
            requestTimestamp = 1,
            hasBeenNotified = false
        )

        whenever(sessionAvailabilityNotifierDAO.getNotificationSubscriptions(any(), any())).thenReturn(listOf(item))
        sessionSubscriptionNotifierHandler.handleRequest(defaultEvent)
        verifyZeroInteractions(smsSenderClient)
    }

    @Test
    fun `does not publish if session duration is too short`() {
        val item = SessionAvailabilityNotifierItem(
            sessionDateAndLocation = "TEST",
            sessionEndDateTime = LocalDateTime.parse("2021-02-01T10:00:00.000").toString(),
            sessionStartDateTime = LocalDateTime.parse("2021-02-01T08:00:00.000").toString(),
            phoneNumber = "+1231231234",
            requestTimestamp = 1,
            hasBeenNotified = false,
            minimumSessionMinutes = 120
        )

        val shortEvent = SessionAvailabilityEvent(
            sessionDate = LocalDate.parse("2021-02-01"),
            location = RGProLocation.HIVE_POCO,
            availableSessions = listOf(
                SessionAvailabilityEntry(
                    spaces = 3,
                    period = SessionAvailabilityPeriod(
                        startTime = LocalDateTime.parse("2021-02-01T10:00:00.000"),
                        endTime = LocalDateTime.parse("2021-02-01T11:59:00.000")
                    )
                )
            )
        )
        whenever(sessionAvailabilityNotifierDAO.getNotificationSubscriptions(any(), any())).thenReturn(listOf(item))
        sessionSubscriptionNotifierHandler.handleRequest(shortEvent)
        verifyZeroInteractions(smsSenderClient)
    }

    @Test
    fun `does not publish event if subscriber has been notified`() {
        val item = SessionAvailabilityNotifierItem(
            sessionDateAndLocation = "TEST",
            sessionEndDateTime = LocalDateTime.parse("2021-02-01T12:00:00.000").toString(),
            sessionStartDateTime = LocalDateTime.parse("2021-02-01T10:00:00.000").toString(),
            phoneNumber = "+1231231234",
            requestTimestamp = 1,
            hasBeenNotified = true
        )

        whenever(sessionAvailabilityNotifierDAO.getNotificationSubscriptions(any(), any())).thenReturn(listOf(item))
        sessionSubscriptionNotifierHandler.handleRequest(defaultEvent)
        verifyZeroInteractions(smsSenderClient)
    }
}