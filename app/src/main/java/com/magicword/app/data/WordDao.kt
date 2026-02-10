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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibrary(library: Library)

    @Query("SELECT * FROM words WHERE word = :text AND libraryId = :libraryId LIMIT 1")
    suspend fun getWordByText(text: String, libraryId: Int): Word?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long

    @androidx.room.Delete
    suspend fun deleteWord(word: Word)

    @Update
    suspend fun updateWord(word: Word)

    @Query("SELECT * FROM words")
    suspend fun getAllWordsList(): List<Word>
}
