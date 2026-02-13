package com.magicword.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SyncClient {
    private const val BASE_URL = "https://mag.upxuu.com/"

    val api: MagicWordApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MagicWordApi::class.java)
    }
}
