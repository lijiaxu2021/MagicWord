package com.magicword.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.magicword.app.data.AppDatabase
import com.magicword.app.network.SyncClient
import com.magicword.app.network.SyncPushRequest
import com.magicword.app.utils.AuthManager
import com.magicword.app.utils.LogUtil
import kotlin.math.max

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = AuthManager.getUserId(applicationContext)
        if (userId == -1) {
            return Result.success()
        }

        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.wordDao()
            val allWords = dao.getAllWordsList()

            // 1. Push
            if (allWords.isNotEmpty()) {
                try {
                    val pushResponse = SyncClient.api.pushWords(SyncPushRequest(userId, allWords))
                    if (!pushResponse.isSuccessful) {
                         LogUtil.logFeature("Sync", "PushFailed", "Code: ${pushResponse.code()}")
                         return Result.retry()
                    }
                } catch (e: Exception) {
                    LogUtil.logFeature("Sync", "PushError", e.message ?: "Net Error")
                    return Result.retry()
                }
            }

            // 2. Pull
            try {
                val pullResponse = SyncClient.api.pullWords(userId)
                if (pullResponse.isSuccessful) {
                    val remoteWords = pullResponse.body()?.words ?: emptyList()
                    remoteWords.forEach { remoteWord ->
                        val existing = dao.getWordByText(remoteWord.word, 1)
                        if (existing == null) {
                            dao.insertWord(remoteWord.copy(id = 0, libraryId = 1))
                        } else {
                            dao.updateWord(existing.copy(
                                reviewCount = max(existing.reviewCount, remoteWord.reviewCount),
                                lastReviewTime = max(existing.lastReviewTime, remoteWord.lastReviewTime)
                            ))
                        }
                    }
                    LogUtil.logFeature("Sync", "Success", "Pulled ${remoteWords.size}")
                    
                    // Update Last Sync Time
                    AuthManager.saveLastSyncTime(applicationContext, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                LogUtil.logFeature("Sync", "PullError", e.message ?: "Net Error")
                return Result.retry()
            }

            Result.success()
        } catch (e: Exception) {
            LogUtil.logFeature("Sync", "FatalError", e.message ?: "Unknown")
            Result.failure()
        }
    }
}
