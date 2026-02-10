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
