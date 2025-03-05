/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.exercise

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.workoutwrecker.workouttracker.ui.exercises.ExerciseViewModel
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val defaultExercisesCollection = firestore.collection("exercises")
    private var cachedExercises: List<Exercise> = emptyList()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val sharedPreferences = context.getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    // Add keys for limits
    private val updateCountKey = "update_count"
    private val deleteCountKey = "delete_count"
    private val lastResetTimeKey = "last_reset_time"
    private val limitPerDay = 16

    // Reset the counts daily
    private fun resetDailyLimits() {
        val currentTime = System.currentTimeMillis()
        val lastResetTime = sharedPreferences.getLong(lastResetTimeKey, 0)
        if (currentTime - lastResetTime > TimeUnit.DAYS.toMillis(1)) {
            sharedPreferences.edit()
                .putInt(updateCountKey, 0)
                .putInt(deleteCountKey, 0)
                .putLong(lastResetTimeKey, currentTime)
                .apply()
        }
    }

    private fun canPerformUpdateOrDelete(operationCountKey: String): Boolean {
        resetDailyLimits()
        val currentCount = sharedPreferences.getInt(operationCountKey, 0)
        return currentCount < limitPerDay
    }

    private fun incrementOperationCount(operationCountKey: String) {
        val currentCount = sharedPreferences.getInt(operationCountKey, 0)
        sharedPreferences.edit().putInt(operationCountKey, currentCount + 1).apply()
    }

    private fun canUpdateExercise(): Boolean {
        return canPerformUpdateOrDelete(updateCountKey)
    }

    private fun canDeleteExercise(): Boolean {
        return canPerformUpdateOrDelete(deleteCountKey)
    }

    private fun Exercise.toMap(): Map<String, Any?> {
        return mapOf(
            "title" to title,
            "type" to type,
            "bodypart" to bodypart,
            "instructions" to instructions,
            "category" to category,
            "private" to private_,
            // id is excluded
        )
    }

    // Add an exercise directly to FireStore
    private suspend fun addExerciseToFireStore(userId: String, exercise: Exercise) {
        val userExercisesCollection = firestore.collection("users").document(userId).collection("exercises")
        try {
            val exerciseMap = exercise.toMap().toMutableMap()
            exerciseMap.remove("id")
            userExercisesCollection.document(exercise.id).set(exerciseMap).await()
        } catch (e: Exception) {
            Log.e("ExerciseRepository", "Error adding exercise to FireStore", e)
        }
    }

    // Add an exercise to the local Room database
    private suspend fun addExerciseToLocalCache(exercise: Exercise) {
        exerciseDao.insertExercises(listOf(exercise))
    }

    // Add an exercise to a user's collection in FireStore
    suspend fun addExercise(userId: String, exercise: Exercise, onCacheSuccess: () -> Unit,
                            onFailure: (Exception) -> Unit) {
        try {
            addExerciseToLocalCache(exercise) //Add to local cache first
            onCacheSuccess()
            addExerciseToFireStore(userId, exercise) //Queue firestore exercise
            Toast.makeText(appContext, "${exercise.title} created successfully!", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception){
            Log.e("ExerciseRepository", "Error adding exercise", e)
            onFailure(e)
        }
    }

    private fun updateExerciseInFireStore(updatedExercise: Exercise, userId: String) {
        try {
            val exerciseMap = updatedExercise.toMap().toMutableMap()
            exerciseMap.remove("id")
            firestore
                .collection("users").document(userId)
                .collection("exercises").document(updatedExercise.id)
                .set(exerciseMap)
        } catch (e: Exception) {
            Log.e("ExerciseRepository", "Error updating exercise in FireStore", e)
        }
    }

    // Update an exercise in the local Room database
    suspend fun updateExerciseInLocalCache(exercise: Exercise) {
        exerciseDao.updateExercise(exercise)
    }

    suspend fun updateExercise(exercise: Exercise, userId: String, onCacheSuccess: () -> Unit,
                               onFailure: (Exception) -> Unit) {
        if (!canUpdateExercise()) {
            Toast.makeText(appContext, "Update limit reached for the day (15)", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            updateExerciseInLocalCache(exercise)
            onCacheSuccess()
            updateExerciseInFireStore(exercise, userId)
            incrementOperationCount(updateCountKey)
            Toast.makeText(appContext, "${exercise.title} updated successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ExerciseRepository", "Error updating exercise", e)
            onFailure(e)
        }
    }

    // Delete an exercise from a user's collection in FireStore
    private suspend fun deleteExerciseFromFireStore(userId: String, exercise: Exercise) {
        val exerciseRef = firestore.collection("users").document(userId).collection("exercises").document(exercise.id)
        try {
            val document = exerciseRef.get().await()
            if (document.exists()) {
                exerciseRef.delete().await()
                Log.d("ExerciseRepository", "Exercise deleted with ID: ${exercise.id}")
            } else {
                Log.e("ExerciseRepository", "Exercise not found in user's collection")
            }
        } catch (e: Exception) {
            Log.e("ExerciseRepository", "Error deleting exercise", e)
        }
    }

    // Update an exercise in the local Room database
    private suspend fun deleteExerciseFromLocalCache(exercise: Exercise) {
        Log.d("DeleteExerciseFromLocalCache", "Deleting exercise: $exercise")
        exerciseDao.deleteExercise(exercise)
    }

    suspend fun deleteExercise(userId: String, exercise: Exercise, onCacheSuccess: () -> Unit,
                               onFailure: (Exception) -> Unit) {
        if (!canDeleteExercise()) {
            Toast.makeText(appContext, "Delete limit reached for the day (15)", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            deleteExerciseFromLocalCache(exercise)
            onCacheSuccess()
            deleteExerciseFromFireStore(userId, exercise)
            incrementOperationCount(deleteCountKey)
            Toast.makeText(appContext, "${exercise.title} deleted successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ExerciseRepository", "Error deleting exercise", e)
            onFailure(e)
        }
    }

    private fun combineExercises(defaultExercises: List<Exercise>, userExercises: List<Exercise>): List<Exercise> {
        val allExercises = defaultExercises.toMutableList()
        allExercises.addAll(userExercises)
        return allExercises
    }

    suspend fun getAllExercises(userId: String): Result<List<Exercise>> {
        return if (isNetworkAvailable()) {
            try {
                val userExercisesCollection = firestore.collection("users").document(userId).collection("exercises")
                val userDocuments = userExercisesCollection.get().await()
                val userExercises = userDocuments.map { document ->
                    document.toObject(Exercise::class.java).copy(id = document.id)
                }

                val defaultDocuments = defaultExercisesCollection.get().await()
                val defaultExercises = defaultDocuments.map { document ->
                    document.toObject(Exercise::class.java).copy(id = document.id, private_ = false)
                }

                val typeConcat = listOf("Other", "Calisthenics", "Resistance Band")

                defaultExercises.forEach { exercise ->
                    if (exercise.type !in typeConcat) {
                        val newTitle = exercise.title+" (${exercise.type})"
                        exercise.title = newTitle
                    }
                }

                cachedExercises = combineExercises(defaultExercises, userExercises)

                withContext(Dispatchers.IO) {
                    exerciseDao.clearExercises()
                    exerciseDao.insertExercises(cachedExercises)
                }

                Log.d("ExerciseRepository", "Fetched exercises from FireStore: $cachedExercises")
                Result.success(cachedExercises)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val exerciseSharedPref = appContext.getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)
            with(exerciseSharedPref.edit()) {
                putBoolean("isFetched", false)
                apply()
            }
            ExerciseViewModel.isFetched = false
            Toast.makeText(appContext, "Come online to view exercises!", Toast.LENGTH_SHORT).show()
            Result.failure(Exception("Network unavailable"))
        }
    }

    suspend fun getAllExercisesFromCache(): List<Exercise> {
        cachedExercises = exerciseDao.getAllExercises()
        Log.d("ExerciseRepository", "Cached Exercises: $cachedExercises")
        return cachedExercises
    }

    suspend fun clearLocalCache() {
        exerciseDao.clearExercises()
        cachedExercises = emptyList()  // Clear cached exercises list in memory
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        Log.d("Exercise Repository", "Network Status: $networkCapabilities")
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun getExerciseById(exerciseId: String):Exercise? {
        return exerciseDao.getExerciseById(exerciseId)
    }
}
