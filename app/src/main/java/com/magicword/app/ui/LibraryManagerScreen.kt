package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
            onConfirm = { name, desc, tags ->
                viewModel.uploadLibraryPackage(name, desc, tags, selectedLibraries.toList())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineLibraryTab(viewModel: LibraryViewModel) {
    val onlineLibraries by viewModel.onlineLibraries.collectAsState()
    val isLoading by viewModel.isNetworkLoading.collectAsState()
    val importLogs by viewModel.importLogs.collectAsState()
    val onlineTags by viewModel.onlineTags.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showTagDrawer by remember { mutableStateOf(false) }

    // 初始加载 (Refresh)
    LaunchedEffect(Unit) {
        viewModel.fetchOnlineLibraries(isRefresh = true)
        viewModel.fetchOnlineTags()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Tag Drawer (Simple visibility toggle for now, or use ModalDrawer)
            if (showTagDrawer) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(200.dp)
                        .padding(end = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("标签筛选", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn {
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { 
                                        selectedTag = null
                                        viewModel.searchOnlineLibraries(searchQuery, null)
                                        showTagDrawer = false
                                    },
                                    label = { Text("全部") }
                                )
                            }
                            items(onlineTags) { (tag, count) ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { 
                                        selectedTag = tag 
                                        viewModel.searchOnlineLibraries(searchQuery, tag)
                                        showTagDrawer = false
                                    },
                                    label = { Text("$tag ($count)") }
                                )
                            }
                        }
                    }
                }
            }

            // Main Content
            Column(modifier = Modifier.weight(1f)) {
                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showTagDrawer = !showTagDrawer }) {
                        Icon(Icons.Default.Menu, "Tags")
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            // Debounce could be added here
                        },
                        placeholder = { Text("搜索词库...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { viewModel.searchOnlineLibraries(searchQuery, selectedTag) }) {
                                Icon(Icons.Default.Search, "Search")
                            }
                        }
                    )
                }
                
                // Selected Tag Indicator
                if (selectedTag != null) {
                    AssistChip(
                        onClick = { 
                            selectedTag = null
                            viewModel.searchOnlineLibraries(searchQuery, null)
                        },
                        label = { Text("标签: $selectedTag") },
                        trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                           Text("暂无在线词库或加载失败")
                           Button(onClick = { viewModel.fetchOnlineLibraries(isRefresh = true) }, modifier = Modifier.padding(top = 8.dp)) {
                               Text("重试")
                           }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(onlineLibraries.size) { index ->
                            val lib = onlineLibraries[index]
                            OnlineLibraryItem(
                                library = lib,
                                onDownload = { viewModel.downloadAndImportLibrary(lib) }
                            )
                            
                            // 简单的加载更多触发机制：当滑动到倒数第3个时触发加载
                            if (index >= onlineLibraries.size - 3 && !isLoading) {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMoreOnlineLibraries()
                                }
                            }
                        }
                        
                        // 底部加载指示器
                        if (isLoading && onlineLibraries.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = library.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = date, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = library.description, 
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            
            // Tags
            if (!library.tags.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    library.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(end = 4.dp).height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "By: ${library.author}", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDownload,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("下载并导入")
            }
        }
    }
}

@Composable
fun UploadLibraryDialog(count: Int, onDismiss: () -> Unit, onConfirm: (String, String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("标签 (逗号分隔)") },
                    placeholder = { Text("例如: 雅思, 听力, 2024") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        val tags = tagsInput.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }
                        onConfirm(name, description, tags) 
                    }
                },
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