package com.codeathome.todo.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.codeathome.todo.R

class DestActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title")
        val message = intent.getStringExtra("message")
        val isVibra = intent.getIntExtra("is_vibra", 0)

        tts = TextToSpeech(this, this)

        val view = layoutInflater.inflate(R.layout.custom_dialog, null)
        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message

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
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
        }

        ttsReady = intent.getIntExtra("is_voice", 0) == 1


        val message = intent.getStringExtra("message") ?: ""
        startRepeatingVoice(message)
    }

    private fun startRepeatingVoice(text: String) {
        repeatRunnable = object : Runnable {
            override fun run() {
                if (ttsReady) {
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "repeatID")
                }

                handler.postDelayed(this, 3000)
            }
        }
        handler.post(repeatRunnable!!)
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
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
        val pattern = longArrayOf(
            0,   //
            500, //
            300, //
            500, //
            300, //
            700  //
        )

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
