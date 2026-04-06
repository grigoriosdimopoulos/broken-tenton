package com.linkedinautomation.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import com.linkedinautomation.domain.usecase.SaveUserPreferencesUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepo: UserPreferencesRepository,
    private val savePrefsUseCase: SaveUserPreferencesUseCase
) {
    companion object {
        const val BACKUP_VERSION = 2
        const val BACKUP_FILENAME = "broken_tenton_backup.json"
    }

    /** Serializes all settings + PDF into a JSON file and returns a shareable URI. */
    suspend fun export(): Uri? = runCatching {
        val prefs = prefsRepo.get()

        val json = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("appName", "Broken Tenton")

            // ── Personal Info ──────────────────────────────────────────
            put("firstName", prefs.firstName)
            put("lastName", prefs.lastName)
            put("phone", prefs.phone)
            put("city", prefs.city)
            put("country", prefs.country)
            put("linkedInUrl", prefs.linkedInUrl)
            put("currentJobTitle", prefs.currentJobTitle)
            put("yearsOfExperience", prefs.yearsOfExperience)
            put("email", prefs.email)

            // ── Credentials ────────────────────────────────────────────
            put("linkedInEmail", prefs.linkedInEmail)
            put("linkedInPassword", prefs.linkedInPassword)
            put("linkedInCookies", prefs.linkedInCookies)

            // ── Source ─────────────────────────────────────────────────
            put("sourceMode", prefs.sourceMode.name)
            put("selectedJobBoards", JSONArray(prefs.selectedJobBoards.map { it.name }))

            // ── Job Search ─────────────────────────────────────────────
            put("jobKeywords", JSONArray(prefs.jobKeywords))
            put("location", prefs.location)
            put("remoteOnly", prefs.remoteOnly)
            put("hybridOk", prefs.hybridOk)
            put("onsiteOk", prefs.onsiteOk)
            put("jobTypes", JSONArray(prefs.jobTypes))
            put("experienceLevels", JSONArray(prefs.experienceLevels))
            put("targetIndustries", JSONArray(prefs.targetIndustries))
            put("excludeKeywords", JSONArray(prefs.excludeKeywords))
            put("excludeCompanies", JSONArray(prefs.excludeCompanies))
            put("minSalary", prefs.minSalary)

            // ── Experience Bio ─────────────────────────────────────────
            put("experienceBio", prefs.experienceBio)

            // ── Claude ─────────────────────────────────────────────────
            put("claudeApiKey", prefs.claudeApiKey)
            put("claudePersonaTone", prefs.claudePersona.tone.name)
            put("claudePersonaStyleNotes", prefs.claudePersona.styleNotes)
            put("claudePersonaAvoidPhrases", JSONArray(prefs.claudePersona.avoidPhrases))
            put("claudePersonaCustomInstructions", prefs.claudePersona.customInstructions)

            // ── Resume ─────────────────────────────────────────────────
            put("resumeFileName", prefs.resumeFileName)
            val resumeFile = File(context.filesDir, "resume.pdf")
            if (resumeFile.exists()) {
                put("resumePdfBase64", Base64.encodeToString(resumeFile.readBytes(), Base64.NO_WRAP))
            }
        }

        val file = File(context.cacheDir, BACKUP_FILENAME)
        file.writeText(json.toString(2))

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    /** Parses the backup JSON and restores all settings + PDF. Returns true on success. */
    suspend fun import(uri: Uri): Boolean = runCatching {
        val raw = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText() ?: return false
        val obj = JSONObject(raw)

        // Restore PDF first
        val pdfBase64 = obj.optString("resumePdfBase64", "")
        if (pdfBase64.isNotBlank()) {
            val bytes = Base64.decode(pdfBase64, Base64.DEFAULT)
            File(context.filesDir, "resume.pdf").writeBytes(bytes)
        }

        val prefs = UserPreferences(
            isSetupComplete = true,
            automationEnabled = true,
            requireApproval = false,
            scanIntervalMinutes = 30,

            // Personal info
            firstName = obj.optString("firstName"),
            lastName = obj.optString("lastName"),
            phone = obj.optString("phone"),
            city = obj.optString("city"),
            country = obj.optString("country"),
            linkedInUrl = obj.optString("linkedInUrl"),
            currentJobTitle = obj.optString("currentJobTitle"),
            yearsOfExperience = obj.optInt("yearsOfExperience", 0),
            email = obj.optString("email"),

            // Credentials
            linkedInEmail = obj.optString("linkedInEmail"),
            linkedInPassword = obj.optString("linkedInPassword"),
            linkedInCookies = obj.optString("linkedInCookies", ""),

            // Source
            sourceMode = runCatching {
                SourceMode.valueOf(obj.optString("sourceMode", "LINKEDIN"))
            }.getOrElse { SourceMode.LINKEDIN },
            selectedJobBoards = obj.optJSONArray("selectedJobBoards")?.toStringList()
                ?.mapNotNull { runCatching { JobBoardSource.valueOf(it) }.getOrNull() }
                ?: listOf(JobBoardSource.INDEED),

            // Job search
            jobKeywords = obj.optJSONArray("jobKeywords")?.toStringList() ?: emptyList(),
            location = obj.optString("location"),
            remoteOnly = obj.optBoolean("remoteOnly", false),
            hybridOk = obj.optBoolean("hybridOk", true),
            onsiteOk = obj.optBoolean("onsiteOk", true),
            jobTypes = obj.optJSONArray("jobTypes")?.toStringList() ?: listOf("FULL_TIME"),
            experienceLevels = obj.optJSONArray("experienceLevels")?.toStringList() ?: listOf("MID_SENIOR"),
            targetIndustries = obj.optJSONArray("targetIndustries")?.toStringList() ?: emptyList(),
            excludeKeywords = obj.optJSONArray("excludeKeywords")?.toStringList() ?: emptyList(),
            excludeCompanies = obj.optJSONArray("excludeCompanies")?.toStringList() ?: emptyList(),
            minSalary = obj.optInt("minSalary", 0),

            // Bio
            experienceBio = obj.optString("experienceBio"),

            // Claude
            claudeApiKey = obj.optString("claudeApiKey"),
            claudePersona = ClaudePersona(
                tone = runCatching {
                    PersonaTone.valueOf(obj.optString("claudePersonaTone", "PROFESSIONAL"))
                }.getOrElse { PersonaTone.PROFESSIONAL },
                styleNotes = obj.optString("claudePersonaStyleNotes"),
                avoidPhrases = obj.optJSONArray("claudePersonaAvoidPhrases")?.toStringList() ?: emptyList(),
                customInstructions = obj.optString("claudePersonaCustomInstructions")
            ),

            // Resume
            resumeFileName = if (pdfBase64.isNotBlank()) "resume.pdf"
                             else obj.optString("resumeFileName")
        )

        savePrefsUseCase(prefs)
        true
    }.getOrElse { false }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }
}
