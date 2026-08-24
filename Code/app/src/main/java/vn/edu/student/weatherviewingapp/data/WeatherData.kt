package vn.edu.student.weatherviewingapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class WeatherResponse(
    @SerialName("main") val main: Main,
    @SerialName("weather") val weather: List<Weather>,
    @SerialName("name") val cityName: String,
    @SerialName("dt") val timestamp: Long,
    @SerialName("coord") val coord: Coord,
    @SerialName("wind") val wind: Wind,
    @SerialName("sys") val sys: Sys
)

@Serializable
data class Coord(
    val lat: Double,
    val lon: Double
)

@Serializable
data class Main(
    @SerialName("temp") val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("temp_min") val tempMin: Double,
    @SerialName("temp_max") val tempMax: Double,
    @SerialName("humidity") val humidity: Int,
    @SerialName("pressure") val pressure: Int
)

@Serializable
data class Weather(
    @SerialName("main") val main: String,
    @SerialName("description") val description: String,
    @SerialName("icon") val icon: String
)

@Serializable
data class Wind(
    val speed: Double
)

@Serializable
data class Sys(
    val country: String? = null
)

@Serializable
data class ForecastResponse(
    val list: List<ForecastItem>
)

@Serializable
data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    @SerialName("dt_txt") val dtTxt: String
)

@Serializable
data class AirPollutionResponse(
    val list: List<AirPollutionItem>
)

@Serializable
data class AirPollutionItem(
    val main: AirPollutionMain,
    val components: Map<String, Double>
)

@Serializable
data class AirPollutionMain(
    val aqi: Int
)

@Serializable
data class LocationResult(
    val name: String,
    @SerialName("local_names") val localNames: Map<String, String>? = null,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
)
