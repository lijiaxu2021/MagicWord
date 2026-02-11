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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

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

    // Study Library Selection (Multiple)
    // Default to current library initially, or user can select multiple
    // Initialize from Prefs if available
    private val _studyLibraryIds = MutableStateFlow<Set<Int>>(
        try {
            val saved = prefs.getStringSet("study_library_ids", null)
            saved?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    )
    val studyLibraryIds: StateFlow<Set<Int>> = _studyLibraryIds.asStateFlow()

    fun toggleStudyLibrary(libraryId: Int) {
        val current = _studyLibraryIds.value.toMutableSet()
        if (current.contains(libraryId)) {
            current.remove(libraryId)
        } else {
            current.add(libraryId)
        }
        _studyLibraryIds.value = current
        saveStudyLibraryIds(current)
    }
    
    fun setStudyLibraries(ids: Set<Int>) {
        _studyLibraryIds.value = ids
        saveStudyLibraryIds(ids)
    }
    
    private fun saveStudyLibraryIds(ids: Set<Int>) {
        prefs.edit().putStringSet("study_library_ids", ids.map { it.toString() }.toSet()).apply()
    }
    
    fun renameLibrary(libraryId: Int, newName: String) {
        viewModelScope.launch {
            // Check if exists? Dao might need a method or we fetch first.
            // Just update.
            // Need to add updateLibraryName to DAO.
            // Assuming we will add updateLibraryName(id, name) to DAO.
            // For now, let's implement updateLibraryName via raw query or @Update if we fetch object.
            val library = wordDao.getLibraryById(libraryId)
            if (library != null) {
                wordDao.insertLibrary(library.copy(name = newName)) // Replace because @Insert(onConflict=REPLACE)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val dueWords: Flow<List<Word>> = combine(_currentLibraryId, _studyLibraryIds) { currentId, studyIds ->
         // If studyIds is empty, default to current library
         if (studyIds.isEmpty()) listOf(currentId) else studyIds.toList()
    }.flatMapLatest { ids ->
        // Fetch words due before NOW for ALL selected libraries
        wordDao.getDueWordsForLibraries(ids, System.currentTimeMillis())
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

    private val _searchResults = MutableStateFlow<List<Word>>(emptyList())
    val searchResults: StateFlow<List<Word>> = _searchResults.asStateFlow()

    private val _globalSearchResult = MutableStateFlow<Word?>(null)
    val globalSearchResult: StateFlow<Word?> = _globalSearchResult.asStateFlow()

    private val _isGlobalSearching = MutableStateFlow(false)
    val isGlobalSearching: StateFlow<Boolean> = _isGlobalSearching.asStateFlow()

    fun clearGlobalSearchResult() {
        _globalSearchResult.value = null
    }

    fun handleGlobalSearch(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isGlobalSearching.value = true
            try {
                // 1. Global Search (Exact Match)
                val existing = wordDao.findWordGlobal(query.trim())
                
                if (existing != null) {
                    // Found: Mark as "Forgotten" (Review logic)
                    // Treat as Quality 0 (Blackout) -> Reset interval, count as review
                    processReview(existing, 0)
                    _globalSearchResult.value = existing
                } else {
                    // Not Found: AI Import (Single Word)
                    importSingleWord(query.trim())
                }
            } catch (e: Exception) {
                _importLogs.value = listOf("Search/Import Error: ${e.message}")
            } finally {
                _isGlobalSearching.value = false
            }
        }
    }

    private suspend fun importSingleWord(text: String) {
        // AI Request for Single Word
        val prompt = """
            You are a strict JSON data generator. Analyze this English word: "$text"
            
            Return a SINGLE JSON Object (NOT Array).
            
            STRICT JSON FORMAT RULES:
            1. "word": String (The LEMMA/ROOT form). e.g., if input is "ran", return "run".
            2. "phonetic": String.
            3. "senses": Object with exactly 10 keys: "sense_1" to "sense_10".
               - Each key must be either null (if unused) or an Object { "pos": "...", "meaning": "..." }.
               - "pos": String (e.g., "n", "v", "adj").
               - "meaning": String (Chinese definition).
            4. "definition_en": String (Brief English definition).
            5. "example": String. Format: "En sentence. Cn translation."
            6. "memory_method": String. Escape double quotes inside strings with backslash.
            7. "forms": Object (Word Variations) or null.
               - "past": String (Past Tense).
               - "participle": String (Past Participle).
               - "plural": String (Plural).
               - "third_person": String (3rd Person Singular).
            
            IMPORTANT: Ensure valid JSON syntax. No trailing commas.
            NO MARKDOWN. NO COMMENTS. ONLY JSON.
        """.trimIndent()

        val request = AiRequest(
            model = "Qwen/Qwen2.5-7B-Instruct",
            messages = listOf(Message("user", prompt)),
            temperature = 0.3
        )

        try {
            val response = RetrofitClient.api.chat(request)
            val content = response.choices.first().message.content
            
            // Extract JSON Object
            val jsonStart = content.indexOf('{')
            val jsonEnd = content.lastIndexOf('}') + 1
            
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val jsonStr = content.substring(jsonStart, jsonEnd)
                val stdWord = com.google.gson.Gson().fromJson(jsonStr, StandardizedWord::class.java)
                
                // Convert to Entity and Insert
                // Ensure timestamps are correct (System.currentTimeMillis())
                val now = System.currentTimeMillis()
                val wordToSave = stdWord.toEntity(
                    libraryId = _currentLibraryId.value,
                    example = stdWord.example,
                    memoryMethod = stdWord.memoryMethod,
                    definitionEn = stdWord.definitionEn
                ).copy(
                    createdAt = now,
                    lastReviewTime = 0, // New word, not yet reviewed
                    reviewCount = 0,
                    nextReviewTime = 0 // Due immediately
                )
                
                val newId = wordDao.insertWord(wordToSave)
                _globalSearchResult.value = wordToSave.copy(id = newId.toInt())
            } else {
                throw Exception("Invalid AI Response")
            }
        } catch (e: Exception) {
            LogUtil.logError("ImportSingle", "Failed", e)
            throw e
        }
    }

    // Search Function
    fun searchWords(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                // If query is blank, we might want to clear or show all? 
                // Currently SearchScreen observes _searchResults. 
                // If query blank, SearchScreen usually clears.
                _searchResults.value = emptyList()
            } else {
                wordDao.searchWords(query).collect { results ->
                    _searchResults.value = results
                }
            }
        }
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
                                name = libData.name, // Removed " (Imported)" suffix
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
                    Identify and extract all unique English words AND phrases from the following text that are suitable for learning (ignore common stop words like 'the', 'is', 'and', etc. unless they are part of a useful phrase). 
                    
                    CRITICAL INSTRUCTION FOR PHRASES:
                    If the text contains a phrase (e.g., "give up", "look forward to"), you MUST:
                    1. Extract the phrase itself as a single entry (e.g., "give up").
                    2. Extract the constituent words individually IF they are meaningful (e.g., "give", "up").
                    3. Do NOT split a phrase if it means you lose the phrase entry. Prioritize keeping the phrase intact.
                    
                    Return ONLY a JSON Array of strings. Example: ["give up", "give", "up", "apple", "banana"]
                    
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
                // Use Synchronized Set to prevent race conditions in parallel processing
                val importedWordsSet = java.util.Collections.synchronizedSet(mutableSetOf<String>())
                
                // Concurrent Processing (Simple approach: Process chunks sequentially but allow parallel request logic if we had multiple queues)
                // For now, simple loop is stable. To increase speed, we can launch parallel coroutines for chunks.
                // LIMIT CONCURRENCY to 3
                
                // Increase concurrency to 3
                while (chunkQueue.isNotEmpty()) {
                    // Take up to 3 chunks to process in parallel
                    val batch = (1..3).mapNotNull { if (chunkQueue.isNotEmpty()) chunkQueue.pollFirst() else null }
                    
                    if (batch.isEmpty()) break
                    
                    _importLogs.value = _importLogs.value + "Step B: 正在并行处理 ${batch.size} 个批次 (剩余: ${chunkQueue.size})..."
                    
                    // Process batch in parallel
                    val deferreds = batch.map { (chunk, retryCount) ->
                        // ... create job
                        async { // Use 'async' from coroutine scope (viewModelScope or runBlocking context?)
                             // We are inside launch { ... } which is a CoroutineScope
                             processChunk(chunk, retryCount, maxRetries, chunkQueue, importedWordsSet)
                        }
                    }
                    deferreds.awaitAll() // Wait for all in batch
                }
                
                // ... (Step C remains same)
                
                // Step C: Verification and Retry for Missing Words
                val missingWords = wordsList.filter { !importedWordsSet.contains(it.lowercase().trim()) }
                
                if (missingWords.isNotEmpty()) {
                    _importLogs.value = _importLogs.value + "🔍 发现 ${missingWords.size} 个单词漏导，正在尝试重新处理..."
                    
                    // Re-queue missing words as new chunks
                    val missingChunks = missingWords.chunked(chunkSize).map { it to 0 } // Reset retry count
                    chunkQueue.addAll(missingChunks)
                    
                    // Process Retry Queue for Missing Words
                    // Sequential for retry to be safe
                    while (chunkQueue.isNotEmpty()) {
                        val (chunk, retryCount) = chunkQueue.pollFirst()!!
                        _importLogs.value = _importLogs.value + "Step C: 补录漏词 (剩余批次: ${chunkQueue.size})..."
                        processChunk(chunk, retryCount, maxRetries, chunkQueue, importedWordsSet)
                    }
                }

                // Step D: Post-Import Sanity Check & Fix
                // Gather all imported words in this session for validation
                val sessionImportedWords = importedWordsSet.toList()
                if (sessionImportedWords.isNotEmpty()) {
                    _importLogs.value = _importLogs.value + "Step D: 正在进行数据质量检查 (AI Sanity Check)..."
                    validateAndFixImportedWords(sessionImportedWords)
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
    
    // Extracted Chunk Processing Logic
    private suspend fun processChunk(
        chunk: List<String>, 
        retryCount: Int, 
        maxRetries: Int, 
        chunkQueue: ArrayDeque<Pair<List<String>, Int>>,
        importedWordsSet: MutableSet<String>
    ) {
        val chunkPrompt = """
            You are a strict JSON data generator. Analyze these English words: $chunk
            
            Return a JSON Array of objects.
            
            STRICT JSON FORMAT RULES:
            1. "word": String (The LEMMA/ROOT form). e.g., if input is "ran", return "run".
            2. "phonetic": String.
            3. "senses": Object with exactly 10 keys: "sense_1" to "sense_10".
               - Each key must be either null (if unused) or an Object { "pos": "...", "meaning": "..." }.
               - "pos": String (e.g., "n", "v", "adj").
               - "meaning": String (Chinese definition).
            4. "definition_en": String (Brief English definition).
            5. "example": String. Format: "En sentence. Cn translation."
            6. "memory_method": String. Escape double quotes inside strings with backslash.
            7. "forms": Object (Word Variations) or null.
               - "past": String (Past Tense).
               - "participle": String (Past Participle).
               - "plural": String (Plural).
               - "third_person": String (3rd Person Singular).
            
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
                
                if (standardizedWords.size < chunk.size) {
                    _importLogs.value = _importLogs.value + "⚠️ AI返回数量不足 (${standardizedWords.size}/${chunk.size})，正在检查漏词..."
                }

                standardizedWords.forEach { stdWord ->
                    val wordToSave = stdWord.toEntity(
                        libraryId = _currentLibraryId.value,
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
            LogUtil.logError("ImportChunk", "Failed", e)
            _importLogs.value = _importLogs.value + "🔍 错误详情: ${e.message}"
        }
    }

    // Step D: Validation Logic
    private suspend fun validateAndFixImportedWords(words: List<String>) {
        val batchSize = 30
        val batches = words.chunked(batchSize)
        
        batches.forEachIndexed { index, batch ->
            _importLogs.value = _importLogs.value + "🔍 检查批次 ${index + 1}/${batches.size}..."
            
            // 1. Fetch current data for these words to show AI (Context)
            // Or just give AI the list of words and ask it to generate "Good JSON" for any that might be broken?
            // User strategy: "把导入完的所有单词的这些数据... 给ai... 问他哪个有问题"
            // To save tokens, we might just send the WORDS. But if we want it to check EXISTING data, we need to fetch it.
            // Sending full JSON for 30 words might be heavy.
            // User said: "问他哪个有问题 ... 只用json返回有问题了"
            // Let's try sending just the WORD list and asking it to GENERATE valid data for them if they are complex/ambiguous,
            // OR we can fetch the `definitionCn` from DB and send "Word: Def" pairs.
            // Let's go with fetching the full Word objects to be precise, as "n.诗;n.诗" is in the definition.
            
            val dbWords = mutableListOf<Word>()
            batch.forEach { wordText ->
                val w = wordDao.getWordByText(wordText, _currentLibraryId.value)
                if (w != null) dbWords.add(w)
            }
            
            if (dbWords.isEmpty()) return@forEachIndexed

            // Construct minimal representation for check
            val checkData = dbWords.map { 
                mapOf(
                    "id" to it.id,
                    "word" to it.word,
                    "definition" to it.definitionCn
                )
            }
            
            val checkPrompt = """
                Analyze the following list of imported words. 
                Identify entries that look incorrect, hallucinated, repetitive (e.g., "n. poem; n. poem; n. poem"), or garbage.
                
                Input Data: ${Gson().toJson(checkData)}
                
                Task:
                1. Find the BAD entries.
                2. RE-GENERATE the correct full JSON data for ONLY the bad entries.
                3. Return a JSON Array of the fixed objects (StandardizedWord format).
                4. If all are good, return empty array [].
                
                StandardizedWord Format (for return):
                {
                  "word": "...",
                  "phonetic": "...",
                  "senses": { ... },
                  "definition_en": "...",
                  "example": "...",
                  "memory_method": "...",
                  "forms": { ... }
                }
            """.trimIndent()
            
            val request = AiRequest(
                model = "Qwen/Qwen2.5-7B-Instruct",
                messages = listOf(Message("user", checkPrompt)),
                temperature = 0.1
            )
            
            try {
                val response = RetrofitClient.api.chat(request)
                val content = response.choices.first().message.content
                
                val jsonStart = content.indexOf('[')
                val jsonEnd = content.lastIndexOf(']') + 1
                
                if (jsonStart != -1 && jsonEnd > jsonStart) {
                    val jsonStr = content.substring(jsonStart, jsonEnd)
                    val fixedWords: List<StandardizedWord> = Gson().fromJson(jsonStr, object : TypeToken<List<StandardizedWord>>() {}.type)
                    
                    if (fixedWords.isNotEmpty()) {
                        _importLogs.value = _importLogs.value + "🛠️ 发现并修复 ${fixedWords.size} 个异常数据..."
                        
                        fixedWords.forEach { stdWord ->
                            // Find original ID to update
                            // We match by word text (assuming uniqueness in library)
                            val original = dbWords.find { it.word.equals(stdWord.word, ignoreCase = true) }
                            
                            if (original != null) {
                                val fixedEntity = stdWord.toEntity(
                                    libraryId = original.libraryId,
                                    example = stdWord.example,
                                    memoryMethod = stdWord.memoryMethod,
                                    definitionEn = stdWord.definitionEn
                                ).copy(
                                    id = original.id, // Preserve ID
                                    createdAt = original.createdAt,
                                    reviewCount = original.reviewCount,
                                    lastReviewTime = original.lastReviewTime,
                                    nextReviewTime = original.nextReviewTime,
                                    easinessFactor = original.easinessFactor,
                                    interval = original.interval,
                                    repetitions = original.repetitions
                                )
                                wordDao.updateWord(fixedEntity)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LogUtil.logError("SanityCheck", "BatchFailed", e)
                _importLogs.value = _importLogs.value + "⚠️ 检查批次失败: ${e.message}"
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
