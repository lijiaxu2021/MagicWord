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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordsScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao())
    )
    val words by viewModel.allWords.collectAsState(initial = emptyList())
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
    val pagerState = rememberPagerState(pageCount = { words.size })
    val scope = rememberCoroutineScope()

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
                        itemsIndexed(words) { index, word ->
                            val isSelected = selectedWords.contains(word.id)
                            ListItem(
                                leadingContent = {
                                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                                },
                                headlineContent = { Text(word.word) },
                                supportingContent = { 
                                    Text("${word.definitionCn} · 复习: ${word.reviewCount}", maxLines = 1) 
                                },
                                trailingContent = {
                                    Checkbox(checked = isSelected, onCheckedChange = { checked ->
                                        selectedWords = if (checked) selectedWords + word.id else selectedWords - word.id
                                    })
                                },
                                modifier = Modifier
                                    .clickable { 
                                        editingWord = word
                                    }
                            )
                            HorizontalDivider()
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
                    val lastIndex = prefs.getInt("last_index_${currentLibraryId}", 0)
                    if (lastIndex in words.indices) {
                        pagerState.scrollToPage(lastIndex)
                    }
                }
            }
            
            // Save Pager State
            LaunchedEffect(pagerState.currentPage) {
                prefs.edit().putInt("last_index_${currentLibraryId}", pagerState.currentPage).apply()
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
                    Button(onClick = { viewModel.addLibrary("新词库 ${libraries.size + 1}") }, modifier = Modifier.fillMaxWidth()) {
                        Text("新建词库")
                    }
                }
            }
        }
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
}
