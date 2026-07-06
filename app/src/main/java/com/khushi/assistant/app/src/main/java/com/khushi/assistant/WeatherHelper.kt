package com.khushi.assistant

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Free weather lookup using Open-Meteo (no API key required).
 * Defaults to Pune if no city is recognized - change DEFAULT_CITY
 * to your own city.
 */
object WeatherHelper {

    private const val DEFAULT_CITY = "Pune"

    fun getWeatherReport(cityGuess: String?): String {
        return try {
            val city = if (cityGuess.isNullOrBlank()) DEFAULT_CITY else cityGuess
            val coords = geocodeCity(city) ?: return "Mujhe $city ka location nahi mila."
            val (lat, lon) = coords

            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code,relative_humidity_2m&timezone=auto"
            )
            val response = fetch(url)
            val json = JSONObject(response)
            val current = json.getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val humidity = current.getInt("relative_humidity_2m")
            val code = current.getInt("weather_code")
            val condition = describeWeatherCode(code)

            "$city mein abhi temperature hai $temp degree Celsius, humidity $humidity percent, aur mausam $condition hai."
        } catch (e: Exception) {
            "Maaf kijiye, weather fetch karne mein problem aa gayi."
        }
    }

    private fun geocodeCity(city: String): Pair<Double, Double>? {
        val url = URL(
            "https://geocoding-api.open-meteo.com/v1/search?name=${city.replace(" ", "+")}&count=1"
        )
        val response = fetch(url)
        val json = JSONObject(response)
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val first = results.getJSONObject(0)
        return Pair(first.getDouble("latitude"), first.getDouble("longitude"))
    }

    private fun fetch(url: URL): String {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "saaf aasman"
        1, 2, 3 -> "halka baadal"
        45, 48 -> "kohra"
        51, 53, 55, 61, 63, 65 -> "baarish"
        71, 73, 75 -> "barfbaari"
        95, 96, 99 -> "toofan"
        else -> "mixed"
    }
}
