package vn.edu.student.weatherviewingapp.data

import java.util.concurrent.TimeUnit

enum class CacheFreshness { FRESH, STALE }

data class CacheStatus(
    val freshness: CacheFreshness,
    val ageMillis: Long
)

/** Defines when cached weather should be presented as out of date to the user. */
object WeatherCachePolicy {
    private val staleAfterMillis = TimeUnit.MINUTES.toMillis(30)

    fun getStatus(refreshedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): CacheStatus {
        val ageMillis = (nowMillis - refreshedAtMillis).coerceAtLeast(0L)
        val freshness = if (ageMillis >= staleAfterMillis) CacheFreshness.STALE else CacheFreshness.FRESH
        return CacheStatus(freshness, ageMillis)
    }
}
