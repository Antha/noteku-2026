package com.codeathome.todo.ui

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.codeathome.todo.R

class PopupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("titleExtra")
        val message = intent.getStringExtra("messageExtra")

        val view = layoutInflater.inflate(R.layout.dialog_alarm, null)
        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("OK") { d, _ -> d.dismiss(); finish() }
            .setCancelable(false)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            background = ContextCompat.getDrawable(context, R.drawable.button_purple)
            setTextColor(Color.WHITE)
        }
    }
}
