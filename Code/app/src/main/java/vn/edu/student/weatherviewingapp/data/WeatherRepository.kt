package vn.edu.student.weatherviewingapp.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class WeatherRepository {
    private val json = Json { ignoreUnknownKeys = true }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val weatherApi = retrofit.create(WeatherApi::class.java)

    suspend fun getWeather(city: String, apiKey: String): WeatherResponse {
        return weatherApi.getCurrentWeather(city, apiKey)
    }

    suspend fun getForecast(city: String, apiKey: String): ForecastResponse {
        return weatherApi.getForecast(city, apiKey)
    }

    suspend fun getAirPollution(lat: Double, lon: Double, apiKey: String): AirPollutionResponse {
        return weatherApi.getAirPollution(lat, lon, apiKey)
    }

    suspend fun searchLocations(query: String, apiKey: String): List<LocationResult> {
        // Try multiple query formats to get the best results for Vietnam
        // Increasing limit to 50 to get more districts/wards
        val search1 = weatherApi.searchLocations("$query, Vietnam", 50, apiKey)
        val search2 = weatherApi.searchLocations(query, 50, apiKey).filter { it.country == "VN" }
        
        // Merge and remove duplicates
        return (search1 + search2).distinctBy { "${it.lat},${it.lon}" }
    }

    suspend fun getWeatherByCoords(lat: Double, lon: Double, apiKey: String): WeatherResponse {
        return weatherApi.getWeatherByCoords(lat, lon, apiKey)
    }

    suspend fun reverseGeocode(lat: Double, lon: Double, apiKey: String): List<LocationResult> {
        return weatherApi.reverseGeocode(lat, lon, 1, apiKey)
    }
}
