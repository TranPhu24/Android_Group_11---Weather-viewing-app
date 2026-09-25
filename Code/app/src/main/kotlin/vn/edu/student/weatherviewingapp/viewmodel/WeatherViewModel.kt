package vn.edu.student.weatherviewingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import vn.edu.student.weatherviewingapp.WeatherApplication
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.data.WeatherSnapshot
import vn.edu.student.weatherviewingapp.ui.WeatherUiState

class WeatherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContainer =
        (application as WeatherApplication).container

    private val repository =
        appContainer.weatherRepository

    private val favoriteStore =
        appContainer.favoriteStore

    private val weatherSyncManager =
        appContainer.weatherSyncManager

    private val weatherCache =
        appContainer.weatherCache

    // =====================================================
    // UI STATE
    // =====================================================

    private val _uiState =
        MutableStateFlow<WeatherUiState>(
            WeatherUiState.Initial
        )

    val uiState: StateFlow<WeatherUiState> =
        _uiState.asStateFlow()

    // =====================================================
    // SEARCH SUGGESTIONS
    // =====================================================

    private val _suggestions =
        MutableStateFlow<List<LocationResult>>(
            emptyList()
        )

    val suggestions: StateFlow<List<LocationResult>> =
        _suggestions.asStateFlow()

    // =====================================================
    // FAVORITES
    // =====================================================

    private val _favorites =
        MutableStateFlow(
            favoriteStore.loadFavorites()
        )

    val favorites: StateFlow<List<LocationResult>> =
        _favorites.asStateFlow()

    // =====================================================
    // RETRY
    // =====================================================

    private var lastAction: (() -> Unit)? = null

    // =====================================================
    // INITIALIZATION
    // =====================================================

    init {

        loadCachedWeather()

        observeCacheUpdates()

        weatherCache.load()?.let { cached ->
            refreshCachedWeatherInBackground(cached)
        }
    }

    // =====================================================
    // CACHE
    // =====================================================

    /**
     * Load cached weather and display it immediately.
     */
    private fun loadCachedWeather() {

        val cached =
            weatherCache.load()
                ?: return

        _uiState.value =
            WeatherUiState.Success(
                weather = cached.weather,
                forecast = cached.forecast,
                airPollution = cached.airPollution,
                refreshedAtMillis = cached.refreshedAtMillis,
                isFromCache = true
            )
    }

    private fun observeCacheUpdates() {

        viewModelScope.launch {

            weatherCache.cacheUpdated.collectLatest {

                val updatedSnapshot =
                    weatherCache.load()

                if (updatedSnapshot != null) {

                    _uiState.value =
                        WeatherUiState.Success(
                            weather = updatedSnapshot.weather,
                            forecast = updatedSnapshot.forecast,
                            airPollution = updatedSnapshot.airPollution,
                            refreshedAtMillis =
                                updatedSnapshot.refreshedAtMillis,
                            isFromCache = false
                        )
                }
            }
        }
    }

    // =====================================================
    // SEARCH LOCATION
    // =====================================================

    fun searchLocations(query: String) {

        if (query.length < 2) {

            _suggestions.value =
                emptyList()

            return
        }

        viewModelScope.launch {

            try {

                val results =
                    repository.searchLocations(query)

                _suggestions.value =
                    results

            } catch (_: Exception) {

                _suggestions.value =
                    emptyList()
            }
        }
    }

    fun clearSuggestions() {

        _suggestions.value =
            emptyList()
    }

    // =====================================================
    // FAVORITES
    // =====================================================

    fun toggleFavorite(
        location: LocationResult
    ) {

        val current =
            _favorites.value.toMutableList()

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

        _favorites.value =
            current

        favoriteStore.saveFavorites(
            current
        )
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

    // =====================================================
    // API KEY
    // =====================================================

    private fun isApiKeyInvalid(): Boolean {

        val apiKey =
            vn.edu.student.weatherviewingapp
                .BuildConfig
                .WEATHER_API_KEY

        if (
            apiKey == "YOUR_API_KEY_HERE" ||
            apiKey.isBlank()
        ) {

            _uiState.value =
                WeatherUiState.Error(
                    "Please provide OPEN_WEATHER_API_KEY in local.properties"
                )

            return true
        }

        return false
    }

    // =====================================================
    // RETRY
    // =====================================================

    fun retry() {

        lastAction?.invoke()
    }

    // =====================================================
    // ERROR MESSAGE
    // =====================================================

    private fun getFriendlyErrorMessage(
        e: Exception
    ): String {

        val msg =
            e.localizedMessage ?: ""

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

    // =====================================================
    // FETCH WEATHER BY CITY
    // =====================================================

    fun fetchWeather(
        city: String
    ) {

        if (city.isBlank()) {
            return
        }

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

                fetchFullWeatherData(
                    weather
                )

            } catch (e: Exception) {

                showCacheOrError(e)
            }
        }
    }

    // =====================================================
    // FETCH WEATHER BY COORDINATES
    // =====================================================

    fun fetchWeatherByCoords(
        lat: Double,
        lon: Double,
        name: String? = null
    ) {

        lastAction = {

            fetchWeatherByCoords(
                lat = lat,
                lon = lon,
                name = name
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

                showCacheOrError(e)
            }
        }
    }

    // =====================================================
    // OFFLINE CACHE
    // =====================================================

    private fun showCacheOrError(
        error: Exception
    ) {

        val cached =
            weatherCache.load()

        if (cached != null) {

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

            _uiState.value =
                WeatherUiState.Error(
                    getFriendlyErrorMessage(
                        error
                    )
                )
        }
    }

    // =====================================================
    // FULL WEATHER DATA
    // =====================================================

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
                weather = snapshot.weather,
                forecast = snapshot.forecast,
                airPollution = snapshot.airPollution,
                refreshedAtMillis =
                    snapshot.refreshedAtMillis,
                isFromCache = false
            )
    }

    // =====================================================
    // REFRESH CACHED WEATHER
    // =====================================================

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

                /*
                 * Keep displaying the cached data.
                 *
                 * The UI can use refreshedAtMillis to show
                 * the cache age / stale status.
                 */
            }
        }
    }
}