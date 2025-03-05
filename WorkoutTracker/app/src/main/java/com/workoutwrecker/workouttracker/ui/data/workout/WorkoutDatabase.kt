/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.workout

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.workoutwrecker.workouttracker.ui.data.Converters

@TypeConverters(Converters::class)
@Database(entities = [Workout::class], version = 1)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var instance: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, WorkoutDatabase::class.java, "workout_database")
                .fallbackToDestructiveMigration()
                .build()
    }
}


