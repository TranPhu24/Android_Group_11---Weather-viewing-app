package vn.edu.student.weatherviewingapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import vn.edu.student.weatherviewingapp.data.CachedWeatherData
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.data.WeatherRepository
import vn.edu.student.weatherviewingapp.data.WeatherResponse

class WeatherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WeatherRepository()

    private val _uiState =
        MutableStateFlow<WeatherUiState>(
            WeatherUiState.Initial
        )

    val uiState: StateFlow<WeatherUiState> =
        _uiState.asStateFlow()

    private val _suggestions =
        MutableStateFlow<List<LocationResult>>(
            emptyList()
        )

    val suggestions: StateFlow<List<LocationResult>> =
        _suggestions.asStateFlow()

    // Bộ nhớ cục bộ dùng để lưu dữ liệu thời tiết gần nhất.
    private val cachePreferences =
        application.getSharedPreferences(
            "weather_cache",
            Context.MODE_PRIVATE
        )

    // Bộ chuyển đổi object Kotlin <-> JSON.
    private val json = Json {
        ignoreUnknownKeys = true
    }

    // Giữ nguyên API key hiện tại của project.
    private val apiKey =
        "0fd5b4d98bdbaca7ce5be44bb322d34f"

    companion object {
        private const val CACHE_KEY =
            "latest_weather_data"
    }

    init {
        // Khi mở ứng dụng, thử khôi phục dữ liệu đã lưu.
        restoreCachedWeather()
    }

    fun searchLocations(query: String) {

        if (query.length < 2) {
            _suggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {

                val results =
                    repository.searchLocations(
                        query,
                        apiKey
                    )

                _suggestions.value = results

            } catch (e: Exception) {

                _suggestions.value = emptyList()
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun fetchWeather(city: String) {

        if (city.isBlank()) {
            return
        }

        _uiState.value =
            WeatherUiState.Loading

        viewModelScope.launch {
            try {

                if (isApiKeyInvalid()) {
                    return@launch
                }

                val weather =
                    repository.getWeather(
                        city,
                        apiKey
                    )

                fetchFullWeatherData(weather)

            } catch (e: Exception) {

                showCacheOrError(
                    e.localizedMessage
                        ?: "Không thể kết nối Internet"
                )
            }
        }
    }

    fun fetchWeatherByCoords(
        lat: Double,
        lon: Double,
        name: String? = null
    ) {

        _uiState.value =
            WeatherUiState.Loading

        viewModelScope.launch {
            try {

                if (isApiKeyInvalid()) {
                    return@launch
                }

                val weather =
                    repository.getWeatherByCoords(
                        lat,
                        lon,
                        apiKey
                    )

                val finalWeather =
                    if (name != null) {
                        weather.copy(
                            cityName = name
                        )
                    } else {
                        weather
                    }

                fetchFullWeatherData(
                    finalWeather
                )

            } catch (e: Exception) {

                showCacheOrError(
                    e.localizedMessage
                        ?: "Không thể kết nối Internet"
                )
            }
        }
    }

    /**
     * Tải dự báo và chất lượng không khí.
     * Khi toàn bộ request thành công, dữ liệu được lưu cache.
     */
    private suspend fun fetchFullWeatherData(
        weather: WeatherResponse
    ) = coroutineScope {

        val forecastDeferred =
            async {
                repository.getForecast(
                    weather.cityName,
                    apiKey
                )
            }

        val pollutionDeferred =
            async {
                repository.getAirPollution(
                    weather.coord.lat,
                    weather.coord.lon,
                    apiKey
                )
            }

        val forecast =
            forecastDeferred.await()

        val airPollution =
            pollutionDeferred.await()

        val cachedAt =
            System.currentTimeMillis()

        val cachedData =
            CachedWeatherData(
                weather = weather,
                forecast = forecast,
                airPollution = airPollution,
                cachedAt = cachedAt
            )

        saveWeatherCache(
            cachedData
        )

        _uiState.value =
            WeatherUiState.Success(
                weather = weather,
                forecast = forecast,
                airPollution = airPollution,
                isFromCache = false,
                cachedAt = cachedAt
            )
    }

    /**
     * Chuyển dữ liệu thời tiết thành JSON
     * rồi lưu xuống SharedPreferences.
     */
    private fun saveWeatherCache(
        cachedData: CachedWeatherData
    ) {

        try {

            val jsonString =
                json.encodeToString(
                    CachedWeatherData.serializer(),
                    cachedData
                )

            cachePreferences
                .edit()
                .putString(
                    CACHE_KEY,
                    jsonString
                )
                .apply()

        } catch (e: Exception) {
            // Cache lỗi không được làm ứng dụng crash.
        }
    }

    /**
     * Đọc JSON từ SharedPreferences
     * và khôi phục CachedWeatherData.
     */
    private fun loadCachedWeather():
            CachedWeatherData? {

        return try {

            val jsonString =
                cachePreferences
                    .getString(
                        CACHE_KEY,
                        null
                    )
                    ?: return null

            json.decodeFromString(
                CachedWeatherData.serializer(),
                jsonString
            )

        } catch (e: Exception) {

            null
        }
    }

    /**
     * Khôi phục dữ liệu gần nhất khi mở ứng dụng.
     */
    private fun restoreCachedWeather() {

        val cachedData =
            loadCachedWeather()
                ?: return

        _uiState.value =
            WeatherUiState.Success(
                weather = cachedData.weather,
                forecast = cachedData.forecast,
                airPollution = cachedData.airPollution,
                isFromCache = true,
                cachedAt = cachedData.cachedAt
            )
    }

    /**
     * Nếu request mạng thất bại:
     * - có cache -> dùng cache;
     * - không có cache -> báo lỗi.
     */
    private fun showCacheOrError(
        errorMessage: String
    ) {

        val cachedData =
            loadCachedWeather()

        if (cachedData != null) {

            _uiState.value =
                WeatherUiState.Success(
                    weather = cachedData.weather,
                    forecast = cachedData.forecast,
                    airPollution = cachedData.airPollution,
                    isFromCache = true,
                    cachedAt = cachedData.cachedAt
                )

        } else {

            _uiState.value =
                WeatherUiState.Error(
                    errorMessage
                )
        }
    }

    private fun isApiKeyInvalid(): Boolean {

        if (
            apiKey == "YOUR_API_KEY_HERE" ||
            apiKey.isBlank()
        ) {

            _uiState.value =
                WeatherUiState.Error(
                    "Please provide a valid OpenWeatherMap API Key"
                )

            return true
        }

        return false
    }
}