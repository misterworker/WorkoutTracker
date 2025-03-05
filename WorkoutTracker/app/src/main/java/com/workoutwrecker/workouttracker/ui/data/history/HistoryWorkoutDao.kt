/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface HistoryWorkoutDao {
    @Query("SELECT * FROM history_workout_table")
    suspend fun getAllHistoryWorkouts(): List<HistoryWorkout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryWorkouts(historyWorkouts: List<HistoryWorkout>)

    @Update
    suspend fun updateHistoryWorkout(workout: HistoryWorkout)

    @Query("DELETE FROM history_workout_table")
    suspend fun clearHistoryWorkouts()

    @Delete
    suspend fun deleteHistoryWorkout(historyWorkout: HistoryWorkout)

    @Query("SELECT * FROM history_workout_table WHERE id = :workoutId")
    suspend fun getHistoryWorkoutById(workoutId: String): HistoryWorkout

}
