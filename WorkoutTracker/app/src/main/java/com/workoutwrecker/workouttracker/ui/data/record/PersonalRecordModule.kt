/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.record

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// WorkoutModule.kt
@Module
@InstallIn(SingletonComponent::class)
object PersonalRecordModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PersonalRecordDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            PersonalRecordDatabase::class.java,
            "record_db"
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun providePersonalRecordDao(database: PersonalRecordDatabase): PersonalRecordDao {
        return database.personalRecordDao() // Use the correct function name here
    }
}
