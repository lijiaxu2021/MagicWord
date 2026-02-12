package com.magicword.app.data

/**
 * Data class for exporting a library with its words.
 * Used for JSON serialization.
 */
data class LibraryExportData(
    val name: String,
    val description: String,
    val words: List<Word>
)

/**
 * Wrapper for exporting multiple libraries.
 */
data class ExportPackage(
    val version: Int = 1,
    val libraries: List<LibraryExportData>
)

/**
 * Data class for Online Library Item (from index.json)
 */
data class OnlineLibrary(
    val id: String,
    val name: String,
    val description: String,
    val timestamp: Long,
    val author: String,
    val downloadUrl: String // Provided by index.json (or constructed)
)
