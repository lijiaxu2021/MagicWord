package com.magicword.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_history")
data class TestHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalQuestions: Int,
    val correctCount: Int,
    val testType: String, // "CHOICE" or "SPELL"
    val durationSeconds: Long,
    val questionsJson: String = "[]" // JSON string of List<TestResultItem>
)

data class TestResultItem(
    val wordId: Int,
    val word: String,
    val isCorrect: Boolean,
    val userAnswer: String? = null // For spelling, or option text
)
