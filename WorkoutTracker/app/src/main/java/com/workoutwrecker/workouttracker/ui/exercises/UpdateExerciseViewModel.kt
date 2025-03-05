/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseDatabase
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class UpdateExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository) : ViewModel() {

    private val auth = Firebase.auth
    val userId = auth.currentUser?.uid.toString()

    fun getAllExercises(onSuccess: (List<Exercise>) -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val cachedExercises = repository.getAllExercisesFromCache()
                onSuccess(cachedExercises)
            }
            catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun getExerciseById(exerciseId: String, onSuccess: (Exercise) -> Unit, onFailure: (Exception) -> Unit) {
        getAllExercises(onSuccess = { exercisesList ->
            val exercise = exercisesList.find { it.id == exerciseId }
            if (exercise != null) {
                onSuccess(exercise)
            } else {
                onFailure(Exception("Exercise not found"))
            }
        }, onFailure = { exception ->
            onFailure(exception)
        })
    }

    fun updateExercise(updatedExercise: Exercise, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            repository.updateExercise(updatedExercise, userId, onSuccess, onFailure)
        }
    }

    fun updateExerciseInCache(updatedExercise: Exercise) {
        viewModelScope.launch {
            repository.updateExerciseInLocalCache(updatedExercise)
        }
    }
}
