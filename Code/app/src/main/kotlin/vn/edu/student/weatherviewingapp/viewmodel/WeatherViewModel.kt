package vn.edu.student.weatherviewingapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.repository.WeatherRepository
import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.BuildConfig
import vn.edu.student.weatherviewingapp.ui.WeatherUiState

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<LocationResult>>(emptyList())
    val suggestions: StateFlow<List<LocationResult>> = _suggestions.asStateFlow()

    // Your API Key
    private val apiKey = BuildConfig.WEATHER_API_KEY

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
        val forecastDeferred = viewModelScope.async { repository.getForecast(weather.cityName, apiKey) }
        val pollutionDeferred = viewModelScope.async { repository.getAirPollution(weather.coord.lat, weather.coord.lon, apiKey) }

        _uiState.value = WeatherUiState.Success(
            weather = weather,
            forecast = forecastDeferred.await(),
            airPollution = pollutionDeferred.await()
        )
    }

    private fun isApiKeyInvalid(): Boolean {
        if (apiKey == "YOUR_API_KEY_HERE" || apiKey.isBlank()) {
            _uiState.value = WeatherUiState.Error("Please provide a valid OpenWeatherMap API Key in WeatherViewModel.kt")
            return true
        }
        return false
    }
}