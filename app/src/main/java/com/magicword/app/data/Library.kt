package com.magicword.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libraries")
data class Library(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)
