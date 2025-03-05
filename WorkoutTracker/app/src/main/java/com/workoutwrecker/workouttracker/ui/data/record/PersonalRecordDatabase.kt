/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.record

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import com.workoutwrecker.workouttracker.ui.data.Converters

@TypeConverters(Converters::class)
@Database(entities = [PersonalRecord::class], version = 1)
abstract class PersonalRecordDatabase : RoomDatabase() {

    abstract fun personalRecordDao(): PersonalRecordDao

    companion object {
        @Volatile private var instance: PersonalRecordDatabase? = null

        fun getDatabase(context: Context): PersonalRecordDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, PersonalRecordDatabase::class.java, "record_db")
                .fallbackToDestructiveMigration()
                .build()
    }
}