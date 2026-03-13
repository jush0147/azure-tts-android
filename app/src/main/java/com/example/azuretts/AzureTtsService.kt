package com.example.azuretts

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeechService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AzureTtsService : TextToSpeechService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        if (lang == null) return android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
        val isSupported = (lang == "zh" || lang == "zho" || lang == "en" || lang == "eng")
        return if (isSupported) android.speech.tts.TextToSpeech.LANG_AVAILABLE else android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("zh", "TW", "")
    }

    override fun onStop() {
        TODO("Not yet implemented")
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        TODO("Not yet implemented")
    }
}
