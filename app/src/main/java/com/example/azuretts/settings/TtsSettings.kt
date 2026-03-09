package com.example.azuretts.settings

data class TtsSettings(
    val apiKey: String = "",
    val region: String = "",
    val voiceName: String = "zh-TW-HsiaoChenNeural",
    val rate: Float = 1.0f,
)
