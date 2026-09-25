package vn.edu.student.weatherviewingapp.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WeatherRefreshScheduler {

    private const val TAG = "WeatherRefreshScheduler"
    private const val UNIQUE_WORK_NAME = "weather-background-refresh"

    fun schedule(context: Context) {

        Log.d(TAG, "Starting background refresh scheduling")

        val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.MINUTES
            )
            .build()

        Log.d(TAG, "Periodic work configured:")
        Log.d(TAG, "Work name: $UNIQUE_WORK_NAME")
        Log.d(TAG, "Interval: 15 minutes")
        Log.d(TAG, "Network constraint: CONNECTED")
        Log.d(TAG, "Backoff policy: EXPONENTIAL")
        Log.d(TAG, "Initial backoff delay: 10 minutes")

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )

        Log.d(TAG, "Background refresh scheduled successfully")
    }
}