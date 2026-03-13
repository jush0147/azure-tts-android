package com.example.tts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class TtsSettings(
    val apiKey: String = "",
    val region: String = "eastus",
    val voice: String = "zh-TW-HsiaoChenNeural",
    val speed: Float = 1.0f
)

class SettingsManager(private val context: Context) {

    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val REGION = stringPreferencesKey("region")
        val VOICE = stringPreferencesKey("voice")
        val SPEED = floatPreferencesKey("speed")
    }

    val settingsFlow: Flow<TtsSettings> = context.dataStore.data
        .map { preferences ->
            TtsSettings(
                apiKey = preferences[API_KEY] ?: "",
                region = preferences[REGION] ?: "eastus",
                voice = preferences[VOICE] ?: "zh-TW-HsiaoChenNeural",
                speed = preferences[SPEED] ?: 1.0f
            )
        }

    suspend fun saveSettings(settings: TtsSettings) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = settings.apiKey
            preferences[REGION] = settings.region
            preferences[VOICE] = settings.voice
            preferences[SPEED] = settings.speed
        }
    }
}
