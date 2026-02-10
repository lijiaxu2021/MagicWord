package com.magicword.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.magicword.app.data.Word
import com.magicword.app.data.WordDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(private val wordDao: WordDao) : ViewModel() {
    val allWords: Flow<List<Word>> = wordDao.getWordsByLibrary(1) // Default library ID 1

    private val _importLogs = MutableStateFlow<List<String>>(emptyList())
    val importLogs: StateFlow<List<String>> = _importLogs.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordDao.deleteWord(word)
        }
    }

    fun bulkImport(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            _isImporting.value = true
            _importLogs.value = listOf("任务开始...", "正在提取单词列表...")
            
            try {
                // Simplified AI logic: Assume text is a comma/newline separated list
                // In a real scenario, this would call the AI API to parse sentences
                val words = text.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
                
                words.forEach { wordText ->
                    // Check if word exists
                    val existing = wordDao.getWordByText(wordText, 1)
                    if (existing == null) {
                        // Fetch detail from AI (Simulated here, reusing SearchViewModel logic ideally)
                        // For bulk, we might want a batch API or sequential calls
                        // Here we just add a placeholder to demonstrate the flow
                        val newWord = Word(
                            word = wordText,
                            phonetic = "",
                            definitionCn = "待 AI 解析...",
                            definitionEn = "",
                            example = "",
                            memoryMethod = "",
                            libraryId = 1
                        )
                        wordDao.insertWord(newWord)
                        _importLogs.value = _importLogs.value + "✅ 已导入: $wordText"
                    } else {
                        _importLogs.value = _importLogs.value + "⏭️ 已存在: $wordText"
                    }
                }
                _importLogs.value = _importLogs.value + "🎉 导入完成！"
            } catch (e: Exception) {
                _importLogs.value = _importLogs.value + "❌ 错误: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }
}

class LibraryViewModelFactory(private val wordDao: WordDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(wordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
