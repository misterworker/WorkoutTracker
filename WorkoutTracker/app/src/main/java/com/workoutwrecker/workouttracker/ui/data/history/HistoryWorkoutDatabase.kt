/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.history

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import com.workoutwrecker.workouttracker.ui.data.Converters

@TypeConverters(Converters::class)
@Database(entities = [HistoryWorkout::class], version = 1)
abstract class HistoryWorkoutDatabase : RoomDatabase() {

    abstract fun HistoryWorkoutDao(): HistoryWorkoutDao

    companion object {
        @Volatile private var instance: HistoryWorkoutDatabase? = null

        fun getDatabase(context: Context): HistoryWorkoutDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, HistoryWorkoutDatabase::class.java, "history_db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
