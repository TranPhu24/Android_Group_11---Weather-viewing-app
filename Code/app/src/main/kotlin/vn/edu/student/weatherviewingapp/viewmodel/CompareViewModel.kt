package vn.edu.student.weatherviewingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.student.weatherviewingapp.WeatherApplication
import vn.edu.student.weatherviewingapp.data.LocationResult
import vn.edu.student.weatherviewingapp.ui.CompareUiState
import vn.edu.student.weatherviewingapp.ui.LocationWeatherComparison
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

class CompareViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as WeatherApplication).container
    private val repository = appContainer.weatherRepository
    private val favoriteStore = appContainer.favoriteStore

    private val _uiState = MutableStateFlow<CompareUiState>(CompareUiState.Empty)
    val uiState: StateFlow<CompareUiState> = _uiState.asStateFlow()

    private val _favorites = MutableStateFlow(favoriteStore.loadFavorites())
    val favorites: StateFlow<List<LocationResult>> = _favorites.asStateFlow()

    private val _selectedLocations = MutableStateFlow<Set<LocationResult>>(emptySet())
    val selectedLocations: StateFlow<Set<LocationResult>> = _selectedLocations.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        val newFavorites = favoriteStore.loadFavorites()
        _favorites.value = newFavorites
        
        val currentSelected = _selectedLocations.value
        val prunedSelected = currentSelected.filter { selected ->
            newFavorites.any { it.lat == selected.lat && it.lon == selected.lon }
        }.toSet()
        
        if (prunedSelected.size != currentSelected.size) {
            _selectedLocations.value = prunedSelected
            if (prunedSelected.size >= 2) {
                compareSelected()
            } else {
                _uiState.value = CompareUiState.Empty
            }
        }
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
                val comparisons = supervisorScope {
                    selected.map { location ->
                        async {
                            try {
                                val weather = repository.getWeatherByCoords(location.lat, location.lon)
                                val air = repository.getAirPollution(location.lat, location.lon)
                                LocationWeatherComparison(
                                    name = location.localNames?.get("vi") ?: location.name,
                                    weather = weather,
                                    airPollution = air,
                                    isError = false
                                )
                            } catch (e: Exception) {
                                LocationWeatherComparison(
                                    name = location.localNames?.get("vi") ?: location.name,
                                    isError = true
                                )
                            }
                        }
                    }.awaitAll()
                }
                _uiState.value = CompareUiState.Success(comparisons)
            } catch (e: Exception) {
                _uiState.value = CompareUiState.Error("Không thể kết nối đến máy chủ thời tiết")
            }
        }
    }
}
