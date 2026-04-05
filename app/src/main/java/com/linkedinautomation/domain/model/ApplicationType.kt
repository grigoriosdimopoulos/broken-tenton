package com.linkedinautomation.domain.model

sealed class ApplicationType {
    object EasyApply : ApplicationType()
    data class External(val atsName: String, val url: String) : ApplicationType()

    fun displayName(): String = when (this) {
        is EasyApply -> "Easy Apply"
        is External -> "External (${atsName})"
    }

    fun toTypeString(): String = when (this) {
        is EasyApply -> "easy_apply"
        is External -> "external:$atsName:$url"
    }

    companion object {
        fun fromTypeString(s: String): ApplicationType = when {
            s == "easy_apply" -> EasyApply
            s.startsWith("external:") -> {
                val parts = s.split(":", limit = 3)
                External(parts.getOrElse(1) { "Unknown" }, parts.getOrElse(2) { "" })
            }
            else -> EasyApply
        }
    }
}
