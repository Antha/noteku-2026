package com.codeathome.todo

import java.util.*

object MyLibrary{
    val colors = mapOf(
        "Red" to "#FFB3B3",       // Pastel Red
        "Green" to "#C8E6C9",     // Pastel Green
        "Blue" to "#9FA8DA",      // Pastel Blue
        "Pink" to "#FFD1DC",      // Pastel Pink
        "Orange" to "#FFE0B2",    // Pastel Orange
        "Chocolate" to "#FFDAB9"
    )

    fun getCustomTime(year: Int,month: Int,day: Int,hour: Int,minute: Int): Long {
        val calendar = Calendar.getInstance()
        val month = month - 1
        calendar.set(year,month,day,hour,minute,0)
        return calendar.timeInMillis
    }
}