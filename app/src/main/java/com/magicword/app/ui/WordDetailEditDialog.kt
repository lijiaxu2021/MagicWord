package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.magicword.app.data.Word

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailEditDialog(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (Word) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    // Edit States
    var definitionCn by remember { mutableStateOf(word.definitionCn) }
    var example by remember { mutableStateOf(word.example ?: "") }
    var memoryMethod by remember { mutableStateOf(word.memoryMethod ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false), // Full width
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        title = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(word.word, style = MaterialTheme.typography.headlineMedium)
                    if (!word.phonetic.isNullOrBlank()) {
                        Text(word.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (!isEditing) {
                    TextButton(onClick = { isEditing = true }) { Text("编辑") }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = definitionCn,
                        onValueChange = { definitionCn = it },
                        label = { Text("中文释义") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = example,
                        onValueChange = { example = it },
                        label = { Text("例句") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = memoryMethod,
                        onValueChange = { memoryMethod = it },
                        label = { Text("记忆方法") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                } else {
                    Text("中文释义", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(word.definitionCn, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!word.definitionEn.isNullOrBlank()) {
                        Text("英文释义", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(word.definitionEn, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("例句", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(word.example ?: "暂无", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("记忆方法", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(word.memoryMethod ?: "暂无", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        val updatedWord = word.copy(
                            definitionCn = definitionCn,
                            example = example,
                            memoryMethod = memoryMethod
                        )
                        onSave(updatedWord)
                    }
                ) {
                    Text("保存")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(onClick = { isEditing = false }) {
                    Text("取消")
                }
            }
        }
    )
}
