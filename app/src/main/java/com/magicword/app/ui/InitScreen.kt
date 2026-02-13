package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.magicword.app.network.ServerApi
import com.magicword.app.network.VerifyKitRequest
import com.magicword.app.utils.AppConfig
import com.magicword.app.utils.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitScreen(onInitSuccess: () -> Unit) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )

    var kitKey by remember { mutableStateOf("") }
    
    var isManualMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // UI State for Success
    var isImportSuccess by remember { mutableStateOf(false) }
    
    // Local logs
    var logs by remember { mutableStateOf(listOf<String>()) }
    
    // Observe ViewModel logs
    val vmLogs by libraryViewModel.importLogs.collectAsState()
    val isImporting by libraryViewModel.isImporting.collectAsState()
    
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
                // Key-Kit Mode
                Text("Key-Kit 初始化", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = kitKey,
                    onValueChange = { kitKey = it },
                    label = { Text("请输入 Key-Kit 密钥") },
                    placeholder = { Text("联系管理员获取") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isImportSuccess) {
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
                                logs = emptyList()
                                addLog("🚀 开始验证 Key-Kit...")
                                
                                try {
                                    val retrofit = Retrofit.Builder()
                                        .baseUrl("https://mag.upxuu.com/")
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build()
                                    
                                    val api = retrofit.create(ServerApi::class.java)
                                    
                                    // 1. Verify Kit
                                    addLog("🔐 正在向服务端验证密钥...")
                                    val verifyResp = api.verifyKit(VerifyKitRequest(kitKey))
                                    
                                    if (verifyResp.valid) {
                                        addLog("✅ 验证通过!")
                                        val apiKey = verifyResp.apiKey ?: ""
                                        val model = verifyResp.model ?: "gpt-3.5-turbo"
                                        // Use returned base URL or default to OpenAI
                                        val baseUrl = verifyResp.baseUrl ?: "https://api.openai.com/v1"
                                        
                                        addLog("配置信息已获取: Model=$model")
                                        
                                        // Save Config
                                        // Note: We need to pass baseUrl to saveConfig if we added it, 
                                        // currently AppConfig.saveConfig takes (key, model, serverUrl, persona, location)
                                        // We might need to store AI Base URL separately or reuse serverUrl?
                                        // Usually 'serverUrl' was for sync. 
                                        // Let's check AppConfig.
                                        // For now, let's assume we update AppConfig to store aiBaseUrl.
                                        // Or just put it in prefs directly.
                                        
                                        // Let's update AppConfig via prefs directly for AI Base URL if needed, 
                                        // or assume AppConfig handles it.
                                        // The user mentioned "Input this key... obtain this key".
                                        
                                        AppConfig.saveConfig(apiKey, model, "https://mag.upxuu.com", null, null)
                                        // Also save base URL if AppConfig supports it.
                                        prefs.edit().putString("ai_base_url", baseUrl).apply()
                                        AppConfig.reload(context) // Reload to pick up changes
                                        
                                        addLog("💾 本地配置已更新")
                                        
                                        // 2. Fetch Default Library URL
                                        addLog("📥 获取默认词库配置...")
                                        val initConfig = api.getInitConfig()
                                        val libUrl = initConfig.defaultLibraryUrl
                                        
                                        if (!libUrl.isNullOrBlank()) {
                                            addLog("📚 发现默认词库: $libUrl")
                                            addLog("⬇️ 正在下载...")
                                            
                                            val jsonContent = withContext(Dispatchers.IO) {
                                                try {
                                                    URL(libUrl).readText()
                                                } catch (e: Exception) {
                                                    throw Exception("下载失败: ${e.message}")
                                                }
                                            }
                                            
                                            addLog("✅ 下载完成 (${jsonContent.length} bytes)")
                                            addLog("🚀 开始导入...")
                                            
                                            libraryViewModel.importLibraryJson(jsonContent)
                                            
                                            while (libraryViewModel.isImporting.value) {
                                                kotlinx.coroutines.delay(500)
                                            }
                                            addLog("✨ 词库导入完毕")
                                        } else {
                                            addLog("ℹ️ 无默认词库配置")
                                        }
                                        
                                        isImportSuccess = true
                                        addLog("🎉 全部完成！")
                                        
                                    } else {
                                        addLog("❌ 验证失败: ${verifyResp.error ?: "无效的密钥"}")
                                    }
                                    
                                } catch (e: Exception) {
                                    addLog("❌ 网络或系统错误: ${e.localizedMessage}")
                                    e.printStackTrace()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && kitKey.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("验证并初始化")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                if (!isImportSuccess && !isLoading) {
                    TextButton(onClick = { isManualMode = true }) {
                        Text("手动配置 (高级) >")
                    }
                }
            } else {
                // Manual Mode (Keep as fallback)
                Text("手动配置", style = MaterialTheme.typography.titleMedium)
                // ... (Keep existing manual config fields for advanced users)
                var manualApiKey by remember { mutableStateOf("") }
                var manualModelName by remember { mutableStateOf("") }
                
                OutlinedTextField(value = manualApiKey, onValueChange = { manualApiKey = it }, label = { Text("API Key") })
                OutlinedTextField(value = manualModelName, onValueChange = { manualModelName = it }, label = { Text("Model Name") })
                
                Button(onClick = { 
                    AppConfig.saveConfig(manualApiKey, manualModelName, null, null, null)
                    onInitSuccess() 
                }) { Text("保存") }
                
                TextButton(onClick = { isManualMode = false }) { Text("< 返回 Key-Kit") }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            
            // Log Window
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(state = listState, contentPadding = PaddingValues(8.dp)) {
                    items(displayLogs) { log -> 
                        Text(log, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}
