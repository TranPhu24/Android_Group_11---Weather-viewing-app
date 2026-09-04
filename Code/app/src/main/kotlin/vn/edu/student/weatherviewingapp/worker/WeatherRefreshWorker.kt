package vn.edu.student.weatherviewingapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.io.IOException
import vn.edu.student.weatherviewingapp.BuildConfig

import vn.edu.student.weatherviewingapp.data.WeatherCache
import vn.edu.student.weatherviewingapp.data.WeatherSnapshot
import vn.edu.student.weatherviewingapp.repository.WeatherRepository

class WeatherRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        if (BuildConfig.WEATHER_API_KEY.isBlank()) {
            return Result.failure()
        }

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

            Result.success()
        } catch (exception: HttpException) {
            // Retry rate limiting (429) and temporary server failures, not invalid requests or keys.
            if (exception.code() == 429 || exception.code() in 500..599) Result.retry() else Result.failure()
        } catch (exception: IOException) {
            // Network errors are transient; WorkManager applies the configured backoff delay.
            Result.retry()
        } catch (exception: Exception) {
            Result.retry()
        }
    }
}