package com.example.tts

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.audio.PullAudioOutputStream
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import android.media.AudioFormat
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException

class AzureTtsService : TextToSpeechService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var settingsManager: SettingsManager

    @Volatile private var speechConfig: SpeechConfig? = null
    @Volatile private var currentSynthesizer: SpeechSynthesizer? = null
    @Volatile private var currentSynthesisJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(applicationContext)

        serviceScope.launch {
            settingsManager.settingsFlow.collect { settings ->
                val old = speechConfig

                if (settings.apiKey.isNotBlank() && settings.region.isNotBlank()) {
                    val newConfig = SpeechConfig.fromSubscription(settings.apiKey, settings.region)
                    newConfig.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Raw24Khz16BitMonoPcm)
                    newConfig.speechSynthesisVoiceName = settings.voice
                    // Note: Settings speed is missing from Azure configuration here, but this setup ensures connectivity
                    speechConfig = newConfig
                    old?.close()
                } else {
                    speechConfig = null
                    old?.close()
                }
            }
        }
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        if (lang == "zho" || lang == "zh" || lang == "eng" || lang == "en") {
            return TextToSpeech.LANG_AVAILABLE
        }
        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("zho", "TW", "")
    }

    override fun onStop() {
        currentSynthesizer?.stopSpeakingAsync()
        currentSynthesisJob?.cancel()
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return

        currentSynthesisJob?.cancel()
        currentSynthesisJob = serviceScope.launch {
            val config = speechConfig
            if (config == null) {
                callback.error()
                return@launch
            }

            var localSynthesizer: SpeechSynthesizer? = null
            var speakFuture: java.util.concurrent.Future<SpeechSynthesisResult>? = null
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

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        runBlocking { serviceJob.join() }
        speechConfig?.close()
        speechConfig = null
    }
}
