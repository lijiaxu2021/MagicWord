package com.magicword.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.magicword.app.data.Library
import com.magicword.app.data.Word
import com.magicword.app.data.WordList
import kotlinx.coroutines.launch

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordListScreen(viewModel: LibraryViewModel) {
    val wordLists by viewModel.allWordLists.collectAsState(initial = emptyList())
    val currentWordListId by viewModel.currentWordListId.collectAsState()
    val allLibraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    
    // Ensure persistence of current word list ID is handled by ViewModel init and updates
    // Check if currentWordListId is valid
    LaunchedEffect(wordLists) {
        if (wordLists.isNotEmpty() && currentWordListId == -1) {
            viewModel.setCurrentWordListId(wordLists.first().id)
        }
    }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Right Drawer State
    var showDrawer by remember { mutableStateOf(false) }
    
    val currentWordList = wordLists.find { it.id == currentWordListId }
    val displayMode = currentWordList?.viewMode ?: 0
    
    // Shared List State for scrolling
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Map of Library ID to Start Index (for jumping)
    val libraryIndexMap = remember { mutableStateMapOf<Int, Int>() }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = { },
                            onDragCancel = { },
                            onHorizontalDrag = { change, dragAmount ->
                                // Swipe Left (Open Drawer): dragAmount < 0
                                // Swipe Right (Close Drawer): dragAmount > 0 (handled by drawer overlay usually, but nice to have)
                                
                                // Open Drawer logic: Swipe Left from Right Edge
                                if (dragAmount < -10 && !showDrawer && change.position.x > (screenWidthPx - 100)) {
                                    showDrawer = true
                                    change.consume()
                                }
                            }
                        )
                    },
                floatingActionButton = {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "Create List")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Top Bar / Selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WordListSelector(
                            wordLists = wordLists,
                            currentList = currentWordList,
                            onSelect = { viewModel.setCurrentWordListId(it.id) },
                            onDelete = { viewModel.deleteWordList(it) }
                        )
                    }
                    // Drawer Trigger
                    IconButton(onClick = { showDrawer = !showDrawer }) {
                        Icon(Icons.Default.Menu, "Libraries")
                    }
                }
                
                if (currentWordList != null) {
                    // Content
                    WordListContent(
                        wordList = currentWordList,
                        allLibraries = allLibraries,
                        viewModel = viewModel,
                        displayMode = displayMode,
                        listState = listState,
                        onToggleMode = {
                            val newMode = (displayMode + 1) % 3
                            viewModel.updateWordList(currentWordList.copy(viewMode = newMode))
                        },
                        onWordClick = { libId, wordId ->
                            viewModel.jumpToWord(libId, wordId)
                        },
                        onLibraryPositionsCalculated = { map ->
                            libraryIndexMap.clear()
                            libraryIndexMap.putAll(map)
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请创建或选择一个单词表")
                    }
                }
            }
        }
        
        // Right Drawer Overlay
        // Dimming Background
        if (showDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(1f)
                    .clickable { showDrawer = false }
            )
        }
        
        // Drawer Content
        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }), // From Right
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd).zIndex(2f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 250.dp, max = 300.dp)
                    .pointerInput(Unit) {
                        // Consume clicks to prevent closing when clicking inside drawer
                        detectTapGestures { } 
                    },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "包含的词库",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (currentWordList != null) {
                        val libraryIds: List<Int> = remember(currentWordList.libraryIdsJson) {
                            try {
                                val type = object : TypeToken<List<Int>>() {}.type
                                Gson().fromJson(currentWordList.libraryIdsJson, type) ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                        
                        LazyColumn {
                            items(libraryIds) { libId ->
                                val lib = allLibraries.find { it.id == libId }
                                if (lib != null) {
                                    ListItem(
                                        headlineContent = { Text(lib.name) },
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                val index = libraryIndexMap[libId]
                                                if (index != null) {
                                                    listState.scrollToItem(index)
                                                }
                                                showDrawer = false
                                            }
                                        }
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
    
    if (showCreateDialog) {
        CreateWordListDialog(
            allLibraries = allLibraries,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, ids ->
                viewModel.createWordList(name, ids)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun WordListSelector(
    wordLists: List<WordList>,
    currentList: WordList?,
    onSelect: (WordList) -> Unit,
    onDelete: (WordList) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(currentList?.name ?: "选择单词表")
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (wordLists.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("无单词表") },
                    onClick = { expanded = false }
                )
            }
            wordLists.forEach { list ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(list.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDelete(list); expanded = false }) {
                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                            }
                        }
                    },
                    onClick = { 
                        onSelect(list)
                        expanded = false 
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordListContent(
    wordList: WordList,
    allLibraries: List<Library>,
    viewModel: LibraryViewModel,
    displayMode: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onToggleMode: () -> Unit,
    onWordClick: (Int, Int) -> Unit,
    onLibraryPositionsCalculated: (Map<Int, Int>) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("word_list_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Parse library IDs
    val libraryIds: List<Int> = remember(wordList.libraryIdsJson) {
        try {
            val type = object : TypeToken<List<Int>>() {}.type
            Gson().fromJson(wordList.libraryIdsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Fetch words
    val libraryWordsMap = remember { mutableStateMapOf<Int, List<Word>>() }
    
    LaunchedEffect(libraryIds) {
        libraryWordsMap.clear()
        libraryIds.forEach { libId ->
             try {
                 val words = viewModel.wordDao.getWordsByLibraryList(libId)
                 libraryWordsMap[libId] = words
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }
    }
    
    // Restore Position
    LaunchedEffect(wordList.id) {
        val index = prefs.getInt("scroll_index_${wordList.id}", 0)
        val offset = prefs.getInt("scroll_offset_${wordList.id}", 0)
        if (index >= 0) {
            listState.scrollToItem(index, offset)
        }
    }
    
    // Save Position
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                prefs.edit()
                    .putInt("scroll_index_${wordList.id}", index)
                    .putInt("scroll_offset_${wordList.id}", offset)
                    .apply()
            }
    }
    
    // Calculate Indices for Jumping
    // Since we build the list dynamically, we need to know where each library starts.
    // We can do this by pre-calculating or just using the sticky header indices?
    // LazyColumn items logic:
    // Header (1) + Items (N) + Header (1) + Items (M)...
    // We can calculate this:
    LaunchedEffect(libraryWordsMap.size, displayMode) {
        val map = mutableMapOf<Int, Int>()
        var currentIndex = 0
        libraryIds.forEach { libId ->
            val words = libraryWordsMap[libId] ?: emptyList()
            if (words.isNotEmpty()) {
                map[libId] = currentIndex // Header is here
                currentIndex++ // Header
                
                // Add items count
                if (displayMode == 1) {
                    // Table Mode: Chunked by 3
                    val rows = (words.size + 2) / 3
                    currentIndex += rows
                } else {
                    // List Mode
                    currentIndex += words.size
                }
            }
        }
        onLibraryPositionsCalculated(map)
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        libraryIds.forEach { libId ->
            val library = allLibraries.find { it.id == libId }
            val words = libraryWordsMap[libId] ?: emptyList()
            
            if (library != null && words.isNotEmpty()) {
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "—— ${library.name} ——",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                if (displayMode == 1) {
                    // Table Mode (English Only, Compact)
                    val chunks = words.chunked(3)
                    items(chunks) { chunk ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            chunk.forEach { word ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                     onWordClick(libId, word.id)
                                                },
                                                onLongPress = {
                                                     onToggleMode()
                                                },
                                                onTap = {
                                                     // Single tap logic if needed
                                                }
                                            )
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = word.word,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }
                            // Fill remaining space if chunk < 3
                            repeat(3 - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    // List Mode
                    items(words) { word ->
                        WordListItem(
                            word = word,
                            displayMode = displayMode,
                            onLongClick = onToggleMode,
                            onClick = { 
                                // Single Click - maybe nothing or edit?
                            },
                            onDoubleClick = {
                                onWordClick(libId, word.id)
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordListItem(
    word: Word,
    displayMode: Int, // 0=Both, 1=En, 2=Cn
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onDoubleTap = { onDoubleClick() },
                    onTap = { onClick() }
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // EN
        if (displayMode == 0 || displayMode == 1) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        
        // CN
        if (displayMode == 0 || displayMode == 2) {
            if (displayMode == 0) Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = word.definitionCn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(if(displayMode == 0) 1f else 1f)
            )
        }
    }
}

@Composable
fun CreateWordListDialog(
    allLibraries: List<Library>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<Int>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateMapOf<Int, Boolean>() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建单词表") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("选择包含的词库:")
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(allLibraries) { lib ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds[lib.id] = !(selectedIds[lib.id] ?: false)
                                }
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = selectedIds[lib.id] ?: false,
                                onCheckedChange = { selectedIds[lib.id] = it }
                            )
                            Text(lib.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ids = selectedIds.filter { it.value }.keys.toList()
                    if (name.isNotBlank() && ids.isNotEmpty()) {
                        onConfirm(name, ids)
                    }
                }
            ) {
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
