package com.ai3dstudio.app

import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// Replace with your backend URL (e.g., Render or Heroku)
const val BASE_URL = "https://your-backend-app.onrender.com"

data class TaskResponse(val taskId: String)
data class StatusResponse(
    val status: String,
    val progress: Int,
    val modelUrl: String?,
    val error: String?
)

interface ApiService {
    @Multipart
    @POST("/api/models")
    suspend fun uploadImage(@Part image: MultipartBody.Part): TaskResponse

    @GET("/api/models/{taskId}")
    suspend fun checkStatus(@Path("taskId") taskId: String): StatusResponse
}

object RetrofitClient {
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
