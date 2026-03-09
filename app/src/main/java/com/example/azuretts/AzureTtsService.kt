package com.example.azuretts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.example.azuretts.settings.TtsSettingsStore
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream
import java.util.concurrent.Future
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AzureTtsService : TextToSpeechService() {

    private val supportedLanguages = setOf(
        Triple("zh", "TW", ""),
        Triple("en", "US", ""),
    )

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    @Volatile
    private var speechConfig: SpeechConfig? = null

    @Volatile
    private var currentSynthesizer: SpeechSynthesizer? = null

    @Volatile
    private var currentSynthesisJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        val settingsStore = TtsSettingsStore(applicationContext)
        serviceScope.launch {
            settingsStore.settingsFlow.collectLatest { settings ->
                speechConfig?.close()

                if (settings.apiKey.isNotBlank() && settings.region.isNotBlank()) {
                    speechConfig = SpeechConfig.fromSubscription(settings.apiKey, settings.region)
                    speechConfig?.setSpeechSynthesisOutputFormat(
                        SpeechSynthesisOutputFormat.Raw24Khz16BitMonoPcm,
                    )
                    speechConfig?.speechSynthesisVoiceName = settings.voiceName
                    speechConfig?.setProperty("SpeechSynthesis_Rate", settings.rate.toString())
                } else {
                    speechConfig = null
                }
            }
        }
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        if (lang == null || country == null) {
            return TextToSpeech.LANG_NOT_SUPPORTED
        }

        return if (supportedLanguages.any { it.first == lang && it.second == country }) {
            TextToSpeech.LANG_COUNTRY_AVAILABLE
        } else {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("zh", "TW", "")
    }

    override fun onStop() {
        currentSynthesizer?.stopSpeakingAsync()
        currentSynthesisJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        speechConfig?.close()
        speechConfig = null
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
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
                pullStream = PullAudioOutputStream.create()
                audioConfig = AudioConfig.fromStreamOutput(pullStream)
                localSynthesizer = SpeechSynthesizer(config, audioConfig)
                currentSynthesizer = localSynthesizer

                speakFuture = localSynthesizer.speakTextAsync(request.charSequenceText.toString())

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
}
