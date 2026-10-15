package com.codeathome.todo.network

import com.codeathome.todo.model.Item
import com.codeathome.todo.model.Todo
import com.codeathome.todo.model.TodoPost
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("todos/{machine_id}")
    fun getAllTodo(
        @Path("machine_id") machine_id: String
    ): Call<Todo>

    @FormUrlEncoded
    @POST("todos")
    fun postTodo(
        @Field("name") name: String,
        @Field("color") color: String,
        @Field("machine_id") machine_id: String
    ): Call<TodoPost>

    @DELETE("todos/{id}")
    fun deletTodo(
        @Path("id") id: Int
    ): Call<Todo>

    @FormUrlEncoded
    @PUT("todos/{id}")
    fun updateTodo(
        @Path("id") id: Int,
        @Field("name") name: String,
        @Field("color") color: String
    ): Call<Todo>

    @FormUrlEncoded
    @POST("items")
    fun postTodoItem(
        @Field("name") name: String,
        @Field("active_time") active_time: String,
        @Field("todo_id") id: Int,
        @Field("period_type") period_type: String,
        @Field("active_day") active_day : String?,
        @Field("is_voice") is_voice : Int,
        @Field("voice_volume") voice_volume : Int,
        @Field("is_vibra") is_vibra : Int,
    ): Call<Item>

    @FormUrlEncoded
    @PUT("items/{id}")
    fun updateTodoItem(
        @Path("id") id: Int,
        @Field("active_time") active_time: String,
        @Field("name") name: String,
        @Field("period_type") period_type: String,
        @Field("active_day") active_day : String?,
        @Field("is_voice") is_voice : Int,
        @Field("voice_volume") voice_volume : Int,
        @Field("is_vibra") is_vibra : Int
    ): Call<Item>

    @FormUrlEncoded
    @PUT("items/is_active/{id}")
    fun updateTodoItemIsActive(
        @Path("id") id: Int,
        @Field("is_active") is_active : Int
    ): Call<Item>

    @DELETE("items/{id}")
    fun deleteTodoItem(
        @Path("id") id: Int
    ): Call<Item>


}