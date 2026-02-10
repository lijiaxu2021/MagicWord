package com.magicword.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import com.magicword.app.data.Word

@Composable
fun StudyScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao())
    )
    val words by viewModel.allWords.collectAsState(initial = emptyList())
    var mode by remember { mutableStateOf("menu") } // menu, flashcard, quiz_choice, quiz_spell

    when (mode) {
        "menu" -> StudyMenu(onModeSelect = { mode = it })
        "flashcard" -> FlashcardMode(words, onBack = { mode = "menu" })
        "quiz_choice" -> QuizChoiceMode(words, onBack = { mode = "menu" })
        "quiz_spell" -> QuizSpellMode(words, onBack = { mode = "menu" })
    }
}

@Composable
fun StudyMenu(onModeSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("选择学习模式", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { onModeSelect("flashcard") },
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text("单词卡片 (Flashcard)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onModeSelect("quiz_choice") },
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text("选择题测试 (Quiz)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onModeSelect("quiz_spell") },
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text("拼写测试 (Spelling)")
        }
    }
}

@Composable
fun FlashcardMode(words: List<Word>, onBack: () -> Unit) {
    var currentWordIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    if (words.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("词库是空的，先去添加单词吧！")
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text("返回")
            }
        }
    } else {
        val word = words.getOrNull(currentWordIndex) ?: words.first()
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onBack) { Text("退出") }
                Spacer(modifier = Modifier.weight(1f))
                Text("卡片 ${currentWordIndex + 1}/${words.size}")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clickable { isFlipped = !isFlipped },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isFlipped) {
                        Text(
                            text = word.word,
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = word.definitionCn,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    if (currentWordIndex > 0) {
                        currentWordIndex--
                        isFlipped = false
                    }
                }) {
                    Text("上一个")
                }
                
                Button(onClick = {
                    if (currentWordIndex < words.size - 1) {
                        currentWordIndex++
                        isFlipped = false
                    }
                }) {
                    Text("下一个")
                }
            }
        }
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
                Button(onClick = onBack) { Text("退出") }
                Spacer(modifier = Modifier.weight(1f))
                Text("进度: ${currentIndex + 1}/${quizWords.size}")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = currentWord.word,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
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
                    Text(option.definitionCn, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun QuizSpellMode(words: List<Word>, onBack: () -> Unit) {
    var currentIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val quizWords = remember { words.shuffled() }

    if (isFinished) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("拼写测试结束！", style = MaterialTheme.typography.headlineLarge)
            Text("得分: $score / ${quizWords.size}", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onBack, modifier = Modifier.padding(top = 32.dp)) {
                Text("返回菜单")
            }
        }
    } else {
        val currentWord = quizWords[currentIndex]

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack) { Text("退出") }
                Spacer(modifier = Modifier.weight(1f))
                Text("进度: ${currentIndex + 1}/${quizWords.size}")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "请拼写单词",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Text(
                text = currentWord.definitionCn,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
            )

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("输入单词") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (input.trim().equals(currentWord.word, ignoreCase = true)) {
                        score++
                    }
                    input = ""
                    if (currentIndex < quizWords.size - 1) {
                        currentIndex++
                    } else {
                        isFinished = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确定")
            }
        }
    }
}
