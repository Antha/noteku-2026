package com.codeathome.todo.model

data class Item(
    val todo_id: Int,
    val id: Int,
    val name: String,
    val active_time: String,
    val period_type: String,
    val active_day: String?,
    val is_voice: Int,
    val voice_volume: Int,
    val is_vibra: Int,
    val is_active: Int
)