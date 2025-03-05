/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise

class MoveExerciseViewModel : ViewModel() {
    private val exercises: MutableList<WorkoutExercise> = mutableListOf()

    fun getExercises(): MutableList<WorkoutExercise> {
        return exercises
    }

    fun setExercises(exercises: List<WorkoutExercise>) {
        this.exercises.clear()
        this.exercises.addAll(exercises)
    }

    fun updateAndSaveExercises(newOrder: List<WorkoutExercise>) {
        // Update exercise order
        exercises.clear()
        exercises.addAll(newOrder)

        // Normalize superset IDs
        val distinctSupersetIds = exercises.mapNotNull { it.supersetid }.distinct().sorted()
        val supersetIdMapping = distinctSupersetIds.mapIndexed { index, oldId -> oldId to (index + 1) }.toMap()

        // Update exercises with new superset IDs
        val normalizedExercises = exercises.map { exercise ->
            if (exercise.supersetid != null) {
                val newSupersetId = supersetIdMapping[exercise.supersetid]
                Log.d("MoveExerciseViewModel", "${exercise.title}: $newSupersetId")
                exercise.copy(supersetid = newSupersetId)
            } else {
                Log.d("MoveExerciseViewModel", "${exercise.title}")
                exercise
            }
        }

        // Save normalized exercises to the list
        exercises.clear()
        exercises.addAll(normalizedExercises)
        Log.d("MoveExerciseViewModel", "$exercises")
    }

}
