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
    private const val GITHUB_OWNER = "lijiaxu2011" // Replace with actual owner
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
                val request = Request.Builder().url(LATEST_RELEASE_URL).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) return@withContext null
                
                val bodyStr = response.body()?.string() ?: return@withContext null
                val release = Gson().fromJson(bodyStr, ReleaseResponse::class.java)
                
                val latestVersion = release.tag_name.removePrefix("v")
                val hasUpdate = compareVersions(latestVersion, currentVersion) > 0
                
                // Find APK asset
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                val downloadUrl = if (apkAsset != null) {
                    // Convert original download URL to proxy URL
                    // Original: https://github.com/owner/repo/releases/download/v1.0.0/app.apk
                    // Proxy: https://mag.upxuu.com/releases/download/v1.0.0/app.apk
                    apkAsset.browser_download_url.replace("https://github.com", PROXY_BASE_URL)
                } else ""

                UpdateInfo(
                    version = latestVersion,
                    downloadUrl = downloadUrl,
                    releaseNotes = release.body,
                    hasUpdate = hasUpdate
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(parts1.size, parts2.size)

        for (i in 0 until length) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    suspend fun downloadApk(url: String, destination: File, onProgress: (Int) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) return@withContext false
                
                // Strictly use body() method to avoid Kotlin property access issues with package-private fields in some OkHttp versions
                val body = response.body() ?: return@withContext false
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
