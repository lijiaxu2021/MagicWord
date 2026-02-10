package com.magicword.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState

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
    var showListMode by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    
    // For Word Editing
    var editingWord by remember { mutableStateOf<Word?>(null) }

    if (showSearch) {
        SearchScreenWrapper(onBack = { showSearch = false })
        return
    }

    if (showListMode) {
        // Reuse/Modify LibraryScreen content or create new List View
        WordListMode(
            words = words,
            onBack = { showListMode = false },
            onWordClick = { word -> editingWord = word }
        )
    } else {
        // Full Screen Card Mode with VerticalPager
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
                        IconButton(onClick = { showListMode = true }) {
                            Icon(Icons.Default.List, contentDescription = "List")
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
                val pagerState = rememberPagerState(pageCount = { words.size })
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) { page ->
                    val word = words[page]
                    
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
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

    // Library Switcher Bottom Sheet
    if (showLibrarySheet) {
        ModalBottomSheet(onDismissRequest = { showLibrarySheet = false }) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                item {
                    Text("切换词库", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                }
                items(libraries) { lib ->
                    ListItem(
                        headlineContent = { Text(lib.name) },
                        modifier = Modifier.clickable {
                            viewModel.switchLibrary(lib.id)
                            LogUtil.logFeature("SwitchLibrary", "Success", "{ \"new_lib\": \"${lib.name}\" }")
                            showLibrarySheet = false
                        },
                        trailingContent = {
                            if (lib.id == currentLibraryId) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
                item {
                    Button(
                        onClick = { 
                             viewModel.addLibrary("新词库 ${libraries.size + 1}")
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("新建词库")
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }

    // Word Edit/Detail Modal
    if (editingWord != null) {
        WordDetailEditDialog(
            word = editingWord!!,
            onDismiss = { editingWord = null },
            onSave = { updatedWord ->
                viewModel.updateWord(updatedWord)
                // Log update
                LogUtil.logFeature("UpdateWord", "Success", "{ \"word\": \"${updatedWord.word}\" }")
                editingWord = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListMode(words: List<Word>, onBack: () -> Unit, onWordClick: (Word) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("单词列表") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(words) { word ->
                ListItem(
                    headlineContent = { Text(word.word) },
                    supportingContent = { Text(word.definitionCn, maxLines = 1) },
                    modifier = Modifier.clickable { onWordClick(word) }
                )
                Divider()
            }
        }
    }
}

@Composable
fun SearchScreenWrapper(onBack: () -> Unit) {
    // Re-using SearchScreen but wrapping it to handle back navigation if needed
    // Assuming SearchScreen handles its own content. 
    // We might need to modify SearchScreen to accept an onBack if we want to return to Home.
    // For now, let's just show it.
    Column {
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        SearchScreen()
    }
}
