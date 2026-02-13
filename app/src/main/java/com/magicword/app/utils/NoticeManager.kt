package com.magicword.app.utils

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NoticeManager {
    private const val NOTICE_URL = "https://mag.upxuu.com/notice.json"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class Notice(
        val title: String?,
        val content: String?,
        val versionCode: Int = 0,
        val timestamp: Long = 0
    ) {
        // ID logic: Use timestamp as ID string
        val id: String get() = timestamp.toString()
    }

    suspend fun checkNotice(context: Context): Notice? {
        val notice = fetchNotice() ?: return null
        
        // Validation: Must have title/content
        if (notice.title.isNullOrBlank() || notice.content.isNullOrBlank()) return null

        // Check if already shown
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val lastReadId = prefs.getString("last_read_notice_id", "")
        
        if (notice.id == lastReadId) {
            return null
        }
        
        return notice
    }

    suspend fun getLatestNotice(): Notice? {
        return fetchNotice()
    }

    private suspend fun fetchNotice(): Notice? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(NOTICE_URL).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) return@withContext null
                
                val bodyStr = response.body()?.string() ?: return@withContext null
                Gson().fromJson(bodyStr, Notice::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun markNoticeAsRead(context: Context, id: String) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("last_read_notice_id", id).apply()
    }
}
