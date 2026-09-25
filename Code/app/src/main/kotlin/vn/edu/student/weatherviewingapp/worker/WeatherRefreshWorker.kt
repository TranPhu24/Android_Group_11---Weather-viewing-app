package vn.edu.student.weatherviewingapp.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import retrofit2.HttpException
import java.io.IOException
import vn.edu.student.weatherviewingapp.WeatherApplication

class WeatherRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {

    companion object {
        private const val TAG = "WeatherRefreshWorker"
    }

    override suspend fun doWork(): Result {

        Log.d(TAG, "========== Worker STARTED ==========")
        Log.d(TAG, "Worker ID: $id")

        if (vn.edu.student.weatherviewingapp.BuildConfig.WEATHER_API_KEY.isBlank()) {
            Log.e(TAG, "Weather API key is blank")
            Log.d(TAG, "Worker finished with FAILURE")
            return Result.failure()
        }

        Log.d(TAG, "Weather API key is available")

        val app = applicationContext as WeatherApplication
        val container = app.container

        Log.d(TAG, "Application container initialized")

        val cache = container.weatherCache

        Log.d(TAG, "Reading previous weather snapshot from cache")

        val previousSnapshot = cache.load()

        if (previousSnapshot == null) {
            Log.d(TAG, "No cached weather snapshot found")
            Log.d(TAG, "Worker finished with SUCCESS - nothing to refresh")
            return Result.success()
        }

        val latitude = previousSnapshot.weather.coord.lat
        val longitude = previousSnapshot.weather.coord.lon

        Log.d(TAG, "Cached location found")
        Log.d(TAG, "Latitude: $latitude")
        Log.d(TAG, "Longitude: $longitude")

        return try {

            Log.d(TAG, "Calling Weather API...")

            val weather = container.weatherRepository.getWeatherByCoords(
                latitude,
                longitude
            )

            Log.d(TAG, "Current weather API call successful")
            Log.d(TAG, "City: ${weather.cityName}")

            Log.d(TAG, "Syncing full weather data...")
            Log.d(TAG, "Fetching forecast and AQI data")

            container.weatherSyncManager.syncFullWeatherData(weather)

            Log.d(TAG, "Weather data synchronized successfully")
            Log.d(TAG, "New weather snapshot saved to cache")

            Log.d(TAG, "========== Worker FINISHED: SUCCESS ==========")

            Result.success()

        } catch (exception: HttpException) {

            Log.e(
                TAG,
                "HTTP error: ${exception.code()} - ${exception.message()}"
            )

            if (exception.code() == 429 || exception.code() in 500..599) {
                Log.d(TAG, "Temporary server/API error -> Result.retry()")
                Result.retry()
            } else {
                Log.d(TAG, "Permanent HTTP error -> Result.failure()")
                Result.failure()
            }

        } catch (exception: IOException) {

            Log.e(
                TAG,
                "Network/IO error: ${exception.message}"
            )

            Log.d(TAG, "Network error -> Result.retry()")

            Result.retry()

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Unexpected error: ${exception.message}",
                exception
            )

            Log.d(TAG, "Unexpected error -> Result.retry()")

            Result.retry()
        }
    }
}