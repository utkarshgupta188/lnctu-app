package com.meow.lnctattendance.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "attendance_prefs"

val Context.dataStore by preferencesDataStore(name = STORE_NAME)

object PrefKeys {
    val LAST_USERNAME: Preferences.Key<String> = stringPreferencesKey("last_username")
    val LAST_BASE_URL: Preferences.Key<String> = stringPreferencesKey("last_base_url")
    val LAST_PASSWORD: Preferences.Key<String> = stringPreferencesKey("last_password")
    val LAST_LOGIN_AT: Preferences.Key<Long> = longPreferencesKey("last_login_at")
    val DARK_MODE: Preferences.Key<Boolean> = booleanPreferencesKey("dark_mode")
}

data class LastLogin(
    val baseUrl: String,
    val username: String,
    val password: String,
    val timestampMillis: Long,
)

sealed interface AuthState {
    data object Loading : AuthState
    data object None : AuthState
    data class Authenticated(val login: LastLogin) : AuthState
}

class PreferencesManager(private val context: Context) {
    val authState: Flow<AuthState> = context.dataStore.data.map { prefs ->
        val username = prefs[PrefKeys.LAST_USERNAME]
        val password = prefs[PrefKeys.LAST_PASSWORD]
        
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            AuthState.None
        } else {
            AuthState.Authenticated(
                LastLogin(
                    baseUrl = prefs[PrefKeys.LAST_BASE_URL] ?: "",
                    username = username,
                    password = password,
                    timestampMillis = prefs[PrefKeys.LAST_LOGIN_AT] ?: 0L
                )
            )
        }
    }

    // Emits null until first read (meaning "use system default")
    val darkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[PrefKeys.DARK_MODE]
    }

    suspend fun saveLastLogin(baseUrl: String, username: String, password: String, timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.LAST_USERNAME] = username
            prefs[PrefKeys.LAST_BASE_URL] = baseUrl
            prefs[PrefKeys.LAST_PASSWORD] = password
            prefs[PrefKeys.LAST_LOGIN_AT] = timestampMillis
        }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.DARK_MODE] = dark
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            // Only clear auth keys, preserve settings like dark mode
            prefs.remove(PrefKeys.LAST_USERNAME)
            prefs.remove(PrefKeys.LAST_BASE_URL)
            prefs.remove(PrefKeys.LAST_PASSWORD)
            prefs.remove(PrefKeys.LAST_LOGIN_AT)
        }
    }
}
