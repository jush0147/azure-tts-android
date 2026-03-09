package com.example.azuretts.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tts_settings")

class TtsSettingsStore(private val context: Context) {

    private object Keys {
        val ApiKey: Preferences.Key<String> = stringPreferencesKey("api_key")
        val Region: Preferences.Key<String> = stringPreferencesKey("region")
        val VoiceName: Preferences.Key<String> = stringPreferencesKey("voice_name")
        val Rate: Preferences.Key<Float> = floatPreferencesKey("rate")
    }

    val settingsFlow: Flow<TtsSettings> = context.dataStore.data.map { prefs ->
        TtsSettings(
            apiKey = prefs[Keys.ApiKey] ?: "",
            region = prefs[Keys.Region] ?: "",
            voiceName = prefs[Keys.VoiceName] ?: "zh-TW-HsiaoChenNeural",
            rate = prefs[Keys.Rate] ?: 1.0f,
        )
    }

    suspend fun updateApiKey(value: String) {
        context.dataStore.edit { it[Keys.ApiKey] = value }
    }

    suspend fun updateRegion(value: String) {
        context.dataStore.edit { it[Keys.Region] = value }
    }

    suspend fun updateVoiceName(value: String) {
        context.dataStore.edit { it[Keys.VoiceName] = value }
    }

    suspend fun updateRate(value: Float) {
        context.dataStore.edit { it[Keys.Rate] = value }
    }
}
