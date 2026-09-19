package vn.edu.student.weatherviewingapp.repository

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import vn.edu.student.weatherviewingapp.alerts.WeatherAlertNotifier
import vn.edu.student.weatherviewingapp.data.WeatherCache
import vn.edu.student.weatherviewingapp.data.WeatherResponse
import vn.edu.student.weatherviewingapp.data.WeatherSnapshot

class WeatherSyncManager(
    private val repository: WeatherRepository,
    private val cache: WeatherCache,
    private val context: Context
) {
    suspend fun syncFullWeatherData(weather: WeatherResponse): WeatherSnapshot = coroutineScope {
        val forecastDeferred = async { repository.getForecastByCoords(weather.coord.lat, weather.coord.lon) }
        val pollutionDeferred = async { repository.getAirPollution(weather.coord.lat, weather.coord.lon) }

        val snapshot = WeatherSnapshot(
            weather = weather,
            forecast = forecastDeferred.await(),
            airPollution = pollutionDeferred.await()
        )
        
        cache.save(snapshot)
        WeatherAlertNotifier.notifyIfNeeded(context, snapshot)
        
        return@coroutineScope snapshot
    }
}
