package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.ClaudePersona
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SaveClaudePersonaUseCase @Inject constructor(
    private val repo: UserPreferencesRepository
) {
    suspend operator fun invoke(persona: ClaudePersona) = repo.savePersona(persona)
}
