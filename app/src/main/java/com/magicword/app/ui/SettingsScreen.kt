package com.magicword.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.magicword.app.utils.AppConfig
import com.magicword.app.utils.LogUtil
import com.magicword.app.utils.UpdateManager
import com.magicword.app.BuildConfig
import java.io.File
import com.magicword.app.data.AppDatabase
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.utils.NoticeManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.magicword.app.network.ServerApi
import com.magicword.app.network.VerifyKitRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToLogs: () -> Unit, onNavigateToAbout: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var isLogEnabled by remember { mutableStateOf(prefs.getBoolean("enable_log", true)) }
    var isAutoUpdateEnabled by remember { mutableStateOf(prefs.getBoolean("auto_update", true)) }
    
    // Config State
    var apiKey by remember { mutableStateOf(AppConfig.apiKey) }
    var modelName by remember { mutableStateOf(AppConfig.modelName) }
    var userPersona by remember { mutableStateOf(AppConfig.userPersona) }
    var saveLocationId by remember { mutableStateOf(AppConfig.saveLocationId) }
    
    // Key-Kit State
    var kitKey by remember { mutableStateOf("") }
    var isVerifyingKit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // ... (Update Logic) ...
    var latestNotice by remember { mutableStateOf<NoticeManager.Notice?>(null) }
    LaunchedEffect(Unit) { latestNotice = NoticeManager.getLatestNotice() }
    
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    fun checkUpdate() {
        scope.launch {
            isCheckingUpdate = true
            updateInfo = withContext(Dispatchers.IO) { UpdateManager.checkUpdate(BuildConfig.VERSION_NAME) }
            isCheckingUpdate = false
            if (updateInfo?.hasUpdate == true) showUpdateDialog = true
        }
    }
    
    if (showUpdateDialog && updateInfo != null) {
        UpdateDialog(updateInfo!!, { showUpdateDialog = false }, {
            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo!!.downloadUrl)))
            showUpdateDialog = false
        })
    }

    // Library Data
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModelFactory(database.wordDao(), prefs))
    val libraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    var showLibraryMenu by remember { mutableStateOf(false) }

    fun saveAllConfig() {
        AppConfig.saveConfig(apiKey, modelName, null, userPersona, saveLocationId)
        prefs.edit().putBoolean("enable_log", isLogEnabled).putBoolean("auto_update", isAutoUpdateEnabled).apply()
        LogUtil.setLogEnabled(isLogEnabled)
    }
    
    DisposableEffect(Unit) { onDispose { saveAllConfig() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            
            // Key-Kit Section
            Text("账户与鉴权 (Key-Kit)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = kitKey,
                    onValueChange = { kitKey = it },
                    label = { Text("更新 Key-Kit 密钥") },
                    placeholder = { Text("输入新密钥以更新配置") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isVerifyingKit = true
                            try {
                                val api = Retrofit.Builder().baseUrl("https://mag.upxuu.com/").addConverterFactory(GsonConverterFactory.create()).build().create(ServerApi::class.java)
                                val resp = api.verifyKit(VerifyKitRequest(kitKey))
                                if (resp.valid) {
                                    apiKey = resp.apiKey ?: ""
                                    modelName = resp.model ?: ""
                                    prefs.edit().putString("ai_base_url", resp.baseUrl).apply()
                                    AppConfig.reload(context) // Reload base URL
                                    kitKey = "" // Clear input
                                    // Toast or Snack?
                                } else {
                                    // Error
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isVerifyingKit = false
                            }
                        }
                    },
                    enabled = !isVerifyingKit && kitKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isVerifyingKit) "验证中..." else "验证并更新配置")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("当前 AI 配置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key (只读/手动)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                enabled = true
            )
            
            // ... (Rest of UI similar to before)
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text("个性化", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            OutlinedTextField(
                value = userPersona,
                onValueChange = { userPersona = it },
                label = { Text("用户身份") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                minLines = 3
            )
            
            // Save Location
            ListItem(
                headlineContent = { Text("新词保存位置") },
                supportingContent = { 
                    val name = if (saveLocationId == -1) "当前所在词库" else libraries.find { it.id == saveLocationId }?.name ?: "未知"
                    Text(name)
                },
                trailingContent = {
                    Box {
                        Button(onClick = { showLibraryMenu = true }) { Text("更改") }
                        DropdownMenu(expanded = showLibraryMenu, onDismissRequest = { showLibraryMenu = false }) {
                            DropdownMenuItem(text = { Text("跟随当前") }, onClick = { saveLocationId = -1; showLibraryMenu = false })
                            libraries.forEach { lib ->
                                DropdownMenuItem(text = { Text(lib.name) }, onClick = { saveLocationId = lib.id; showLibraryMenu = false })
                            }
                        }
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // System
            ListItem(
                headlineContent = { Text("日志记录") },
                trailingContent = { Switch(checked = isLogEnabled, onCheckedChange = { isLogEnabled = it; LogUtil.setLogEnabled(it) }) }
            )
            ListItem(
                headlineContent = { Text("查看日志") },
                modifier = Modifier.clickable { onNavigateToLogs() },
                trailingContent = { Text("查看 >") }
            )
            
            // Update
            ListItem(
                headlineContent = { Text("自动更新") },
                trailingContent = { Switch(checked = isAutoUpdateEnabled, onCheckedChange = { isAutoUpdateEnabled = it }) }
            )
            ListItem(
                headlineContent = { Text("检查更新") },
                supportingContent = { Text(if(isCheckingUpdate) "Checking..." else "v${BuildConfig.VERSION_NAME}") },
                modifier = Modifier.clickable { checkUpdate() }
            )
             ListItem(
                headlineContent = { Text("关于") },
                modifier = Modifier.clickable { onNavigateToAbout() },
                trailingContent = { Text("查看 >") }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ... (UpdateDialog, AboutDialog, LogListScreen, LogDetailDialog, shareLogFile helper functions kept same as original or simplified)
// Note: I'm omitting the full copy of helper functions to save space if they are not changed, 
// BUT for `Write` tool I must provide the full file content or I will overwrite them with nothing.
// I will copy the helpers back.

@Composable
fun UpdateDialog(updateInfo: UpdateManager.UpdateInfo, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 v${updateInfo.version}") },
        text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { Text(updateInfo.releaseNotes) } },
        confirmButton = { Button(onClick = onUpdate) { Text("立即更新") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("稍后") } }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于 MagicWord") },
        text = { Column { Text("MagicWord v1.0.0\nBy lijiaxu2011 & UpXuu") } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var logFiles by remember { mutableStateOf(emptyList<File>()) }
    var selectedLogContent by remember { mutableStateOf<String?>(null) }
    var showLogDetail by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { logFiles = LogUtil.getAllLogFiles() }

    if (showLogDetail && selectedLogContent != null) {
        LogDetailDialog(selectedLogContent!!, { showLogDetail = false })
    }

    Scaffold(topBar = { TopAppBar(title = { Text("日志") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(logFiles) { file ->
                ListItem(
                    headlineContent = { Text(file.name) },
                    trailingContent = { IconButton(onClick = { shareLogFile(context, file) }) { Icon(Icons.Default.Share, "Share") } },
                    modifier = Modifier.clickable { selectedLogContent = file.readText(); showLogDetail = true }
                )
                Divider()
            }
        }
    }
}

@Composable
fun LogDetailDialog(content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("日志详情") },
        text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { Text(content) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

fun shareLogFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "分享日志"))
}
