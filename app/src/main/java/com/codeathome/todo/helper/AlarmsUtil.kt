package com.codeathome.todo.helper

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmUtils {
    fun getNextDayTriggerTime(
        hour: Int,
        minute: Int,
        activeDay: String
    ): Long {

        if (activeDay.isBlank()) {
            return -1L
        }

        val dayMap = mapOf(
            "sun" to Calendar.SUNDAY,
            "mon" to Calendar.MONDAY,
            "tue" to Calendar.TUESDAY,
            "wed" to Calendar.WEDNESDAY,
            "thu" to Calendar.THURSDAY,
            "fri" to Calendar.FRIDAY,
            "sat" to Calendar.SATURDAY
        )

        val activeDays = activeDay
            .split(",")
            .mapNotNull {
                dayMap[it.trim().lowercase()]
            }

        if (activeDays.isEmpty()) {
            return -1L
        }

        val now = Calendar.getInstance()

        for (i in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (
                candidate.get(Calendar.DAY_OF_WEEK) in activeDays &&
                candidate.timeInMillis > now.timeInMillis
            ) {
                return candidate.timeInMillis
            }
        }

        return -1L
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(
        context: Context,
        triggerTime: Long,
        notificationId: Int,
        originalIntent: Intent
    ) {

        if (triggerTime <= 0L) {
            return
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            originalIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}