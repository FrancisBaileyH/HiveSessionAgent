package com.francisbailey.hive.rgproclient

import com.francisbailey.hive.common.HiveLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.features.BrowserUserAgent


class RGProHiveClientConfig {

    val endpoint = "https://app.rockgympro.com"

    val origin = "https://app.rockgympro.com"

    val locationGuidMap = mapOf(
        HiveLocation.POCO to "b405d11ff01346e8bce172d854720c3d",
        HiveLocation.VANCOUVER to "484c1a7ca09145419ef258eeb894c38f",
        HiveLocation.NORTH_VANCOUVER to "6fa9139cc3584fc0a5662a5c36d68958",
        HiveLocation.SURREY to "b41f7158c38e43f5adb1ee5b003e4bd5"
    )

    fun buildClient() = HttpClient(CIO) {
        BrowserUserAgent()
        engine {
            this.endpoint {
                this.connectAttempts = 3
            }
        }
    }
}