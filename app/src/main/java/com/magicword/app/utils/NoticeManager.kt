package com.magicword.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NoticeManager {
    private const val PROXY_BASE_URL = "https://mag.upxuu.com"
    private const val NOTICE_URL = "$PROXY_BASE_URL/notice.json"
    private const val PREF_KEY_LAST_NOTICE_ID = "last_notice_id"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class Notice(
        val id: Int,
        val title: String,
        val content: String
    )

    suspend fun checkNotice(context: Context): Notice? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(NOTICE_URL).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                val bodyStr = response.body()?.string() ?: return@withContext null
                val notice = Gson().fromJson(bodyStr, Notice::class.java)

                if (notice != null) {
                    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    val lastId = prefs.getInt(PREF_KEY_LAST_NOTICE_ID, -1)
                    if (notice.id > lastId) {
                        return@withContext notice
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    // Fetch latest notice regardless of ID (for Settings screen)
    suspend fun getLatestNotice(): Notice? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(NOTICE_URL).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body()?.string() ?: return@withContext null
                Gson().fromJson(bodyStr, Notice::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun markNoticeAsRead(context: Context, noticeId: Int) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putInt(PREF_KEY_LAST_NOTICE_ID, noticeId).apply()
    }
}
