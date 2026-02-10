package com.magicword.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.magicword.app.data.Word
import kotlinx.coroutines.launch
import com.magicword.app.data.WordDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

import com.magicword.app.data.Library
import com.magicword.app.network.RetrofitClient
import com.magicword.app.network.AiRequest
import com.magicword.app.network.Message

class LibraryViewModel(private val wordDao: WordDao) : ViewModel() {
    private val _currentLibraryId = MutableStateFlow(1)
    val currentLibraryId: StateFlow<Int> = _currentLibraryId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allWords: Flow<List<Word>> = _currentLibraryId.flatMapLatest { id ->
        wordDao.getWordsByLibrary(id)
    }

    val allLibraries: Flow<List<Library>> = wordDao.getAllLibraries()

    fun switchLibrary(libraryId: Int) {
        _currentLibraryId.value = libraryId
    }

    fun addLibrary(name: String) {
        viewModelScope.launch {
            wordDao.insertLibrary(Library(name = name, description = ""))
        }
    }

    private val _importLogs = MutableStateFlow<List<String>>(emptyList())
    val importLogs: StateFlow<List<String>> = _importLogs.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordDao.deleteWord(word)
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            wordDao.updateWord(word)
        }
    }

    fun bulkImport(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            _isImporting.value = true
            _importLogs.value = listOf("任务开始...", "Step A: 正在提取单词列表 (AI)...")
            
            try {
                // Step A: Extract words using AI
                val extractPrompt = """
                    Identify and extract all unique English words from the following text that are suitable for learning (ignore common stop words like 'the', 'is', 'and', etc.). 
                    Return ONLY a JSON Array of strings. Example: ["apple", "banana", "cherry"]
                    
                    Text: "${text.take(4000)}"
                """.trimIndent()

                val extractRequest = AiRequest(
                    model = "Qwen/Qwen2.5-7B-Instruct",
                    messages = listOf(Message("user", extractPrompt)),
                    temperature = 0.1
                )
                
                val extractResponse = RetrofitClient.api.chat(extractRequest)
                val content = extractResponse.choices.first().message.content
                
                // Parse JSON array from content
                val jsonStart = content.indexOf('[')
                val jsonEnd = content.lastIndexOf(']') + 1
                val wordsList: List<String> = if (jsonStart != -1 && jsonEnd > jsonStart) {
                    val jsonStr = content.substring(jsonStart, jsonEnd)
                    com.google.gson.Gson().fromJson(jsonStr, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
                } else {
                    emptyList()
                }

                if (wordsList.isEmpty()) {
                    _importLogs.value = _importLogs.value + "❌ 未提取到单词，请检查输入。"
                    return@launch
                }

                _importLogs.value = _importLogs.value + "✅ 提取到 ${wordsList.size} 个单词: $wordsList"

                // Step B: Process in chunks
                val chunkSize = 5
                wordsList.chunked(chunkSize).forEachIndexed { index, chunk ->
                    _importLogs.value = _importLogs.value + "Step B: 正在分析第 ${index + 1} 批单词..."
                    
                    val chunkPrompt = """
                        You are a strict JSON data generator. Analyze these English words: $chunk
                        
                        Return a JSON Array of objects.
                        
                        STRICT JSON FORMAT RULES:
                        1. "word": String.
                        2. "phonetic": String.
                        3. "definition_cn": String (NOT List). Format: "pos. meaning".
                        4. "definition_en": String.
                        5. "example": String (NOT List). Format: "En sentence. Cn translation.\nEn sentence 2. Cn translation."
                        6. "memory_method": String (NOT List).
                        
                        NO MARKDOWN. NO COMMENTS. ONLY JSON.
                    """.trimIndent()

                    val chunkRequest = AiRequest(
                        model = "Qwen/Qwen2.5-7B-Instruct",
                        messages = listOf(Message("user", chunkPrompt)),
                        temperature = 0.3
                    )

                    try {
                        val chunkResponse = RetrofitClient.api.chat(chunkRequest)
                        val chunkContent = chunkResponse.choices.first().message.content
                        
                        val chunkJsonStart = chunkContent.indexOf('[')
                        val chunkJsonEnd = chunkContent.lastIndexOf(']') + 1
                        if (chunkJsonStart != -1 && chunkJsonEnd > chunkJsonStart) {
                            val chunkJsonStr = chunkContent.substring(chunkJsonStart, chunkJsonEnd)
                            val wordDetails: List<Word> = com.google.gson.Gson().fromJson(chunkJsonStr, object : com.google.gson.reflect.TypeToken<List<Word>>() {}.type)
                            
                            wordDetails.forEach { detail ->
                                val wordToSave = detail.copy(libraryId = _currentLibraryId.value)
                                wordDao.insertWord(wordToSave)
                                _importLogs.value = _importLogs.value + "📥 已保存: ${detail.word}"
                            }
                        } else {
                            _importLogs.value = _importLogs.value + "⚠️ 解析失败: AI 返回格式错误"
                        }
                    } catch (e: Exception) {
                        _importLogs.value = _importLogs.value + "❌ 网络/解析错误: ${e.message}"
                    }
                }
                _importLogs.value = _importLogs.value + "🎉 所有任务完成！"
                
            } catch (e: Exception) {
                _importLogs.value = _importLogs.value + "❌ 致命错误: ${e.message}"
                e.printStackTrace()
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
