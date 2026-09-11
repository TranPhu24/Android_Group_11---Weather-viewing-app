package vn.edu.student.weatherviewingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.student.weatherviewingapp.BuildConfig
import vn.edu.student.weatherviewingapp.data.FavoriteLocationStore
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.repository.WeatherRepository
import vn.edu.student.weatherviewingapp.ui.CompareUiState
import vn.edu.student.weatherviewingapp.ui.LocationWeatherComparison

class CompareViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository()
    private val favoriteStore = FavoriteLocationStore(application)
    private val apiKey = BuildConfig.WEATHER_API_KEY

    private val _uiState = MutableStateFlow<CompareUiState>(CompareUiState.Empty)
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    private val _favorites = MutableStateFlow<List<LocationResult>>(emptyList())
    val favorites: StateFlow<List<LocationResult>> = _favorites.asStateFlow()

    private val _selectedLocations = MutableStateFlow<Set<LocationResult>>(emptySet())
    val selectedLocations: StateFlow<Set<LocationResult>> = _selectedLocations.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        _favorites.value = favoriteStore.loadFavorites()
    }

    fun toggleSelection(location: LocationResult) {
        val current = _selectedLocations.value.toMutableSet()
        if (current.contains(location)) {
            current.remove(location)
        } else {
            current.add(location)
        }
        _selectedLocations.value = current
        
        if (current.size >= 2) {
            compareSelected()
        } else {
            _uiState.value = CompareUiState.Empty
        }
    }

    fun compareSelected() {
        val selected = _selectedLocations.value
        if (selected.size < 2) return

        _uiState.value = CompareUiState.Loading
        viewModelScope.launch {
            try {
                val comparisons = selected.map { location ->
                    async {
                        val weather = repository.getWeatherByCoords(location.lat, location.lon, apiKey)
                        val air = repository.getAirPollution(location.lat, location.lon, apiKey)
                        LocationWeatherComparison(
                            name = location.localNames?.get("vi") ?: location.name,
                            weather = weather,
                            airPollution = air
                        )
                    }
                }.map { it.await() }
                
                _uiState.value = CompareUiState.Success(comparisons)
            } catch (e: Exception) {
                _uiState.value = CompareUiState.Error(e.localizedMessage ?: "Lỗi khi tải dữ liệu so sánh")
            }
        }
    }
}
