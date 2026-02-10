package com.magicword.app.network

import com.magicword.app.data.Word
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AuthRequest(val username: String, val password: String)
data class AuthResponse(val message: String, val user_id: Int?, val error: String?)

data class SyncPushRequest(val user_id: Int, val words: List<Word>)
data class SyncPushResponse(val message: String, val synced_count: Int)

data class SyncPullResponse(val words: List<Word>)

interface MagicWordApi {
    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("sync/push")
    suspend fun pushWords(@Body request: SyncPushRequest): Response<SyncPushResponse>

    @GET("sync/pull")
    suspend fun pullWords(@Query("user_id") userId: Int): Response<SyncPullResponse>
}
