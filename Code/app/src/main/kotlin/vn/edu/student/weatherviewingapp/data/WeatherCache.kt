package vn.edu.student.weatherviewingapp.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Stores the last complete weather response so background work and the UI share one snapshot. */
class WeatherCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun save(snapshot: WeatherSnapshot) {
        preferences.edit().putString(SNAPSHOT_KEY, json.encodeToString(WeatherSnapshot.serializer(), snapshot)).apply()
    }

    fun load(): WeatherSnapshot? = preferences.getString(SNAPSHOT_KEY, null)?.let { encoded ->
        runCatching { json.decodeFromString(WeatherSnapshot.serializer(), encoded) }.getOrNull()
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
