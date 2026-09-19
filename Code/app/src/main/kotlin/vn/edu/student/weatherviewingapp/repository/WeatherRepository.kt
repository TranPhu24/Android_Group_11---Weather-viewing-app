package vn.edu.student.weatherviewingapp.repository

import vn.edu.student.weatherviewingapp.data.AirPollutionResponse
import vn.edu.student.weatherviewingapp.data.ForecastResponse
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.data.WeatherApi
import vn.edu.student.weatherviewingapp.data.WeatherResponse

class WeatherRepository(private val weatherApi: WeatherApi) {

    suspend fun getWeather(city: String): WeatherResponse {
        return weatherApi.getCurrentWeather(city)
    }

    suspend fun getForecast(city: String): ForecastResponse {
        return weatherApi.getForecast(city)
    }

    suspend fun getForecastByCoords(lat: Double, lon: Double): ForecastResponse {
        return weatherApi.getForecastByCoords(lat, lon)
    }

    suspend fun getAirPollution(lat: Double, lon: Double): AirPollutionResponse {
        return weatherApi.getAirPollution(lat, lon)
    }

    suspend fun searchLocations(query: String): List<LocationResult> {
        return try {
            // Call API with national key VN
            val searchVn = try { weatherApi.searchLocations("$query,VN", 50) } catch (e: Exception) { emptyList() }

            // Tìm kiếm chung đề phòng trường hợp API sót kết quả
            val searchGlobal = try { weatherApi.searchLocations(query, 50) } catch (e: Exception) { emptyList() }

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

    suspend fun getWeatherByCoords(lat: Double, lon: Double): WeatherResponse {
        return weatherApi.getWeatherByCoords(lat, lon)
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): List<LocationResult> {
        return weatherApi.reverseGeocode(lat, lon, 1)
    }
}