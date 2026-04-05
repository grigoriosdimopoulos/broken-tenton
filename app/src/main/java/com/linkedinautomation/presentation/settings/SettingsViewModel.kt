package com.linkedinautomation.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkedinautomation.domain.model.ClaudePersona
import com.linkedinautomation.domain.model.UserPreferences
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import com.linkedinautomation.domain.usecase.SaveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository,
    private val savePrefsUseCase: SaveUserPreferencesUseCase
) : ViewModel() {

    val prefs: Flow<UserPreferences> = prefsRepo.observe()

    fun savePersona(persona: ClaudePersona) {
        viewModelScope.launch { prefsRepo.savePersona(persona) }
    }

    fun save(prefs: UserPreferences) {
        viewModelScope.launch { savePrefsUseCase(prefs) }
    }
}
