package vn.edu.student.weatherviewingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.student.weatherviewingapp.WeatherApplication
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.data.WeatherSnapshot
import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.ui.WeatherUiState

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = (application as WeatherApplication).container
    private val repository = appContainer.weatherRepository
    private val favoriteStore = appContainer.favoriteStore
    private val weatherSyncManager = appContainer.weatherSyncManager

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<LocationResult>>(emptyList())
    val suggestions: StateFlow<List<LocationResult>> = _suggestions.asStateFlow()

    private val _favorites = MutableStateFlow<List<LocationResult>>(favoriteStore.loadFavorites())
    val favorites: StateFlow<List<LocationResult>> = _favorites.asStateFlow()

    private var lastAction: (() -> Unit)? = null

    init {
        appContainer.weatherCache.load()?.let { cached ->
            _uiState.value = WeatherUiState.Success(
                cached.weather,
                cached.forecast,
                cached.airPollution,
                cached.refreshedAtMillis,
                isFromCache = true
            )
            refreshCachedWeatherInBackground(cached)
        }
    }

    fun searchLocations(query: String) {
        if (query.length < 2) {
            _suggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val results = repository.searchLocations(query)
                _suggestions.value = results
            } catch (e: Exception) {
                _suggestions.value = emptyList()
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun toggleFavorite(location: LocationResult) {
        val current = _favorites.value.toMutableList()
        val existingIndex =
            current.indexOfFirst {
                it.lat == location.lat &&
                        it.lon == location.lon
            }

        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(location)
        }

        _favorites.value = current
        favoriteStore.saveFavorites(current)
    }

    fun isFavorite(
        lat: Double,
        lon: Double
    ): Boolean {
        return _favorites.value.any {
            it.lat == lat &&
                    it.lon == lon
        }
    }

    private fun isApiKeyInvalid(): Boolean {
        if (
            vn.edu.student.weatherviewingapp.BuildConfig.WEATHER_API_KEY ==
            "YOUR_API_KEY_HERE" ||
            vn.edu.student.weatherviewingapp.BuildConfig.WEATHER_API_KEY.isBlank()
        ) {
            _uiState.value =
                WeatherUiState.Error(
                    "Please provide OPEN_WEATHER_API_KEY in local.properties"
                )

            return true
        }

        return false
    }

    fun retry() {
        lastAction?.invoke()
    }

    private fun getFriendlyErrorMessage(
        e: Exception
    ): String {

        val msg = e.localizedMessage ?: ""

        return when {
            e is java.net.UnknownHostException ->
                "Không có kết nối mạng. Vui lòng kiểm tra kết nối Wifi/3G của bạn."

            e is java.net.SocketTimeoutException ->
                "Kết nối quá hạn. Vui lòng thử lại sau."

            e is retrofit2.HttpException &&
                    e.code() == 401 ->
                "Lỗi xác thực (API Key không hợp lệ)."

            e is retrofit2.HttpException &&
                    e.code() == 404 ->
                "Không tìm thấy dữ liệu khu vực này."

            e is retrofit2.HttpException &&
                    e.code() == 429 ->
                "Đã vượt quá giới hạn lượt truy cập API."

            else ->
                "Đã xảy ra lỗi: $msg"
        }
    }

    fun fetchWeather(city: String) {
        if (city.isBlank()) return

        lastAction = {
            fetchWeather(city)
        }

        _uiState.value =
            WeatherUiState.Loading

        viewModelScope.launch {

            if (isApiKeyInvalid()) {
                return@launch
            }

            try {

                val weather =
                    repository.getWeather(city)

                fetchFullWeatherData(weather)

            } catch (e: Exception) {

                // Mục 10 - Offline Cache
                showCacheOrError(e)
            }
        }
    }

    fun fetchWeatherByCoords(
        lat: Double,
        lon: Double,
        name: String? = null
    ) {

        lastAction = {
            fetchWeatherByCoords(
                lat,
                lon,
                name
            )
        }

        _uiState.value =
            WeatherUiState.Loading

        viewModelScope.launch {

            if (isApiKeyInvalid()) {
                return@launch
            }

            try {

                val weather =
                    repository.getWeatherByCoords(
                        lat,
                        lon
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

                // Mục 10 - Offline Cache
                showCacheOrError(e)
            }
        }
    }

    // =====================================================
    // MỤC 10 - OFFLINE CACHE
    // =====================================================

    private fun showCacheOrError(
        error: Exception
    ) {

        val cached =
            appContainer.weatherCache.load()

        if (cached != null) {

            // Có dữ liệu cache:
            // tiếp tục hiển thị dữ liệu gần nhất.
            _uiState.value =
                WeatherUiState.Success(
                    weather = cached.weather,
                    forecast = cached.forecast,
                    airPollution = cached.airPollution,
                    refreshedAtMillis =
                        cached.refreshedAtMillis,
                    isFromCache = true
                )

        } else {

            // Chưa có cache:
            // hiển thị lỗi như bình thường.
            _uiState.value =
                WeatherUiState.Error(
                    getFriendlyErrorMessage(
                        error
                    )
                )
        }
    }

    private suspend fun fetchFullWeatherData(
        weather: WeatherResponse
    ) {

        val snapshot =
            weatherSyncManager
                .syncFullWeatherData(
                    weather
                )

        _uiState.value =
            WeatherUiState.Success(
                snapshot.weather,
                snapshot.forecast,
                snapshot.airPollution,
                snapshot.refreshedAtMillis,
                isFromCache = false
            )
    }

    private fun refreshCachedWeatherInBackground(
        cached: WeatherSnapshot
    ) {

        viewModelScope.launch {

            try {

                val weather =
                    repository.getWeatherByCoords(
                        cached.weather.coord.lat,
                        cached.weather.coord.lon
                    )

                fetchFullWeatherData(
                    weather
                )

            } catch (_: Exception) {

                // Keep displaying the cache;
                // its age banner tells the user
                // it could not be refreshed.
            }
        }
    }
}