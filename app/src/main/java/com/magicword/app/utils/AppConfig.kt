package com.magicword.app.utils

import android.content.Context
import android.content.SharedPreferences

object AppConfig {
    private const val PREF_NAME = "app_config"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_MODEL_NAME = "model_name"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_USER_PERSONA = "user_persona"
    private const val KEY_SAVE_LOCATION = "save_location_id" // -1 for current, >0 for specific library ID
    
    private lateinit var prefs: SharedPreferences
    
    // Default fallback
    var apiKey: String = ""
        private set
    var modelName: String = "Qwen/Qwen2.5-7B-Instruct"
        private set
    var serverUrl: String = ""
        private set
    var userPersona: String = ""
        private set
    var saveLocationId: Int = -1 // Default: Current Library
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        modelName = prefs.getString(KEY_MODEL_NAME, "Qwen/Qwen2.5-7B-Instruct") ?: "Qwen/Qwen2.5-7B-Instruct"
        serverUrl = prefs.getString(KEY_SERVER_URL, "") ?: ""
        userPersona = prefs.getString(KEY_USER_PERSONA, "") ?: ""
        saveLocationId = prefs.getInt(KEY_SAVE_LOCATION, -1)
    }
    
    fun saveConfig(newApiKey: String, newModelName: String, newServerUrl: String?, newPersona: String?, newSaveLocationId: Int?) {
        apiKey = newApiKey
        modelName = newModelName
        if (newServerUrl != null) serverUrl = newServerUrl
        if (newPersona != null) userPersona = newPersona
        if (newSaveLocationId != null) saveLocationId = newSaveLocationId
        
        prefs.edit().apply {
            putString(KEY_API_KEY, apiKey)
            putString(KEY_MODEL_NAME, modelName)
            if (newServerUrl != null) putString(KEY_SERVER_URL, serverUrl)
            if (newPersona != null) putString(KEY_USER_PERSONA, userPersona)
            if (newSaveLocationId != null) putInt(KEY_SAVE_LOCATION, saveLocationId)
        }.apply()
    }
    
    fun isConfigured(): Boolean {
        return apiKey.isNotBlank()
    }
}
