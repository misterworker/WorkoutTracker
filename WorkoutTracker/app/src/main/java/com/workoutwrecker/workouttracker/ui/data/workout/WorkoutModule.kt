/*
 * Copyright (c) 2024. 
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.workout

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
object WorkoutModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            WorkoutDatabase::class.java,
            "workout_database"
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideExerciseDao(database: WorkoutDatabase): WorkoutDao {
        return database.workoutDao() // Use the correct function name here
    }
}
