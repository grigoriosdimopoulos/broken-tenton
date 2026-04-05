package com.linkedinautomation.di

import android.content.Context
import androidx.room.Room
import com.linkedinautomation.data.local.db.AppDatabase
import com.linkedinautomation.data.local.db.dao.ActivityLogDao
import com.linkedinautomation.data.local.db.dao.ClaudeUsageLogDao
import com.linkedinautomation.data.local.db.dao.JobApplicationDao
import com.linkedinautomation.data.repository.ActivityLogRepositoryImpl
import com.linkedinautomation.data.repository.ClaudeUsageLogRepositoryImpl
import com.linkedinautomation.data.repository.JobApplicationRepositoryImpl
import com.linkedinautomation.data.repository.UserPreferencesRepositoryImpl
import com.linkedinautomation.domain.repository.ActivityLogRepository
import com.linkedinautomation.domain.repository.ClaudeUsageLogRepository
import com.linkedinautomation.domain.repository.JobApplicationRepository
import com.linkedinautomation.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "automation.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideJobApplicationDao(db: AppDatabase): JobApplicationDao = db.jobApplicationDao()

    @Provides
    fun provideActivityLogDao(db: AppDatabase): ActivityLogDao = db.activityLogDao()

    @Provides
    fun provideClaudeUsageLogDao(db: AppDatabase): ClaudeUsageLogDao = db.claudeUsageLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindJobApplicationRepository(impl: JobApplicationRepositoryImpl): JobApplicationRepository

    @Binds
    @Singleton
    abstract fun bindActivityLogRepository(impl: ActivityLogRepositoryImpl): ActivityLogRepository

    @Binds
    @Singleton
    abstract fun bindClaudeUsageLogRepository(impl: ClaudeUsageLogRepositoryImpl): ClaudeUsageLogRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
