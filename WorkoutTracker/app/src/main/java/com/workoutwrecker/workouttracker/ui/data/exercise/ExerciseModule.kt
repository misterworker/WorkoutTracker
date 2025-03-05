/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.exercise

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
object ExerciseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExerciseDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ExerciseDatabase::class.java,
            "exercise_db"
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideExerciseDao(database: ExerciseDatabase): ExerciseDao {
        return database.exerciseDao() // Use the correct function name here
    }
}
