package com.magicword.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.magicword.app.ui.MainScreen
import com.magicword.app.ui.theme.EasyWordTheme

import com.magicword.app.utils.LogUtil
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.magicword.app.worker.SyncWorker
import java.util.concurrent.TimeUnit

import com.magicword.app.utils.AppConfig
import com.magicword.app.ui.InitScreen
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.init(this)
        AppConfig.init(this) // Initialize Config
        
        setContent {
            EasyWordTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Check if Configured
                    var isConfigured by remember { mutableStateOf(AppConfig.isConfigured()) }
                    
                    if (isConfigured) {
                        MainScreen()
                    } else {
                        InitScreen(onInitSuccess = {
                            isConfigured = true
                        })
                    }
                }
            }
        }
    }
}
