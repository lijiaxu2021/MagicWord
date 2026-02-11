package com.magicword.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.magicword.app.data.Word
import com.magicword.app.data.AppDatabase
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import sh.calvin.reorderable.ReorderableItem

import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordsScreen(onOpenSettings: () -> Unit, onOpenProfile: () -> Unit, onJumpToTest: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )
    
    // Init TTS
    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }
    
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
    
    // Global Search State
    val globalSearchResult by viewModel.globalSearchResult.collectAsState()
    val isGlobalSearching by viewModel.isGlobalSearching.collectAsState()
    
    // Declare pagerState earlier so it can be used in globalSearchResult logic
    val pagerState = rememberPagerState(pageCount = { words.size })

    if (isGlobalSearching) {
        Dialog(onDismissRequest = {}) {
            CircularProgressIndicator()
        }
    }

    // State to track if we just navigated from search to prevent auto-restore
    var isNavigatingFromSearch by remember { mutableStateOf(false) }

    // Side List State
    var showSideList by remember { mutableStateOf(false) }
    
    // Jump Navigation Logic
    // Listen to ViewModel state
    val pendingJumpWordId by viewModel.pendingJumpWordId.collectAsState()
    
    LaunchedEffect(pendingJumpWordId) {
        if (pendingJumpWordId != null) {
            isNavigatingFromSearch = true // Prevent auto-restore logic interfering
            isListMode = false // Force Card Mode
        }
    }

    if (globalSearchResult != null) {
        val foundWord = globalSearchResult!!
        val foundLibId = foundWord.libraryId
        
        // Signal navigation
        LaunchedEffect(foundWord) {
            isNavigatingFromSearch = true
            if (foundLibId != currentLibraryId) {
                viewModel.switchLibrary(foundLibId)
            }
        }
        
        // Use a persistent index to scroll
        var targetIndex by remember { mutableIntStateOf(-1) }
        
        LaunchedEffect(words, foundWord) {
             val index = words.indexOfFirst { it.id == foundWord.id }
             if (index != -1) {
                 targetIndex = index
                 isListMode = false
                 pagerState.scrollToPage(index)
                 viewModel.clearGlobalSearchResult()
             }
        }
    }
    
    // Handle Pending Jump (from WordListScreen or other sources)
    LaunchedEffect(words, pendingJumpWordId) {
        if (pendingJumpWordId != null && words.isNotEmpty()) {
             val index = words.indexOfFirst { it.id == pendingJumpWordId }
             if (index != -1) {
                 pagerState.scrollToPage(index)
                 viewModel.clearPendingJump()
             }
        }
    }
    
    // List Selection & Sorting
    var selectedWords by remember { mutableStateOf(setOf<Int>()) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Scroll Control
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        reorderableWords = reorderableWords.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        val updatedList = reorderableWords.mapIndexed { index, word -> word.copy(sortOrder = index) }
        viewModel.updateWords(updatedList)
    }

    val scope = rememberCoroutineScope()
    
    // Auto-scroll when dragging near edges
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (reorderableState.isAnyItemDragging) {
             // Logic handled by library usually, but if "flying back", ensure state is stable
        }
    }
    
    // Export Library Selection State
    var selectedExportLibraries by remember { mutableStateOf(setOf<Int>()) }

    // Bulk Import Sheet State
    var showImportSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Export/Import JSON Logic
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                val targetIds = if (selectedExportLibraries.isEmpty()) null else selectedExportLibraries.toList()
                val json = viewModel.getLibraryJson(targetIds)
                context.contentResolver.openOutputStream(it)?.use { output ->
                    output.write(json.toByteArray())
                }
                selectedExportLibraries = emptySet()
            }
        }
    }
    
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val json = input.bufferedReader().readText()
                    viewModel.importLibraryJson(json)
                }
            }
        }
    }

    // Sync Pager State with List Scroll
    LaunchedEffect(isListMode) {
        if (isListMode) {
            listState.scrollToItem(pagerState.currentPage)
        }
    }

    // MAIN LAYOUT STRUCTURE
    // Replaced Drawer with direct Scaffold
    // Use BoxWithConstraints to get screen width for gesture detection
    BoxWithConstraints {
        val screenWidthPx = constraints.maxWidth.toFloat()
        // Use screenWidthPx to suppress unused warning or actually use it
        if (screenWidthPx < 0) { } 
        
        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    // Swipe Right to Open Library Sheet (from Left Edge)
                    if (dragAmount > 10 && !showLibrarySheet && change.position.x < 100) {
                        showLibrarySheet = true
                        change.consume()
                    }
                }
            },
            topBar = {
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    // 1. Top Bar (Title + Actions)
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
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch Library", modifier = Modifier.size(20.dp))
                            }
                        },
                        navigationIcon = {
                            if (isListMode && selectedWords.isNotEmpty()) {
                                Row {
                                    // Move Test and Delete to left
                                    var showTestTypeDialog by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showTestTypeDialog = true }) {
                                        Icon(Icons.Default.PlayArrow, "Test", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        viewModel.deleteWords(selectedWords.toList())
                                        selectedWords = emptySet()
                                    }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                    
                                    if (showTestTypeDialog) {
                                        TestTypeSelectionDialog(onDismiss = { showTestTypeDialog = false }, onConfirm = { type ->
                                            val selectedList = words.filter { selectedWords.contains(it.id) }
                                            viewModel.setTestCandidates(selectedList)
                                            viewModel.setTestType(type)
                                            showTestTypeDialog = false
                                            onJumpToTest() // Navigate to Test Screen
                                        })
                                    }
                                }
                            } else {
                                // Menu Icon for Library Sheet
                                IconButton(onClick = { showLibrarySheet = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Libraries"
                                    )
                                }
                            }
                        },
                        actions = {
                            // Action buttons based on mode
                            if (isListMode) {
                                // Test/Delete Selected - Moved to Left (navigationIcon)
                                
                                // Sort
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
                            } else {
                                // Card Mode Actions
                                IconButton(onClick = { isListMode = true }) {
                                    Icon(Icons.Default.List, contentDescription = "List Mode")
                                }
                                // Profile Icon (Moved to Right)
                                IconButton(onClick = onOpenProfile) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Profile",
                                        modifier = Modifier.size(28.dp).clip(CircleShape)
                                    )
                                }
                                // Settings
                                IconButton(onClick = onOpenSettings) {
                                   Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            }
                        }
                    )
                    
                    // 2. Persistent Search Bar (Always Visible under Title)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query -> 
                                searchQuery = query
                                // Auto-scroll logic if in List Mode
                                if (isListMode && query.isNotEmpty()) {
                                     val index = words.indexOfFirst { it.word.contains(query, ignoreCase = true) }
                                     if (index != -1) {
                                         scope.launch { listState.animateScrollToItem(index) }
                                     }
                                }
                            },
                            placeholder = { Text("全局搜索 / AI 录入 (Enter)") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(50), // Rounded style
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.handleGlobalSearch(searchQuery)
                                }
                            )
                        )
                        
                        if (!isListMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            // Toggle Side List Button
                            IconButton(onClick = { showSideList = !showSideList }) {
                                Icon(
                                    imageVector = if (showSideList) Icons.Default.KeyboardArrowRight else Icons.Default.List,
                                    contentDescription = "Toggle List"
                                )
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isListMode) {
                    ExtendedFloatingActionButton(
                        onClick = { showImportSheet = true },
                        icon = { Icon(Icons.Default.Add, "Import") },
                        text = { Text("导入") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { padding ->
        // CONTENT AREA
        AnimatedContent(
            targetState = isListMode,
            label = "ModeSwitch",
            modifier = Modifier.padding(padding)
        ) { mode ->
            if (mode) {
                // LIST MODE CONTENT
                if (words.isEmpty()) {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Text("当前词库为空")
                     }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
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
                                            .clickable { }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onDoubleTap = {
                                                        scope.launch {
                                                            prefs.edit().putInt("last_index_${currentLibraryId}", index).apply()
                                                            if (currentLibrary != null) {
                                                                viewModel.updateLibraryLastIndex(currentLibrary.id, index)
                                                            }
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
            } else {
                // CARD MODE CONTENT
    LaunchedEffect(currentLibraryId, words) {
        if (words.isNotEmpty()) {
            if (pendingJumpWordId != null) {
                 val index = words.indexOfFirst { it.id == pendingJumpWordId }
                 if (index != -1) {
                     pagerState.scrollToPage(index)
                     // pendingJumpWordId is read-only from StateFlow, so we clear it via ViewModel
                     viewModel.clearPendingJump()
                 }
            } else if (!isNavigatingFromSearch) {
                // Check if we have a saved index
                val lastIndex = viewModel.getInitialLastIndex(currentLibraryId)
                if (lastIndex in words.indices && pagerState.currentPage != lastIndex) {
                    pagerState.scrollToPage(lastIndex)
                }
            } else {
                // If this triggered because of search navigation (e.g. library switch), we skip restore.
                // Reset flag for future normal library switches.
                isNavigatingFromSearch = false
            }
        }
    }
                
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.updateLibraryLastIndex(currentLibraryId, pagerState.currentPage)
                }
                
                if (words.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("当前词库为空，请添加单词")
                    }
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) { page ->
                            val word = words[page]
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
                                        onEditClick = { editingWord = word },
                                        onSpeakClick = { viewModel.speak(word.word) }
                                    )
                                }
                            }
                        }
                        
                        // Side List
                        AnimatedVisibility(
                            visible = showSideList,
                            enter = slideInHorizontally { it } + expandHorizontally(expandFrom = Alignment.Start),
                            exit = slideOutHorizontally { it } + shrinkHorizontally(shrinkTowards = Alignment.Start)
                        ) {
                            val sideListState = rememberLazyListState()
                            val density = LocalDensity.current
                            val itemHeightDp = 40.dp
                            
                            LaunchedEffect(pagerState.currentPage) {
                                 // Center the item: offset = (viewportHeight / 2) - (itemHeight / 2)
                                 // We need viewport height. If not available yet, we rely on layout info change?
                                 // Actually, we can get it from sideListState.layoutInfo.viewportSize.height
                                 // But inside LaunchedEffect, it might be 0 initially.
                                 // A simple way is to use a large enough offset or wait for layout.
                                 // Let's try to get it from layoutInfo.
                                 val viewportHeight = sideListState.layoutInfo.viewportSize.height
                                 if (viewportHeight > 0) {
                                     val itemHeightPx = with(density) { itemHeightDp.toPx() }
                                     val offset = (viewportHeight / 2) - (itemHeightPx / 2)
                                     sideListState.animateScrollToItem(pagerState.currentPage, -offset.toInt())
                                 } else {
                                     sideListState.animateScrollToItem(pagerState.currentPage)
                                 }
                            }
                            
                            LazyColumn(
                                state = sideListState,
                                modifier = Modifier
                                    .width(90.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                itemsIndexed(words) { index, word ->
                                    val isSelected = index == pagerState.currentPage
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(itemHeightDp)
                                            .background(if (isSelected) Color(0xFF2196F3) else Color.Transparent)
                                            .clickable { scope.launch { pagerState.scrollToPage(index) } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = word.word,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
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
                item {
                     // Select All Header for Export
                     Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                     ) {
                         Text("选择词库 (${libraries.size})", style = MaterialTheme.typography.titleMedium)
                         TextButton(onClick = {
                             selectedExportLibraries = if (selectedExportLibraries.size == libraries.size) {
                                 emptySet()
                             } else {
                                 libraries.map { it.id }.toSet()
                             }
                         }) {
                             Text(if (selectedExportLibraries.size == libraries.size) "全不选" else "全选")
                         }
                     }
                }
                
                items(libraries) { lib ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                             // If clicking item, just switch (default behavior)
                             viewModel.switchLibrary(lib.id)
                             showLibrarySheet = false
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedExportLibraries.contains(lib.id),
                            onCheckedChange = { checked ->
                                selectedExportLibraries = if (checked) {
                                    selectedExportLibraries + lib.id
                                } else {
                                    selectedExportLibraries - lib.id
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lib.name)
                        }
                        
                        // Delete Button (Don't allow deleting default library id=1)
                        if (lib.id != 1) {
                            IconButton(onClick = { viewModel.deleteLibrary(lib.id) }) {
                                Icon(Icons.Default.Delete, "Delete Library", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        if (lib.id == currentLibraryId) {
                            Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        // Instead of auto-adding, show dialog
                        showAddLibraryDialog = true
                        showLibrarySheet = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("新建词库")
                    }
                }
                
                // Export/Import Buttons
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { 
                                showLibrarySheet = false
                                exportLauncher.launch("magicword_export_${if(selectedExportLibraries.size > 1) "multi" else "single"}_${System.currentTimeMillis()}.json")
                            }, 
                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                            enabled = selectedExportLibraries.isNotEmpty() || currentLibraryId != 0 // Fallback to current if none selected? User said "Select check box". Let's enforce selection for multi, or default to current.
                            // Actually user said: "Check box on right".
                            // Let's assume if selection is empty, we export CURRENT. If selection not empty, export SELECTED.
                        ) {
                            Text(if (selectedExportLibraries.isEmpty()) "导出当前" else "导出选中 (${selectedExportLibraries.size})")
                        }
                        Button(
                            onClick = { 
                                showLibrarySheet = false
                                importLauncher.launch(arrayOf("application/json"))
                            }, 
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) {
                            Text("导入词库")
                        }
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
            },
            onSpeak = { viewModel.speak(it) }
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
}

@Composable
fun TestTypeSelectionDialog(onDismiss: () -> Unit, onConfirm: (LibraryViewModel.TestType) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择测试类型") },
        text = {
            Column {
                TextButton(onClick = { onConfirm(LibraryViewModel.TestType.CHOICE) }) {
                    Text("选择题模式")
                }
                TextButton(onClick = { onConfirm(LibraryViewModel.TestType.SPELL) }) {
                    Text("拼写模式")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
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
