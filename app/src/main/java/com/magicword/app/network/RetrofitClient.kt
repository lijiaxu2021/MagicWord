package com.magicword.app.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.siliconflow.cn/v1/"
    // Note: In production, API Key should be secured
    private const val API_KEY = "sk-gxfzbsarwmnbmfeocgiozfpfmpbwqgaquxzirslobqjafmac"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Increased timeout
        .readTimeout(120, TimeUnit.SECONDS)    // Increased read timeout for long AI generation
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $API_KEY")
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
