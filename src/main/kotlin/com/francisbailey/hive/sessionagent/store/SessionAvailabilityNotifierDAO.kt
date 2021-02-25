package com.francisbailey.hive.sessionagent.store

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.francisbailey.hive.common.HiveLocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

open class SessionAvailabilityNotifierDAO(
    ddbClient: AmazonDynamoDB
) {
    private val mapper = DynamoDBMapper(ddbClient)

    fun getNotificationSubscriptions(date: LocalDate, location: HiveLocation): List<SessionAvailabilityNotifierItem> {
        return mapper.query(SessionAvailabilityNotifierItem::class.java, DynamoDBQueryExpression<SessionAvailabilityNotifierItem>()
            .withKeyConditionExpression("sessionDateAndLocation=:val1")
            .withExpressionAttributeValues(mapOf(
                ":val1" to AttributeValue().withS(buildHashKey(date, location))
            ))
        )
    }

    fun create(location: HiveLocation, sessionStart: LocalDateTime, sessionEnd: LocalDateTime, phoneNumber: String) {
        val item = SessionAvailabilityNotifierItem(
            sessionDateAndLocation = buildHashKey(sessionStart.toLocalDate(), location),
            phoneNumber = phoneNumber,
            sessionStartDateTime = sessionStart.toString(),
            sessionEndDateTime = sessionEnd.toString(),
            requestTimestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
            hasBeenNotified = false
        )

        save(item)
    }

    fun save(item: SessionAvailabilityNotifierItem) = mapper.save(item)

    private fun buildHashKey(date: LocalDate, location: HiveLocation) = "$date-${location.name}"
}