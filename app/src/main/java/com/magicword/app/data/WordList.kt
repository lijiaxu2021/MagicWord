package com.magicword.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_lists")
data class WordList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val libraryIdsJson: String, // JSON Array of library IDs e.g. "[1, 2]"
    val createdAt: Long = System.currentTimeMillis()
)
