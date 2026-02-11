package com.magicword.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE libraryId = :libraryId")
    fun getWordsByLibrary(libraryId: Int): Flow<List<Word>>

    @Query("SELECT * FROM libraries")
    fun getAllLibraries(): Flow<List<Library>>

    @Query("SELECT * FROM libraries WHERE id = :id")
    suspend fun getLibraryById(id: Int): Library?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibrary(library: Library): Long

    @Query("SELECT * FROM words WHERE word = :text AND libraryId = :libraryId LIMIT 1")
    suspend fun getWordByText(text: String, libraryId: Int): Word?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long

    @androidx.room.Delete
    suspend fun deleteWord(word: Word)

    @Update
    suspend fun updateWord(word: Word)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteWordById(id: Int)

    @Query("UPDATE libraries SET lastIndex = :index WHERE id = :libraryId")
    suspend fun updateLibraryLastIndex(libraryId: Int, index: Int)

    @Query("SELECT * FROM words WHERE libraryId = :libraryId")
    suspend fun getWordsByLibraryList(libraryId: Int): List<Word>

    @Query("SELECT * FROM words WHERE libraryId = :libraryId AND nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC")
    fun getDueWords(libraryId: Int, currentTime: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE libraryId IN (:libraryIds) AND nextReviewTime <= :currentTime ORDER BY nextReviewTime ASC")
    fun getDueWordsForLibraries(libraryIds: List<Int>, currentTime: Long): Flow<List<Word>>

    @Query("SELECT * FROM words")
    suspend fun getAllWordsList(): List<Word>

    // Test History
    @Insert
    suspend fun insertTestHistory(history: TestHistory)

    @Query("SELECT * FROM test_history ORDER BY timestamp DESC")
    fun getAllTestHistory(): Flow<List<TestHistory>>
    
    @Query("UPDATE words SET reviewCount = reviewCount + 1, correctCount = correctCount + :correct WHERE id = :id")
    suspend fun updateWordStats(id: Int, correct: Int)

    // Test Session
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTestSession(session: TestSession)

    @Query("SELECT * FROM test_session WHERE id = :id")
    suspend fun getTestSession(id: Int): TestSession?
    
    @Query("DELETE FROM test_session WHERE id = :id")
    suspend fun clearTestSession(id: Int)

    @Query("DELETE FROM libraries WHERE id = :libraryId")
    suspend fun deleteLibrary(libraryId: Int)

    @Query("DELETE FROM words WHERE libraryId = :libraryId")
    suspend fun deleteWordsByLibrary(libraryId: Int)

    @Query("SELECT * FROM words WHERE word LIKE '%' || :query || '%' OR definitionCn LIKE '%' || :query || '%' OR formsJson LIKE '%' || :query || '%'")
    fun searchWords(query: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE word = :text COLLATE NOCASE LIMIT 1")
    suspend fun findWordGlobal(text: String): Word?

    // Word Lists
    @Query("SELECT * FROM word_lists ORDER BY createdAt DESC")
    fun getAllWordLists(): Flow<List<WordList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordList(wordList: WordList): Long

    @androidx.room.Delete
    suspend fun deleteWordList(wordList: WordList)
    
    @Update
    suspend fun updateWordList(wordList: WordList)
}
