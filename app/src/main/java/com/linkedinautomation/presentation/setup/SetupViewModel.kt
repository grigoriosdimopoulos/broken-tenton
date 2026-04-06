package com.linkedinautomation.presentation.setup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.background.worker.WorkScheduler
import com.linkedinautomation.domain.model.*
import com.linkedinautomation.domain.usecase.SaveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val savePrefsUseCase: SaveUserPreferencesUseCase,
    private val workScheduler: WorkScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _prefs = MutableStateFlow(UserPreferences())
    val prefs: StateFlow<UserPreferences> = _prefs

    private val _setupDone = MutableStateFlow(false)
    val setupDone: StateFlow<Boolean> = _setupDone

    fun setSourceMode(mode: SourceMode) {
        _prefs.value = _prefs.value.copy(sourceMode = mode)
    }

    fun setCredentials(email: String, password: String) {
        _prefs.value = _prefs.value.copy(linkedInEmail = email, linkedInPassword = password)
    }

    fun setSelectedJobBoards(boards: List<JobBoardSource>) {
        _prefs.value = _prefs.value.copy(selectedJobBoards = boards)
    }

    fun setJobKeywords(keywords: List<String>) {
        _prefs.value = _prefs.value.copy(jobKeywords = keywords)
    }

    fun setLocation(location: String, remoteOnly: Boolean, hybridOk: Boolean, onsiteOk: Boolean) {
        _prefs.value = _prefs.value.copy(
            location = location, remoteOnly = remoteOnly, hybridOk = hybridOk, onsiteOk = onsiteOk
        )
    }

    fun setMinSalary(minSalary: Int) {
        _prefs.value = _prefs.value.copy(minSalary = minSalary)
    }

    fun setFilters(jobTypes: List<String>, experienceLevels: List<String>, industries: List<String>) {
        _prefs.value = _prefs.value.copy(
            jobTypes = jobTypes, experienceLevels = experienceLevels, targetIndustries = industries
        )
    }

    fun setExclusions(keywords: List<String>, companies: List<String>) {
        _prefs.value = _prefs.value.copy(excludeKeywords = keywords, excludeCompanies = companies)
    }

    fun setPersonalInfo(
        firstName: String, lastName: String, phone: String,
        city: String, country: String, linkedInUrl: String,
        currentJobTitle: String, yearsOfExperience: Int,
        email: String = ""
    ) {
        _prefs.value = _prefs.value.copy(
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            city = city,
            country = country,
            linkedInUrl = linkedInUrl,
            currentJobTitle = currentJobTitle,
            yearsOfExperience = yearsOfExperience,
            email = email
        )
    }

    fun setExperienceBio(bio: String) {
        _prefs.value = _prefs.value.copy(experienceBio = bio)
    }

    fun setClaudeSetup(apiKey: String, persona: ClaudePersona) {
        _prefs.value = _prefs.value.copy(claudeApiKey = apiKey, claudePersona = persona)
    }

    fun copyResume(uri: Uri): Boolean {
        return runCatching {
            val dest = File(context.filesDir, "resume.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            _prefs.value = _prefs.value.copy(resumeFileName = dest.name)
            true
        }.getOrElse { false }
    }

    fun complete() {
        viewModelScope.launch {
            val finalPrefs = _prefs.value.copy(
                isSetupComplete = true,
                automationEnabled = true
            )
            savePrefsUseCase(finalPrefs)
            workScheduler.schedule(finalPrefs.scanIntervalMinutes)
            _setupDone.value = true
        }
    }
}
