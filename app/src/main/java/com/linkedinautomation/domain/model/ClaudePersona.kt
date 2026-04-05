package com.linkedinautomation.domain.model

data class ClaudePersona(
    val tone: PersonaTone = PersonaTone.PROFESSIONAL,
    val styleNotes: String = "",
    val avoidPhrases: List<String> = emptyList(),
    val customInstructions: String = ""
)

enum class PersonaTone(val displayName: String) {
    FORMAL("Formal"),
    PROFESSIONAL("Professional"),
    CONFIDENT("Confident"),
    FRIENDLY("Friendly"),
    CASUAL("Casual")
}
