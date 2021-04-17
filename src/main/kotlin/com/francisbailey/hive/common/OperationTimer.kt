package com.francisbailey.hive.common

import mu.KotlinLogging
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit

private val log = KotlinLogging.logger {}


class CloudWatchOperationTimer(
    private val cloudwatchClient: CloudWatchClient,
    private val service: String,
    private val operation: String
): AutoCloseable {

    private val startTime = System.nanoTime()

    override fun close() {
        try {
            val totalTime = System.nanoTime() - startTime
            cloudwatchClient.putMetricData(
                PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(MetricDatum.builder()
                        .metricName("RequestLatency")
                        .value(totalTime / 1000000.0)
                        .unit(StandardUnit.MILLISECONDS)
                        .dimensions(
                            Dimension.builder().name("Operation").value(operation).build(),
                            Dimension.builder().name("Service").value(service).build()
                        )
                        .build())
                    .build()
            )
        } catch (e: Exception) {
            log.error(e) { "Failed to publish metric" }
        }
    }

    companion object {
        private const val NAMESPACE = "HiveSessionAgent"
    }
}