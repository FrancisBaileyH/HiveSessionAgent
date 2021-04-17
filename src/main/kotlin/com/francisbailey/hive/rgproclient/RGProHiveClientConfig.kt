package com.francisbailey.hive.rgproclient

import com.francisbailey.hive.common.RGProLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.features.BrowserUserAgent


class RGProHiveClientConfig {

    val endpoint = "https://app.rockgympro.com"

    val origin = "https://app.rockgympro.com"

    fun buildClient() = HttpClient(CIO) {
        BrowserUserAgent()
        engine {
            this.endpoint {
                this.connectAttempts = 3
            }
            this.requestTimeout = 3000 // ms
        }
    }
}