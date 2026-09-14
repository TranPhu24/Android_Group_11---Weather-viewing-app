package vn.edu.student.weatherviewingapp.repository

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import vn.edu.student.weatherviewingapp.data.AirPollutionResponse
import vn.edu.student.weatherviewingapp.data.ForecastResponse
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.data.WeatherApi
import vn.edu.student.weatherviewingapp.data.WeatherResponse

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

    suspend fun getForecastByCoords(lat: Double, lon: Double, apiKey: String): ForecastResponse {
        return weatherApi.getForecastByCoords(lat, lon, apiKey)
    }

    suspend fun getAirPollution(lat: Double, lon: Double, apiKey: String): AirPollutionResponse {
        return weatherApi.getAirPollution(lat, lon, apiKey)
    }

    suspend fun searchLocations(query: String, apiKey: String): List<LocationResult> {
        return try {
            // Call API with national key VN
            val searchVn = try { weatherApi.searchLocations("$query,VN", 50, apiKey) } catch (e: Exception) { emptyList() }

            // Tìm kiếm chung đề phòng trường hợp API sót kết quả
            val searchGlobal = try { weatherApi.searchLocations(query, 50, apiKey) } catch (e: Exception) { emptyList() }

            // Gộp kết quả, chỉ lấy VN
            val combinedResults = (searchVn + searchGlobal)
                .filter { it.country.equals("VN", ignoreCase = true) }
                .distinctBy { "${it.lat},${it.lon}" }

            // 4. Sắp xếp ưu tiên:
            combinedResults.sortedWith(compareBy(
                {
                    val nameMatch = it.name.startsWith(query, ignoreCase = true)
                    val localMatch = it.localNames?.get("vi")?.startsWith(query, ignoreCase = true) ?: false
                    !(nameMatch || localMatch) // false (0) ưu tiên xếp trước true (1)
                },
                { it.name.length }
            ))
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWeatherByCoords(lat: Double, lon: Double, apiKey: String): WeatherResponse {
        return weatherApi.getWeatherByCoords(lat, lon, apiKey)
    }

    suspend fun reverseGeocode(lat: Double, lon: Double, apiKey: String): List<LocationResult> {
        return weatherApi.reverseGeocode(lat, lon, 1, apiKey)
    }
}