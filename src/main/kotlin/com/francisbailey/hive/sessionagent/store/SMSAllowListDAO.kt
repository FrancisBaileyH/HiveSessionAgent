package com.francisbailey.hive.sessionagent.store

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper

open class SMSAllowListDAO(
    ddbClient: AmazonDynamoDB
) {
    private val mapper = DynamoDBMapper(ddbClient)

    open fun isAllowed(phoneNumber: String): Boolean {
        val result: SMSAllowListItem? = mapper.load(SMSAllowListItem::class.java, phoneNumber)
        return result != null && result.allowed
    }

    open fun ban(phoneNumber: String) {
        val allowListItem = SMSAllowListItem(
            phoneNumber = phoneNumber,
            allowed = false
        )

        mapper.save(allowListItem)
    }
}