package vn.edu.student.weatherviewingapp.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Stores and manages the list of favorite locations using SharedPreferences.
 */
class FavoriteLocationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun saveFavorites(favorites: List<LocationResult>) {
        val encoded = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(LocationResult.serializer()), favorites)
        preferences.edit().putString(FAVORITES_KEY, encoded).apply()
    }

    fun loadFavorites(): List<LocationResult> {
        val encoded = preferences.getString(FAVORITES_KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(LocationResult.serializer()), encoded)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "favorite_locations"
        private const val FAVORITES_KEY = "favorites_list"
    }
}
