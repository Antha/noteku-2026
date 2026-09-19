package com.codeathome.todo.model

data class Todo(
    val `data`: List<Data>,
    val message: String
)

data class TodoPost(
    val `data`: Data,
    val message: String
)