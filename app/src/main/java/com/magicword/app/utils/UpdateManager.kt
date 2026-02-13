package com.magicword.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import com.magicword.app.BuildConfig

object UpdateManager {
    private const val PROXY_BASE_URL = "https://mag.upxuu.com"
    private const val GITHUB_OWNER = "lijiaxu2021" // Replace with actual owner
    private const val GITHUB_REPO = "MagicWord" // Replace with actual repo name
    
    // API endpoint via proxy: https://mag.upxuu.com/api/repos/{owner}/{repo}/releases/latest
    private val LATEST_RELEASE_URL = "$PROXY_BASE_URL/api/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    data class UpdateInfo(
        val version: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val hasUpdate: Boolean
    )

    suspend fun checkUpdate(currentVersion: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // Use notice.json for version check as it is more reliable and faster
                // https://mag.upxuu.com/notice.json
                val noticeUrl = "$PROXY_BASE_URL/notice.json?t=${System.currentTimeMillis()}"
                val request = Request.Builder().url(noticeUrl).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) return@withContext null
                
                val bodyStr = response.body?.string() ?: return@withContext null
                val notice = Gson().fromJson(bodyStr, NoticeResponse::class.java)
                
                // Compare version codes if available, or fallback to name
                // Current app version code is available in BuildConfig.VERSION_CODE
                val currentCode = com.magicword.app.BuildConfig.VERSION_CODE
                val hasUpdate = notice.versionCode > currentCode
                
                val downloadUrl = "$PROXY_BASE_URL/MagicWordLatest.apk"

                if (hasUpdate) {
                    UpdateInfo(
                        version = "New Version (${notice.versionCode})",
                        downloadUrl = downloadUrl,
                        releaseNotes = notice.content ?: "New update available",
                        hasUpdate = true
                    )
                } else {
                     null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private data class NoticeResponse(
        val title: String?,
        val content: String?,
        val versionCode: Int,
        val timestamp: Long
    )

    suspend fun downloadApk(url: String, destination: File, onProgress: (Int) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) return@withContext false
                
                // Strictly use body() method to avoid Kotlin property access issues with package-private fields in some OkHttp versions
                val body = response.body
                if (body == null) return@withContext false
                val totalLength = body.contentLength()
                
                body.byteStream().use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead: Long = 0
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalLength > 0) {
                                onProgress((totalRead * 100 / totalLength).toInt())
                            }
                        }
                        onProgress(100)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun installApk(context: Context, file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private data class ReleaseResponse(
        val tag_name: String,
        val body: String,
        val assets: List<Asset>
    )

    private data class Asset(
        val name: String,
        val browser_download_url: String
    )
}
