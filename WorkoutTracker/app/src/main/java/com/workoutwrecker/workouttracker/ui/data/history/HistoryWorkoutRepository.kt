/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.history

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.workoutwrecker.workouttracker.ui.history.HistoryWorkoutViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HistoryWorkoutRepository @Inject constructor(
    private val historyWorkoutDao: HistoryWorkoutDao,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var cachedHistoryWorkouts: List<HistoryWorkout> = emptyList()

    // Firestore collections
    private val usersCollection = firestore.collection("users")
    private val appContext = context.applicationContext

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val sharedPreferences = context.getSharedPreferences("history_workout_prefs", Context.MODE_PRIVATE)

    // Add keys for limits
    private val updateCountKey = "update_count"
    private val deleteCountKey = "delete_count"
    private val lastResetTimeKey = "last_reset_time"
    private val limitPerDay = 100

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

    private fun canUpdateHistoryWorkout(): Boolean {
        return canPerformUpdateOrDelete(updateCountKey)
    }

    private fun canDeleteHistoryWorkout(): Boolean {
        return canPerformUpdateOrDelete(deleteCountKey)
    }

    // Function to convert HistoryWorkout to Map
    private fun HistoryWorkout.toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "date" to date,
            "notes" to notes,
            "length" to length,
        )
    }

    // Function to convert WorkoutExerciseDetail to Map
    private fun WorkoutExercise.toMap(): Map<String, Any?> {
        return mapOf(
            "sets" to sets,
            "reps" to reps,
            "weights" to weights,
            "exerciseid" to exerciseid,
            "order" to order,
            "notes" to notes,
            "completion" to completion,
            "title" to title,
            "supersetid" to supersetid
        )
    }

    private suspend fun updateHistoryWorkoutInFireStore(
        userId: String, historyWorkout: HistoryWorkout, oldExerciseIds: List<String>) {
        try {
            val historyWorkoutRef = usersCollection.document(userId)
                .collection("history_workouts")
                .document(historyWorkout.id)
            val historyWorkoutMap = historyWorkout.toMap().toMutableMap()
            historyWorkoutMap.remove("id")
            historyWorkoutRef.set(historyWorkoutMap)

            // Determine which exercises to delete
            val exercisesToDelete = oldExerciseIds.filter { exerciseId ->
                historyWorkout.exercises.none { it.id == exerciseId }
            }

            // Delete exercises that are not in the updated workout
            exercisesToDelete.forEach { exerciseId ->
                historyWorkoutRef
                    .collection("exercises")
                    .document(exerciseId)
                    .delete()

                Log.d("HistoryWorkoutRepository", "Exercise deleted from Firestore with ID: $exerciseId")
            }

            for (exercise in historyWorkout.exercises) {
                historyWorkoutRef
                    .collection("exercises")
                    .document(exercise.id)
                    .set(exercise.toMap())
            }
        } catch (e: Exception) {
            Log.e("HistoryWorkoutRepository", "Error updating history workout in FireStore", e)
        }
    }

    suspend fun updateHistoryWorkoutInLocalCache(historyWorkout: HistoryWorkout) {
        historyWorkoutDao.updateHistoryWorkout(historyWorkout)
    }

    suspend fun updateHistoryWorkout(userId: String, historyWorkout: HistoryWorkout, oldExerciseIds: List<String>,
                                     onCacheSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (!canUpdateHistoryWorkout()) {
            Toast.makeText(appContext, "Update limit reached for the day (15)", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            updateHistoryWorkoutInLocalCache(historyWorkout)
            onCacheSuccess()
            updateHistoryWorkoutInFireStore(userId, historyWorkout, oldExerciseIds)
            incrementOperationCount(updateCountKey)
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error updating workout", e)
            onFailure(e)
        }
    }

    // Add workout to Firestore
    private suspend fun addHistoryWorkoutToFirestore(userId: String, historyWorkout: HistoryWorkout) {
        try {
            // Create a new document for the workout under the user's workouts collection
            val historyWorkoutRef = usersCollection.document(userId)
                .collection("history_workouts")
                .document(historyWorkout.id)

            // Convert HistoryWorkout object to map
            val workoutData = historyWorkout.toMap()

            // Set the workout document
            historyWorkoutRef.set(workoutData)

            for (exercise in historyWorkout.exercises) {
                historyWorkoutRef
                    .collection("exercises")
                    .document(exercise.id)
                    .set(exercise.toMap())
            }
        } catch (e: Exception) {
            Log.e("HistoryWorkoutRepository", "Error adding history workout to Firestore", e)
            throw e // Re-throw exception to propagate failure
        }
    }

    private suspend fun addHistoryWorkoutToLocalCache(historyWorkout: HistoryWorkout) {
        historyWorkoutDao.insertHistoryWorkouts(listOf(historyWorkout))
    }

    // Add an exercise to a user's collection in Firestore
    suspend fun addHistoryWorkout(userId: String, historyWorkout: HistoryWorkout,
                                  onCacheSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        try {
            addHistoryWorkoutToLocalCache(historyWorkout) // Add to local cache first
            onCacheSuccess()
            addHistoryWorkoutToFirestore(userId, historyWorkout) // Queue Firestore exercise
        } catch (e: Exception) {
            Log.e("HistoryWorkoutRepository", "Error adding history workout", e)
            onFailure(e)
        }
    }

    // Combine default workouts with user workouts
    private fun combineHistoryWorkouts(defaultWorkouts: List<HistoryWorkout>, userWorkouts: List<HistoryWorkout>): List<HistoryWorkout> {
        val allWorkouts = defaultWorkouts.toMutableList()
        allWorkouts.addAll(userWorkouts)
        return allWorkouts
    }

    // Get all workouts either from Firestore or cache
    suspend fun getAllHistoryWorkouts(userId: String): Result<List<HistoryWorkout>> {
        return if (isNetworkAvailable()) {
            try {
                val userWorkoutsCollection = usersCollection.document(userId).collection("history_workouts")
                val userDocuments = userWorkoutsCollection.get().await()
                val userWorkouts = mutableListOf<HistoryWorkout>()

                for (document in userDocuments) {
                    val workout = document.toObject(HistoryWorkout::class.java).copy(id = document.id)

                    // Fetch exercises for each workout
                    val exercisesSnapshot = userWorkoutsCollection.document(workout.id).collection("exercises").get().await()
                    val exercises = exercisesSnapshot.map { exerciseDocument ->
                        val exercise = exerciseDocument.toObject(WorkoutExercise::class.java).copy(id = exerciseDocument.id)
                        val completedSetsCount = exercise.sets.indices.count { index ->
                            exercise.completion.getOrElse(index) { false } && exercise.sets[index] != "W" // Count completed sets
                        }
                        exercise.copy(completedsetscount = completedSetsCount)
                    }

                    workout.exercises = exercises
                    userWorkouts.add(workout)
                }

                cachedHistoryWorkouts = userWorkouts

                // Ensure database operations are properly awaited
                withContext(Dispatchers.IO) {
                    historyWorkoutDao.clearHistoryWorkouts()
                    historyWorkoutDao.insertHistoryWorkouts(cachedHistoryWorkouts)
                }

                Log.d("HistoryWorkoutRepository", "Fetched history workouts from Firestore: $cachedHistoryWorkouts")
                Result.success(cachedHistoryWorkouts)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val historyWorkoutSharedPref = appContext.getSharedPreferences(
                "history_workout_prefs", Context.MODE_PRIVATE)
            with(historyWorkoutSharedPref.edit()) {
                putBoolean("isFetched", false)
                apply()
            }
            HistoryWorkoutViewModel.isFetched = false
            Toast.makeText(appContext, "Come online to view workouts!", Toast.LENGTH_SHORT).show()
            Result.failure(Exception("Network unavailable"))
        }
    }


    // Get all workouts from local Room database cache
    suspend fun getAllHistoryWorkoutsFromCache(): List<HistoryWorkout> {
        cachedHistoryWorkouts = historyWorkoutDao.getAllHistoryWorkouts()
        Log.d("HistoryWorkoutRepository", "Cached workouts: $cachedHistoryWorkouts")
        return cachedHistoryWorkouts
    }

    private suspend fun deleteHistoryWorkoutFromFireStore(userId: String, historyWorkout: HistoryWorkout) {
        val historyWorkoutRef = usersCollection.document(userId)
            .collection("history_workouts")
            .document(historyWorkout.id)
        try {
            val subCollections = listOf("exercises")
            subCollections.forEach { subCollectionName ->
                val subCollectionRef = historyWorkoutRef.collection(subCollectionName)
                deleteCollection(subCollectionRef)
            }
            historyWorkoutRef.delete().await()
            Log.d("HistoryWorkoutRepository", "History Workout deleted with ID: ${historyWorkout.id}")
        } catch (e: Exception) {
            Log.e("HistoryWorkoutRepository", "Error deleting history workout", e)
        }
    }

    private suspend fun deleteCollection(collection: CollectionReference) {
        try {
            // Get all documents in the collection
            val documents = collection.get().await()
            // Delete each document in the collection
            for (document in documents.documents) {
                collection.document(document.id).delete().await()
            }
        } catch (e: Exception) {
            Log.e("HistoryWorkoutRepository", "Error deleting subcollection", e)
        }
    }

    private suspend fun deleteHistoryWorkoutFromLocalCache(historyWorkout: HistoryWorkout) {
        historyWorkoutDao.deleteHistoryWorkout(historyWorkout)
    }

    suspend fun deleteWorkout(userId: String, historyWorkout: HistoryWorkout,
                              onCacheSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (!canDeleteHistoryWorkout()) {
            Toast.makeText(appContext, "Delete limit reached for the day (15)", Toast.LENGTH_SHORT).show()
        }
        try {
            deleteHistoryWorkoutFromLocalCache(historyWorkout)
            onCacheSuccess()
            deleteHistoryWorkoutFromFireStore(userId, historyWorkout)
            incrementOperationCount(deleteCountKey)
        } catch (e: Exception) {
            Log.e("HistoryWorkoutRepository", "Error deleting history workout", e)
            onFailure(e)
        }
    }

    suspend fun clearLocalCache() {
        historyWorkoutDao.clearHistoryWorkouts()
        cachedHistoryWorkouts = emptyList()  // Clear cached exercises list in memory
    }

    // Check if network is available
    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun getHistoryWorkoutById(historyWorkoutId: String):HistoryWorkout? {
        return historyWorkoutDao.getHistoryWorkoutById(historyWorkoutId)
    }
}
