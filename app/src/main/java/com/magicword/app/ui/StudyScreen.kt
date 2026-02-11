package com.magicword.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import com.magicword.app.data.Word
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.window.Dialog

@Composable
fun StudyScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )
    
    val dueWords by viewModel.dueWords.collectAsState(initial = emptyList())
    val allLibraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    val studyLibraryIds by viewModel.studyLibraryIds.collectAsState()
    
    var currentWordIndex by remember { mutableIntStateOf(0) }
    var isReviewing by remember { mutableStateOf(false) }
    var showAnswer by remember { mutableStateOf(false) }
    var showLibrarySelector by remember { mutableStateOf(false) }
    
    // When review session starts or words update, reset
    val wordsToReview = remember(dueWords, isReviewing) {
        if (isReviewing) dueWords else emptyList()
    }
    
    val currentWord = wordsToReview.getOrNull(currentWordIndex)
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isReviewing) {
            // Dashboard View - Redesigned
            
            // 1. Multi-Library Selector at Top
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { showLibrarySelector = true }
                    .padding(8.dp)
            ) {
                Text(
                    text = if (studyLibraryIds.isEmpty()) "所有词库" else "已选 ${studyLibraryIds.size} 个词库",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Libraries")
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 2. "Today's Study" prominent at top
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("今日待复习", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${dueWords.size}", 
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("个单词", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { 
                            isReviewing = true 
                            currentWordIndex = 0
                            showAnswer = false
                        },
                        enabled = dueWords.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始复习", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    if (dueWords.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("🎉 时间非常宝贵，今日任务已完成！", color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                    }
                }
            }
            
            // Library Selection Dialog
            if (showLibrarySelector) {
                Dialog(onDismissRequest = { showLibrarySelector = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .heightIn(max = 400.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "选择学习词库",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                items(allLibraries) { library ->
                                    val isSelected = studyLibraryIds.contains(library.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleStudyLibrary(library.id) }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.toggleStudyLibrary(library.id) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = library.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Divider()
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showLibrarySelector = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("确定")
                            }
                        }
                    }
                }
            }
            
        } else {
            // Review Session View (Keep existing logic)
            if (currentWord != null) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = (currentWordIndex + 1).toFloat() / wordsToReview.size,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("进度: ${currentWordIndex + 1}/${wordsToReview.size}")
                    TextButton(onClick = { isReviewing = false }) { Text("退出") }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Flashcard
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { showAnswer = !showAnswer },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentWord.word,
                                style = MaterialTheme.typography.displayMedium,
                                textAlign = TextAlign.Center
                            )
                            if (currentWord.phonetic != null) {
                                Text(
                                    text = currentWord.phonetic,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            AnimatedContent(
                                targetState = showAnswer,
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "Answer"
                            ) { isVisible ->
                                if (isVisible) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = currentWord.definitionCn,
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center
                                        )
                                        if (!currentWord.example.isNullOrBlank()) {
                                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                                            Text(
                                                text = currentWord.example,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (!currentWord.memoryMethod.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "💡 ${currentWord.memoryMethod}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "点击查看释义",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Rating Buttons
                if (showAnswer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ReviewButton("忘记", Color(0xFFE57373)) { processResult(viewModel, currentWord, 0, { currentWordIndex++ }, { isReviewing = false }) }
                        ReviewButton("困难", Color(0xFFFFB74D)) { processResult(viewModel, currentWord, 3, { currentWordIndex++ }, { isReviewing = false }) }
                        ReviewButton("良好", Color(0xFF81C784)) { processResult(viewModel, currentWord, 4, { currentWordIndex++ }, { isReviewing = false }) }
                        ReviewButton("简单", Color(0xFF64B5F6)) { processResult(viewModel, currentWord, 5, { currentWordIndex++ }, { isReviewing = false }) }
                    }
                } else {
                    Button(
                        onClick = { showAnswer = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("显示答案")
                    }
                }
            } else {
                // Session Finished
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉 复习完成！", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { isReviewing = false }) {
                        Text("返回首页")
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewButton(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.width(80.dp)
    ) {
        Text(label, fontSize = 12.sp, maxLines = 1)
    }
}

fun processResult(
    viewModel: LibraryViewModel, 
    word: Word, 
    quality: Int, 
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    viewModel.processReview(word, quality)
    // Delay slightly? No need for instant update
    onNext()
}
