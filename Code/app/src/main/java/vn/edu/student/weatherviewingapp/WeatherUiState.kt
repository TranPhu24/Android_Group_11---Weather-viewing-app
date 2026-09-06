package vn.edu.student.weatherviewingapp

import vn.edu.student.weatherviewingapp.data.AirPollutionResponse
import vn.edu.student.weatherviewingapp.data.ForecastResponse
import vn.edu.student.weatherviewingapp.data.WeatherResponse

sealed interface WeatherUiState {

    object Initial : WeatherUiState

    object Loading : WeatherUiState

    data class Success(
        val weather: WeatherResponse,
        val forecast: ForecastResponse,
        val airPollution: AirPollutionResponse,
        val isFromCache: Boolean = false,
        val cachedAt: Long? = null
    ) : WeatherUiState

    data class Error(
        val message: String
    ) : WeatherUiState
}