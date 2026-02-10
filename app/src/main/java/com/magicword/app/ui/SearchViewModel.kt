package com.magicword.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.magicword.app.network.AiRequest
import com.magicword.app.network.Message
import com.magicword.app.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.magicword.app.data.WordDao

class SearchViewModel(private val wordDao: WordDao) : ViewModel() {
    private val _searchResult = MutableStateFlow("")
    val searchResult: StateFlow<String> = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun searchWord(word: String) {
        if (word.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _searchResult.value = ""
            try {
                val prompt = "请解释单词 \"$word\"。包含音标、中文释义、英文释义和例句。格式清晰一点。"
                val request = AiRequest(
                    messages = listOf(Message(role = "user", content = prompt))
                )
                val response = RetrofitClient.api.chat(request)
                val content = response.choices.firstOrNull()?.message?.content ?: "未获取到结果"
                _searchResult.value = content
            } catch (e: Exception) {
                _searchResult.value = "查询失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveWord(word: String, definition: String) {
        viewModelScope.launch {
            val newWord = com.magicword.app.data.Word(
                word = word,
                phonetic = null,
                definitionCn = definition.take(100), // Simplified storage for now
                definitionEn = null,
                example = null,
                memoryMethod = null,
                libraryId = 1
            )
            wordDao.insertWord(newWord)
        }
    }
}

class SearchViewModelFactory(private val wordDao: WordDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(wordDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
