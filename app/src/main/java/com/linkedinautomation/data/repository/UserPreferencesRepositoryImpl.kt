package com.linkedinautomation.data.repository

import android.content.Context
import com.linkedinautomation.data.local.datastore.userPreferencesDataStore
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val dataStore = context.userPreferencesDataStore

    override fun observe(): Flow<UserPreferences> = dataStore.data.map { it.toDomain() }

    override suspend fun get(): UserPreferences = dataStore.data.first().toDomain()

    override suspend fun save(prefs: UserPreferences) {
        dataStore.updateData { prefs.toProto() }
    }

    override suspend fun savePersona(persona: ClaudePersona) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setPersonaTone(persona.tone.name)
                .setPersonaStyleNotes(persona.styleNotes)
                .clearPersonaAvoidPhrases()
                .addAllPersonaAvoidPhrases(persona.avoidPhrases)
                .setPersonaCustomInstructions(persona.customInstructions)
                .build()
        }
    }

    override suspend fun setAutomationEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setAutomationEnabled(enabled).build() }
    }

    override suspend fun setRequireApproval(requireApproval: Boolean) {
        dataStore.updateData { it.toBuilder().setRequireApproval(requireApproval).build() }
    }

    override suspend fun setLinkedInCookies(cookies: String) {
        dataStore.updateData { it.toBuilder().setLinkedInCookies(cookies).build() }
    }

    private fun com.linkedinautomation.UserPreferencesProto.toDomain() = UserPreferences(
        isSetupComplete = isSetupComplete,
        sourceMode = if (sourceMode == "DIRECT") SourceMode.DIRECT else SourceMode.LINKEDIN,
        linkedInEmail = linkedInEmail,
        linkedInPassword = linkedInPassword,
        selectedJobBoards = selectedJobBoardsList.mapNotNull { name ->
            runCatching { JobBoardSource.valueOf(name) }.getOrNull()
        }.ifEmpty { listOf(JobBoardSource.INDEED) },
        jobKeywords = jobKeywordsList,
        location = location,
        remoteOnly = remoteOnly,
        hybridOk = hybridOk,
        onsiteOk = onsiteOk,
        jobTypes = jobTypesList.ifEmpty { listOf("FULL_TIME") },
        experienceLevels = experienceLevelsList.ifEmpty { listOf("MID_SENIOR") },
        targetIndustries = targetIndustriesList,
        excludeKeywords = excludeKeywordsList,
        excludeCompanies = excludeCompaniesList,
        experienceBio = experienceBio,
        claudeApiKey = claudeApiKey,
        claudePersona = ClaudePersona(
            tone = runCatching { PersonaTone.valueOf(personaTone) }.getOrElse { PersonaTone.PROFESSIONAL },
            styleNotes = personaStyleNotes,
            avoidPhrases = personaAvoidPhrasesList,
            customInstructions = personaCustomInstructions
        ),
        resumeFileName = resumeFileName,
        automationEnabled = automationEnabled,
        requireApproval = requireApproval,
        scanIntervalMinutes = if (scanIntervalMinutes > 0) scanIntervalMinutes else 30,
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        city = city,
        country = country,
        linkedInUrl = linkedInUrl,
        currentJobTitle = currentJobTitle,
        yearsOfExperience = yearsOfExperience,
        minSalary = minSalary,
        email = email,
        linkedInCookies = linkedInCookies,
        easyApplyOnly = easyApplyOnly,
        aiAssistFallback = aiAssistFallback,
        scanLookbackDays = when {
            scanLookbackDays < 0 -> -1  // -1 = "All time" (user explicitly selected)
            scanLookbackDays > 0 -> scanLookbackDays
            else -> 1                   // 0 = proto default (never set) → 1 day
        },
        easyApplyMaxAttempts = if (easyApplyMaxAttempts > 0) easyApplyMaxAttempts else 5,
        smartApplyMode = smartApplyMode,
        smartApplyModel = smartApplyModel.ifBlank { "claude-sonnet-4-6" }
    )

    private fun UserPreferences.toProto(): com.linkedinautomation.UserPreferencesProto =
        com.linkedinautomation.UserPreferencesProto.newBuilder()
            .setIsSetupComplete(isSetupComplete)
            .setSourceMode(sourceMode.name)
            .setLinkedInEmail(linkedInEmail)
            .setLinkedInPassword(linkedInPassword)
            .addAllSelectedJobBoards(selectedJobBoards.map { it.name })
            .addAllJobKeywords(jobKeywords)
            .setLocation(location)
            .setRemoteOnly(remoteOnly)
            .setHybridOk(hybridOk)
            .setOnsiteOk(onsiteOk)
            .addAllJobTypes(jobTypes)
            .addAllExperienceLevels(experienceLevels)
            .addAllTargetIndustries(targetIndustries)
            .addAllExcludeKeywords(excludeKeywords)
            .addAllExcludeCompanies(excludeCompanies)
            .setExperienceBio(experienceBio)
            .setClaudeApiKey(claudeApiKey)
            .setPersonaTone(claudePersona.tone.name)
            .setPersonaStyleNotes(claudePersona.styleNotes)
            .addAllPersonaAvoidPhrases(claudePersona.avoidPhrases)
            .setPersonaCustomInstructions(claudePersona.customInstructions)
            .setResumeFileName(resumeFileName)
            .setAutomationEnabled(automationEnabled)
            .setRequireApproval(requireApproval)
            .setScanIntervalMinutes(scanIntervalMinutes)
            .setFirstName(firstName)
            .setLastName(lastName)
            .setPhone(phone)
            .setCity(city)
            .setCountry(country)
            .setLinkedInUrl(linkedInUrl)
            .setCurrentJobTitle(currentJobTitle)
            .setYearsOfExperience(yearsOfExperience)
            .setMinSalary(minSalary)
            .setEmail(email)
            .setLinkedInCookies(linkedInCookies)
            .setEasyApplyOnly(easyApplyOnly)
            .setAiAssistFallback(aiAssistFallback)
            .setScanLookbackDays(scanLookbackDays)
            .setEasyApplyMaxAttempts(easyApplyMaxAttempts)
            .setSmartApplyMode(smartApplyMode)
            .setSmartApplyModel(smartApplyModel)
            .build()
}
