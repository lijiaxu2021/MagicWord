package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.magicword.app.data.OnlineLibrary
import com.magicword.app.data.Library
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryManagerScreen(
    viewModel: LibraryViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Local, 1: Online
    val tabs = listOf("本地词库", "在线词库")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("词库管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LocalLibraryTab(viewModel)
                1 -> OnlineLibraryTab(viewModel)
            }
        }
    }
}

@Composable
fun LocalLibraryTab(viewModel: LibraryViewModel) {
    val libraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    val currentLibraryId by viewModel.currentLibraryId.collectAsState()
    var selectedLibraries by remember { mutableStateOf(setOf<Int>()) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Logs for feedback
    val importLogs by viewModel.importLogs.collectAsState()
    val isWorking by viewModel.isNetworkLoading.collectAsState() // Reusing network loading for upload

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            // Logs / Status
            if (importLogs.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = importLogs.lastOrNull() ?: "", 
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(libraries) { lib ->
                    LibraryItem(
                        library = lib,
                        isCurrent = lib.id == currentLibraryId,
                        isSelected = selectedLibraries.contains(lib.id),
                        onSelect = { selected ->
                            selectedLibraries = if (selected) selectedLibraries + lib.id else selectedLibraries - lib.id
                        },
                        onSwitch = { viewModel.switchLibrary(lib.id) },
                        onDelete = { if (lib.id != 1) viewModel.deleteLibrary(lib.id) }
                    )
                }
            }
        }

        // FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (selectedLibraries.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showUploadDialog = true },
                    icon = { Icon(Icons.Default.CloudUpload, null) },
                    text = { Text("上传 (${selectedLibraries.size})") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Create")
            }
        }
    }

    if (showCreateDialog) {
        AddLibraryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.addLibrary(name)
                showCreateDialog = false
            }
        )
    }

    if (showUploadDialog) {
        UploadLibraryDialog(
            count = selectedLibraries.size,
            onDismiss = { showUploadDialog = false },
            onConfirm = { name, desc ->
                viewModel.uploadLibraryPackage(name, desc, selectedLibraries.toList())
                showUploadDialog = false
                selectedLibraries = emptySet()
            }
        )
    }
}

@Composable
fun LibraryItem(
    library: Library,
    isCurrent: Boolean,
    isSelected: Boolean,
    onSelect: (Boolean) -> Unit,
    onSwitch: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        leadingContent = {
            Checkbox(checked = isSelected, onCheckedChange = onSelect)
        },
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(library.name)
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, "Current", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        },
        supportingContent = { Text(if (library.description.isNotBlank()) library.description else "本地词库") },
        trailingContent = {
            if (library.id != 1) { // Prevent deleting default
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        modifier = Modifier.clickable { onSwitch() }
    )
    Divider()
}

@Composable
fun OnlineLibraryTab(viewModel: LibraryViewModel) {
    val onlineLibraries by viewModel.onlineLibraries.collectAsState()
    val isLoading by viewModel.isNetworkLoading.collectAsState()
    val importLogs by viewModel.importLogs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchOnlineLibraries()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
             if (isLoading) {
                 LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
             }
             
             // Logs
             if (importLogs.isNotEmpty() && isLoading) {
                 Text(
                     text = importLogs.lastOrNull() ?: "",
                     modifier = Modifier.padding(8.dp),
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.primary
                 )
             }

             if (onlineLibraries.isEmpty() && !isLoading) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                     Text("暂无在线词库或加载失败")
                     Button(onClick = { viewModel.fetchOnlineLibraries() }, modifier = Modifier.padding(top = 8.dp)) {
                         Text("重试")
                     }
                 }
             } else {
                 LazyColumn(modifier = Modifier.fillMaxSize()) {
                     items(onlineLibraries) { lib ->
                         OnlineLibraryItem(
                             library = lib,
                             onDownload = { viewModel.downloadAndImportLibrary(lib) }
                         )
                     }
                 }
             }
        }
    }
}

@Composable
fun OnlineLibraryItem(library: OnlineLibrary, onDownload: () -> Unit) {
    val date = remember(library.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(library.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(library.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(library.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("By: ${library.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDownload,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("下载并导入")
            }
        }
    }
}

@Composable
fun UploadLibraryDialog(count: Int, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("上传词库 ($count 个)") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("词库包名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("简介/描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("上传")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
