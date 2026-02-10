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

@Composable
fun TestScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("选择题", "拼写", "听写")
    
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao())
    )
    val words by viewModel.allWords.collectAsState(initial = emptyList())

    // State persistence using rememberSaveable for basic quiz state across tabs
    // Note: Ideally this should be in a ViewModel, but rememberSaveable works for simple cases
    // where we want to survive configuration changes and simple composition changes.
    // However, for HorizontalPager in MainScreen, state might be lost if page is destroyed.
    // Given user complaint "leaving resets", we need stronger persistence.
    // Since we are reusing LibraryViewModel, let's keep it simple with rememberSaveable which survives process death too.
    
    val choiceQuizState = rememberSaveable(saver = QuizState.Saver) {
        mutableStateOf(QuizState())
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
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
            2 -> DictationPlaceholder()
        }
    }
}

// Simple State Holder for Quiz
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.compose.runtime.saveable.Saver

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
    onBack: () -> Unit
) {
    if (words.size < 4) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("词库单词不足4个，无法开始选择题测试！")
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("返回") }
        }
        return
    }

    // Initialize shuffled order if empty or size mismatch (re-init)
    if (state.shuffledIndices.isEmpty() || state.shuffledIndices.size != words.size) {
        // We need to trigger a state update, but side-effects in Composable body are bad.
        // Use LaunchedEffect.
        LaunchedEffect(words) {
            if (words.isNotEmpty()) {
                onStateChange(state.copy(shuffledIndices = words.indices.toList().shuffled()))
            }
        }
    }

    // If still empty after LaunchedEffect (e.g. first frame), show loading or return
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
                    // Reset
                    onStateChange(QuizState(shuffledIndices = words.indices.toList().shuffled()))
                }, 
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text("重新开始")
            }
        }
    } else {
        // Safe access
        val currentWordIndex = state.shuffledIndices.getOrNull(state.currentIndex) ?: 0
        val currentWord = words.getOrNull(currentWordIndex) ?: return

        // Generate options: correct answer + 3 random wrong answers
        // We use remember with currentWord to avoid regenerating on every recomposition
        val options = remember(currentWord) {
            val wrongOptions = words.filter { it.id != currentWord.id }.shuffled().take(3)
            (wrongOptions + currentWord).shuffled()
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                    Button(
                        onClick = {
                            val newScore = if (option.id == currentWord.id) state.score + 1 else state.score
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
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(option.definitionCn, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuizSpellMode(words: List<Word>, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("拼写模式开发中...")
    }
}
