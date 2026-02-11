package com.magicword.app.ui

import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.MutableState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.magicword.app.data.TestSession
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import com.magicword.app.data.TestHistory
import com.magicword.app.data.TestResultItem
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import com.magicword.app.data.Word
import com.magicword.app.utils.LogUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.clickable

@Composable
fun TestScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showHistoryScreen by remember { mutableStateOf(false) } // Use full screen for history
    val tabs = listOf("选择题", "拼写")
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )
    val allWords by viewModel.allWords.collectAsState(initial = emptyList())
    val testCandidates by viewModel.testCandidates.collectAsState()
    
    // Use testCandidates if available (Testing Selected), otherwise allWords (Testing Library)
    val words = testCandidates ?: allWords

    val choiceQuizState = rememberSaveable(saver = QuizState.Saver) {
        mutableStateOf(QuizState())
    }
    
    val testType by viewModel.testType.collectAsState()
    
    LaunchedEffect(testType) {
        val targetIndex = when(testType) {
            LibraryViewModel.TestType.CHOICE -> 0
            LibraryViewModel.TestType.SPELL -> 1
        }
        if (selectedTab != targetIndex) {
            selectedTab = targetIndex
        }
    }
    
    // FULL SCREEN HISTORY VIEW
    if (showHistoryScreen) {
        TestHistoryScreen(
            viewModel = viewModel,
            onBack = { showHistoryScreen = false }
        )
        return // Exit TestScreen composable when showing history
    }

    // DB Persistence for Test State
    val savedSession by viewModel.testSession.collectAsState()
    
    // Initialize or Restore State
    LaunchedEffect(savedSession, words) {
        if (words.isNotEmpty()) {
            if (savedSession != null && savedSession!!.libraryId == (testCandidates?.firstOrNull()?.libraryId ?: 1)) {
                 // Restore from DB
                 val indicesType = object : TypeToken<List<Int>>() {}.type
                 val indices: List<Int> = Gson().fromJson(savedSession!!.shuffledIndicesJson, indicesType)
                 
                 // Restore Choice Mode
                 if (savedSession!!.testType == "CHOICE") {
                     choiceQuizState.value = QuizState(
                         currentIndex = savedSession!!.currentIndex,
                         score = savedSession!!.score,
                         isFinished = savedSession!!.isFinished,
                         shuffledIndices = indices
                     )
                     // Sync tab
                     if (selectedTab != 0) selectedTab = 0
                 } 
            }
        }
    }

    // Auto-save State on Change
    LaunchedEffect(choiceQuizState.value) {
        if (words.isNotEmpty() && choiceQuizState.value.shuffledIndices.isNotEmpty()) {
            val session = TestSession(
                id = 1,
                currentIndex = choiceQuizState.value.currentIndex,
                score = choiceQuizState.value.score,
                isFinished = choiceQuizState.value.isFinished,
                shuffledIndicesJson = Gson().toJson(choiceQuizState.value.shuffledIndices),
                testType = "CHOICE",
                libraryId = testCandidates?.firstOrNull()?.libraryId ?: 1 // Approximate library ID
            )
            viewModel.saveTestSession(session)
            
            // If finished, ensure word stats are updated immediately (already handled in onFinish callback but good to be safe)
        }
    }
    
    // Handle App Pause/Resume (Optional, but LaunchedEffect is tied to composition, which is good enough for now)

    Column(modifier = Modifier.fillMaxSize()) {
        // Show Test Source Info and History Button
        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            if (testCandidates != null) {
                Text(
                    text = "正在测试选中单词 (${testCandidates!!.size}个)", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                IconButton(
                    onClick = { viewModel.setTestCandidates(null) },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Filled.Close, "Exit Selection Mode")
                }
            }
            
            IconButton(
                onClick = { showHistoryScreen = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(Icons.Filled.History, "Test History")
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { 
                        selectedTab = index 
                        LogUtil.logFeature("TestTabSwitch", "Manual", "{ \"tab\": \"$title\" }")
                    },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> QuizChoiceMode(
                words = words, 
                state = choiceQuizState.value,
                onStateChange = { choiceQuizState.value = it },
                onBack = {},
                onFinish = { finalState, results ->
                    val history = TestHistory(
                        totalQuestions = words.size,
                        correctCount = finalState.score,
                        testType = "CHOICE",
                        durationSeconds = 0, // Need to track duration
                        questionsJson = Gson().toJson(results)
                    )
                    viewModel.saveTestResult(history)
                    viewModel.clearTestSession()
                    
                    // Update Word Stats for all results - CRITICAL FIX: Ensure this runs!
                    results.forEach { result ->
                        viewModel.updateWordStats(result.wordId, result.isCorrect)
                        LogUtil.logFeature("TestStatsUpdate", "Auto", "{ \"wordId\": ${result.wordId}, \"isCorrect\": ${result.isCorrect} }")
                    }
                }
            )
            1 -> QuizSpellMode(words = words, onBack = {})
        }
    }
}

