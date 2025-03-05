/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.SecurePreferenceManager
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkout
import com.workoutwrecker.workouttracker.ui.data.record.PersonalRecord
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlin.math.ceil


@HiltViewModel
class SignInViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = FirebaseFirestore.getInstance()
    private val _signInState = MutableLiveData<SignInState>()
    val signInState: LiveData<SignInState> = _signInState

    fun signInUser(email: String, password: String) {
        Log.d("SignInViewModel", "Starting sign-in for email: $email")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""
                    _signInState.value = SignInState.Success(userId)
                } else {
                    _signInState.value = SignInState.Error(task.exception?.message ?: "Authentication failed")
                }
            }
    }

    sealed class SignInState {
        data class Success(val userId: String) : SignInState()
        data class Error(val message: String) : SignInState()
    }

    fun updateWorkoutExercise(workoutExercise: WorkoutExercise, category: String): WorkoutExercise {

        val filteredIndices = workoutExercise.sets.indices.filter { index ->
            workoutExercise.completion.getOrElse(index) { false } &&
                    workoutExercise.sets.getOrElse(index) { "" } != "W"
        }
        // Sets already calculated
        when (category){
            "Compound", "Isolation" -> {
                var totalRepsCount = 0
                var totalVolume = 0
                var bestMax = 0

                filteredIndices.forEach { index ->
                    val reps = workoutExercise.reps.getOrElse(index) { 0 }
                    val weight = workoutExercise.weights.getOrElse(index) { 0f }
                    val curMax = epleyFormula(weight, reps)
                    if (curMax>bestMax){
                        bestMax = curMax
                    }
                    totalRepsCount += reps
                    totalVolume += ceil(weight * reps).toInt()
                }

                return workoutExercise.copy(
                    max = bestMax,
                    completedsetscount = filteredIndices.size,
                    completedreps = totalRepsCount,
                    volume = totalVolume
                )
            }
            "Cardio" -> {
                var bestPace = 0f
                var totalDistance = 0f
                var totalTime = 0f

                filteredIndices.forEach { index ->
                    val distance = workoutExercise.distances.getOrElse(index) { 0f } * 1000
                    val time = workoutExercise.times.getOrElse(index) { 0f } * 3600
                    val curPace = pace("km/h", distance, time)
                    if (curPace>bestPace){
                        bestPace = curPace
                    }
                    totalDistance += distance
                    totalTime += time
                }

                return workoutExercise.copy(
                    bestPace = bestPace,
                    completedsetscount = filteredIndices.size,
                    totalDistance = totalDistance,
                    totalTime = totalTime
                )
            }
            "Duration" -> {
                return workoutExercise
            }
            else -> {
                return workoutExercise
            }
        }
    }

    private fun epleyFormula(weight: Float, reps: Int): Int {
        if (reps == 0) {
            return 0
        }
        if (reps == 1){
            return ceil(weight).toInt()
        }
        return ceil(weight * (1 + reps / 30f)).toInt()
    }

    private fun pace(mode: String, distance: Float, time: Float): Float {
        return if (mode == "km/h"){
            distance*1000/time*3600
        } else {
            0f
        }
    }

    suspend fun obtainUserData(currentTime: SignInFragment.WorldTime?,
                               userId:String):List<String?> {
        val userDocRef = firestore.collection("users").document(userId)
        val userSnapshot = userDocRef.get().await()
        if (userSnapshot.exists()) {
            val userData = userSnapshot.data
            val expiryTime = userData?.get("expiryTime") as? String
            val currentTimeString = currentTime?.datetime
            val username = userData?.get("username") as? String

            if (expiryTime != null && currentTimeString != null) {
                val currentDate = parseDate(currentTimeString)
                val expiryDate = parseDate(expiryTime)

                Log.d("SignInFragment", "Current Date: $currentDate")
                Log.d("SignInFragment", "Expiry Date: $expiryTime")

                val basePlanIdExists =
                    userData.containsKey("basePlanId") && userData["basePlanId"] != null

                if (currentDate != null && expiryDate != null
                    && basePlanIdExists && currentDate.before(expiryDate)) {
                    val basePlanId = userData["basePlanId"] as? String
                    val paymentState = userData["paymentState"] as? String
                    val purchaseTime = userData["purchaseTime"] as? String
                    val purchaseToken = userData["purchaseToken"] as? String

                    return listOf(expiryTime, basePlanId, purchaseToken,
                        paymentState, purchaseTime, username)
                }
            }
        }
        return emptyList()
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            // Adjust the pattern to match your date format
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX")
            formatter.parse(dateString)
        } catch (e: Exception) {
            Log.e("SignInFragment", "Date parsing error: ${e.message}")
            null
        }
    }

}
