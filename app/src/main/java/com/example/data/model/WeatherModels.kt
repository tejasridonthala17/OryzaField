package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    @Json(name = "timezone_abbreviation") val timezoneAbbreviation: String? = null,
    val current: CurrentWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    val time: String? = null,
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double? = null,
    val precipitation: Double? = null,
    @Json(name = "soil_moisture_0_to_1cm") val soilMoisture0To1Cm: Double? = null
)

data class SiteWeather(
    val temperatureC: Double,
    val humidityPct: Double,
    val precipitationMm: Double,
    val soilMoistureM3: Double,
    val observedAt: String,
    val timezone: String
)
