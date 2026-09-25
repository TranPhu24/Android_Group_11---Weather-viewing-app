package vn.edu.student.weatherviewingapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class WeatherCache(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val _cacheUpdated = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1
    )

    val cacheUpdated: SharedFlow<Unit> =
        _cacheUpdated.asSharedFlow()

    fun save(snapshot: WeatherSnapshot) {

        preferences.edit()
            .putString(
                SNAPSHOT_KEY,
                json.encodeToString(
                    WeatherSnapshot.serializer(),
                    snapshot
                )
            )
            .apply()

        // Notify ViewModel that new data is available.
        _cacheUpdated.tryEmit(Unit)
    }

    fun load(): WeatherSnapshot? {

        val encoded =
            preferences.getString(
                SNAPSHOT_KEY,
                null
            )

        if (encoded == null) {
            return null
        }

        return runCatching {
            json.decodeFromString(
                WeatherSnapshot.serializer(),
                encoded
            )
        }.getOrNull()
    }

    companion object {

        private const val PREFERENCES_NAME = "weather_cache"
        private const val SNAPSHOT_KEY = "latest_snapshot"
    }
}

@Serializable
data class WeatherSnapshot(
    val weather: WeatherResponse,
    val forecast: ForecastResponse,
    val airPollution: AirPollutionResponse,
    val refreshedAtMillis: Long = System.currentTimeMillis()
)