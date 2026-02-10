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
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtil.init(this)
        
        // Trigger Sync on App Start and Periodic (every 15 mins min by system, but we need 10s?)
        // Android WorkManager minimum period is 15 minutes.
        // For 10s sync, we need a Handler or Coroutine loop in a Foreground Service or Singleton.
        // But user asked for "10s sync". Let's use a Coroutine loop in MainActivity/Application scope for active sync while app is open.
        
        try {
            // Background sync (robust, 15m)
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "MagicWordSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            // Immediate sync on start
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
            
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Active Sync Loop (While App is Open)
        // Ideally this should be in a Service or ViewModel, but MainScreen is root.
        // We can launch it in MainScreen or here.
        
        setContent {
            EasyWordTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
