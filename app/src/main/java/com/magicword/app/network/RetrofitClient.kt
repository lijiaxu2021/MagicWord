package com.magicword.app.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.util.concurrent.TimeUnit

import com.magicword.app.utils.AppConfig

object RetrofitClient {
    private const val BASE_URL = "https://api.siliconflow.cn/v1/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Increased timeout
        .readTimeout(120, TimeUnit.SECONDS)    // Increased read timeout for long AI generation
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            // Use API Key from AppConfig
            val apiKey = AppConfig.apiKey
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: SiliconFlowApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SiliconFlowApi::class.java)
    }
}
