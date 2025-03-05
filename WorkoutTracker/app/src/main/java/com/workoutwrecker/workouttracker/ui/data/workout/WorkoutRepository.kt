/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.workout

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.workoutwrecker.workouttracker.ui.workout.WorkoutViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var cachedWorkouts: List<Workout> = emptyList()
    private var cachedWorkoutExercises: List<WorkoutExercise> = emptyList()

    // FireStore collections
    private val sampleWorkoutCollection = firestore.collection("workouts")
    private val usersCollection = firestore.collection("users")
    private val appContext = context.applicationContext
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val sharedPreferences = context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)

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

    private fun canUpdateWorkout(): Boolean {
        return canPerformUpdateOrDelete(updateCountKey)
    }

    private fun canDeleteWorkout(): Boolean {
        return canPerformUpdateOrDelete(deleteCountKey)
    }

    // Function to convert Workout to Map
    private fun Workout.toMap(): Map<String, Any> {
        return mapOf(
            "title" to title,
            "notes" to notes,
            "programName" to programName,
            "order" to order,
            "type" to type,
        )
    }

    // Function to convert WorkoutExercise to Map
    private fun WorkoutExercise.toMap(): Map<String, Any?> {
        return mapOf(
            "sets" to sets,
            "reps" to reps,
            "weights" to weights,
            "title" to title,
            "exerciseid" to exerciseid,
            "order" to order,
            "notes" to notes,
            "supersetid" to supersetid,
        )
    }

    // Add workout to FireStore
    private suspend fun addWorkoutToFireStore(userId: String, workout: Workout) {
        try {
            // Create a new document for the workout under the user's workouts collection
            val workoutRef = usersCollection.document(userId)
                .collection("workouts")
                .document(workout.id)

            // Convert workout object to map
            val workoutData = workout.toMap()

            // Set the workout document
            workoutRef.set(workoutData)

            for (exercise in workout.exercises) {
                workoutRef
                    .collection("exercises")
                    .document(exercise.id)
                    .set(exercise.toMap())
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error adding workout to FireStore", e)
            throw e // Re-throw exception to propagate failure
        }
    }

    private suspend fun addWorkoutToLocalCache(workout: Workout) {
        workoutDao.insertWorkout(listOf(workout))
    }

    // Add an exercise to a user's collection in FireStore
    suspend fun addWorkout(userId: String, workout: Workout, onCacheSuccess: () -> Unit,
                           onFailure: (Exception) -> Unit) {
        try {
            addWorkoutToLocalCache(workout) //Add to local cache first
            onCacheSuccess()
            addWorkoutToFireStore(userId, workout) //Queue firestore exercise
            Toast.makeText(appContext, "${workout.title} created successfully!", Toast.LENGTH_SHORT).show()
        }
        catch (e: Exception){
            Log.e("WorkoutRepository", "Error adding workout", e)
            onFailure(e)
        }
    }

    // Combine default workouts with user workouts
    private fun combineWorkouts(defaultWorkouts: List<Workout>, userWorkouts: List<Workout>): List<Workout> {
        val allExercises = defaultWorkouts.toMutableList()
        allExercises.addAll(userWorkouts)
        return allExercises
    }

    // Get all workouts either from FireStore or cache
    suspend fun getAllWorkouts(userId: String): Result<List<Workout>> {
        return if (isNetworkAvailable()) {
            try {
                val userWorkoutsCollection = usersCollection.document(userId)
                    .collection("workouts")
                val userDocuments = userWorkoutsCollection.get().await()
                val userWorkouts = mutableListOf<Workout>()

                for (document in userDocuments) {
                    val workout = document.toObject(Workout::class.java).copy(id = document.id)

                    // Fetch exercises for each workout
                    val exercisesSnapshot = userWorkoutsCollection
                        .document(workout.id)
                        .collection("exercises")
                        .get()
                        .await()

                    val exercises = exercisesSnapshot.map { exerciseDocument ->
                        exerciseDocument.toObject(WorkoutExercise::class.java).copy(id = exerciseDocument.id)
                    }

                    workout.exercises = exercises
                    userWorkouts.add(workout)
                }

                val defaultDocuments = sampleWorkoutCollection.get().await()
                val defaultWorkouts = mutableListOf<Workout>()

                for (document in defaultDocuments) {
                    val workout = document.toObject(Workout::class.java).copy(id = document.id)

                    // Fetch exercises for each default workout
                    val exercisesSnapshot = sampleWorkoutCollection
                        .document(workout.id)
                        .collection("exercises")
                        .get()
                        .await()

                    val exercises = exercisesSnapshot.map { exerciseDocument ->
                        exerciseDocument.toObject(WorkoutExercise::class.java).copy(id = exerciseDocument.id)
                    }

                    workout.exercises = exercises
                    defaultWorkouts.add(workout)
                }

                cachedWorkouts = combineWorkouts(defaultWorkouts, userWorkouts)

                withContext(Dispatchers.IO) {
                    workoutDao.clearWorkouts()
                    workoutDao.insertWorkout(cachedWorkouts)
                }

                Log.d("WorkoutRepository", "Fetched workouts from FireStore: $cachedWorkouts")
                Result.success(cachedWorkouts)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            val workoutSharedPref = appContext.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
            with(workoutSharedPref.edit()) {
                putBoolean("isFetched", false)
                apply()
            }
            WorkoutViewModel.isFetched = false
            Toast.makeText(appContext, "Come online to view workouts!", Toast.LENGTH_SHORT).show()
            Result.failure(Exception("Network unavailable"))
        }
    }


    // Get all workouts from local Room database cache
    suspend fun getAllWorkoutsFromCache(): List<Workout> {
        cachedWorkouts = workoutDao.getAllWorkouts()
        Log.d("WorkoutRepository", "Cached workouts: $cachedWorkouts")
        return cachedWorkouts
    }

    private suspend fun updateWorkoutInFireStore(userId: String, workout: Workout, oldExerciseIds: List<String>) {
        try {
            val workoutRef = usersCollection.document(userId)
                .collection("workouts")
                .document(workout.id)
            val workoutMap = workout.toMap().toMutableMap()
            workoutMap.remove("id")
            workoutRef.set(workoutMap)


            // Determine which exercises to delete
            val exercisesToDelete = oldExerciseIds.filter { exerciseId ->
                workout.exercises.none { it.id == exerciseId }
            }

            // Delete exercises that are not in the updated workout
            exercisesToDelete.forEach { exerciseId ->
                workoutRef
                    .collection("exercises")
                    .document(exerciseId)
                    .delete()

                Log.d("WorkoutRepository", "Exercise deleted from Firestore with ID: $exerciseId")
            }

            for (exercise in workout.exercises) {
                workoutRef
                    .collection("exercises")
                    .document(exercise.id)
                    .set(exercise.toMap())
            }
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error updating workout in FireStore", e)
        }
    }

    private suspend fun updateWorkoutInLocalCache(workout: Workout) {
        workoutDao.updateWorkout(workout)
    }

    suspend fun updateWorkout(userId: String, workout: Workout, oldExerciseIds: List<String>,
                              onCacheSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (!canUpdateWorkout()) {
            Toast.makeText(appContext, "Update limit reached for the day (15)", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            updateWorkoutInLocalCache(workout)
            onCacheSuccess()
            updateWorkoutInFireStore(userId, workout, oldExerciseIds)
            incrementOperationCount(updateCountKey)
            Toast.makeText(appContext, "${workout.title} updated successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error updating workout", e)
            onFailure(e)
        }
    }

    private suspend fun deleteWorkoutFromFireStore(userId: String, workout: Workout) {
        val workoutRef = usersCollection.document(userId)
            .collection("workouts")
            .document(workout.id)
        try {
            // Get all subcollections of the workout document
            val subCollections = listOf("exercises") // List all known subcollections
            subCollections.forEach { subCollectionName ->
                val subCollectionRef = workoutRef.collection(subCollectionName)
                // Delete all documents within each subcollection
                deleteCollection(subCollectionRef)
            }
            // Delete the workout document itself
            workoutRef.delete().await()
            Log.d("WorkoutRepository", "Workout deleted with ID: ${workout.id}")
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error deleting workout", e)
        }
    }

    suspend fun clearLocalCache() {
        workoutDao.clearWorkouts()
        cachedWorkouts = emptyList()  // Clear cached exercises list in memory
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
            Log.e("WorkoutRepository", "Error deleting subcollection", e)
        }
    }

    private suspend fun deleteWorkoutFromLocalCache(workout: Workout) {
        workoutDao.deleteWorkout(workout)
    }

    suspend fun deleteWorkout(userId: String, workout: Workout, onCacheSuccess: () -> Unit,
                              onFailure: (Exception) -> Unit) {
        if (!canDeleteWorkout()) {
            Toast.makeText(appContext, "Delete limit reached for the day (15)", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            deleteWorkoutFromLocalCache(workout)
            onCacheSuccess()
            deleteWorkoutFromFireStore(userId, workout)
            incrementOperationCount(deleteCountKey)
            Toast.makeText(appContext,  "${workout.title} deleted successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("WorkoutRepository", "Error deleting workout", e)
            onFailure(e)
        }
    }

    // Check if network is available
    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

