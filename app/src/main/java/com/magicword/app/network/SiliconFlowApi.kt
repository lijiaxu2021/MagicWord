package com.magicword.app.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class AiRequest(
    val model: String = "deepseek-ai/DeepSeek-V3",
    val messages: List<Message>,
    val max_tokens: Int = 2048,
    val temperature: Double = 0.7
)

data class Message(val role: String, val content: String)

data class AiResponse(val choices: List<Choice>)
data class Choice(val message: Message)

interface SiliconFlowApi {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun chat(@Body request: AiRequest): AiResponse
}
