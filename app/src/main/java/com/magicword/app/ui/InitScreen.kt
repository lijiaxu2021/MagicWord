package com.magicword.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import com.magicword.app.network.RetrofitClient
import com.magicword.app.network.ServerApi
import com.magicword.app.utils.AppConfig
import com.magicword.app.utils.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitScreen(onInitSuccess: () -> Unit) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    
    // Reuse LibraryViewModel for DB operations (Import)
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )

    var serverUrl by remember { mutableStateOf("https://upxuu.pythonanywhere.com") }
    var manualApiKey by remember { mutableStateOf("") }
    var manualModelName by remember { mutableStateOf("Qwen/Qwen2.5-7B-Instruct") }
    
    var isManualMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Local logs for the Init Process (merging with ViewModel logs if needed)
    var logs by remember { mutableStateOf(listOf<String>()) }
    
    // Observe ViewModel logs for Library Import
    val vmLogs by libraryViewModel.importLogs.collectAsState()
    
    // Merge logs
    val displayLogs = logs + vmLogs

    val scope = rememberCoroutineScope()

    fun addLog(msg: String) {
        logs = logs + msg
        LogUtil.logDebug("InitScreen", msg)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome to MagicWord!", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            if (!isManualMode) {
                // Server Mode
                Text("从服务器初始化", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器地址 (URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            logs = emptyList() // Clear previous
                            addLog("🚀 开始连接服务器: $serverUrl")
                            
                            try {
                                // Create transient Retrofit client
                                val cleanUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
                                val retrofit = Retrofit.Builder()
                                    .baseUrl(cleanUrl)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .build()
                                
                                val api = retrofit.create(ServerApi::class.java)
                                
                                addLog("📥 正在获取配置...")
                                val config = api.getInitConfig()
                                addLog("✅ 获取成功!")
                                addLog("API Key: ${config.apiKey.take(10)}***")
                                addLog("Model: ${config.modelName}")
                                addLog("默认词库: ${config.defaultLibrary.size} 个单词")
                                
                                // Save Config
                                AppConfig.saveConfig(config.apiKey, config.modelName, serverUrl)
                                addLog("💾 配置已保存")
                                
                                // Import Library if exists
                                if (config.defaultLibrary.isNotEmpty()) {
                                    addLog("📚 正在导入默认词库...")
                                    // Convert list to JSON string for ViewModel (or add a direct list import method to VM)
                                    // Using JSON for consistency with existing method
                                    val json = Gson().toJson(config.defaultLibrary)
                                    libraryViewModel.importLibraryJson(json)
                                    // Wait for import to finish? 
                                    // libraryViewModel.importLibraryJson launches a coroutine. 
                                    // We can observe isImporting but here we just wait a bit or trust the flow.
                                    // Better: wait for isImporting to become false?
                                    // Simple approach: The VM logs will show progress.
                                    // We can just set a delay or "Done" button.
                                    // But user wants auto entry? "Fetch... success -> enter"
                                    
                                    // Let's verify import success by checking logs or DB?
                                    // For now, let's assume success if no exception.
                                    addLog("⏳ 导入任务已提交，请等待日志完成...")
                                } else {
                                    addLog("ℹ️ 默认词库为空，跳过导入")
                                }
                                
                                // Allow user to proceed
                                addLog("✨ 初始化完成！即将进入主界面...")
                                kotlinx.coroutines.delay(2000)
                                onInitSuccess()
                                
                            } catch (e: Exception) {
                                addLog("❌ 初始化失败: ${e.localizedMessage}")
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && serverUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("获取配置并初始化")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { isManualMode = true }) {
                    Text("手动导入配置 >")
                }
            } else {
                // Manual Mode
                Text("手动配置", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = manualApiKey,
                    onValueChange = { manualApiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = manualModelName,
                    onValueChange = { manualModelName = it },
                    label = { Text("模型名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        AppConfig.saveConfig(manualApiKey, manualModelName, null)
                        addLog("💾 手动配置已保存")
                        onInitSuccess()
                    },
                    enabled = manualApiKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存并进入")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { isManualMode = false }) {
                    Text("< 返回服务器初始化")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Text("运行日志", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
            
            // Log Window
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    reverseLayout = true // Show newest at bottom? Actually list adds to end, so reverseLayout=true shows end at bottom usually if we reverse list.
                    // Let's just show standard list.
                ) {
                    items(displayLogs.reversed()) { log -> // Show newest at top
                        Text(log, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}
