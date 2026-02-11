package com.magicword.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

import androidx.room.Index

@Entity(
    tableName = "words",
    indices = [Index(value = ["word", "libraryId"], unique = true)]
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val phonetic: String?,
    @SerializedName("definition_cn") val definitionCn: String,
    @SerializedName("definition_en") val definitionEn: String?,
    val example: String?,
    @SerializedName("memory_method") val memoryMethod: String?,
    val libraryId: Int,
    val reviewCount: Int = 0,
    val lastReviewTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val sortOrder: Int = 0,
    // SRS Fields
    val nextReviewTime: Long = 0, // 0 means new/unstudied
    val easinessFactor: Float = 2.5f,
    val interval: Int = 0, // Days
    val repetitions: Int = 0,
    // Word Forms (JSON String: {"past": "...", "participle": "...", "plural": "..."})
    val formsJson: String? = null
)
