/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository) : ViewModel() {

    private val auth = Firebase.auth
    val userId = auth.currentUser?.uid

    fun createExercise(exercise: Exercise, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        userId ?: return
        viewModelScope.launch {
            exerciseRepository.addExercise(userId, exercise, onSuccess, onFailure)
            Log.d("CreateViewModel", "Pass")
        }
    }

    fun getAllExercises(onSuccess: (List<Exercise>) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val cachedExercises = exerciseRepository.getAllExercisesFromCache()
                onSuccess(cachedExercises)
            } catch (e: Exception) {
                onFailure(e)  // Pass the exception to the onFailure callback
            }
        }
    }
}
