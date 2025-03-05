/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.exercise

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import com.workoutwrecker.workouttracker.ui.data.Converters

@TypeConverters(Converters::class)
@Database(entities = [Exercise::class], version = 1)
abstract class ExerciseDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile private var instance: ExerciseDatabase? = null

        fun getDatabase(context: Context): ExerciseDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, ExerciseDatabase::class.java, "exercise_db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
