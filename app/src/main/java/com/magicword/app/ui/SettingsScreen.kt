package com.magicword.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.magicword.app.utils.AppConfig
import com.magicword.app.utils.LogUtil
import java.io.File
import com.magicword.app.data.AppDatabase
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToLogs: () -> Unit, onNavigateToAbout: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var isLogEnabled by remember { mutableStateOf(prefs.getBoolean("enable_log", true)) }
    
    // Config State
    var apiKey by remember { mutableStateOf(AppConfig.apiKey) }
    var modelName by remember { mutableStateOf(AppConfig.modelName) }
    var userPersona by remember { mutableStateOf(AppConfig.userPersona) }
    var saveLocationId by remember { mutableStateOf(AppConfig.saveLocationId) }
    
    // Library Data for Selection
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )
    val libraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    var showLibraryMenu by remember { mutableStateOf(false) }

    fun saveAllConfig() {
        AppConfig.saveConfig(apiKey, modelName, null, userPersona, saveLocationId)
        // Also update log pref
        prefs.edit().putBoolean("enable_log", isLogEnabled).apply()
        LogUtil.setLogEnabled(isLogEnabled)
    }
    
    // Save on disappear or explicit save? 
    // Let's autosave on change or back?
    // User expects "Configuration" to be saved.
    // Let's add a "Save" button or save on back.
    // Saving on Back is good, but let's make it explicit or auto.
    // Let's auto-save when values change? 
    // Text fields are tricky with auto-save on every char.
    // Let's save on Back.
    
    DisposableEffect(Unit) {
        onDispose {
            saveAllConfig()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ) {
            Text("AI 配置", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text("个性化与功能", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            
            OutlinedTextField(
                value = userPersona,
                onValueChange = { userPersona = it },
                label = { Text("用户身份 (Persona)") },
                placeholder = { Text("例如：我是一个高中生，请用简单的词汇解释...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                minLines = 3,
                maxLines = 5
            )
            Text("此文本将附加在每次 AI 请求中，帮助 AI 适应您的偏好。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Location Selector
            ListItem(
                headlineContent = { Text("新词默认保存位置") },
                supportingContent = { 
                    val name = if (saveLocationId == -1) "当前所在词库" 
                               else libraries.find { it.id == saveLocationId }?.name ?: "未知词库"
                    Text(name)
                },
                trailingContent = {
                    Box {
                        Button(onClick = { showLibraryMenu = true }) {
                            Text("更改")
                        }
                        DropdownMenu(expanded = showLibraryMenu, onDismissRequest = { showLibraryMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("当前所在词库 (跟随)") },
                                onClick = { 
                                    saveLocationId = -1
                                    showLibraryMenu = false 
                                }
                            )
                            libraries.forEach { lib ->
                                DropdownMenuItem(
                                    text = { Text(lib.name) },
                                    onClick = { 
                                        saveLocationId = lib.id
                                        showLibraryMenu = false 
                                    }
                                )
                            }
                        }
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text("系统", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))

            // Log Switch
            ListItem(
                headlineContent = { Text("开启日志记录") },
                supportingContent = { Text("记录应用操作行为，便于问题排查") },
                trailingContent = {
                    Switch(
                        checked = isLogEnabled,
                        onCheckedChange = {
                            isLogEnabled = it
                            LogUtil.setLogEnabled(it)
                        }
                    )
                }
            )
            
            // View Logs
            ListItem(
                headlineContent = { Text("查看所有日志") },
                modifier = Modifier.clickable { onNavigateToLogs() },
                trailingContent = { Text("查看 >") }
            )

            // About
            ListItem(
                headlineContent = { Text("关于与使用说明") },
                modifier = Modifier.clickable { onNavigateToAbout() },
                trailingContent = { Text("查看 >") }
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "By lijiaxu2011 UpXuu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于 MagicWord") },
        text = {
            Column {
                Text("MagicWord 是一个极简风格的单词记忆应用。")
                Spacer(modifier = Modifier.height(8.dp))
                Text("版本: 1.0.0")
                Spacer(modifier = Modifier.height(8.dp))
                Text("开发者: lijiaxu2011 & UpXuu")
                Spacer(modifier = Modifier.height(16.dp))
                Text("感谢您的使用！")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var logFiles by remember { mutableStateOf(emptyList<File>()) }
    var selectedLogContent by remember { mutableStateOf<String?>(null) }
    var showLogDetail by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logFiles = LogUtil.getAllLogFiles()
    }

    if (showLogDetail && selectedLogContent != null) {
        LogDetailDialog(
            content = selectedLogContent!!,
            onDismiss = { showLogDetail = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日志文件") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (logFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无日志文件")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(logFiles) { file ->
                    ListItem(
                        headlineContent = { Text(file.name) },
                        supportingContent = { Text("Size: ${file.length() / 1024} KB") },
                        trailingContent = {
                            IconButton(onClick = { shareLogFile(context, file) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                        },
                        modifier = Modifier.clickable {
                            selectedLogContent = try {
                                file.readText()
                            } catch (e: Exception) {
                                "无法读取文件: ${e.message}"
                            }
                            showLogDetail = true
                        }
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
fun LogDetailDialog(content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("日志详情") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(content, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

fun shareLogFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享日志"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
