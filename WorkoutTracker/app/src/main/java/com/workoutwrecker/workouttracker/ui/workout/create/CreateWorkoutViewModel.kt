/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val auth = Firebase.auth
    val userId = auth.currentUser?.uid

    fun createWorkout(workout: Workout, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        userId ?: return
        viewModelScope.launch {
            workoutRepository.addWorkout(userId, workout, onSuccess, onFailure)
        }
    }

    fun getAllWorkouts(onSuccess: (List<Workout>) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val cachedWorkout = workoutRepository.getAllWorkoutsFromCache()
                onSuccess(cachedWorkout)
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun updateWorkout(workout: Workout, oldExerciseIds: List<String>,
                      onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        userId ?: return
        try {
            viewModelScope.launch {
                workoutRepository.updateWorkout(userId, workout, oldExerciseIds, onSuccess, onFailure)
            }
        }
        catch (e: Exception){
            onFailure(e)
        }
    }

    suspend fun getExerciseById(exerciseId: String): Exercise? {
        return exerciseRepository.getExerciseById(exerciseId)
    }
}
