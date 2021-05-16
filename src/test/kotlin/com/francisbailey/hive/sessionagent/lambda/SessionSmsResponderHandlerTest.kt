package com.francisbailey.hive.sessionagent.lambda

import com.francisbailey.hive.common.RGProLocation
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
        on(mock.isBanned(any())).thenReturn(false)
    }

    private val sessionAvailabilityNotifierDAO = mock<SessionAvailabilityNotifierDAO>()

    private val smsSenderClient = mock<SMSSenderClient>()

    private val smsResponderBotHandler = SessionSmsResponderHandler(smsSenderClient, sessionAvailabilityNotifierDAO, smsAllowListDAO, false)

    private val smsResponderBotHandlerWithAllowList = SessionSmsResponderHandler(smsSenderClient, sessionAvailabilityNotifierDAO, smsAllowListDAO, true)

    private val phoneNumber = "+1231231234"

    @Test
    fun `creates new subscription when correctly formatted create alert text is received`() {
        val date = "2021-02-11"
        val message = "CreateAlert from Hive-PoCo on $date for 11:00AM to 1:00PM"
        val expectedStartTime = LocalDateTime.parse("${date}T11:00")
        val expectedEndTime = LocalDateTime.parse("${date}T13:00")
        smsResponderBotHandler.handleRequest(phoneNumber, message)

        verify(smsSenderClient).sendMessage("Successfully registered alert for: Hive-Poco on: 2021-02-11 from 11:00AM to 1:00PM", phoneNumber)
        verify(sessionAvailabilityNotifierDAO).create(
            location = RGProLocation.HIVE_POCO,
            sessionStart = expectedStartTime,
            sessionEnd = expectedEndTime,
            phoneNumber = phoneNumber
        )
    }

    @Test
    fun `sends failure SMS when CreateAlert command can not be parsed`() {
        val message = "CreateAlert for Hive-Poco bad format"
        smsResponderBotHandler.handleRequest(phoneNumber, message)
        verify(smsSenderClient).sendMessage("Failed to register alert. Please try again", phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `sends unknown command SMS when unknown command is received`() {
        val message = "Not a command"
        smsResponderBotHandler.handleRequest(phoneNumber, message)
        verify(smsSenderClient).sendMessage("Unknown command. Available commands: CreateAlert, ListLocations. Example CreateAlert command: CreateAlert for Hive-Poco on 2021-04-17 from 11:00AM to 1:00PM", phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `sends unregistered message when number not in allow list and adds number in banned state`() {
        whenever(smsAllowListDAO.isAllowed(phoneNumber)).thenReturn(false)
        smsResponderBotHandlerWithAllowList.handleRequest(phoneNumber, "CreateAlert")

        verify(smsSenderClient).sendMessage("Sorry you are not registered to use this service. No further responses will be sent.", phoneNumber)
        verify(smsAllowListDAO).ban(phoneNumber)
        verify(smsAllowListDAO).isBanned(phoneNumber)
        verify(smsAllowListDAO).isAllowed(phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `does not resend unregistered message if number is already banned`() {
        whenever(smsAllowListDAO.isBanned(phoneNumber)).thenReturn(true)
        smsResponderBotHandlerWithAllowList.handleRequest(phoneNumber, "CreateAlert")

        verify(smsAllowListDAO).isBanned(phoneNumber)
        verifyZeroInteractions(smsSenderClient)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `passes through when number is allow listed`() {
        val date = "2021-02-11"
        val message = "CreateAlert for Hive-PoCo on $date for 11:00AM to 1:00PM"
        val expectedStartTime = LocalDateTime.parse("${date}T11:00")
        val expectedEndTime = LocalDateTime.parse("${date}T13:00")
        smsResponderBotHandlerWithAllowList.handleRequest(phoneNumber, message)

        verify(smsSenderClient).sendMessage("Successfully registered alert for: Hive-Poco on: 2021-02-11 from 11:00AM to 1:00PM", phoneNumber)
        verify(sessionAvailabilityNotifierDAO).create(
            location = RGProLocation.HIVE_POCO,
            sessionStart = expectedStartTime,
            sessionEnd = expectedEndTime,
            phoneNumber = phoneNumber
        )
    }

    @Test
    fun `sends available locations when unknown location is sent`() {
        val message = "CreateAlert for Bad-Location on 2021-02-011 for 11:00AM to 1:00PM"

        smsResponderBotHandler.handleRequest(phoneNumber, message)

        verify(smsSenderClient).sendMessage("Invalid location. Supported locations: Hive-Poco, Hive-Surrey, Hive-Vancouver, Hive-North-Vancouver, Base5-North-Vancouver, Base5-Coquitlam", phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }

    @Test
    fun `lists available locations`() {
        val message = "ListLocations"
        smsResponderBotHandler.handleRequest(phoneNumber, message)
        verify(smsSenderClient).sendMessage("Supported locations: Hive-Poco, Hive-Surrey, Hive-Vancouver, Hive-North-Vancouver, Base5-North-Vancouver, Base5-Coquitlam", phoneNumber)
        verifyZeroInteractions(sessionAvailabilityNotifierDAO)
    }
}