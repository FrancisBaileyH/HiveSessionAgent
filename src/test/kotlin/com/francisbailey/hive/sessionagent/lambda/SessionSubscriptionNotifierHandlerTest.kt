package com.francisbailey.hive.sessionagent.lambda

import com.francisbailey.hive.common.HiveLocation
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEntry
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityEvent
import com.francisbailey.hive.sessionagent.event.SessionAvailabilityPeriod
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierDAO
import com.francisbailey.hive.sessionagent.store.SessionAvailabilityNotifierItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

class SessionSubscriptionNotifierHandlerTest {

    private val sessionAvailabilityNotifierDAO = mock<SessionAvailabilityNotifierDAO>()

    private val snsClient = mock<SnsClient>()

    private val sessionSubscriptionNotifierHandler = SessionSubscriptionNotifierHandler(sessionAvailabilityNotifierDAO, snsClient)

    private val defaultEvent = SessionAvailabilityEvent(
        sessionDate = LocalDate.parse("2021-02-01"),
        location = HiveLocation.POCO,
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
            phoneNumber = "1231231234",
            requestTimestamp = 1,
            hasBeenNotified = false
        )

        whenever(sessionAvailabilityNotifierDAO.getNotificationSubscriptions(any<LocalDate>(), any())).thenReturn(listOf(item))

        println(defaultEvent)
        sessionSubscriptionNotifierHandler.handleRequest(defaultEvent)

        verify(snsClient).publish(eq(PublishRequest.builder()
            .phoneNumber(item.phoneNumber)
            .message("")
            .build()
        ))
    }

    @Test
    fun `does not publish if no matching sessions are found`() {

    }

    @Test
    fun `does not publish event if subscriber has been notified`() {

    }
}