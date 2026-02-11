package com.magicword.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.magicword.app.data.Library
import com.magicword.app.data.Word
import com.magicword.app.data.WordList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WordListScreen(viewModel: LibraryViewModel) {
    val wordLists by viewModel.allWordLists.collectAsState(initial = emptyList())
    val currentWordListId by viewModel.currentWordListId.collectAsState()
    val allLibraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Display Mode: 0=Both, 1=En, 2=Cn
    var displayMode by remember { mutableIntStateOf(0) }
    
    val currentWordList = wordLists.find { it.id == currentWordListId }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "Create List")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Top Bar / Selector
            WordListSelector(
                wordLists = wordLists,
                currentList = currentWordList,
                onSelect = { viewModel.setCurrentWordListId(it.id) },
                onDelete = { viewModel.deleteWordList(it) }
            )
            
            if (currentWordList != null) {
                // Content
                WordListContent(
                    wordList = currentWordList,
                    allLibraries = allLibraries,
                    viewModel = viewModel,
                    displayMode = displayMode,
                    onToggleMode = {
                        displayMode = (displayMode + 1) % 3
                    },
                    onWordClick = { libId, wordId ->
                        viewModel.jumpToWord(libId, wordId)
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("请创建或选择一个单词表")
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
    
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
    onToggleMode: () -> Unit,
    onWordClick: (Int, Int) -> Unit
) {
    // Parse library IDs
    val libraryIds: List<Int> = remember(wordList.libraryIdsJson) {
        try {
            val type = object : TypeToken<List<Int>>() {}.type
            Gson().fromJson(wordList.libraryIdsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Fetch words for these libraries
    // Note: We need to fetch words. Since this is a Composable, we can use `produceState` or observe flows.
    // Ideally viewModel should provide a flow for "Words in Current WordList".
    // But for simplicity, we can fetch them here using a LaunchedEffect if viewModel exposes a fetch function,
    // or better, create a derived flow in ViewModel. 
    // Given the constraints, let's just query via Dao in ViewModel and expose a State.
    // BUT, we can reuse `viewModel.wordDao.getWordsByLibrary(id)` flow.
    
    // Let's load them all. This might be heavy if lists are huge.
    // "WordList" implies a collection. 
    
    // Quick solution: Create a map of Library -> List<Word>
    val libraryWordsMap = remember { mutableStateMapOf<Int, List<Word>>() }
    
    LaunchedEffect(libraryIds) {
        libraryWordsMap.clear()
        libraryIds.forEach { libId ->
            // We need a suspend function to get list, flow is annoying inside loop for map
            // Assuming we added `getWordsByLibraryList` (suspend) in Dao
             try {
                 val words = viewModel.wordDao.getWordsByLibraryList(libId)
                 libraryWordsMap[libId] = words
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        }
    }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                
                items(words) { word ->
                    WordListItem(
                        word = word,
                        displayMode = displayMode,
                        onLongClick = onToggleMode,
                        onClick = { onWordClick(libId, word.id) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
