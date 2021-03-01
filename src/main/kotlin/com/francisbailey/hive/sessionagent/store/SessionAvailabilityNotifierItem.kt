package com.francisbailey.hive.sessionagent.store

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped

@DynamoDBTable(tableName = "HiveSessionAvailabilityNotifierItem")
data class SessionAvailabilityNotifierItem(
    @DynamoDBHashKey
    var sessionDateAndLocation: String = "",
    @DynamoDBRangeKey
    var phoneNumber: String = "",
    @DynamoDBAttribute
    var sessionStartDateTime: String = "",
    @DynamoDBAttribute
    var sessionEndDateTime: String = "",
    @DynamoDBAttribute
    var requestTimestamp: Long = 0,
    @DynamoDBAttribute
    @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.BOOL)
    var hasBeenNotified: Boolean = false,
    @DynamoDBAttribute
    var minimumSessionMinutes: Long = 90,
    @DynamoDBAttribute
    var ttl: Long = 0
)

