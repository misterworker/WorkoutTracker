/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkout
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkoutRepository
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class WorkoutStartViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val historyWorkoutRepository: HistoryWorkoutRepository) : ViewModel() {

    private val auth = Firebase.auth
    val userId = auth.currentUser?.uid

    fun saveWorkoutToHistory(historyWorkout: HistoryWorkout, onSuccess: () -> Unit,
                             onFailure: (Exception) -> Unit) {
        userId ?: return
        viewModelScope.launch {
            historyWorkoutRepository.addHistoryWorkout(userId, historyWorkout, onSuccess, onFailure)
        }
    }

    fun updateHistoryWorkout(historyWorkout: HistoryWorkout, oldExercises: List<String>,
                             onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        userId ?: return
        viewModelScope.launch {
            historyWorkoutRepository.updateHistoryWorkout(userId, historyWorkout,
                oldExercises, onSuccess, onFailure)
        }
    }

    fun getAllWorkouts(onSuccess: (List<Workout>) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val cachedWorkout = workoutRepository.getAllWorkoutsFromCache()
                onSuccess(cachedWorkout)
            } catch (e: Exception) {
                onFailure(e)  // Pass the exception to the onFailure callback
            }
        }
    }

    suspend fun getExerciseById(exerciseId: String) =
        exerciseRepository.getExerciseById(exerciseId)

    suspend fun getHistoryWorkoutById(historyWorkoutId: String) =
        historyWorkoutRepository.getHistoryWorkoutById(historyWorkoutId)

    suspend fun getMaxByExerciseId(exerciseId: String): Int {


        return 0
    }
}
