package com.francisbailey.hive.rgproclient


import com.francisbailey.hive.common.HiveBookingClient
import com.francisbailey.hive.common.HiveLocation
import com.francisbailey.hive.common.ScheduleEntry
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import java.time.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class RGProHiveClient(
    private val config: RGProHiveClientConfig,
    private val scheduleParser: RGProScheduleParser
): HiveBookingClient, AutoCloseable {

    private val client = config.buildClient()

    override suspend fun getBookingAvailability(date: LocalDate, location: HiveLocation): List<ScheduleEntry> {
        val locationGuid = config.locationGuidMap[location] ?: error("Missing mapping for $location")

        val response = client.post<HttpResponse>(urlString = "${config.endpoint}/b/widget/?a=equery") {
            headers {
                append("Accept", "*/*")
                append("Accept-Encoding", "gzip, deflate, br")
                append("Connection", "keep-alive")
                append("Origin", config.origin)
                append("Host", config.origin)
            }
            body = FormDataContent(Parameters.build {
                append("mode", "e")
                append("fctrl_1", "offering_guid")
                append("offering_guid", locationGuid)
                append("fctrl_2", "course_guid")
                append("course_guid", "")
                append("fctrl_3", "limited_to_course_guid_for_offering_guid_$locationGuid}")
                append("limited_to_course_guid_for_offering_guid_$locationGuid", "")
                append("fctrl_4", "show_date")
                append("show_date", date.toString())
            })
        }

        if (response.status != HttpStatusCode.OK) {
            throw RGProHiveClientException("Request failed with status: ${response.status}")
        }

        val scheduleResponse = Json.decodeFromString<RGProScheduleResponse>(response.readText())
        return scheduleParser.parse(scheduleResponse.htmlScheduleTable, date)
    }

    override fun close() {
        client.close()
    }
}

class RGProHiveClientException(override val message: String): RuntimeException()

@Serializable
data class RGProScheduleResponse(
    @SerialName("error_messages")
    val errorMessages: String,
    @SerialName("has_promo_code")
    val hasPromoCode: Boolean,
    @SerialName("promo_code_is_valid")
    val promoCodeIsValid: Boolean,
    @SerialName("promo_code_error_message")
    val promoCodeErrorMessage: String,
    @SerialName("event_list_html")
    val htmlScheduleTable: String,
    @SerialName("date_label")
    val dateLabel: String
)