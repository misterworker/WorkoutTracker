/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    @ApplicationContext private val context: Context,) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)

    private val _exercises = MutableLiveData<List<Pair<String, Exercise?>>>()
    val exercises: LiveData<List<Pair<String, Exercise?>>>
        get() = _exercises

    private var filteredExercises: List<Exercise> = listOf()

    companion object {
        var isFetched = true
    }

    init {
        isFetched = context.getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE).getBoolean("isFetched", false)
    }

    suspend fun fetchExercises(userId: String, selectedTypes: List<String>, selectedBodyParts: List<String>) {
        if (!isFetched) {
            Log.d("ExerciseViewModel", "Fetching from FireStore")
            val result = repository.getAllExercises(userId)
            result.onSuccess { exercises ->
                if (selectedTypes.isNotEmpty() || selectedBodyParts.isNotEmpty()) {
                    filterExercises(selectedTypes, selectedBodyParts, exercises)
                } else {
                    filteredExercises = exercises
                    updateExerciseListWithLabels(exercises)
                }
                isFetched = true
                sharedPreferences.edit().putBoolean("isFetched", true).apply()
            }.onFailure {
                isFetched = false
                sharedPreferences.edit().putBoolean("isFetched", false).apply()
            }
        }
        else {
            Log.d("ExerciseViewModel", "Fetching from Cache")
            try {
                val cachedExercises = withContext(Dispatchers.IO) {
                    repository.getAllExercisesFromCache()
                }
                if (selectedTypes.isNotEmpty() || selectedBodyParts.isNotEmpty()) {
                    filterExercises(selectedTypes, selectedBodyParts, cachedExercises)
                } else {
                    filteredExercises = cachedExercises
                    updateExerciseListWithLabels(cachedExercises)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun searchExercises(keyword: String) {
        viewModelScope.launch {
            try {
                val searchResults = filteredExercises.filter { it.title.contains(keyword, ignoreCase = true) }
                updateExerciseListWithLabels(searchResults)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun filterExercises(selectedTypes: List<String>, selectedBodyParts: List<String>, exercises: List<Exercise>) {
        filteredExercises = exercises.filter { exercise ->
            (selectedTypes.isEmpty() || exercise.type in selectedTypes) &&
                    (selectedBodyParts.isEmpty() || exercise.bodypart in selectedBodyParts)
        }
        updateExerciseListWithLabels(filteredExercises)
    }

    private fun updateExerciseListWithLabels(exercises: List<Exercise>) {
        val sortedExercises = exercises.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        val items = mutableListOf<Pair<String, Exercise?>>()
        var currentLabel = ""
        for (exercise in sortedExercises) {
            val firstLetter = exercise.title.first().uppercaseChar().toString()
            if (firstLetter != currentLabel) {
                currentLabel = firstLetter
                items.add(Pair(currentLabel, null))
            }
            items.add(Pair("", exercise))
        }
        _exercises.value = items
    }

    fun deleteExercise(userId: String, exercise: Exercise, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            repository.deleteExercise(userId, exercise, onSuccess, onFailure)
        }
    }
}

