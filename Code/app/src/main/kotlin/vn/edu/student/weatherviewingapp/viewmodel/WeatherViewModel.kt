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

    init {
        appContainer.weatherCache.load()?.let { cached ->
            _uiState.value = WeatherUiState.Success(
                cached.weather,
                cached.forecast,
                cached.airPollution,
                cached.refreshedAtMillis
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
        val existingIndex = current.indexOfFirst { it.lat == location.lat && it.lon == location.lon }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(location)
        }
        _favorites.value = current
        favoriteStore.saveFavorites(current)
    }

    fun isFavorite(lat: Double, lon: Double): Boolean {
        return _favorites.value.any { it.lat == lat && it.lon == lon }
    }

    private fun isApiKeyInvalid(): Boolean {
        if (vn.edu.student.weatherviewingapp.BuildConfig.WEATHER_API_KEY == "YOUR_API_KEY_HERE" || vn.edu.student.weatherviewingapp.BuildConfig.WEATHER_API_KEY.isBlank()) {
            _uiState.value = WeatherUiState.Error("Please provide OPEN_WEATHER_API_KEY in local.properties")
            return true
        }
        return false
    }

    fun fetchWeather(city: String) {
        if (city.isBlank()) return
        _uiState.value = WeatherUiState.Loading
        viewModelScope.launch {
            if (isApiKeyInvalid()) return@launch
            try {
                val weather = repository.getWeather(city)
                fetchFullWeatherData(weather)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double, name: String? = null) {
        _uiState.value = WeatherUiState.Loading
        viewModelScope.launch {
            if (isApiKeyInvalid()) return@launch
            try {
                val weather = repository.getWeatherByCoords(lat, lon)
                val finalWeather = if (name != null) weather.copy(cityName = name) else weather
                fetchFullWeatherData(finalWeather)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    private suspend fun fetchFullWeatherData(weather: WeatherResponse) {
        val snapshot = weatherSyncManager.syncFullWeatherData(weather)
        _uiState.value = WeatherUiState.Success(
            snapshot.weather,
            snapshot.forecast,
            snapshot.airPollution,
            snapshot.refreshedAtMillis
        )
    }

    private fun refreshCachedWeatherInBackground(cached: WeatherSnapshot) {
        viewModelScope.launch {
            try {
                val weather = repository.getWeatherByCoords(
                    cached.weather.coord.lat,
                    cached.weather.coord.lon
                )
                fetchFullWeatherData(weather)
            } catch (_: Exception) {
                // Keep displaying the cache; its age banner tells the user it could not be refreshed.
            }
        }
    }
}