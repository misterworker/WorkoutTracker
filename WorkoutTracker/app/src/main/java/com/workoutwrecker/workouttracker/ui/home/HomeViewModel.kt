/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkoutRepository
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.workoutwrecker.workouttracker.ui.data.record.PersonalRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val personalRecordRepository: PersonalRecordRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val historyWorkoutRepository: HistoryWorkoutRepository) : ViewModel() {

    private val storageReference = Firebase.storage.reference
    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
//    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> get() = _profileImageUrl
    private val _logoutStatus = MutableLiveData<Boolean>()
    val logoutStatus: LiveData<Boolean> get() = _logoutStatus

    private fun clearLocalCache() {
        viewModelScope.launch {
            exerciseRepository.clearLocalCache()
        }
        viewModelScope.launch {
            workoutRepository.clearLocalCache()
        }
        viewModelScope.launch {
            historyWorkoutRepository.clearLocalCache()
        }
        viewModelScope.launch {
            personalRecordRepository.clearLocalCache()
        }
    }

    fun pickImageFromGallery(pickImageLauncher: (String) -> Unit, context: Context) {
        if (!isNetworkAvailable(context)) {
            Toast.makeText(context, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
            return
        }
        pickImageLauncher.invoke("image/*")
    }

    fun uploadImageToFirebase(context: Context, uri: Uri) {
        val sharedPref = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        if (!isNetworkAvailable(context)) {
            Toast.makeText(context, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val fileExtension = getFileExtension(context, uri)
        val profileImageRef = storageReference.child("profile_images/$userId.$fileExtension")

        profileImageRef.putFile(uri)
            .addOnSuccessListener {
                profileImageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    updateProfileImageUrl(downloadUrl.toString())
                    with (sharedPref.edit()) {
                        putString("profileImageUrl", downloadUrl.toString())
                        apply()
                    }
                }
                loadProfileImage(context)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to upload image: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileExtension(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeTypeMap = android.webkit.MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ?: "jpg"
    }

    private fun updateProfileImageUrl(url: String) {
        val user = auth.currentUser
        val profileUpdates = userProfileChangeRequest {
            photoUri = Uri.parse(url)
        }

        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveProfileImageUrlToFirestore(url)
            } else {
                // Handle error
            }
        }
    }

    private fun saveProfileImageUrlToFirestore(url: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.set(mapOf("profileImageUrl" to url), SetOptions.merge())
            .addOnSuccessListener {
                _profileImageUrl.postValue(url)
            }
            .addOnFailureListener {
                // Handle error
            }
    }

    fun loadProfileImage(context: Context) {
        val sharedPref = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val cachedUrl = sharedPref.getString("profileImageUrl", null)
        val userId = auth.currentUser?.uid ?: return
        if (cachedUrl != null) {
            _profileImageUrl.postValue(cachedUrl)
        } else {
            fetchProfileImage(context, userId)
        }
    }

    fun fetchProfileImage(context: Context, userId: String) {
        val sharedPref = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val userDocRef = db.collection("users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                val photoUrl = document.getString("profileImageUrl")
                _profileImageUrl.postValue(photoUrl)
                with (sharedPref.edit()) {
                    putString("profileImageUrl", photoUrl)
                    apply()
                }
            }
            .addOnFailureListener {
                // Handle error
            }
    }

    fun logout(context: Context) {
        val exerciseSharedPref =
            context.getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)
        val workoutSharedPref =
            context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
        val historyWorkoutSharedPref =
            context.getSharedPreferences("history_workout_prefs", Context.MODE_PRIVATE)
        with(exerciseSharedPref.edit()) {
            putBoolean("isFetched", false)
            apply()
        }
        with(workoutSharedPref.edit()) {
            putBoolean("isFetched", false)
            apply()
        }
        with(historyWorkoutSharedPref.edit()) {
            putBoolean("isFetched", false)
            apply()
        }
        // Clear cached data on logout
        clearLocalCache()


        val sharedPref = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        with(sharedPref.edit()) {
            remove("profileImageUrl")
            apply()
        }

        Firebase.auth.signOut()
        _logoutStatus.postValue(true)  // Notify that logout is complete
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
