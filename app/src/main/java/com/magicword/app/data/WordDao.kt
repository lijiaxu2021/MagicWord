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
}
