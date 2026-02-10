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

import com.magicword.app.data.Word
import com.google.gson.Gson

class SearchViewModel(private val wordDao: WordDao) : ViewModel() {
    private val _searchResult = MutableStateFlow<Word?>(null)
    val searchResult: StateFlow<Word?> = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun searchWord(word: String) {
        if (word.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _searchResult.value = null
            try {
                val prompt = """
                    You are a strict JSON data generator. Analyze the English word: "$word"
                    
                    Return a SINGLE JSON Object.
                    
                    STRICT JSON FORMAT RULES:
                    1. "word": String (The word itself).
                    2. "phonetic": String.
                    3. "definition_cn": String (NOT List). Format: "pos. meaning".
                    4. "definition_en": String.
                    5. "example": String (NOT List). Format: "En sentence. Cn translation.\nEn sentence 2. Cn translation."
                    6. "memory_method": String (NOT List).
                    
                    NO MARKDOWN. NO COMMENTS. ONLY JSON.
                """.trimIndent()

                val request = AiRequest(
                    model = "Qwen/Qwen2.5-7B-Instruct",
                    messages = listOf(Message(role = "user", content = prompt)),
                    temperature = 0.3
                )
                val response = RetrofitClient.api.chat(request)
                val content = response.choices.firstOrNull()?.message?.content ?: ""
                
                // Parse JSON
                val jsonStart = content.indexOf('{')
                val jsonEnd = content.lastIndexOf('}') + 1
                if (jsonStart != -1 && jsonEnd > jsonStart) {
                    val jsonStr = content.substring(jsonStart, jsonEnd)
                    val wordObj = Gson().fromJson(jsonStr, Word::class.java)
                    _searchResult.value = wordObj
                } else {
                    // Fallback if parsing fails (should handle better but strictly adhering to task)
                    // _searchResult.value = null 
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveWord(word: Word, libraryId: Int) {
        viewModelScope.launch {
            val wordToSave = word.copy(
                id = 0, // Ensure auto-generate
                libraryId = libraryId
            )
            wordDao.insertWord(wordToSave)
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
