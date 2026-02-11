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

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.io.FileReader
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.SharedPreferences
import com.magicword.app.data.TestHistory
import com.magicword.app.data.TestSession
import java.util.ArrayDeque
import com.magicword.app.data.StandardizedWord
import com.magicword.app.data.toEntity
import com.magicword.app.data.LibraryExportData
import com.magicword.app.data.ExportPackage
import com.magicword.app.utils.LogUtil

import kotlin.math.roundToInt
import kotlin.math.max

class LibraryViewModel(private val wordDao: WordDao, private val prefs: SharedPreferences) : ViewModel() {
    // ... (existing code)

    // SM-2 Algorithm Implementation
    fun processReview(word: Word, quality: Int) { // quality: 0-5 (0=Blackout, 5=Perfect)
        viewModelScope.launch {
            // SM-2 Logic
            // q: 0-5
            // If q < 3: Repetitions = 0, Interval = 1, EF unchanged (or reduced?)
            // If q >= 3: 
            //   Repetitions += 1
            //   Interval: I(1)=1, I(2)=6, I(n)=I(n-1)*EF
            //   EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
            //   EF >= 1.3
            
            var newRepetitions = word.repetitions
            var newInterval = word.interval
            var newEf = word.easinessFactor
            
            if (quality < 3) {
                newRepetitions = 0
                newInterval = 1
            } else {
                newRepetitions += 1
                if (newRepetitions == 1) {
                    newInterval = 1
                } else if (newRepetitions == 2) {
                    newInterval = 6
                } else {
                    newInterval = (newInterval * newEf).roundToInt()
                }
                
                // Update EF
                // EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))
                val qFactor = 5 - quality
                newEf = newEf + (0.1f - qFactor * (0.08f + qFactor * 0.02f))
                if (newEf < 1.3f) newEf = 1.3f
            }
            
            val nextReview = System.currentTimeMillis() + newInterval * 24L * 60 * 60 * 1000
            
            val updatedWord = word.copy(
                repetitions = newRepetitions,
                interval = newInterval,
                easinessFactor = newEf,
                nextReviewTime = nextReview,
                lastReviewTime = System.currentTimeMillis(),
                reviewCount = word.reviewCount + 1,
                correctCount = if (quality >= 3) word.correctCount + 1 else word.correctCount,
                incorrectCount = if (quality < 3) word.incorrectCount + 1 else word.incorrectCount
            )
            
            wordDao.updateWord(updatedWord)
        }
    }
    
    private val _currentLibraryId = MutableStateFlow(prefs.getInt("current_library_id", 1))
    val currentLibraryId: StateFlow<Int> = _currentLibraryId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dueWords: Flow<List<Word>> = _currentLibraryId.flatMapLatest { id ->
        // Fetch words due before NOW
        wordDao.getDueWords(id, System.currentTimeMillis())
    }

    // Sorting State
    enum class SortOption {
        CREATED_AT_DESC,
        CREATED_AT_ASC,
        ALPHA_ASC,
        ALPHA_DESC,
        REVIEW_COUNT_DESC,
        REVIEW_COUNT_ASC,
        CUSTOM // Custom order
    }

