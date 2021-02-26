package com.francisbailey.hive.sessionagent.store

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped

@DynamoDBTable(tableName = "SMSAllowListItem")
data class SMSAllowListItem(
    @DynamoDBHashKey
    var phoneNumber: String = "",
    @DynamoDBAttribute
    @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.BOOL)
    var allowed: Boolean = false
)