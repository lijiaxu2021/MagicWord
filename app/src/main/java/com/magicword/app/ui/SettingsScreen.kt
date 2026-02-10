package com.magicword.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.magicword.app.utils.LogUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onNavigateToLogs: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var isLogEnabled by remember { mutableStateOf(prefs.getBoolean("enable_log", true)) }

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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
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
            Divider()
            
            // View Logs
            ListItem(
                headlineContent = { Text("查看所有日志") },
                modifier = Modifier.clickable { onNavigateToLogs() },
                trailingContent = { Text("查看 >") }
            )
            Divider()
        }
    }
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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
