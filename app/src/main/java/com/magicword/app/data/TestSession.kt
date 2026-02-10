package com.magicword.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_session")
data class TestSession(
    @PrimaryKey val id: Int = 1, // Single session for now, or per library
    val currentIndex: Int = 0,
    val score: Int = 0,
    val isFinished: Boolean = false,
    val shuffledIndicesJson: String = "[]", // Store as JSON string
    val testType: String = "CHOICE",
    val libraryId: Int = 0 // Which library is being tested
)