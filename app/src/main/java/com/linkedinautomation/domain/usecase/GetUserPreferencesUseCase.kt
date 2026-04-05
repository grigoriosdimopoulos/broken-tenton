package com.linkedinautomation.domain.usecase

import com.linkedinautomation.domain.model.UserPreferences
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserPreferencesUseCase @Inject constructor(
    private val repo: UserPreferencesRepository
) {
    fun observe(): Flow<UserPreferences> = repo.observe()
    suspend fun get(): UserPreferences = repo.get()
}
