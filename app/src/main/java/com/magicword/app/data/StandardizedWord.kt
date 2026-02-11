package com.magicword.app.data

import com.google.gson.annotations.SerializedName

/**
 * Standardized JSON structure for AI response.
 * Strictly enforces 10 explicit sense slots to ensure data completeness.
 */
data class StandardizedWord(
    @SerializedName("word") val word: String,
    @SerializedName("phonetic") val phonetic: String?,
    @SerializedName("senses") val senses: Senses,
    @SerializedName("definition_en") val definitionEn: String?,
    @SerializedName("example") val example: String?,
    @SerializedName("memory_method") val memoryMethod: String?
)

data class Senses(
    @SerializedName("sense_1") val sense1: SenseItem?,
    @SerializedName("sense_2") val sense2: SenseItem?,
    @SerializedName("sense_3") val sense3: SenseItem?,
    @SerializedName("sense_4") val sense4: SenseItem?,
    @SerializedName("sense_5") val sense5: SenseItem?,
    @SerializedName("sense_6") val sense6: SenseItem?,
    @SerializedName("sense_7") val sense7: SenseItem?,
    @SerializedName("sense_8") val sense8: SenseItem?,
    @SerializedName("sense_9") val sense9: SenseItem?,
    @SerializedName("sense_10") val sense10: SenseItem?
)

data class SenseItem(
    @SerializedName("pos") val pos: String, // e.g., "n", "v", "adj"
    @SerializedName("meaning") val meaning: String // Chinese meaning
)

/**
 * Helper to convert standardized format to existing flat Word entity.
 * Concatenates valid senses into a single definition string.
 */
fun StandardizedWord.toEntity(libraryId: Int, example: String? = null, memoryMethod: String? = null, definitionEn: String? = null): Word {
    val validSenses = listOfNotNull(
        senses.sense1, senses.sense2, senses.sense3, senses.sense4, senses.sense5,
        senses.sense6, senses.sense7, senses.sense8, senses.sense9, senses.sense10
    )
    
    // Format: "n. 含义1; v. 含义2"
    val combinedDefinition = validSenses.joinToString("; ") { "${it.pos}. ${it.meaning}" }
    
    return Word(
        word = this.word,
        phonetic = this.phonetic,
        definitionCn = combinedDefinition.ifBlank { "暂无释义" },
        definitionEn = definitionEn ?: "",
        example = example,
        memoryMethod = memoryMethod,
        libraryId = libraryId
    )
}
