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

@Composable
fun TestScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("选择题", "拼写", "听写")
    
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao())
    )
    val words by viewModel.allWords.collectAsState(initial = emptyList())

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
            0 -> QuizChoiceMode(words = words, onBack = {})
            1 -> QuizSpellMode(words = words, onBack = {})
            2 -> DictationPlaceholder()
        }
    }
}

@Composable
fun DictationPlaceholder() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("听写模式开发中...")
    }
}

@Composable
fun QuizChoiceMode(words: List<Word>, onBack: () -> Unit) {
    if (words.size < 4) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("词库单词不足4个，无法开始选择题测试！")
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("返回") }
        }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    // Shuffle words only once when entering
    val quizWords = remember { words.shuffled() }
    
    if (isFinished) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("测试结束！", style = MaterialTheme.typography.headlineLarge)
            Text("得分: $score / ${quizWords.size}", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onBack, modifier = Modifier.padding(top = 32.dp)) {
                Text("返回菜单")
            }
        }
    } else {
        val currentWord = quizWords[currentIndex]
        // Generate options: correct answer + 3 random wrong answers
        val options = remember(currentWord) {
            val wrongOptions = words.filter { it.id != currentWord.id }.shuffled().take(3)
            (wrongOptions + currentWord).shuffled()
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Button(onClick = onBack) { Text("退出") } // Remove Exit button to simplify, use Tab switch to exit
                Spacer(modifier = Modifier.weight(1f))
                Text("进度: ${currentIndex + 1}/${quizWords.size}")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Question area (scrollable if word is long)
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
            
            // Options area (Scrollable to fix tablet display issue)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    Button(
                        onClick = {
                            if (option.id == currentWord.id) score++
                            if (currentIndex < quizWords.size - 1) {
                                currentIndex++
                            } else {
                                isFinished = true
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
