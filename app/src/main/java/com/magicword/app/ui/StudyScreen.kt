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

import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.window.Dialog

@Composable
fun StudyScreen() {
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

    val dueWords by viewModel.dueWords.collectAsState(initial = emptyList())
    val allLibraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    val studyLibraryIds by viewModel.studyLibraryIds.collectAsState()
    
    var currentWordIndex by remember { mutableIntStateOf(0) }
    var isReviewing by remember { mutableStateOf(false) }
    var showAnswer by remember { mutableStateOf(false) }
    var showLibrarySelector by remember { mutableStateOf(false) }
    
    // Auto-start review if configured or maybe not to be too intrusive?
    // User said "进去以后就显示单词了" -> Means auto start?
    // Let's check dueWords size. If > 0, we can default to isReviewing=true?
    // But dueWords might load async.
    // Let's add a "Auto Start" effect.
    // BUT user also said "Display dashboard". 
    // Wait, "就是学习界面 进去以后就显示单词了要" -> He wants to skip the dashboard if there are words?
    // Or maybe he means the dashboard should show words directly?
    // "学习界面 进去以后就显示单词了要" -> Likely means: Don't show "Start Review" button, show the first card immediately.
    // So default isReviewing = true if dueWords.isNotEmpty().
    
    // However, dueWords is a Flow. It starts empty then populates.
    // We can use a LaunchedEffect to trigger once when dueWords becomes non-empty for the first time?
    // Or just default UI state.
    
    // Let's try: If dueWords > 0, show Card immediately.
    // But we need to handle the "Finished" state too.
    
    // User also said: "学完以后谁说不能在学一下呢 至少要有入口啊" -> Add "Review Again" button.
    
    // Auto-start Logic:
    // We need to distinguish "Just entered screen" vs "Finished review".
    // Let's use a state `hasStartedReview`.
    
    // Actually, simply setting isReviewing = true when dueWords > 0 might be what he wants.
    // But if he exits, he might want to see dashboard?
    // "进去以后就显示单词" -> On entry.
    
    LaunchedEffect(Unit) {
        // Delay slightly to wait for DB load? 
        // No, flow will update.
        // If we want to auto-start, we can watch dueWords.
        // But we don't want to auto-restart after finishing.
    }
    
    // Let's make "isReviewing" default to true if we have words? 
    // But dueWords is initially empty.
    
    // Better approach: Show Dashboard only if dueWords is EMPTY (Finished).
    // If dueWords has content, show Flashcard immediately.
    // But wait, if dueWords has content, user might want to configure libraries first?
    // User said: "以及学习的词库列表要记住啊" -> We did persist it.
    // So if persistence works, he enters, sees words immediately.
    // To change libraries, he needs a way to access selector while reviewing?
    // Or we keep the top bar visible even during review.
    
    // Let's modify the UI structure:
    // Top Bar (Library Selector) always visible? 
    // Or just make "Dashboard" the "Empty State".
    // And "Review Mode" the "Default State" when words exist.
    
    val wordsToReview = remember(dueWords, isReviewing) {
        if (dueWords.isNotEmpty()) dueWords else emptyList() // Always use dueWords if available?
    }
    
    // Logic: 
    // If dueWords.isNotEmpty(), we are effectively "reviewing".
    // Unless we manually stopped?
    // Let's introduce `isSessionActive` default true.
    
    var isSessionActive by remember { mutableStateOf(true) }
    
    // If dueWords is empty, isSessionActive doesn't matter, we show "Finished/Empty" view.
    // If dueWords is not empty, and isSessionActive is true, we show Card.
    // If dueWords is not empty, and isSessionActive is false (user clicked Exit), we show Dashboard?
    
    // Actually, user wants "enter -> show words".
    // So:
    // 1. Load StudyScreen.
    // 2. dueWords loads.
    // 3. If dueWords > 0, show Card.
    // 4. User can click "Exit" to go to Dashboard (to switch libraries etc).
    
    // Let's adjust the `isReviewing` logic.
    
    // We need to persist `isReviewing`? No.
    
    // Effect to auto-start once when words load?
    // Use a flag `hasAutoStarted` to prevent re-starting after manual exit.
    var hasAutoStarted by remember { mutableStateOf(false) }
    
    LaunchedEffect(dueWords.size) {
        if (dueWords.isNotEmpty() && !hasAutoStarted) {
            isReviewing = true
            hasAutoStarted = true
        }
    }

    val currentWord = if (isReviewing && dueWords.isNotEmpty()) dueWords.getOrNull(currentWordIndex) else null
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Always show Library Selector at top?
        // User said: "这个学习为啥一定只要学一个词库能 ... 学习页面应该上面也加一个词库切换"
        // If we show card immediately, we should put Library Selector above the card?
        // Or put it in the top bar.
        
        // Let's put Library Selector at the top ALWAYS.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { showLibrarySelector = true }
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = if (studyLibraryIds.isEmpty()) "所有词库" else "已选 ${studyLibraryIds.size} 个词库",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Libraries")
        }

        if (!isReviewing || dueWords.isEmpty()) {
            // Dashboard / Empty View
            // ... (Existing Dashboard code, but modified for "Finished" state)
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (dueWords.isNotEmpty()) {
                        // Paused State
                        Text("待复习单词", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${dueWords.size}", 
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                isReviewing = true 
                                currentWordIndex = 0
                                showAnswer = false
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("继续复习")
                        }
                    } else {
                        // Finished State
                        Text("🎉 今日任务完成！", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("你已经完成了所有待复习单词。", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // "Review Again" / "Learn More" Entry
                        Button(
                            onClick = { 
                                // Reset logic? 
                                // If no due words, maybe "Review Ahead"? 
                                // Or "Random Review"?
                                // Or just "Force Review 20 words"?
                                // For now, maybe just "Consolidate" (Review All)?
                                // Or user meant: "Even if finished, let me enter review mode again (maybe review future words?)"
                                // WordDao has `getDueWords`. If we want to review *again*, we might need `getWordsForReview` ignoring time?
                                // Let's just provide a button "巩固复习 (随机20个)" for now.
                                // We need a new ViewModel method for this.
                                // For now, let's just show a toast or placeholder if we can't easily implement "Force Review".
                                // User said: "学完以后谁说不能在学一下呢"
                                // Let's assume he wants to review words that are *not yet due* or *already reviewed*.
                                // Let's add a "自由复习" mode?
                                // Or just "Review All"?
                                
                                // Simpler: Just refresh? If DB updates, maybe new words appear?
                                // Or "Review Future Due"?
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = false // Placeholder for now as we need backend support for "Extra Review"
                        ) {
                            Text("巩固复习 (开发中)")
                        }
                    }
                }
            }
        } else {
            // Review Session View
            // ... (Existing Flashcard code)
            // ...
            if (currentWord != null) {
                // Progress Bar
                LinearProgressIndicator(
                    progress = (currentWordIndex + 1).toFloat() / dueWords.size,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                
                // ... (Flashcard Content)
                // Flashcard
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { showAnswer = !showAnswer },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    // ... (Card Content)
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentWord.word,
                                style = MaterialTheme.typography.displayMedium,
                                textAlign = TextAlign.Center
                            )
                            if (currentWord.phonetic != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.padding(top = 8.dp).clickable { viewModel.speak(currentWord.word) }
                                ) {
                                    Icon(Icons.Default.VolumeUp, "Speak", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentWord.phonetic,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
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
                        ReviewButton("忘记", Color(0xFFE57373)) { processResult(viewModel, currentWord, 0, { currentWordIndex++ }, { showAnswer = false }) }
                        ReviewButton("困难", Color(0xFFFFB74D)) { processResult(viewModel, currentWord, 3, { currentWordIndex++ }, { showAnswer = false }) }
                        ReviewButton("良好", Color(0xFF81C784)) { processResult(viewModel, currentWord, 4, { currentWordIndex++ }, { showAnswer = false }) }
                        ReviewButton("简单", Color(0xFF64B5F6)) { processResult(viewModel, currentWord, 5, { currentWordIndex++ }, { showAnswer = false }) }
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
                 // Finished all words in this session
                 isReviewing = false
                 // This will trigger Dashboard view which shows "Finished"
            }
        }
        
        // Library Selection Dialog (Same as before)
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
                                        
                                        // Editable Text for Rename?
                                        // User said "修一个老问题 就是这个词库没法重命名"
                                        // Let's add an Edit button or Long Press to rename.
                                        var isRenaming by remember { mutableStateOf(false) }
                                        var renameText by remember { mutableStateOf(library.name) }
                                        
                                        if (isRenaming) {
                                            OutlinedTextField(
                                                value = renameText,
                                                onValueChange = { renameText = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                trailingIcon = {
                                                    IconButton(onClick = { 
                                                        viewModel.renameLibrary(library.id, renameText)
                                                        isRenaming = false
                                                    }) {
                                                        Icon(Icons.Default.Check, "Save")
                                                    }
                                                }
                                            )
                                        } else {
                                            Text(
                                                text = library.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f).clickable { 
                                                    // Allow click to select, long click to rename?
                                                    // But row is clickable.
                                                    // Let's add a Edit icon button.
                                                }
                                            )
                                            IconButton(onClick = { isRenaming = true }) {
                                                Icon(Icons.Default.Edit, "Rename", modifier = Modifier.size(16.dp))
                                            }
                                        }
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
    resetCardState: () -> Unit
) {
    viewModel.processReview(word, quality)
    // Delay slightly? No need for instant update
    onNext()
    resetCardState()
}