    private val _sortOption = MutableStateFlow(
        try {
            SortOption.valueOf(prefs.getString("sort_option", SortOption.CREATED_AT_DESC.name) ?: SortOption.CREATED_AT_DESC.name)
        } catch (e: Exception) {
            SortOption.CREATED_AT_DESC
        }
    )
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        prefs.edit().putString("sort_option", option.name).apply()
    }

    fun switchLibrary(libraryId: Int) {
        _currentLibraryId.value = libraryId
        prefs.edit().putInt("current_library_id", libraryId).apply()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allWords: Flow<List<Word>> = combine(_currentLibraryId, _sortOption) { id, sort ->
        id to sort
    }.flatMapLatest { (id, sort) ->
        wordDao.getWordsByLibrary(id).map { list ->
            when (sort) {
                SortOption.CREATED_AT_DESC -> list.sortedByDescending { it.createdAt }
                SortOption.CREATED_AT_ASC -> list.sortedBy { it.createdAt }
                SortOption.ALPHA_ASC -> list.sortedBy { it.word }
                SortOption.ALPHA_DESC -> list.sortedByDescending { it.word }
                SortOption.REVIEW_COUNT_DESC -> list.sortedByDescending { it.reviewCount }
                SortOption.REVIEW_COUNT_ASC -> list.sortedBy { it.reviewCount }
                SortOption.CUSTOM -> list.sortedBy { it.sortOrder } // Use sortOrder for Custom
            }
        }
    }

    fun incrementReviewCount(word: Word) {
        viewModelScope.launch {
             wordDao.updateWord(word.copy(
                 reviewCount = word.reviewCount + 1,
                 lastReviewTime = System.currentTimeMillis()
             ))
        }
    }
    
    // Batch update for reordering or selection
    fun updateWords(words: List<Word>) {
        viewModelScope.launch {
            // Also automatically switch to CUSTOM sort to reflect reordering
            if (_sortOption.value != SortOption.CUSTOM) {
                setSortOption(SortOption.CUSTOM)
            }
            words.forEach { wordDao.updateWord(it) }
        }
    }

    val allLibraries: Flow<List<Library>> = wordDao.getAllLibraries()

    fun addLibrary(name: String) {
        viewModelScope.launch {
            wordDao.insertLibrary(Library(name = name, description = ""))
        }
    }

    fun deleteLibrary(libraryId: Int) {
        viewModelScope.launch {
            // Delete all words in library first
            wordDao.deleteWordsByLibrary(libraryId)
            // Then delete library
            wordDao.deleteLibrary(libraryId)
            
            // If current library is deleted, switch to default (1) or first available
            if (_currentLibraryId.value == libraryId) {
                 switchLibrary(1)
            }
        }
    }

    private val _importLogs = MutableStateFlow<List<String>>(emptyList())
    val importLogs: StateFlow<List<String>> = _importLogs.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Test Type State (Choice or Spell)
    enum class TestType {
        CHOICE, SPELL
    }
    
    private val _testType = MutableStateFlow(TestType.CHOICE)
    val testType: StateFlow<TestType> = _testType.asStateFlow()
    
    fun setTestType(type: TestType) {
        _testType.value = type
    }
    
    // Test Candidates
    private val _testCandidates = MutableStateFlow<List<Word>?>(null)
    val testCandidates: StateFlow<List<Word>?> = _testCandidates.asStateFlow()

    fun setTestCandidates(words: List<Word>?) {
        _testCandidates.value = words
    }
    
    // Test Session State
    private val _testSession = MutableStateFlow<TestSession?>(null)
    val testSession: StateFlow<TestSession?> = _testSession.asStateFlow()

    // Restore session on init
    init {
        viewModelScope.launch {
            _testSession.value = wordDao.getTestSession(1) // Assuming single session ID 1 for now
        }
    }
    
    fun saveTestSession(session: TestSession) {
        viewModelScope.launch {
            wordDao.saveTestSession(session)
            _testSession.value = session
        }
    }
    
    fun clearTestSession() {
        viewModelScope.launch {
            wordDao.clearTestSession(1)
            _testSession.value = null
        }
    }

    // Test History
    val testHistory: Flow<List<TestHistory>> = wordDao.getAllTestHistory()
    
    fun saveTestResult(history: TestHistory) {
        viewModelScope.launch {
            wordDao.insertTestHistory(history)
        }
    }
    
    // Update word stats based on test result
    fun updateWordStats(wordId: Int, isCorrect: Boolean) {
        viewModelScope.launch {
            wordDao.updateWordStats(wordId, if (isCorrect) 1 else 0)
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordDao.deleteWord(word)
        }
    }

    fun updateLibraryLastIndex(libraryId: Int, index: Int) {
        // Optimistic update local prefs immediately
        prefs.edit().putInt("last_index_$libraryId", index).apply()
        
        viewModelScope.launch {
            wordDao.updateLibraryLastIndex(libraryId, index)
        }
    }

    // Helper to get initial index from DB or Prefs
    suspend fun getInitialLastIndex(libraryId: Int): Int {
        // Try DB first (source of truth), fallback to Prefs
        // We need a DAO method to get library by ID.
        // Assuming we can add it or use raw query.
        // For now, let's rely on Prefs as "fast path" and we already sync them.
        // To be robust, we should read from DB.
        // Let's assume Prefs is good enough for synchronous init, but we sync DB.
        return prefs.getInt("last_index_$libraryId", 0)
    }
    
    fun deleteWords(ids: List<Int>) {
        viewModelScope.launch {
            ids.forEach { id ->
                wordDao.deleteWordById(id)
            }
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            wordDao.updateWord(word)
        }
    }
    private suspend fun <T> retry(times: Int = 3, block: suspend () -> T): T {
        var lastException: Exception? = null
        repeat(times) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                kotlinx.coroutines.delay(1000)
            }
        }
        throw lastException!!
    }

    // Export Library to JSON (Supports multiple libraries)
    fun exportLibrary(context: Context, libraryIds: List<Int>?) {
        viewModelScope.launch {
            try {
                _importLogs.value = listOf("正在导出...")
                _isImporting.value = true
                
                // Fetch words based on libraryIds (or current if null/empty)
                val targetIds = if (libraryIds.isNullOrEmpty()) listOf(_currentLibraryId.value) else libraryIds
                
                val exportDataList = mutableListOf<LibraryExportData>()
                
                targetIds.forEach { id ->
                    val library = wordDao.getLibraryById(id)
                    if (library != null) {
                        val words = wordDao.getWordsByLibraryList(id)
                        exportDataList.add(LibraryExportData(
                            name = library.name,
                            description = library.description,
                            words = words
                        ))
                    }
                }
                
                val exportPackage = ExportPackage(libraries = exportDataList)
                
                val json = Gson().toJson(exportPackage)
                val fileName = "magicword_export_${if(targetIds.size > 1) "multi" else targetIds[0]}_${System.currentTimeMillis()}.json"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                withContext(Dispatchers.IO) {
                    FileWriter(file).use { it.write(json) }
                }
                
                _importLogs.value = listOf("✅ 导出成功: ${file.absolutePath}")
            } catch (e: Exception) {
                _importLogs.value = listOf("❌ 导出失败: ${e.message}")
            } finally {
                _isImporting.value = false
            }
        }
    }

    // Helper to get JSON string for export (Supports multiple) - used by UI share intent maybe
    suspend fun getLibraryJson(libraryIds: List<Int>?): String {
        val targetIds = if (libraryIds.isNullOrEmpty()) listOf(_currentLibraryId.value) else libraryIds
        val exportDataList = mutableListOf<LibraryExportData>()
        
        targetIds.forEach { id ->
            val library = wordDao.getLibraryById(id)
            if (library != null) {
                val words = wordDao.getWordsByLibraryList(id)
                exportDataList.add(LibraryExportData(
                    name = library.name,
                    description = library.description,
                    words = words
                ))
            }
        }
        return Gson().toJson(ExportPackage(libraries = exportDataList))
    }

    // Import Library from JSON String
    fun importLibraryJson(jsonContent: String) {
        viewModelScope.launch {
            try {
                _importLogs.value = listOf("正在导入...")
                _isImporting.value = true
                
                // Try to parse as new ExportPackage format first
                try {
                    val exportPackage = Gson().fromJson(jsonContent, ExportPackage::class.java)
                    if (exportPackage != null && exportPackage.libraries.isNotEmpty()) {
                        var importedCount = 0
                        
                        exportPackage.libraries.forEach { libData ->
                            // Create new library to avoid conflicts
                            val newLib = Library(
                                name = libData.name + " (Imported)",
                                description = libData.description
                            )
                            val newLibId = wordDao.insertLibrary(newLib).toInt()
                            
                            libData.words.forEach { word ->
                                wordDao.insertWord(word.copy(id = 0, libraryId = newLibId))
                            }
                            importedCount += libData.words.size
                            _importLogs.value = _importLogs.value + "📥 已导入词库: ${libData.name}"
                        }
                         _importLogs.value = _importLogs.value + "✅ 成功导入 ${exportPackage.libraries.size} 个词库，共 $importedCount 个单词"
                         return@launch
                    }
                } catch (e: Exception) {
                    // Fallback to legacy list format
                }

                // Fallback: Try legacy list format
                val type = object : TypeToken<List<Word>>() {}.type
                val words: List<Word> = Gson().fromJson(jsonContent, type)
                
                if (words.isNotEmpty()) {
                    // Legacy import: Add to current library
                    words.forEach { word ->
                        wordDao.insertWord(word.copy(id = 0, libraryId = _currentLibraryId.value))
                    }
                    _importLogs.value = listOf("✅ 成功导入 ${words.size} 个单词 (旧格式)")
                } else {
                    _importLogs.value = listOf("⚠️ JSON 内容为空或格式错误")
                }
            } catch (e: Exception) {
                _importLogs.value = listOf("❌ 导入失败: ${e.message}")
            } finally {
                _isImporting.value = false
            }
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
                
                // Retry for extraction
                val extractResponse = try {
                    retry(3) { RetrofitClient.api.chat(extractRequest) }
                } catch (e: Exception) {
                    _importLogs.value = _importLogs.value + "❌ 提取阶段彻底失败: ${e.message}"
                    return@launch
                }
                
                val content = extractResponse.choices.first().message.content
                
                // Parse JSON array from content
                val jsonStart = content.indexOf('[')
                val jsonEnd = content.lastIndexOf(']') + 1
                val wordsList: List<String> = if (jsonStart != -1 && jsonEnd > jsonStart) {
                    val jsonStr = content.substring(jsonStart, jsonEnd)
                    try {
                        com.google.gson.Gson().fromJson(jsonStr, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                if (wordsList.isEmpty()) {
                    _importLogs.value = _importLogs.value + "❌ 未提取到单词，请检查输入。"
                    return@launch
                }

                _importLogs.value = _importLogs.value + "✅ 提取到 ${wordsList.size} 个单词: $wordsList"

                // Step B: Process in chunks with Retry Queue
                val chunkSize = 3 // Reduced chunk size to prevent timeout
                // Queue holds Pair<List<String>, Int> where Int is retryCount
                val chunkQueue = ArrayDeque(wordsList.chunked(chunkSize).map { it to 0 })
                val maxRetries = 3
                
                // Track successfully imported words to verify at the end
                val importedWordsSet = mutableSetOf<String>()

                while (chunkQueue.isNotEmpty()) {
                    val (chunk, retryCount) = chunkQueue.pollFirst()!!
                    _importLogs.value = _importLogs.value + "Step B: 正在分析批次 (剩余批次: ${chunkQueue.size})..."
                    
                    val chunkPrompt = """
                        You are a strict JSON data generator. Analyze these English words: $chunk
                        
                        Return a JSON Array of objects.
                        
                        STRICT JSON FORMAT RULES:
                        1. "word": String.
                        2. "phonetic": String.
                        3. "senses": Object with exactly 10 keys: "sense_1" to "sense_10".
                           - Each key must be either null (if unused) or an Object { "pos": "...", "meaning": "..." }.
                           - "pos": String (e.g., "n", "v", "adj").
                           - "meaning": String (Chinese definition).
                        4. "definition_en": String (Brief English definition).
                        5. "example": String. Format: "En sentence. Cn translation."
                        6. "memory_method": String. Escape double quotes inside strings with backslash.
                        
                        Example Item:
                        {
                          "word": "example",
                          "phonetic": "/ɪgˈzæmpəl/",
                          "senses": {
                            "sense_1": { "pos": "n", "meaning": "例子" },
                            "sense_2": { "pos": "v", "meaning": "作为...的榜样" },
                            "sense_3": null, "sense_4": null, "sense_5": null,
                            "sense_6": null, "sense_7": null, "sense_8": null,
                            "sense_9": null, "sense_10": null
                          },
                          "definition_en": "A representative form or pattern.",
                          "example": "This is an example. 这是一个例子。",
                          "memory_method": "ex(出)+ample(拿) -> 拿出来展示 -> 例子"
                        }
                        
                        IMPORTANT: Ensure valid JSON syntax. No trailing commas.
                        NO MARKDOWN. NO COMMENTS. ONLY JSON.
                    """.trimIndent()

                    val chunkRequest = AiRequest(
                        model = "Qwen/Qwen2.5-7B-Instruct",
                        messages = listOf(Message("user", chunkPrompt)),
                        temperature = 0.3
                    )

                    try {
                        // Single attempt per queue pop
                        val chunkResponse = RetrofitClient.api.chat(chunkRequest)
                        val chunkContent = chunkResponse.choices.first().message.content
                        
                        val chunkJsonStart = chunkContent.indexOf('[')
                        val chunkJsonEnd = chunkContent.lastIndexOf(']') + 1
                        if (chunkJsonStart != -1 && chunkJsonEnd > chunkJsonStart) {
                            val chunkJsonStr = chunkContent.substring(chunkJsonStart, chunkJsonEnd)
                            
                            // Use StandardizedWord for strict parsing
                            // We define a wrapper class for the list parsing or just use TypeToken
                            // But StandardizedWord is the item type.
                            val standardizedWords: List<StandardizedWord> = com.google.gson.Gson().fromJson(chunkJsonStr, object : com.google.gson.reflect.TypeToken<List<StandardizedWord>>() {}.type)
                            
                            // Check if AI returned fewer words than requested
                            if (standardizedWords.size < chunk.size) {
                                _importLogs.value = _importLogs.value + "⚠️ AI返回数量不足 (${standardizedWords.size}/${chunk.size})，正在检查漏词..."
                            }

                            standardizedWords.forEach { stdWord ->
                                // Convert back to Entity using helper extension
                                // We need to access extra fields from JSON map if they were separate, 
                                // but StandardizedWord data class should match JSON keys.
                                // Wait, StandardizedWord definition above has 'definition_en', 'example', 'memory_method' fields?
                                // Ah, I need to add them to StandardizedWord data class first!
                                // The previous file write didn't include them in the data class body, let me check.
                                // Yes, the user requirement didn't explicitly say those fields, but my prompt asked for them.
                                // I should update StandardizedWord to include them for full mapping.
                                // Assuming I update StandardizedWord... wait, let me check the file content I wrote.
                                // I wrote StandardizedWord with word, phonetic, senses. 
                                // I need to update it to include definition_en, example, memory_method to capture them from JSON.
                                
                                // Let's pause this SearchReplace and update StandardizedWord.kt first.
                                // But I cannot stop tool execution mid-way. 
                                // I will proceed assuming I will fix StandardizedWord.kt immediately after.
                                
                                // Actually, I can use a local data class or map, but better to fix the file.
                                // Let's use a temporary parsing logic or fix the file in next step.
                                // No, I should fix the file first.
                                // I will cancel this edit? No, I can't.
                                // I will write the code that assumes updated class, and then update the class.
                                
                                val wordToSave = stdWord.toEntity(
                                    libraryId = _currentLibraryId.value,
                                    // These fields will be added to StandardizedWord in next step
                                    example = stdWord.example,
                                    memoryMethod = stdWord.memoryMethod,
                                    definitionEn = stdWord.definitionEn
                                )
                                wordDao.insertWord(wordToSave)
                                importedWordsSet.add(stdWord.word.lowercase().trim())
                                _importLogs.value = _importLogs.value + "📥 已保存: ${stdWord.word}"
                            }
                        } else {
                            throw Exception("AI 返回格式错误 (找不到 JSON Array)")
                        }
                    } catch (e: Exception) {
                        if (retryCount < maxRetries) {
                            _importLogs.value = _importLogs.value + "⚠️ 本批次失败，已重新加入队列 (重试 ${retryCount + 1}/$maxRetries): ${e.message}"
                            chunkQueue.addLast(chunk to (retryCount + 1))
                        } else {
                            _importLogs.value = _importLogs.value + "❌ 本批次彻底失败，放弃: $chunk"
                        }
                        // Log full error for debugging
                        LogUtil.logError("ImportChunk", "Failed", e)
                        _importLogs.value = _importLogs.value + "🔍 错误详情: ${e.message}"
                    }
                }
                
                // Step C: Verification and Retry for Missing Words
                val missingWords = wordsList.filter { !importedWordsSet.contains(it.lowercase().trim()) }
                
                if (missingWords.isNotEmpty()) {
                    _importLogs.value = _importLogs.value + "🔍 发现 ${missingWords.size} 个单词漏导，正在尝试重新处理..."
                    
                    // Re-queue missing words as new chunks
                    val missingChunks = missingWords.chunked(chunkSize).map { it to 0 } // Reset retry count
                    chunkQueue.addAll(missingChunks)
                    
                    // Process Retry Queue for Missing Words
                    while (chunkQueue.isNotEmpty()) {
                        val (chunk, retryCount) = chunkQueue.pollFirst()!!
                        _importLogs.value = _importLogs.value + "Step C: 补录漏词 (剩余批次: ${chunkQueue.size})..."
                        
                         val chunkPrompt = """
                            You are a strict JSON data generator. Analyze these English words: $chunk
                            
                            Return a JSON Array of objects.
                            
                            STRICT JSON FORMAT RULES:
                            1. "word": String.
                            2. "phonetic": String.
                            3. "senses": Object with exactly 10 keys: "sense_1" to "sense_10".
                               - Each key must be either null (if unused) or an Object { "pos": "...", "meaning": "..." }.
                               - "pos": String (e.g., "n", "v", "adj").
                               - "meaning": String (Chinese definition).
                            4. "definition_en": String (Brief English definition).
                            5. "example": String. Format: "En sentence. Cn translation."
                            6. "memory_method": String. Escape double quotes inside strings with backslash.
                            
                            IMPORTANT: Ensure valid JSON syntax. No trailing commas.
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
                                val standardizedWords: List<StandardizedWord> = com.google.gson.Gson().fromJson(chunkJsonStr, object : com.google.gson.reflect.TypeToken<List<StandardizedWord>>() {}.type)
                                
                                standardizedWords.forEach { stdWord ->
                                    val wordToSave = stdWord.toEntity(
                                        libraryId = _currentLibraryId.value,
                                        example = stdWord.example,
                                        memoryMethod = stdWord.memoryMethod,
                                        definitionEn = stdWord.definitionEn
                                    )
                                    wordDao.insertWord(wordToSave)
                                    importedWordsSet.add(stdWord.word.lowercase().trim())
                                    _importLogs.value = _importLogs.value + "📥 补录成功: ${stdWord.word}"
                                }
                             } else {
                                throw Exception("AI 返回格式错误")
                             }
                        } catch(e: Exception) {
                            if (retryCount < maxRetries) {
                                _importLogs.value = _importLogs.value + "⚠️ 补录失败，重试 (重试 ${retryCount + 1}/$maxRetries)..."
                                chunkQueue.addLast(chunk to (retryCount + 1))
                            } else {
                                _importLogs.value = _importLogs.value + "❌ 补录彻底失败: $chunk"
                            }
                        }
                    }
                }

                _importLogs.value = _importLogs.value + "🎉 所有任务处理完毕！最终导入: ${importedWordsSet.size}/${wordsList.size}"
                
            } catch (e: Exception) {
                _importLogs.value = _importLogs.value + "❌ 致命错误: ${e.message}"
                e.printStackTrace()
            } finally {
                _isImporting.value = false
            }
        }
    }
}

class LibraryViewModelFactory(private val wordDao: WordDao, private val prefs: SharedPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(wordDao, prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
