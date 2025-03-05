/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.workout

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_table")
    suspend fun getAllWorkouts(): List<Workout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: List<Workout>)

    @Update
    suspend fun updateWorkout(workout : Workout)

    @Query("DELETE FROM workout_table")
    suspend fun clearWorkouts()

    @Delete
    suspend fun deleteWorkout(workout : Workout)
}

