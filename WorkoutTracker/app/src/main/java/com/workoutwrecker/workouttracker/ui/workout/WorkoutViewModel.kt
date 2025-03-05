/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    @ApplicationContext private val context: Context) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
    private val _workouts = MutableLiveData<List<Workout>>()
    private val _nonDefaultWorkouts = MutableLiveData<List<Workout>>()
    private val _programs =  MutableLiveData<List<String>>()
    val workouts: LiveData<List<Workout>> get() = _workouts
    val nonDefaultWorkouts: LiveData<List<Workout>> get() = _nonDefaultWorkouts
    val programs: LiveData<List<String>> get() = _programs

    companion object {
        var isFetched = false
    }

    init {
        isFetched = sharedPreferences.getBoolean("isFetched", false)
    }

    fun resetIsFetched() {
        isFetched = false
        sharedPreferences.edit().putBoolean("isFetched", false).apply()
    }

    fun createWorkout(userId: String?, workout: Workout, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        userId ?: return
        viewModelScope.launch {
            repository.addWorkout(userId, workout, onSuccess, onFailure)
        }
    }

    suspend fun fetchWorkouts(userId: String) {
        if (!isFetched) {
            Log.d("WorkoutViewModel", "Fetching from Firestore")
            val result = repository.getAllWorkouts(userId)
            result.onSuccess { workouts ->
                _workouts.value = workouts
                _programs.value = workouts.map {it.programName}.distinct()
                isFetched = true
                sharedPreferences.edit().putBoolean("isFetched", true).apply()
            }.onFailure { exception ->
                Log.e("WorkoutViewModel", "Error fetching workouts", exception)
                isFetched = false
                sharedPreferences.edit().putBoolean("isFetched", false).apply()
            }
        } else {
            Log.d("WorkoutViewModel", "Fetching from Cache")
            try {
                val cachedWorkouts = withContext(Dispatchers.IO) {
                    repository.getAllWorkoutsFromCache()
                }
                _workouts.value = cachedWorkouts
                _programs.value = cachedWorkouts.map {it.programName}.distinct()
            } catch (e: Exception) {
                Log.e("WorkoutViewModel", "Error fetching cached workouts", e)
            }
        }
        fetchNonDefaultWorkouts()
    }

    private fun fetchNonDefaultWorkouts() {
        _nonDefaultWorkouts.value = _workouts.value?.filter { it.id.startsWith("user") }
    }

    suspend fun fetchFilteredWorkouts(userId: String, programName: String) {
        try {
            val cachedWorkouts = withContext(Dispatchers.IO) {
                repository.getAllWorkoutsFromCache()
            }
            val filteredWorkouts = cachedWorkouts.filter { it.programName == programName }
            _workouts.value = filteredWorkouts
        } catch (e: Exception) {
            Log.e("WorkoutViewModel", "Error fetching cached workouts", e)
        }
    }

    fun deleteWorkout(userId: String, workout: Workout, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            repository.deleteWorkout(userId, workout, onSuccess, onFailure)
        }
    }

    fun updateWorkoutProgram(userId: String, program: String, workouts: List<Workout>,
                                  onSuccess: () -> Unit, onFailure: (Exception) -> Unit){
        for (workout in workouts){
            try {
                Log.d("WorkoutViewModel", "Program: $program")
                workout.programName = program
                viewModelScope.launch {
                    repository.updateWorkout(userId, workout,
                        workout.exercises.map {it.id}, onSuccess, onFailure)
                }
            }
            catch (e: Exception){
                onFailure(e)
            }
        }
    }
}
