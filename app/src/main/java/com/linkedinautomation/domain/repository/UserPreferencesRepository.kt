package com.linkedinautomation.domain.repository

import com.linkedinautomation.domain.model.ClaudePersona
import com.linkedinautomation.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun observe(): Flow<UserPreferences>
    suspend fun get(): UserPreferences
    suspend fun save(prefs: UserPreferences)
    suspend fun savePersona(persona: ClaudePersona)
    suspend fun setAutomationEnabled(enabled: Boolean)
    suspend fun setRequireApproval(requireApproval: Boolean)
}
