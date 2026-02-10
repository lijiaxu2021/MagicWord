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
    val durationSeconds: Long
)