package vn.edu.student.weatherviewingapp.alerts

import android.content.Context

data class WeatherAlertSettings(
    val temperatureEnabled: Boolean = false,
    val temperatureThreshold: Double = 35.0,
    val windEnabled: Boolean = false,
    val windThresholdKmh: Double = 40.0,
    val conditionEnabled: Boolean = false,
    val alertRain: Boolean = true,
    val alertThunderstorm: Boolean = true,
    val alertSnow: Boolean = true,
    val alertExtreme: Boolean = true
)

class WeatherAlertSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load() = WeatherAlertSettings(
        temperatureEnabled = preferences.getBoolean(TEMPERATURE_ENABLED, false),
        temperatureThreshold = preferences.getString(TEMPERATURE_THRESHOLD, "35")!!.toDoubleOrNull() ?: 35.0,
        windEnabled = preferences.getBoolean(WIND_ENABLED, false),
        windThresholdKmh = preferences.getString(WIND_THRESHOLD, "40")!!.toDoubleOrNull() ?: 40.0,
        conditionEnabled = preferences.getBoolean(CONDITION_ENABLED, false),
        alertRain = preferences.getBoolean(ALERT_RAIN, true),
        alertThunderstorm = preferences.getBoolean(ALERT_THUNDERSTORM, true),
        alertSnow = preferences.getBoolean(ALERT_SNOW, true),
        alertExtreme = preferences.getBoolean(ALERT_EXTREME, true)
    )

    fun save(settings: WeatherAlertSettings) {
        preferences.edit()
            .putBoolean(TEMPERATURE_ENABLED, settings.temperatureEnabled)
            .putString(TEMPERATURE_THRESHOLD, settings.temperatureThreshold.toString())
            .putBoolean(WIND_ENABLED, settings.windEnabled)
            .putString(WIND_THRESHOLD, settings.windThresholdKmh.toString())
            .putBoolean(CONDITION_ENABLED, settings.conditionEnabled)
            .putBoolean(ALERT_RAIN, settings.alertRain)
            .putBoolean(ALERT_THUNDERSTORM, settings.alertThunderstorm)
            .putBoolean(ALERT_SNOW, settings.alertSnow)
            .putBoolean(ALERT_EXTREME, settings.alertExtreme)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "weather_alert_settings"
        private const val TEMPERATURE_ENABLED = "temperature_enabled"
        private const val TEMPERATURE_THRESHOLD = "temperature_threshold"
        private const val WIND_ENABLED = "wind_enabled"
        private const val WIND_THRESHOLD = "wind_threshold"
        private const val CONDITION_ENABLED = "condition_enabled"
        private const val ALERT_RAIN = "alert_rain"
        private const val ALERT_THUNDERSTORM = "alert_thunderstorm"
        private const val ALERT_SNOW = "alert_snow"
        private const val ALERT_EXTREME = "" +
                "alert_extreme"
    }
}