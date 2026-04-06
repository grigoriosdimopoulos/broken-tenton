package com.linkedinautomation.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.domain.model.ClaudePersona
import com.linkedinautomation.domain.model.UserPreferences
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import com.linkedinautomation.domain.usecase.SaveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val savePrefsUseCase: SaveUserPreferencesUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val prefs: Flow<UserPreferences> = prefsRepo.observe()

    fun savePersona(persona: ClaudePersona) {
        viewModelScope.launch { prefsRepo.savePersona(persona) }
    }

    fun save(prefs: UserPreferences) {
        viewModelScope.launch { savePrefsUseCase(prefs) }
    }

    fun updatePersonalInfo(
        firstName: String, lastName: String, phone: String,
        city: String, country: String, linkedInUrl: String,
        currentJobTitle: String, yearsOfExperience: Int
    ) {
        viewModelScope.launch {
            val current = prefsRepo.get()
            savePrefsUseCase(current.copy(
                firstName = firstName, lastName = lastName, phone = phone,
                city = city, country = country, linkedInUrl = linkedInUrl,
                currentJobTitle = currentJobTitle, yearsOfExperience = yearsOfExperience
            ))
        }
    }

    fun updateCredentials(email: String, password: String) {
        viewModelScope.launch {
            val current = prefsRepo.get()
            savePrefsUseCase(current.copy(linkedInEmail = email, linkedInPassword = password))
        }
    }

    fun updateJobPrefs(
        keywords: List<String>, location: String,
        remoteOnly: Boolean, hybridOk: Boolean, onsiteOk: Boolean,
        excludeKeywords: List<String>, excludeCompanies: List<String>
    ) {
        viewModelScope.launch {
            val current = prefsRepo.get()
            savePrefsUseCase(current.copy(
                jobKeywords = keywords, location = location,
                remoteOnly = remoteOnly, hybridOk = hybridOk, onsiteOk = onsiteOk,
                excludeKeywords = excludeKeywords, excludeCompanies = excludeCompanies
            ))
        }
    }

    fun copyResume(uri: Uri): Boolean {
        return runCatching {
            val dest = File(context.filesDir, "resume.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            viewModelScope.launch {
                val current = prefsRepo.get()
                savePrefsUseCase(current.copy(resumeFileName = dest.name))
            }
            true
        }.getOrElse { false }
    }
}
