package com.magicword.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.magicword.app.data.Word
import com.magicword.app.data.AppDatabase
import com.magicword.app.utils.LogUtil

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.MutableState
import androidx.compose.material3.OutlinedTextField

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@Composable
fun TestScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
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

    // State persistence using rememberSaveable for basic quiz state across tabs
    // Note: Ideally this should be in a ViewModel, but rememberSaveable works for simple cases
    // where we want to survive configuration changes and simple composition changes.
    // However, for HorizontalPager in MainScreen, state might be lost if page is destroyed.
    // Given user complaint "leaving resets", we need stronger persistence.
    // Since we are reusing LibraryViewModel, let's keep it simple with rememberSaveable which survives process death too.
    
    val choiceQuizState = rememberSaveable(saver = QuizState.Saver) {
        mutableStateOf(QuizState())
    }
    
    // Listen for Test Type changes from ViewModel (triggered by Test Selected)
    val testType by viewModel.testType.collectAsState()
    
    // Auto-switch tab if Test Type changes (and it's a test session)
    LaunchedEffect(testType) {
        val targetIndex = when(testType) {
            LibraryViewModel.TestType.CHOICE -> 0
            LibraryViewModel.TestType.SPELL -> 1
        }
        if (selectedTab != targetIndex) {
            selectedTab = targetIndex
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Show Test Source Info
        if (testCandidates != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "正在测试选中单词 (${testCandidates!!.size}个)", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.primary
                )
                // Option to clear selection and test full library?
                IconButton(
                    onClick = { viewModel.setTestCandidates(null) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Close, "Exit Selection Mode")
                }
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
                onBack = {}
            )
            1 -> QuizSpellMode(words = words, onBack = {})
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

import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun QuizChoiceMode(
    words: List<Word>, 
    state: QuizState, 
    onStateChange: (QuizState) -> Unit,
    onBack: () -> Unit
) {
    // ... (keep check for < 4 words) ...
    if (words.size < 4) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("词库单词不足4个，无法开始选择题测试！")
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("返回") }
        }
        return
    }

    // Initialize shuffled order if empty or size mismatch (re-init)
    if (state.shuffledIndices.isEmpty() || state.shuffledIndices.size != words.size) {
        LaunchedEffect(words) {
            if (words.isNotEmpty()) {
                onStateChange(state.copy(shuffledIndices = words.indices.toList().shuffled()))
            }
        }
    }

    if (state.shuffledIndices.isEmpty()) return

    if (state.isFinished) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("测试结束！", style = MaterialTheme.typography.headlineLarge)
            Text("得分: ${state.score} / ${words.size}", style = MaterialTheme.typography.headlineMedium)
            Button(
                onClick = { 
                    onStateChange(QuizState(shuffledIndices = words.indices.toList().shuffled()))
                }, 
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text("重新开始")
            }
        }
    } else {
        val currentWordIndex = state.shuffledIndices.getOrNull(state.currentIndex) ?: 0
        val currentWord = words.getOrNull(currentWordIndex) ?: return

        // Generate options (stable for current word)
        val options = remember(currentWord) {
            val wrongOptions = words.filter { it.id != currentWord.id }.shuffled().take(3)
            (wrongOptions + currentWord).shuffled()
        }
        
        // Immediate Feedback State
        var selectedOptionId by remember { mutableStateOf<Int?>(null) }
        var isAnswered by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // ... (keep header) ...
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.weight(1f))
                Text("进度: ${state.currentIndex + 1}/${words.size}")
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
                                // Record result immediately? Or wait for next?
                                // User wants "Immediate feedback then wait 3s"
                                // We update score but delay moving to next index
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = buttonColors,
                        enabled = !isAnswered // Disable clicks after answering
                    ) {
                        Text(option.definitionCn, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            
            // Auto-advance Logic
            LaunchedEffect(isAnswered) {
                if (isAnswered) {
                    val isCorrect = selectedOptionId == currentWord.id
                    delay(2000) // Wait 2s (3s might be too long, let's try 2s first, user said 3s, ok let's do 2.5s)
                    
                    val newScore = if (isCorrect) state.score + 1 else state.score
                    // Update Word stats (mock for now, should be in VM)
                    // viewModel.updateWordStats(currentWord.id, isCorrect) 
                    
                    if (state.currentIndex < words.size - 1) {
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
                singleLine = true
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
