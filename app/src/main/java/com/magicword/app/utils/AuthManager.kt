package com.magicword.app.utils

import android.content.Context
import androidx.core.content.edit

object AuthManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"

    fun saveUser(context: Context, userId: Int, username: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
        }
    }

    fun getUserId(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(KEY_USER_ID, -1)
    }

    fun getUsername(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_USERNAME, null)
    }

    fun logout(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            clear()
        }
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getUserId(context) != -1
    }

    private const val KEY_LAST_SYNC_TIME = "last_sync_time"

    fun saveLastSyncTime(context: Context, timestamp: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_SYNC_TIME, timestamp)
        }
    }

    fun getLastSyncTime(context: Context): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getLong(KEY_LAST_SYNC_TIME, 0)
    }
}
