package com.francisbailey.hive.sessionagent.lambda

import com.francisbailey.hive.common.HiveLocation
import com.francisbailey.hive.sessionagent.sms.SMSSenderClient
import com.francisbailey.hive.sessionagent.store.SMSAllowListDAO
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierDAO
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

class SessionSmsResponderHandlerTest {

    private val smsAllowListDAO = mock<SMSAllowListDAO> {
        on(mock.isAllowed(any())).thenReturn(true)
    }

    private val sessionAvailabilityNotifierDAO = mock<SessionAvailabilityNotifierDAO>()

    private val smsSenderClient = mock<SMSSenderClient>()

    private val smsResponderBotHandler = SessionSmsResponderHandler(smsSenderClient, sessionAvailabilityNotifierDAO, smsAllowListDAO, false)

    private val smsResponderBotHandlerWithAllowList = SessionSmsResponderHandler(smsSenderClient, sessionAvailabilityNotifierDAO, smsAllowListDAO, true)

    private val phoneNumber = "+1231231234"

    @Test
    fun `creates new subscription when correctly formatted create alert text is received`() {
        val date = "2021-02-11"
        val message = "CreateAlert from PoCo on $date for 11:00AM to 1:00PM"
        val expectedStartTime = LocalDateTime.parse("${date}T11:00")
        val expectedEndTime = LocalDateTime.parse("${date}T13:00")
        smsResponderBotHandler.handleRequest(phoneNumber, message)

        verify(smsSenderClient).sendMessage("Successfully registered alert for: PoCo on: 2021-02-11 from 11:00AM to 1:00PM", phoneNumber)
        verify(sessionAvailabilityNotifierDAO).create(
            location = HiveLocation.POCO,
            sessionStart = expectedStartTime,
            sessionEnd = expectedEndTime,
            phoneNumber = phoneNumber
        )
    }

    @Test
    fun `sends failure SMS when CreateAlert command can not be parsed`() {
        val message = "CreateAlert bad format"
        smsResponderBotHandler.handleRequest(phoneNumber, message)
        verify(smsSenderClient).sendMessage("Failed to register alert. Please try again", phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `sends unknown command SMS when unknown command is received`() {
        val message = "Not a command"
        smsResponderBotHandler.handleRequest(phoneNumber, message)
        verify(smsSenderClient).sendMessage("Unknown command. To create an alert type: CreateAlert for <Location> on <Date> from <Start Time> to <End Time>", phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `sends unregistered message when number not in allow list`() {
        whenever(smsAllowListDAO.isAllowed(any())).thenReturn(false)
        smsResponderBotHandlerWithAllowList.handleRequest(phoneNumber, "Some Message")

        verify(smsSenderClient).sendMessage("Sorry you are not registered to use this service. Cannot complete request.", phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `passes through when number is allow listed`() {
        val date = "2021-02-11"
        val message = "CreateAlert from PoCo on $date for 11:00AM to 1:00PM"
        val expectedStartTime = LocalDateTime.parse("${date}T11:00")
        val expectedEndTime = LocalDateTime.parse("${date}T13:00")
        smsResponderBotHandlerWithAllowList.handleRequest(phoneNumber, message)

        verify(smsSenderClient).sendMessage("Successfully registered alert for: PoCo on: 2021-02-11 from 11:00AM to 1:00PM", phoneNumber)
        verify(sessionAvailabilityNotifierDAO).create(
            location = HiveLocation.POCO,
            sessionStart = expectedStartTime,
            sessionEnd = expectedEndTime,
            phoneNumber = phoneNumber
        )
    }
}