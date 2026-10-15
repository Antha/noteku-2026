package com.codeathome.todo.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.codeathome.todo.R
import java.text.SimpleDateFormat

class DestActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title")
        val message = intent.getStringExtra("message")
        val isVibra = intent.getIntExtra("is_vibra", 0)
        val activeDay = intent.getStringExtra("active_day")
        val periodType = intent.getStringExtra("period_type")
        val year = intent.getIntExtra("year", 0)
        val month = intent.getIntExtra("month", 0)
        val day = intent.getIntExtra("day", 0)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)


        tts = TextToSpeech(this, this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val view = layoutInflater.inflate(R.layout.dialog_alarm, null)
        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message
        val dialogInfo = view.findViewById<TextView>(R.id.dialogInfo)

        val formattedTime = String.format("%02d:%02d", hour, minute)
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }

        val formattedDate = SimpleDateFormat(
            "MMMM d, yyyy",
            Locale.ENGLISH
        ).format(calendar.time)


        if (periodType == "specific") {
            dialogInfo.text =
                "📅 Scheduled for $formattedDate • $formattedTime"
        } else if (periodType == "daily") {
            dialogInfo.text =
                if (!activeDay.isNullOrEmpty() && activeDay != "-")
                    "🔄 Repeats on $activeDay • $formattedTime"
                else
                    "🔄 Repeats daily • $formattedTime"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        val btnOk = view.findViewById<Button>(R.id.btnDialog)
        btnOk.setOnClickListener {
            stopRepeatingVoice()
            stopLoopVibration()
            finish()
        }

        if (isVibra == 1) triggerVibration()

        dialog.show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set bahasa
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            // Pakai USAGE_ALARM supaya suara diarahkan ke stream alarm
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttrs)
        }

        ttsReady = intent.getIntExtra("is_voice", 0) == 1
        val message = intent.getStringExtra("message") ?: ""
        startRepeatingVoice(message)
    }

    private fun startRepeatingVoice(text: String) {
        repeatRunnable = object : Runnable {
            override fun run() {
                if (ttsReady) {
                    val voiceVolume = intent.getIntExtra("voice_volume", 5) // default 5
                    // Pastikan stream alarm dinaikkan
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, voiceVolume, 0)

                    // Speak tanpa Bundle volume (deprecated)
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alarmID")
                }
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(repeatRunnable!!)
    }

    private fun stopRepeatingVoice() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 300, 500, 300, 700)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopLoopVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel()
    }
}
