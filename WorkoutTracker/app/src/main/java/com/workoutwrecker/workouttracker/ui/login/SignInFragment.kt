/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.login

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.workoutwrecker.workouttracker.SecurePreferenceManager
import com.workoutwrecker.workouttracker.databinding.FragmentSignInBinding
import com.workoutwrecker.workouttracker.ui.exercises.ExerciseViewModel
import com.workoutwrecker.workouttracker.ui.history.HistoryWorkoutViewModel
import com.workoutwrecker.workouttracker.ui.home.HomeViewModel
import com.workoutwrecker.workouttracker.ui.record.PersonalRecordViewModel
import com.workoutwrecker.workouttracker.ui.workout.WorkoutViewModel
import com.workoutwrecker.workouttracker.ui.workout.start.WorkoutStartViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private val workoutStartViewModel: WorkoutStartViewModel by viewModels()
    private val exercisesViewModel: ExerciseViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()
    private val historyWorkoutViewModel: HistoryWorkoutViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val personalRecordViewModel: PersonalRecordViewModel by viewModels()
    private val signInViewModel: SignInViewModel by viewModels()
    private val auth = Firebase.auth

    interface WorldTimeApi {
        @GET("/api/timezone/Etc/UTC") // Use the correct endpoint
        suspend fun getCurrentTime(): WorldTime
    }

    data class WorldTime(
        val abbreviation: String,
        val client_ip: String,
        val datetime: String,
        val day_of_week: Int,
        val day_of_year: Int,
        val dst: Boolean,
        val dst_from: Any?,
        val dst_offset: Int,
        val dst_until: Any?,
        val raw_offset: Int,
        val timezone: String,
        val unixtime: Long,
        val utc_datetime: String,
        val utc_offset: String,
        val week_number: Int
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var sendingEmail = false

        val securePrefManager = SecurePreferenceManager(requireContext())
        securePrefManager.clearData()

        fun showPopup(show:Boolean){
            if (show) {
                binding.forgotPasswordPopup.visibility = View.VISIBLE
                binding.dimView.visibility = View.VISIBLE
            }
            else {
                binding.forgotPasswordPopup.visibility = View.GONE
                binding.dimView.visibility = View.GONE
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}

        sharedPreferences = requireContext().getSharedPreferences("register_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("sign_out_flag", true)
            putString("register_email", "NA")
            putString("register_password", "NA")
            putString("register_page", "NA")
            apply()
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (!binding.loginButton.isEnabled) return@setOnClickListener
                showLoading(true)
                signInViewModel.signInUser(email, password)
            } else {
                Toast.makeText(context, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToRegisterButton.setOnClickListener {
            if (!binding.loginButton.isEnabled) return@setOnClickListener
            val action = SignInFragmentDirections.actionNavigationSignInToNavigationRegister()
            findNavController().navigate(action)
        }

        binding.resetPasswordPopupButton.setOnClickListener {
            showPopup(true)
            binding.forgotPasswordEmailInput.setText(binding.emailInput.text.toString().trim())
        }

        binding.resetPasswordButton.setOnClickListener {
            val email = binding.forgotPasswordEmailInput.text.toString().trim()
            if (!sendingEmail && (email.isNotEmpty())) {
                sendingEmail = true

                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        showPopup(false)
                        sendingEmail = false
                    } else {
                        Toast.makeText(context, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        sendingEmail = false
                    }
                }
            }
            else if (email.isEmpty()){
                Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }

        binding.closeForgotPasswordPopupButton.setOnClickListener{
            showPopup(false)
        }

        signInViewModel.signInState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SignInViewModel.SignInState.Success -> {
                    val retrofit = Retrofit.Builder()
                        .baseUrl("https://worldtimeapi.org")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val worldTimeApi = retrofit.create(WorldTimeApi::class.java)

                    val currentTime = runBlocking {
                        try {
                            worldTimeApi.getCurrentTime()
                        } catch (e: Exception) {
                            Log.e("SignInFragment", "Error getting current time", e)
                            null
                        }
                    }

                    val exerciseSharedPref = requireActivity().getSharedPreferences("exercise_prefs", Context.MODE_PRIVATE)
                    val workoutSharedPref = requireActivity().getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
                    val historyWorkoutSharedPref = requireActivity().getSharedPreferences("history_workout_prefs", Context.MODE_PRIVATE)


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
                    sharedPreferences.edit().putBoolean("sign_out_flag", false).apply()
                    val userId = state.userId
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val userData = signInViewModel.obtainUserData(currentTime, userId)
                            if (userData.isEmpty()){securePrefManager.clearData()}
                            else {
                                userData[0]?.let { securePrefManager.storeString("expiryTime", it) }
                                userData[1]?.let { securePrefManager.storeString("basePlanId", it) }
                                userData[2]?.let {
                                    securePrefManager.storeString(
                                        "purchaseToken",
                                        it
                                    )
                                }
                                userData[3]?.let { securePrefManager.storeString(
                                        "paymentState",
                                        it
                                    )
                                }
                                userData[4]?.let {
                                    securePrefManager.storeString(
                                        "purchaseTime",
                                        it
                                    )
                                }
                                userData[5]?.let { securePrefManager.storeString("username", it) }
                            }
                            // Launch all fetch operations and wait for all of them to complete
                            awaitAll(
                                async { exercisesViewModel.fetchExercises(userId, emptyList(),
                                    emptyList()) },
                                async { workoutViewModel.fetchWorkouts(userId) },
                                async { homeViewModel.fetchProfileImage(requireContext(), userId) },
                                async { historyWorkoutViewModel.fetchHistoryWorkouts(userId) },
                            )

                            val historyWorkouts = historyWorkoutViewModel.historyWorkouts.value
                                ?.sortedBy{it.date}?: emptyList()
                            personalRecordViewModel.calculateAllRecords(historyWorkouts)
                            for (historyWorkout in historyWorkouts) {
                                // Update workout exercise maxes
                                val exercises = historyWorkout.exercises.map { workoutExercise ->
                                    val exercise = workoutStartViewModel.getExerciseById(workoutExercise.exerciseid)

                                    if (exercise != null) {
                                        signInViewModel.updateWorkoutExercise(workoutExercise, exercise.category)
                                    }
                                    else {
                                        workoutExercise
                                    }
                                }
                                val updatedHistoryWorkout = historyWorkout.copy(
                                    exercises = exercises
                                )
                                historyWorkoutViewModel.updateHistoryWorkout(updatedHistoryWorkout)
                            }

                            showLoading(false)
                            val action = SignInFragmentDirections.actionNavigationSignInToNavigationHome()
                            findNavController().navigate(action)
                        } catch (e: Exception) {
                            // Handle any errors during the fetch operations
                            Toast.makeText(context, "Error fetching data: ${e.message}",
                                Toast.LENGTH_SHORT).show()
                            Log.e("SignInFragment", "Error fetching data: ${e.message}", e)
                            showLoading(false)
                        }
                    }
                }
                is SignInViewModel.SignInState.Error -> {
                    Toast.makeText(context, "Authentication failed: ${state.message}",
                        Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
                else -> {
                    showLoading(false)
                    // Handle any other states or do nothing
                }
            }
        }

    }

    private fun showLoading(isLoading: Boolean) {
        binding.dimView.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !isLoading
        binding.goToRegisterButton.isEnabled = !isLoading
        binding.resetPasswordPopupButton.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
