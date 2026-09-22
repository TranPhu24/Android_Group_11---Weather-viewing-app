package vn.edu.student.weatherviewingapp.ui

import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.data.AirPollutionResponse

sealed interface CompareUiState {
    object Empty : CompareUiState
    object Loading : CompareUiState
    data class Success(
        val comparisons: List<LocationWeatherComparison>
    ) : CompareUiState
    data class Error(val message: String) : CompareUiState
}

data class LocationWeatherComparison(
    val name: String,
    val weather: WeatherResponse? = null,
    val airPollution: AirPollutionResponse? = null,
    val isError: Boolean = false
)
