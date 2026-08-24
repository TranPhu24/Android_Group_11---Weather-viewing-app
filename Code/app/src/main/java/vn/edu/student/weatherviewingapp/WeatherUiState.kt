package vn.edu.student.weatherviewingapp

import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.data.ForecastResponse
import vn.edu.student.weatherviewingapp.data.AirPollutionResponse

sealed interface WeatherUiState {
    object Initial : WeatherUiState
    object Loading : WeatherUiState
    data class Success(
        val weather: WeatherResponse,
        val forecast: ForecastResponse,
        val airPollution: AirPollutionResponse
    ) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
