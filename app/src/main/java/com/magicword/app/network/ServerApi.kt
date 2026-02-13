package com.magicword.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

data class ServerInitResponse(
    @SerializedName("defaultLibraryUrl") val defaultLibraryUrl: String?
)

data class VerifyKitRequest(
    val kitKey: String
)

data class VerifyKitResponse(
    val valid: Boolean,
    val apiKey: String?,
    val model: String?,
    val baseUrl: String?,
    val error: String?
)

interface ServerApi {
    @GET("api/init")
    suspend fun getInitConfig(): ServerInitResponse

    @POST("api/verify-kit")
    suspend fun verifyKit(@Body request: VerifyKitRequest): VerifyKitResponse
}
