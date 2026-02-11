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
import java.net.URL
import androidx.compose.foundation.lazy.rememberLazyListState

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
    
    // UI State for Success
    var isImportSuccess by remember { mutableStateOf(false) }
    
    // Local logs for the Init Process (merging with ViewModel logs if needed)
    var logs by remember { mutableStateOf(listOf<String>()) }
    
    // Observe ViewModel logs for Library Import
    val vmLogs by libraryViewModel.importLogs.collectAsState()
    val isImporting by libraryViewModel.isImporting.collectAsState()
    
    // Merge logs
    val displayLogs = logs + vmLogs

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Auto-scroll logs
    LaunchedEffect(displayLogs.size) {
        if (displayLogs.isNotEmpty()) {
            listState.animateScrollToItem(displayLogs.size - 1)
        }
    }

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
                
                if (isImportSuccess) {
                    // Show "Enter App" button ONLY if success
                    Button(
                        onClick = { onInitSuccess() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("✅ 初始化成功，进入 App")
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                isImportSuccess = false
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
                                    
                                    // Save Config
                                    AppConfig.saveConfig(config.apiKey, config.modelName, serverUrl)
                                    addLog("💾 配置已保存")
                                    
                                    // Import Library if URL exists
                                    if (!config.defaultLibraryUrl.isNullOrBlank()) {
                                        addLog("📚 发现默认词库链接: ${config.defaultLibraryUrl}")
                                        addLog("⬇️ 正在下载词库文件...")
                                        
                                        // Download File
                                        val jsonContent = withContext(Dispatchers.IO) {
                                            try {
                                                URL(config.defaultLibraryUrl).readText()
                                            } catch (e: Exception) {
                                                throw Exception("下载失败: ${e.message}")
                                            }
                                        }
                                        
                                        addLog("✅ 下载完成，大小: ${jsonContent.length} bytes")
                                        addLog("🚀 开始导入词库...")
                                        
                                        // Trigger Import
                                        libraryViewModel.importLibraryJson(jsonContent)
                                        
                                        // Wait for import to complete
                                        while (libraryViewModel.isImporting.value) {
                                            kotlinx.coroutines.delay(500)
                                        }
                                        addLog("✨ 词库处理完毕")
                                    } else {
                                        addLog("ℹ️ 未配置默认词库，跳过导入")
                                    }
                                    
                                    // Mark as success to show button
                                    isImportSuccess = true
                                    addLog("🎉 全部流程完成！请点击上方按钮进入 App")
                                    
                                } catch (e: Exception) {
                                    addLog("❌ 失败: ${e.localizedMessage}")
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
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                if (!isImportSuccess && !isLoading) {
                    TextButton(onClick = { isManualMode = true }) {
                        Text("手动导入配置 >")
                    }
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
                    state = listState,
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(displayLogs) { log -> 
                        Text(log, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}
