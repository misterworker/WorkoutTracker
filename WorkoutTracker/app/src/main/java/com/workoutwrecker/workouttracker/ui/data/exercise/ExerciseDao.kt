/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.exercise

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<Exercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Delete
    suspend fun deleteExercise(exercise: Exercise)

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: String): Exercise?

}
