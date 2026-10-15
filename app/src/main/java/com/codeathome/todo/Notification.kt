package com.codeathome.todo

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codeathome.todo.helper.AlarmUtils.getNextDayTriggerTime
import com.codeathome.todo.helper.AlarmUtils.scheduleNotification
import com.codeathome.todo.ui.DestActivity
import com.codeathome.todo.ui.PopupActivity

const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"
const val isVoiceExtra = "isVoiceExtra"
const val voiceVolumeExtra = "voiceVolumeExtra"
const val isVibraExtra = "isVibraExtra"
const val periodTypeExtra = "periodTypeExtra"
const val activeDayExtra = "activeDayExtra"
const val yearExtra = "yearExtra"
const val monthExtra = "monthExtra"
const val dayExtra = "dayExtra"
const val hourExtra = "hourExtra"
const val minutExtra = "minutExtra"

class Notification : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(titleExtra)
        val message = intent.getStringExtra(messageExtra)
        val is_voice = intent.getIntExtra(isVoiceExtra, 0)
        val voice_volume = intent.getIntExtra(voiceVolumeExtra, 0)
        val is_vibra = intent.getIntExtra(isVibraExtra, 0)
        val period_type = intent.getStringExtra(periodTypeExtra)
        val active_day = intent.getStringExtra(activeDayExtra) ?: ""
        val year = intent.getIntExtra(yearExtra, 0)
        val month = intent.getIntExtra(monthExtra, 0)
        val day = intent.getIntExtra(dayExtra, 0)
        val hour = intent.getIntExtra(hourExtra, 0)
        val minute = intent.getIntExtra(minutExtra, 0)
        val notificationID = intent.getIntExtra("notificationID", 0)

        val fullScreenIntent = Intent(context, DestActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("title", title)
            putExtra("message", message)
            putExtra("is_voice", is_voice)
            putExtra("is_vibra", is_vibra)
            putExtra("voice_volume", voice_volume)
            putExtra("period_type", period_type)
            putExtra("active_day", active_day)
            putExtra("year", year)
            putExtra("month", month)
            putExtra("day", day)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationID,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_note_ku_app)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationID, notification)

        if (!active_day.isNotEmpty() && active_day != "-") {
            val nextTime =
                getNextDayTriggerTime(
                    hour,
                    minute,
                    active_day
                )

            if (nextTime > 0) {
                scheduleNotification(
                    context,
                    nextTime,
                    notificationID,
                    intent
                )
            }
        }
    }
}
