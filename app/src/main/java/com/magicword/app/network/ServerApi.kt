package com.magicword.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import com.magicword.app.data.Word

data class ServerInitConfig(
    @SerializedName("api_key") val apiKey: String,
    @SerializedName("model_name") val modelName: String,
    @SerializedName("default_library_url") val defaultLibraryUrl: String?
)

interface ServerApi {
    @GET("api/init-config")
    suspend fun getInitConfig(): ServerInitConfig
}
