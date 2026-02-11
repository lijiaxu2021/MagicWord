package com.magicword.app.utils

import android.content.Context
import android.content.SharedPreferences

object AppConfig {
    private const val PREF_NAME = "app_config"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL_NAME = "model_name"
    private const val KEY_SERVER_URL = "server_url"
    
    private lateinit var prefs: SharedPreferences
    
    // Default fallback (though we want to avoid hardcoding, these are just safe defaults if config fails)
    var apiKey: String = ""
        private set
    var modelName: String = "Qwen/Qwen2.5-7B-Instruct"
        private set
    var serverUrl: String = ""
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        modelName = prefs.getString(KEY_MODEL_NAME, "Qwen/Qwen2.5-7B-Instruct") ?: "Qwen/Qwen2.5-7B-Instruct"
        serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: ""
    }
    
    fun saveConfig(newApiKey: String, newModelName: String, newServerUrl: String?) {
        apiKey = newApiKey
        modelName = newModelName
        if (newServerUrl != null) {
            serverUrl = newServerUrl
        }
        
        prefs.edit().apply {
            putString(KEY_API_KEY, apiKey)
            putString(KEY_MODEL_NAME, modelName)
            if (newServerUrl != null) {
                putString(KEY_SERVER_URL, serverUrl)
            }
        }.apply()
    }
    
    fun isConfigured(): Boolean {
        return apiKey.isNotBlank()
    }
}
