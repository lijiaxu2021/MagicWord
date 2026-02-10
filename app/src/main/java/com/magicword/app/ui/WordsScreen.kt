package com.magicword.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import com.magicword.app.data.Word
import com.magicword.app.utils.LogUtil
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding

import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordsScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )
    val words by viewModel.allWords.collectAsState(initial = emptyList())
    // Create a mutable state list for reordering to work smoothly in UI before DB update
    var reorderableWords by remember { mutableStateOf(words) }
    LaunchedEffect(words) {
        reorderableWords = words
    }

    val libraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    val currentLibraryId by viewModel.currentLibraryId.collectAsState()
    
    val currentLibrary = libraries.find { it.id == currentLibraryId }
    
    var showLibrarySheet by remember { mutableStateOf(false) }
    var isListMode by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<Word?>(null) }
    
    // Search State
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // List Selection & Sorting
    var selectedWords by remember { mutableStateOf(setOf<Int>()) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Scroll Control
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        // Update UI list first
        reorderableWords = reorderableWords.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        // Then update DB (debounce this in real app, but for now direct update)
        // We need to update sortOrder for affected items.
        // Assuming we have sortOrder field and logic.
        // For simplicity, let's just re-assign sortOrder based on new index
        val updatedList = reorderableWords.mapIndexed { index, word -> word.copy(sortOrder = index) }
        viewModel.updateWords(updatedList)
    }

    val pagerState = rememberPagerState(pageCount = { words.size })
    val scope = rememberCoroutineScope()
    
    // Bulk Import Sheet State
    var showImportSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sync Pager State with List Scroll
    LaunchedEffect(isListMode) {
        if (isListMode) {
            // Scroll list to current pager item
            listState.scrollToItem(pagerState.currentPage)
        } else {
            // Scroll pager to current list item (if user scrolled list) - Optional, usually user taps item
        }
    }

    // Animation Transition
    AnimatedContent(
        targetState = isListMode,
        label = "ModeSwitch"
    ) { mode ->
        if (mode) {
            // LIST MODE
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("单词列表 (${words.size})") },
                        actions = {
                            // Bulk Import Button
                             IconButton(onClick = { showImportSheet = true }) {
                                Icon(Icons.Default.Add, "Import")
                            }
                            
                            // Delete Selected Button
                            if (selectedWords.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.deleteWords(selectedWords.toList())
                                    selectedWords = emptySet()
                                }) {
                                    Icon(Icons.Default.Delete, "Delete Selected", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        
                            // Sort Button
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.Sort, "Sort")
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    DropdownMenuItem(text = { Text("创建时间 (新->旧)") }, onClick = { viewModel.setSortOption(LibraryViewModel.SortOption.CREATED_AT_DESC); showSortMenu = false })
                                    DropdownMenuItem(text = { Text("创建时间 (旧->新)") }, onClick = { viewModel.setSortOption(LibraryViewModel.SortOption.CREATED_AT_ASC); showSortMenu = false })
                                    DropdownMenuItem(text = { Text("字母顺序 (A->Z)") }, onClick = { viewModel.setSortOption(LibraryViewModel.SortOption.ALPHA_ASC); showSortMenu = false })
                                    DropdownMenuItem(text = { Text("学习次数 (高->低)") }, onClick = { viewModel.setSortOption(LibraryViewModel.SortOption.REVIEW_COUNT_DESC); showSortMenu = false })
                                }
                            }
                            
                            // Select All
                            TextButton(onClick = {
                                selectedWords = if (selectedWords.size == words.size) emptySet() else words.map { it.id }.toSet()
                            }) {
                                Text(if (selectedWords.size == words.size) "全不选" else "全选")
                            }
                            
                            IconButton(onClick = { isListMode = false }) {
                                Icon(Icons.Default.Close, "Close List")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(modifier = Modifier.padding(padding)) {
                    // Search Bar within List Mode
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query -> 
                            searchQuery = query
                            // Auto-scroll to first match
                            val index = words.indexOfFirst { it.word.contains(query, ignoreCase = true) }
                            if (index != -1) {
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                        placeholder = { Text("搜索列表中的单词...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        singleLine = true
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(reorderableWords, key = { _, word -> word.id }) { index, word ->
                            ReorderableItem(reorderableState, key = word.id) { isDragging ->
                                val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)
                                val isSelected = selectedWords.contains(word.id)
                                
                                Surface(shadowElevation = elevation.value) {
                                    ListItem(
                                        leadingContent = {
                                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                                        },
                                        headlineContent = { Text(word.word) },
                                        supportingContent = { 
                                            Text("${word.definitionCn} · 复习: ${word.reviewCount}", maxLines = 1) 
                                        },
                                        trailingContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                                    selectedWords = if (checked) selectedWords + word.id else selectedWords - word.id
                                                })
                                                // Reorder Handle
                                                Icon(
                                                    Icons.Default.DragHandle,
                                                    "Reorder",
                                                    modifier = Modifier.draggableHandle().padding(start = 8.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .clickable { 
                                                // Double tap logic handled below
                                            }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onDoubleTap = {
                                                        scope.launch {
                                                            // Save state first
                                                            prefs.edit().putInt("last_index_${currentLibraryId}", index).apply()
                                                            if (currentLibrary != null) {
                                                                viewModel.updateLibraryLastIndex(currentLibrary.id, index)
                                                            }
                                                            // Switch and Scroll
                                                            pagerState.scrollToPage(index)
                                                            isListMode = false
                                                        }
                                                    },
                                                    onTap = {
                                                        editingWord = word
                                                    }
                                                )
                                            }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // CARD MODE
            // Restore Pager State from Prefs (Needs a bit of logic, simplified here)
            // We use LaunchedEffect to scroll once
            val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
            LaunchedEffect(words) {
                if (words.isNotEmpty()) {
                    // Try to restore from DB (Library lastIndex)
                    // Since we don't have direct DB access here synchronously, we rely on ViewModel or just use Prefs for now as DB migration was just added.
                    // For now, let's use the Prefs we already implemented, or update to use Library entity if available.
                    // Actually, let's rely on the Prefs logic we just built, it works.
                    val lastIndex = prefs.getInt("last_index_${currentLibraryId}", 0)
                    if (lastIndex in words.indices) {
                        pagerState.scrollToPage(lastIndex)
                    }
                }
            }
            
            // Save Pager State
            LaunchedEffect(pagerState.currentPage) {
                prefs.edit().putInt("last_index_${currentLibraryId}", pagerState.currentPage).apply()
                // Also update DB
                if (currentLibrary != null) {
                    viewModel.updateLibraryLastIndex(currentLibrary.id, pagerState.currentPage)
                }
            }
            
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showLibrarySheet = true }
                            ) {
                                Text(
                                    text = currentLibrary?.name ?: "默认词库",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Library", modifier = Modifier.size(16.dp))
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { padding ->
                if (words.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("当前词库为空，请添加单词")
                    }
                } else {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) { page ->
                        val word = words[page]
                        
                        // Increment Review Count on View
                        LaunchedEffect(page) {
                             if (pagerState.currentPage == page) {
                                 viewModel.incrementReviewCount(word)
                             }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Detect Long Press on the Card itself to switch
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                     detectTapGestures(
                                         onLongPress = { isListMode = true }
                                     )
                                }
                            ) {
                                WordCard(
                                    word = word,
                                    onEditClick = { editingWord = word }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Handle Global Search Trigger
    if (showSearch) {
        LaunchedEffect(Unit) {
            isListMode = true
            showSearch = false
        }
    }

    // Add Library Dialog State
    var showAddLibraryDialog by remember { mutableStateOf(false) }

    // Library Switcher Bottom Sheet
    if (showLibrarySheet) {
        ModalBottomSheet(onDismissRequest = { showLibrarySheet = false }) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(libraries) { lib ->
                    ListItem(
                        headlineContent = { Text(lib.name) },
                        modifier = Modifier.clickable {
                            viewModel.switchLibrary(lib.id)
                            showLibrarySheet = false
                        },
                        trailingContent = {
                            if (lib.id == currentLibraryId) Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
                item {
                    Button(onClick = { 
                        // Instead of auto-adding, show dialog
                        showAddLibraryDialog = true
                        showLibrarySheet = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("新建词库")
                    }
                }
            }
        }
    }
    
    // Add Library Dialog
    if (showAddLibraryDialog) {
        AddLibraryDialog(
            onDismiss = { showAddLibraryDialog = false },
            onConfirm = { name ->
                viewModel.addLibrary(name)
                showAddLibraryDialog = false
            }
        )
    }

    // Edit Dialog
    if (editingWord != null) {
        WordDetailEditDialog(
            word = editingWord!!,
            onDismiss = { editingWord = null },
            onSave = { updatedWord ->
                viewModel.updateWord(updatedWord)
                editingWord = null
            }
        )
    }
    
    // Import Sheet
    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            sheetState = sheetState
        ) {
            BulkImportContent(
                viewModel = viewModel,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showImportSheet = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AddLibraryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建词库") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("词库名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun BulkImportContent(viewModel: LibraryViewModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val logs by viewModel.importLogs.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
    ) {
        // Move input to top
        Text("AI 批量导入", style = MaterialTheme.typography.titleLarge)
        Text("输入单词列表（用逗号或换行分隔）", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text("apple, banana\nor sentences...") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (logs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(contentPadding = PaddingValues(8.dp)) {
                    items(logs) { log ->
                        Text(log, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                Text("关闭")
            }
            Button(
                onClick = { viewModel.bulkImport(text) },
                enabled = !isImporting && text.isNotBlank()
            ) {
                Text(if (isImporting) "导入中..." else "开始导入")
            }
        }
        
        // Push content up when keyboard opens
        Spacer(modifier = Modifier.weight(1f)) 
    }
}
