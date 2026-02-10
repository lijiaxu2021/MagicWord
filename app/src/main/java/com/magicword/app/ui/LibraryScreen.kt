package com.magicword.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import com.magicword.app.data.Word
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.clickable
import com.magicword.app.utils.LogUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )
    val words by viewModel.allWords.collectAsState(initial = emptyList())
    val libraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    val currentLibraryId by viewModel.currentLibraryId.collectAsState()
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var showLibraryMenu by remember { mutableStateOf(false) }
    var showAddLibraryDialog by remember { mutableStateOf(false) }
    
    var editingWord by remember { mutableStateOf<Word?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = libraries.find { it.id == currentLibraryId }?.name ?: "词库",
                    style = MaterialTheme.typography.titleLarge
                )
                Box {
                    IconButton(onClick = { showLibraryMenu = true }) {
                        Icon(Icons.Default.List, contentDescription = "Switch Library")
                    }
                    DropdownMenu(
                        expanded = showLibraryMenu,
                        onDismissRequest = { showLibraryMenu = false }
                    ) {
                        libraries.forEach { lib ->
                            DropdownMenuItem(
                                text = { Text(lib.name) },
                                onClick = {
                                    viewModel.switchLibrary(lib.id)
                                    showLibraryMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("➕ 新建词库") },
                            onClick = {
                                showLibraryMenu = false
                                showAddLibraryDialog = true
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Bulk Import")
            }
        }
    ) { padding ->
        if (words.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("当前词库是空的，快去添加单词吧！")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(words) { word ->
                    WordItem(
                        word = word, 
                        onDelete = { viewModel.deleteWord(word) },
                        onClick = { editingWord = word }
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                BulkImportContent(
                    viewModel = viewModel,
                    onDismiss = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        }
        
        if (showAddLibraryDialog) {
            AddLibraryDialog(
                onDismiss = { showAddLibraryDialog = false },
                onConfirm = { name ->
                    viewModel.addLibrary(name)
                    showAddLibraryDialog = false
                }
            )
        }
        
        // Word Edit Dialog
        if (editingWord != null) {
            WordDetailEditDialog(
                word = editingWord!!,
                onDismiss = { editingWord = null },
                onSave = { updatedWord ->
                    viewModel.updateWord(updatedWord)
                    LogUtil.logFeature("UpdateWord", "Success", "{ \"word\": \"${updatedWord.word}\" }")
                    editingWord = null
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
        Spacer(modifier = Modifier.weight(1f)) // Push content up when keyboard opens
        
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
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun WordItem(word: Word, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = word.definitionCn,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
