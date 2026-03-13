package com.example.azuretts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = SettingsStore(applicationContext)
        setContent {
            MaterialTheme {
                SettingsScreen(settingsStore = settingsStore)
            }
        }
    }
}

@Composable
private fun SettingsScreen(settingsStore: SettingsStore) {
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = TtsSettings())
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var voice by remember { mutableStateOf("zh-TW-HsiaoChenNeural") }
    var speakingRate by remember { mutableStateOf(1.0f) }

    LaunchedEffect(settings) {
        apiKey = settings.apiKey
        region = settings.region
        voice = settings.voice
        speakingRate = settings.speakingRate
    }

    val voices = listOf(
        "zh-TW-HsiaoChenNeural",
        "zh-TW-YunJheNeural",
        "en-US-JennyNeural",
        "en-US-GuyNeural"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiKey,
            onValueChange = {
                apiKey = it
                scope.launch { settingsStore.updateApiKey(it) }
            },
            label = { Text("API Key") }
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = region,
            onValueChange = {
                region = it
                scope.launch { settingsStore.updateRegion(it) }
            },
            label = { Text("Region") }
        )

        VoiceSelector(
            voices = voices,
            selectedVoice = voice,
            onVoiceSelected = {
                voice = it
                scope.launch { settingsStore.updateVoice(it) }
            }
        )

        Text("語速: ${"%.2f".format(speakingRate)}")
        Slider(
            value = speakingRate,
            onValueChange = {
                speakingRate = it
                scope.launch { settingsStore.updateSpeakingRate(it) }
            },
            valueRange = 0.5f..2.0f,
            steps = 14
        )
    }
}

@Composable
private fun VoiceSelector(
    voices: List<String>,
    selectedVoice: String,
    onVoiceSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("發音人")
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { expanded = true }) {
                Text(selectedVoice)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                voices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice) },
                        onClick = {
                            onVoiceSelected(voice)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
