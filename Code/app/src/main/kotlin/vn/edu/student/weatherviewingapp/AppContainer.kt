package vn.edu.student.weatherviewingapp

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import vn.edu.student.weatherviewingapp.data.FavoriteLocationStore
import vn.edu.student.weatherviewingapp.data.WeatherApi
import vn.edu.student.weatherviewingapp.data.WeatherCache
import vn.edu.student.weatherviewingapp.repository.WeatherRepository
import vn.edu.student.weatherviewingapp.repository.WeatherSyncManager

class AppContainer(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    // Interceptor to inject API Key and language automatically
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val url = originalUrl.newBuilder()
            .addQueryParameter("appid", BuildConfig.WEATHER_API_KEY)
            .addQueryParameter("lang", "vi")
            .build()
        val requestBuilder = originalRequest.newBuilder().url(url)
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val weatherApi: WeatherApi by lazy {
        retrofit.create(WeatherApi::class.java)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(weatherApi)
    }

    val weatherCache: WeatherCache by lazy {
        WeatherCache(context)
    }

    val favoriteStore: FavoriteLocationStore by lazy {
        FavoriteLocationStore(context)
    }
    
    val weatherSyncManager: WeatherSyncManager by lazy {
        WeatherSyncManager(weatherRepository, weatherCache, context)
    }
}
