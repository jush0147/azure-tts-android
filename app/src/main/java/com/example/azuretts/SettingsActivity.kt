package com.example.azuretts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.azuretts.settings.TtsSettings
import com.example.azuretts.settings.TtsSettingsStore
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private lateinit var settingsStore: TtsSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsStore = TtsSettingsStore(applicationContext)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    settingsStore = settingsStore,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(settingsStore: TtsSettingsStore) {
    val settings by settingsStore.settingsFlow.collectAsState(initial = TtsSettings())
    val scope = rememberCoroutineScope()
    val voiceOptions = listOf(
        "zh-TW-HsiaoChenNeural",
        "zh-TW-YunJheNeural",
        "en-US-JennyNeural",
        "en-US-GuyNeural",
    )
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = settings.apiKey,
            onValueChange = { value ->
                scope.launch { settingsStore.updateApiKey(value) }
            },
            label = { Text("API Key") },
            singleLine = true,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = settings.region,
            onValueChange = { value ->
                scope.launch { settingsStore.updateRegion(value) }
            },
            label = { Text("Region") },
            singleLine = true,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = settings.voiceName,
            onValueChange = {},
            readOnly = true,
            label = { Text("發音人") },
            trailingIcon = { Text("▼") },
        )
        Text(
            text = "點擊下方選擇發音人",
            style = MaterialTheme.typography.bodySmall,
        )
        androidx.compose.material3.Button(onClick = { expanded = true }) {
            Text("選擇發音人")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            voiceOptions.forEach { voice ->
                DropdownMenuItem(
                    text = { Text(voice) },
                    onClick = {
                        expanded = false
                        scope.launch { settingsStore.updateVoiceName(voice) }
                    },
                )
            }
        }

        Text(text = "語速: ${"%.2f".format(settings.rate)}")
        Slider(
            value = settings.rate,
            onValueChange = { value ->
                scope.launch { settingsStore.updateRate(value) }
            },
            valueRange = 0.5f..2.0f,
        )
    }
}
