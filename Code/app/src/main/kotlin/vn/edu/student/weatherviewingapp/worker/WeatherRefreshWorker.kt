package vn.edu.student.weatherviewingapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import retrofit2.HttpException
import java.io.IOException
import vn.edu.student.weatherviewingapp.WeatherApplication

class WeatherRefreshWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {

    override suspend fun doWork(): Result {
        if (vn.edu.student.weatherviewingapp.BuildConfig.WEATHER_API_KEY.isBlank()) {
            return Result.failure()
        }

        val app = applicationContext as WeatherApplication
        val container = app.container
        
        val cache = container.weatherCache
        val previousSnapshot = cache.load() ?: return Result.success()

        return try {
            val weather = container.weatherRepository.getWeatherByCoords(
                previousSnapshot.weather.coord.lat,
                previousSnapshot.weather.coord.lon
            )
            container.weatherSyncManager.syncFullWeatherData(weather)
            Result.success()
        } catch (exception: HttpException) {
            if (exception.code() == 429 || (exception.code() in 500..599)) Result.retry() else Result.failure()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