@Composable
fun TestHistoryScreen(viewModel: LibraryViewModel, onBack: () -> Unit) {
    val history by viewModel.testHistory.collectAsState(initial = emptyList())
    var selectedHistoryItem by remember { mutableStateOf<TestHistory?>(null) }
    
    if (selectedHistoryItem != null) {
        TestHistoryDetailScreen(
            history = selectedHistoryItem!!, 
            onBack = { selectedHistoryItem = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Filled.ArrowBack, "Back")
                }
                Text(
                    "测试历史记录", 
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (history.isEmpty()) {
                    item { 
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("暂无测试记录") 
                        }
                    }
                } else {
                    items(history) { item ->
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                        val percent = if (item.totalQuestions > 0) (item.correctCount * 100 / item.totalQuestions) else 0
                        
                        ListItem(
                            headlineContent = { Text(if(item.testType == "CHOICE") "选择题测试" else "拼写测试") },
                            supportingContent = { Text("$date · 得分: ${item.correctCount}/${item.totalQuestions} ($percent%)") },
                            trailingContent = { Icon(Icons.Filled.List, "Details") },
                            modifier = Modifier.clickable { selectedHistoryItem = item }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun TestHistoryDetailScreen(history: TestHistory, onBack: () -> Unit) {
    val resultItems: List<TestResultItem> = remember(history.questionsJson) {
        try {
             val type = object : TypeToken<List<TestResultItem>>() {}.type
             Gson().fromJson(history.questionsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(history.timestamp))

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Filled.ArrowBack, "Back")
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("测试详情", style = MaterialTheme.typography.titleMedium)
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总题数", style = MaterialTheme.typography.labelMedium)
                    Text("${history.totalQuestions}", style = MaterialTheme.typography.headlineMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("正确", style = MaterialTheme.typography.labelMedium)
                    Text("${history.correctCount}", style = MaterialTheme.typography.headlineMedium, color = Color.Green)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("错误", style = MaterialTheme.typography.labelMedium)
                    Text("${history.totalQuestions - history.correctCount}", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
                }
            }
        }
        
        Text("答题详情", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(resultItems) { item ->
                ListItem(
                    headlineContent = { Text(item.word, style = MaterialTheme.typography.bodyLarge) },
                    supportingContent = { 
                         if (!item.isCorrect && !item.userAnswer.isNullOrBlank()) {
                             Text("你的答案: ${item.userAnswer}", color = Color.Red)
                         }
                    },
                    trailingContent = {
                        if (item.isCorrect) {
                            Icon(Icons.Filled.Check, "Correct", tint = Color.Green)
                        } else {
                            Icon(Icons.Filled.Clear, "Wrong", tint = Color.Red)
                        }
                    }
                )
                Divider()
            }
        }
    }
}

// Simple State Holder for Quiz

@Parcelize
data class QuizState(
    val currentIndex: Int = 0,
    val score: Int = 0,
    val isFinished: Boolean = false,
    val shuffledIndices: List<Int> = emptyList() // Store indices instead of full objects to be Parcelable
) : Parcelable {
    companion object {
        val Saver = Saver<MutableState<QuizState>, QuizState>(
            save = { it.value },
            restore = { mutableStateOf(it) }
        )
    }
}

@Composable
fun QuizChoiceMode(
    words: List<Word>, 
    state: QuizState, 
    onStateChange: (QuizState) -> Unit,
    onBack: () -> Unit,
    onFinish: (QuizState, List<TestResultItem>) -> Unit
) {
    // Filter words reviewed in the last 24h
    val now = System.currentTimeMillis()
    val availableWords = remember(words) {
        words.filter { 
            // Include if reviewCount is 0 OR last review was > 24h ago
            it.reviewCount == 0 || (now - it.lastReviewTime) > 24 * 60 * 60 * 1000 
        }
    }
    
    // If all words are filtered out (all reviewed today), allow reset or show message
    val finalWords = if (availableWords.isEmpty() && words.isNotEmpty()) {
        // Option: Reset/Allow reviewing anyway? Let's just use all words if list is empty to avoid blocking.
        // Or show a "You're done for today" message. User said: "All done can restart".
        // Let's default to all words if filter result is empty, implies "Restart".
        words 
    } else {
        availableWords
    }

    if (finalWords.size < 4) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("可用单词不足4个 (需复习或新词)，无法开始测试！")
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("返回") }
        }
        return
    }

    // Initialize shuffled order if empty or size mismatch (re-init)
    // Note: state.shuffledIndices stores INDICES of `finalWords`, not global IDs.
    // If `finalWords` changes size, we must reset.
    if (state.shuffledIndices.isEmpty() || state.shuffledIndices.maxOrNull() ?: 0 >= finalWords.size) {
        LaunchedEffect(finalWords) {
            if (finalWords.isNotEmpty()) {
                onStateChange(state.copy(
                    shuffledIndices = finalWords.indices.toList().shuffled(),
                    currentIndex = 0,
                    score = 0,
                    isFinished = false
                ))
            }
        }
    }

    if (state.shuffledIndices.isEmpty()) return
    
    // Track results locally for this session
    val results = remember { mutableListOf<TestResultItem>() }

    if (state.isFinished) {
        // Trigger finish callback once when finished
        LaunchedEffect(Unit) {
             // Only save if we have results (simple check)
             if (results.isNotEmpty()) {
                 onFinish(state, results)
             }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("测试结束！", style = MaterialTheme.typography.headlineLarge)
            Text("得分: ${state.score} / ${finalWords.size}", style = MaterialTheme.typography.headlineMedium)
            Button(
                onClick = { 
                    results.clear()
                    onStateChange(QuizState(shuffledIndices = finalWords.indices.toList().shuffled()))
                }, 
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text("重新开始")
            }
        }
    } else {
        val currentWordIndex = state.shuffledIndices.getOrNull(state.currentIndex) ?: 0
        val currentWord = finalWords.getOrNull(currentWordIndex) ?: return

        // Generate options (stable for current word)
        val options = remember(currentWord) {
            val wrongOptions = finalWords.filter { it.id != currentWord.id }.shuffled().take(3)
            // If not enough wrong options in filtered list, grab from global 'words'
            val safeWrongOptions = if (wrongOptions.size < 3) {
                 val moreWrong = words.filter { it.id != currentWord.id && !wrongOptions.contains(it) }.shuffled().take(3 - wrongOptions.size)
                 wrongOptions + moreWrong
            } else wrongOptions
            
            (safeWrongOptions + currentWord).shuffled()
        }
        
        // Immediate Feedback State
        var selectedOptionId by remember { mutableStateOf<Int?>(null) }
        var isAnswered by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header with Finish Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("进度: ${state.currentIndex + 1}/${finalWords.size}")
                
                // Always allow early finish if at least one question done
                if (state.currentIndex > 0) {
                    TextButton(onClick = {
                        onStateChange(state.copy(isFinished = true))
                    }) {
                        Text("结束测试")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Question area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentWord.word,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Options area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    val isCorrect = option.id == currentWord.id
                    val isSelected = selectedOptionId == option.id
                    
                    val buttonColors = if (isAnswered) {
                        if (isCorrect) {
                            ButtonDefaults.buttonColors(containerColor = Color.Green) // Correct answer always Green
                        } else if (isSelected) {
                            ButtonDefaults.buttonColors(containerColor = Color.Red) // Selected wrong answer Red
                        } else {
                            ButtonDefaults.buttonColors() // Others default
                        }
                    } else {
                        ButtonDefaults.buttonColors()
                    }

                    Button(
                        onClick = {
                            if (!isAnswered) {
                                selectedOptionId = option.id
                                isAnswered = true
                                
                                // Record Result
                                results.add(TestResultItem(
                                    wordId = currentWord.id,
                                    word = currentWord.word,
                                    isCorrect = (option.id == currentWord.id),
                                    userAnswer = option.definitionCn
                                ))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = buttonColors,
                        enabled = !isAnswered // Disable clicks after answering
                    ) {
                        // Fix layout for long Chinese definitions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = option.definitionCn, 
                                modifier = Modifier.weight(1f).padding(8.dp),
                                maxLines = 2, // Limit lines to prevent too tall buttons
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (isAnswered) {
                                if (isCorrect) {
                                    Icon(Icons.Filled.Check, "Correct", tint = Color.White)
                                } else if (isSelected) {
                                    Icon(Icons.Filled.Clear, "Wrong", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            
            // Auto-advance Logic
            LaunchedEffect(isAnswered) {
                if (isAnswered) {
                    val isCorrect = selectedOptionId == currentWord.id
                    delay(1000) // Reduced delay for faster flow
                    
                    val newScore = if (isCorrect) state.score + 1 else state.score
                    
                    if (state.currentIndex < finalWords.size - 1) {
                        onStateChange(state.copy(
                            score = newScore,
                            currentIndex = state.currentIndex + 1
                        ))
                    } else {
                        onStateChange(state.copy(
                            score = newScore,
                            isFinished = true
                        ))
                    }
                    // Reset local state for next question
                    selectedOptionId = null
                    isAnswered = false
                }
            }
        }
    }
}

@Composable
fun QuizSpellMode(words: List<Word>, onBack: () -> Unit) {
    if (words.size < 1) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("词库为空，无法开始拼写测试！")
        }
        return
    }

    var currentIndex by rememberSaveable { mutableStateOf(0) }
    var score by rememberSaveable { mutableStateOf(0) }
    var isFinished by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    
    // We need to persist the shuffled order to avoid reset on recomposition/tab switch if we want robust behavior
    // For now, using rememberSaveable for index/score is better than before.
    // Ideally we should use a state object like QuizState for spelling too.
    // To keep it simple and fix "placeholder" issue:
    
    val spellIndices = rememberSaveable {
        mutableStateOf(words.indices.toList().shuffled())
    }
    
    if (isFinished) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("拼写测试结束！", style = MaterialTheme.typography.headlineLarge)
            Text("得分: $score / ${words.size}", style = MaterialTheme.typography.headlineMedium)
            Button(
                onClick = { 
                    currentIndex = 0
                    score = 0
                    isFinished = false
                    input = ""
                    spellIndices.value = words.indices.toList().shuffled()
                }, 
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text("重新开始")
            }
        }
    } else {
        val currentWordIndex = spellIndices.value.getOrNull(currentIndex) ?: 0
        val currentWord = words.getOrNull(currentWordIndex) ?: return

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                Text("进度: ${currentIndex + 1}/${words.size}")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "请拼写单词",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = currentWord.definitionCn,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入单词") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (input.isNotBlank()) {
                            if (input.trim().equals(currentWord.word, ignoreCase = true)) {
                                score++
                            }
                            input = ""
                            if (currentIndex < words.size - 1) {
                                currentIndex++
                            } else {
                                isFinished = true
                            }
                        }
                    }
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (input.trim().equals(currentWord.word, ignoreCase = true)) {
                        score++
                    }
                    input = ""
                    if (currentIndex < words.size - 1) {
                        currentIndex++
                    } else {
                        isFinished = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotBlank()
            ) {
                Text("确定")
            }
            
            // Hint for testing (optional, maybe remove in production)
            // Text(text = "Hint: ${currentWord.word.first()}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
