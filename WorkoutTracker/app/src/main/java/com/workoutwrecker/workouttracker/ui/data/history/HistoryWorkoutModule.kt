/*
 * Copyright (c) 2024. 
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.history

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HistoryWorkoutModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HistoryWorkoutDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HistoryWorkoutDatabase::class.java,
            "history_db"
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideExerciseDao(database: HistoryWorkoutDatabase): HistoryWorkoutDao {
        return database.HistoryWorkoutDao() // Use the correct function name here
    }
}
