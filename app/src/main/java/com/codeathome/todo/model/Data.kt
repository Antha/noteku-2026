package com.codeathome.todo.model

data class Data(
    val items: List<Item>,
    val id: Int,
    val name: String,
    val color: String,
    val created_at: String
)