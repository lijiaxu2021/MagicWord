package com.magicword.app.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object LogUtil {
    private const val TAG = "LogUtil"
    private var isLogEnabled = true
    private var context: Context? = null
    
    // Memory buffer for fallback (Ring buffer simulation)
    private const val MAX_MEMORY_LOGS = 200
    private val memoryLogBuffer = ConcurrentLinkedQueue<String>()
    
    // File writing channel to offload main thread
    private val logChannel = Channel<String>(capacity = 500, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun init(ctx: Context) {
        context = ctx.applicationContext
        // Start the log consumer coroutine
        GlobalScope.launch(Dispatchers.IO) {
            for (logMsg in logChannel) {
                writeLogToFile(logMsg)
            }
        }
        
        // Load enabled state from prefs
        val prefs = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        isLogEnabled = prefs.getBoolean("enable_log", true)
    }
    
    fun setLogEnabled(enabled: Boolean) {
        isLogEnabled = enabled
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("enable_log", enabled)
            ?.apply()
    }

    fun isLogEnabled(): Boolean = isLogEnabled

    fun d(tag: String, msg: String) = log("DEBUG", tag, msg)
    fun logDebug(tag: String, msg: String) = d(tag, msg) // Alias
    fun i(tag: String, msg: String) = log("INFO", tag, msg)
    fun w(tag: String, msg: String) = log("WARN", tag, msg)
    fun e(tag: String, msg: String, tr: Throwable? = null) = log("ERROR", tag, msg + (tr?.let { "\n${Log.getStackTraceString(it)}" } ?: ""))
    fun logError(tag: String, msg: String, tr: Throwable? = null) = e(tag, msg, tr) // Alias for clearer intent

    fun logFeature(featureName: String, status: String, extraJson: String = "{}") {
        if (!isLogEnabled) return
        val msg = "FEATURE|$featureName|$status|$extraJson"
        log("FEATURE", "FeatureTrace", msg)
    }

    private fun log(level: String, tag: String, msg: String) {
        if (!isLogEnabled) return

        val threadName = Thread.currentThread().name
        val stackTrace = Thread.currentThread().stackTrace
        // Find the caller (skip LogUtil methods)
        val caller = stackTrace.firstOrNull { it.className != LogUtil::class.java.name && it.className != Thread::class.java.name }
        
        val className = caller?.className ?: "Unknown"
        val methodName = caller?.methodName ?: "Unknown"
        val lineNumber = caller?.lineNumber ?: 0
        
        val timestamp = dateFormat.format(Date())
        val formattedLog = "$timestamp|$threadName|$level|$tag|$className|$methodName|$lineNumber|$msg"

        // Log to Android Logcat as well
        when(level) {
            "DEBUG" -> Log.d(tag, msg)
            "INFO" -> Log.i(tag, msg)
            "WARN" -> Log.w(tag, msg)
            "ERROR" -> Log.e(tag, msg)
        }

        // Send to file writer channel (non-blocking for main thread)
        logChannel.trySend(formattedLog)
    }

    private suspend fun writeLogToFile(logMsg: String) {
        val ctx = context ?: return
        
        try {
            val logDir = getLogDir(ctx) ?: return
            
            // Clean old logs if disk full (simulation or simple check) or periodically
            // Ideally we check disk space here, but for simplicity let's just clean old logs
            cleanOldLogs(logDir)

            val dateStr = fileDateFormat.format(Date())
            val logFile = File(logDir, "$dateStr.log")

            FileWriter(logFile, true).use { writer ->
                writer.append(logMsg).append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log to file", e)
            // Fallback to memory buffer
            if (memoryLogBuffer.size >= MAX_MEMORY_LOGS) {
                memoryLogBuffer.poll()
            }
            memoryLogBuffer.add(logMsg)
        }
    }

    private fun getLogDir(ctx: Context): File? {
        // Primary: External Files Dir
        var dir = ctx.getExternalFilesDir("logs")
        if (dir == null || !dir.exists()) {
            // Fallback: Internal Files Dir
            dir = File(ctx.filesDir, "logs_fallback")
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
        return dir
    }

    private fun cleanOldLogs(logDir: File) {
        try {
            val files = logDir.listFiles { _, name -> name.endsWith(".log") } ?: return
            if (files.size > 7) {
                files.sortBy { it.lastModified() }
                // Delete oldest until we have 7 or fewer
                for (i in 0 until (files.size - 7)) {
                    files[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old logs", e)
        }
    }
    
    fun getAllLogFiles(): List<File> {
        val ctx = context ?: return emptyList()
        val dir = getLogDir(ctx) ?: return emptyList()
        return dir.listFiles { _, name -> name.endsWith(".log") }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }
}
