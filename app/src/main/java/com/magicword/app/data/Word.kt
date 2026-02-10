package com.magicword.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val phonetic: String?,
    val definitionCn: String,
    val definitionEn: String?,
    val example: String?,
    val memoryMethod: String?,
    val libraryId: Int,
    val reviewCount: Int = 0,
    val lastReviewTime: Long = 0
)
