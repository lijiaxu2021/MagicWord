package com.easyword.app.data

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

    @Query("SELECT * FROM words WHERE word = :word AND libraryId = :libraryId LIMIT 1")
    suspend fun getWordByText(word: String, libraryId: Int): Word?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Word): Long

    @Update
    suspend fun updateWord(word: Word)
}
