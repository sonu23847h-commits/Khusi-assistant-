package com.khushi.assistant

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceAssistantService : Service(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private var awaitingCommand = false
    private var isTtsReady = false
    private val handler = Handler(Looper.getMainLooper())

    private val channelId = "khushi_channel"
    private val notificationId = 42

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForeground(notificationId, buildNotification("Khushi is listening..."))
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val indianEnglish = Locale("en", "IN")
            val result = tts.setLanguage(indianEnglish)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale("hi", "IN"))
            }

            tts.voices?.firstOrNull { voice ->
                voice.locale.country == "IN" &&
                    (voice.name.contains("female", ignoreCase = true) ||
                     voice.name.contains("#female", ignoreCase = true))
            }?.let { tts.voice = it }

            tts.setPitch(1.15f)
            tts.setSpeechRate(1.0f)
            isTtsReady = true

            speak("Namaste! Main Khushi hoon, aapki voice assistant. Main taiyar hoon.")
        }
    }

    private fun speak(text: String) {
        if (!isTtsReady) return
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { pauseListening() }
            override fun onDone(utteranceId: String?) { resumeListening() }
            override fun onError(utteranceId: String?) { resumeListening() }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "khushi_utterance")
        updateNotification(text)
    }

    private fun startListening() {
        if (speechRecognizer != null) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val heard = matches?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""
                    handleHeardText(heard)
                    restartListening()
                }

                override fun onError(error: Int) {
                    restartListening()
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        beginRecognition()
    }

    private fun beginRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            restartListening()
        }
    }

    private fun restartListening() {
        handler.postDelayed({ beginRecognition() }, 400)
    }

    private fun pauseListening() {
        speechRecognizer?.stopListening()
    }

    private fun resumeListening() {
        handler.postDelayed({ beginRecognition() }, 300)
    }

    private fun handleHeardText(text: String) {
        if (text.isBlank()) return

        if (!awaitingCommand) {
            if (text.contains("khushi")) {
                awaitingCommand = true
                speak("Haan, boliye. Main sun rahi hoon.")
            }
            return
        }

        awaitingCommand = false
        val reply = CommandProcessor.process(this, text)
        speak(reply)
    }

    private fun buildNotification(content: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Khushi Assistant", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Khushi")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, buildNotification(content))
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
