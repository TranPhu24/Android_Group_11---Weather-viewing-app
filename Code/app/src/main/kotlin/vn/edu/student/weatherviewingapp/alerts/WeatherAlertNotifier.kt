package vn.edu.student.weatherviewingapp.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import vn.edu.student.weatherviewingapp.R
import vn.edu.student.weatherviewingapp.data.WeatherSnapshot

object WeatherAlertNotifier {
    private const val CHANNEL_ID = "weather_alerts"
    private const val NOTIFICATION_ID = 1001
    private const val PREFERENCES_NAME = "weather_alert_state"
    private const val LAST_ALERT_SIGNATURE = "last_alert_signature"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Cảnh báo thời tiết",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo khi thời tiết đạt điều kiện bạn thiết lập"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyIfNeeded(context: Context, snapshot: WeatherSnapshot) {
        val settings = WeatherAlertSettingsStore(context).load()
        val weather = snapshot.weather
        val messages = buildList {
            if (settings.temperatureEnabled && weather.main.temp >= settings.temperatureThreshold) {
                add("Nhiệt độ ${weather.main.temp.toInt()}°C, từ ngưỡng ${settings.temperatureThreshold.toInt()}°C")
            }
            val windKmh = weather.wind.speed * 3.6
            if (settings.windEnabled && windKmh >= settings.windThresholdKmh) {
                add("Gió ${windKmh.toInt()} km/h, từ ngưỡng ${settings.windThresholdKmh.toInt()} km/h")
            }
            val condition = weather.weather.firstOrNull()?.main.orEmpty()
            if (settings.conditionEnabled && isSelectedCondition(condition, settings)) {
                add("Điều kiện thời tiết: ${weather.weather.firstOrNull()?.description ?: condition}")
            }
        }

        val state = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (messages.isEmpty()) {
            state.edit().remove(LAST_ALERT_SIGNATURE).apply()
            return
        }

        val signature = "${weather.cityName}:${messages.joinToString("|")}"
        if (state.getString(LAST_ALERT_SIGNATURE, null) == signature ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Cảnh báo thời tiết — ${weather.cityName}")
                .setContentText(messages.joinToString(" • "))
                .setStyle(NotificationCompat.BigTextStyle().bigText(messages.joinToString("\n")))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        )
        state.edit().putString(LAST_ALERT_SIGNATURE, signature).apply()
    }

    private fun isSelectedCondition(condition: String, settings: WeatherAlertSettings): Boolean = when (condition) {
        "Rain", "Drizzle" -> settings.alertRain
        "Thunderstorm" -> settings.alertThunderstorm
        "Snow" -> settings.alertSnow
        "Extreme", "Tornado", "Squall" -> settings.alertExtreme
        else -> false
    }
}
