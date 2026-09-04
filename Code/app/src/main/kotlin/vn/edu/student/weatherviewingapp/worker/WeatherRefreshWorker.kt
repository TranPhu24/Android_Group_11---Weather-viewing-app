package vn.edu.student.weatherviewingapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import vn.edu.student.weatherviewingapp.BuildConfig
import vn.edu.student.weatherviewingapp.data.WeatherCache
import vn.edu.student.weatherviewingapp.alerts.WeatherAlertNotifier
import vn.edu.student.weatherviewingapp.data.WeatherSnapshot
import vn.edu.student.weatherviewingapp.repository.WeatherRepository

class WeatherRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        val cache = WeatherCache(applicationContext)
        val previousSnapshot = cache.load() ?: return Result.success()

        return try {
            val snapshot = coroutineScope {
                val weather = WeatherRepository().getWeatherByCoords(
                    previousSnapshot.weather.coord.lat,
                    previousSnapshot.weather.coord.lon,
                    BuildConfig.WEATHER_API_KEY
                )
                val repository = WeatherRepository()
                val forecast = async {
                    repository.getForecastByCoords(weather.coord.lat, weather.coord.lon, BuildConfig.WEATHER_API_KEY)
                }
                val airPollution = async {
                    repository.getAirPollution(weather.coord.lat, weather.coord.lon, BuildConfig.WEATHER_API_KEY)
                }
                WeatherSnapshot(weather, forecast.await(), airPollution.await())
            }
            cache.save(snapshot)
            WeatherAlertNotifier.notifyIfNeeded(applicationContext, snapshot)
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }
}
