package com.example.azuretts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tts_settings")

data class TtsSettings(
    val apiKey: String = "",
    val region: String = "",
    val voice: String = "zh-TW-HsiaoChenNeural",
    val speakingRate: Float = 1.0f
)

class SettingsStore(private val context: Context) {

    val settingsFlow: Flow<TtsSettings> = context.settingsDataStore.data.map { preferences ->
        TtsSettings(
            apiKey = preferences[KEY_API_KEY].orEmpty(),
            region = preferences[KEY_REGION].orEmpty(),
            voice = preferences[KEY_VOICE] ?: "zh-TW-HsiaoChenNeural",
            speakingRate = preferences[KEY_RATE] ?: 1.0f
        )
    }

    suspend fun updateApiKey(value: String) {
        context.settingsDataStore.edit { it[KEY_API_KEY] = value }
    }

    suspend fun updateRegion(value: String) {
        context.settingsDataStore.edit { it[KEY_REGION] = value }
    }

    suspend fun updateVoice(value: String) {
        context.settingsDataStore.edit { it[KEY_VOICE] = value }
    }

    suspend fun updateSpeakingRate(value: Float) {
        context.settingsDataStore.edit { it[KEY_RATE] = value }
    }

    private companion object {
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_REGION = stringPreferencesKey("region")
        val KEY_VOICE = stringPreferencesKey("voice")
        val KEY_RATE = floatPreferencesKey("rate")
    }
}
