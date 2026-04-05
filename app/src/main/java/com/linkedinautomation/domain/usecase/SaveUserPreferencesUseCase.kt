package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.UserPreferences
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SaveUserPreferencesUseCase @Inject constructor(
    private val repo: UserPreferencesRepository
) {
    suspend operator fun invoke(prefs: UserPreferences) = repo.save(prefs)
}
