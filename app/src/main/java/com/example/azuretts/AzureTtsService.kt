package com.example.azuretts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.audio.AudioOutputStream
import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Future

class AzureTtsService : TextToSpeechService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @Volatile
    private var speechConfig: SpeechConfig? = null

    @Volatile
    private var currentSynthesizer: SpeechSynthesizer? = null

    @Volatile
    private var currentSynthesisJob: Job? = null

    @Volatile
    private var speakingRate: Float = 1.0f

    @Volatile
    private var selectedVoice: String = "zh-TW-HsiaoChenNeural"

    private val supportedLanguages = setOf(
        Triple("zh", "TW", ""),
        Triple("en", "US", "")
    )

    override fun onCreate() {
        super.onCreate()
        val settingsStore = SettingsStore(applicationContext)
        serviceScope.launch {
            settingsStore.settingsFlow.collect { settings ->
                val old = speechConfig
                speakingRate = settings.speakingRate
                selectedVoice = settings.voice

                if (settings.apiKey.isNotBlank() && settings.region.isNotBlank()) {
                    val newConfig = SpeechConfig.fromSubscription(settings.apiKey, settings.region)
                    newConfig.speechSynthesisVoiceName = settings.voice
                    newConfig.setSpeechSynthesisOutputFormat(
                        SpeechSynthesisOutputFormat.Raw24Khz16BitMonoPcm
                    )
                    speechConfig = newConfig
                    old?.close()
                } else {
                    speechConfig = null
                    old?.close()
                }
            }
        }
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("zh", "TW", "")
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        if (lang == null) return TextToSpeech.LANG_NOT_SUPPORTED

        val exactMatch = Triple(lang, country ?: "", variant ?: "")
        if (exactMatch in supportedLanguages) {
            return when {
                !variant.isNullOrBlank() -> TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                !country.isNullOrBlank() -> TextToSpeech.LANG_COUNTRY_AVAILABLE
                else -> TextToSpeech.LANG_AVAILABLE
            }
        }

        val languageOnlyMatch = supportedLanguages.any { it.first == lang }
        return if (languageOnlyMatch) TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onStop() {
        currentSynthesizer?.stopSpeakingAsync()
        currentSynthesisJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        runBlocking { serviceJob.join() }
        speechConfig?.close()
        speechConfig = null
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) {
            callback?.error()
            return
        }

        currentSynthesisJob?.cancel()
        currentSynthesisJob = serviceScope.launch {
            val config = speechConfig
            if (config == null) {
                callback.error()
                return@launch
            }

            var localSynthesizer: SpeechSynthesizer? = null
            var speakFuture: Future<SpeechSynthesisResult>? = null
            var pullStream: PullAudioOutputStream? = null
            var audioConfig: AudioConfig? = null

            try {
                pullStream = AudioOutputStream.createPullStream()
                audioConfig = AudioConfig.fromStreamOutput(pullStream)
                localSynthesizer = SpeechSynthesizer(config, audioConfig)
                currentSynthesizer = localSynthesizer

                val text = request.charSequenceText?.toString().orEmpty()
                val ratePercent = ((speakingRate - 1.0f) * 100.0f).toInt()
                val ssml = """
                    <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis" xml:lang="en-US">
                      <voice name="$selectedVoice">
                        <prosody rate="${if (ratePercent >= 0) "+$ratePercent" else "$ratePercent"}%">${escapeForSsml(text)}</prosody>
                      </voice>
                    </speak>
                """.trimIndent()
                speakFuture = localSynthesizer.speakSsmlAsync(ssml)

                callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)

                val buffer = ByteArray(4096)
                var bytesRead: Long
                while (pullStream.read(buffer).also { bytesRead = it } > 0) {
                    if (!isActive) {
                        callback.error()
                        return@launch
                    }
                    callback.audioAvailable(buffer, 0, bytesRead.toInt())
                }
                callback.done()
            } catch (e: CancellationException) {
                callback.error()
                throw e
            } catch (e: Exception) {
                Log.e("AzureTTS", "Synthesis failed", e)
                callback.error()
            } finally {
                speakFuture?.cancel(true)
                localSynthesizer?.close()
                audioConfig?.close()
                pullStream?.close()

                if (currentSynthesizer === localSynthesizer) {
                    currentSynthesizer = null
                }
            }
        }
    }

    private fun escapeForSsml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
