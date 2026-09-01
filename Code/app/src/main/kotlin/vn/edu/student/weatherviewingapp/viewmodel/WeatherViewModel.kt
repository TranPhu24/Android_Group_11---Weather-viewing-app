package vn.edu.student.weatherviewingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.data.WeatherCache
import vn.edu.student.weatherviewingapp.data.WeatherSnapshot
import vn.edu.student.weatherviewingapp.repository.WeatherRepository
import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.ui.WeatherUiState

object WeatherConfig {
    const val OPEN_WEATHER_API_KEY = "0fd5b4d98bdbaca7ce5be44bb322d34f"
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository()
    private val weatherCache = WeatherCache(application)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<LocationResult>>(emptyList())
    val suggestions: StateFlow<List<LocationResult>> = _suggestions.asStateFlow()

    private val apiKey = WeatherConfig.OPEN_WEATHER_API_KEY

    init {
        weatherCache.load()?.let { cached ->
            _uiState.value = WeatherUiState.Success(cached.weather, cached.forecast, cached.airPollution)
        }
    }

    fun searchLocations(query: String) {
        if (query.length < 2) {
            _suggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val results = repository.searchLocations(query, apiKey)
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
        if (city.isBlank()) return
        _uiState.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                if (isApiKeyInvalid()) return@launch
                val weather = repository.getWeather(city, apiKey)
                fetchFullWeatherData(weather)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun fetchWeatherByCoords(lat: Double, lon: Double, name: String? = null) {
        _uiState.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                if (isApiKeyInvalid()) return@launch
                val weather = repository.getWeatherByCoords(lat, lon, apiKey)
                // Use provided name if available (e.g., from search or GPS reverse geocoding)
                val finalWeather = if (name != null) weather.copy(cityName = name) else weather
                fetchFullWeatherData(finalWeather)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    private suspend fun fetchFullWeatherData(weather: WeatherResponse) {
        val forecastDeferred = viewModelScope.async {
            repository.getForecastByCoords(weather.coord.lat, weather.coord.lon, apiKey)
        }
        val pollutionDeferred = viewModelScope.async { repository.getAirPollution(weather.coord.lat, weather.coord.lon, apiKey) }

        val snapshot = WeatherSnapshot(
            weather = weather,
            forecast = forecastDeferred.await(),
            airPollution = pollutionDeferred.await()
        )
        weatherCache.save(snapshot)
        _uiState.value = WeatherUiState.Success(snapshot.weather, snapshot.forecast, snapshot.airPollution)
    }

    private fun isApiKeyInvalid(): Boolean {
        if (apiKey == "YOUR_API_KEY_HERE" || apiKey.isBlank()) {
            _uiState.value = WeatherUiState.Error("Please provide a valid OpenWeatherMap API Key in WeatherViewModel.kt")
            return true
        }
        return false
    }
}
