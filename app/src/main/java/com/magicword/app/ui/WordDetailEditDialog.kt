package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import com.magicword.app.data.Word
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailEditDialog(
    word: Word,
    onDismiss: () -> Unit,
    onSave: (Word) -> Unit,
    onSpeak: (String) -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    
    // Edit States
    var definitionCn by remember { mutableStateOf(word.definitionCn) }
    var example by remember { mutableStateOf(word.example ?: "") }
    var memoryMethod by remember { mutableStateOf(word.memoryMethod ?: "") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false), // Full width
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        title = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(word.word, style = MaterialTheme.typography.headlineMedium)
                        IconButton(onClick = { onSpeak(word.word) }) {
                            Icon(Icons.Default.VolumeUp, "Speak", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
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
                    // Statistics Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📊 学习统计", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("录入时间: ${dateFormat.format(Date(word.createdAt))}", style = MaterialTheme.typography.bodySmall)
                            Text("上次复习: ${if (word.lastReviewTime > 0) dateFormat.format(Date(word.lastReviewTime)) else "从未"}", style = MaterialTheme.typography.bodySmall)
                            Text("复习次数: ${word.reviewCount}", style = MaterialTheme.typography.bodySmall)
                            val total = word.correctCount + word.incorrectCount
                            val accuracy = if (total > 0) (word.correctCount.toFloat() / total * 100).toInt() else 0
                            Text("正确率: $accuracy% (对${word.correctCount}/错${word.incorrectCount})", style = MaterialTheme.typography.bodySmall)
                        }
                    }

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
