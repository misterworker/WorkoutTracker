/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentExerciseRecordsBinding
import com.workoutwrecker.workouttracker.ui.history.HistoryWorkoutViewModel
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.ceil

class ExerciseRecordsFragment : Fragment() {
    private var _binding: FragmentExerciseRecordsBinding? = null
    private val binding get() = _binding!!
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()

    private val historyWorkoutViewModel: HistoryWorkoutViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseRecordsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val weightSystem: String = sharedPreferences.getString("weight_system_preference", "kg") ?: "kg"

        val exercise = try {
            (parentFragment as ExerciseInformationFragment).exerciseInfoArgs.exercise
        } catch (e: ClassCastException) {
            try {
                (parentFragment as ExerciseInformationSepFragment).exerciseInfoArgs.exercise
            } catch (e: ClassCastException) {
                null // Handle the case when both casts fail
            }
        }

        if (exercise != null) {
            val exerciseId = exercise.id
            // Fetch history workouts data
            viewLifecycleOwner.lifecycleScope.launch {
                historyWorkoutViewModel.fetchHistoryWorkouts(userId)
                val historyWorkouts = historyWorkoutViewModel.historyWorkouts.value ?: emptyList()
                val allWorkoutExercises = historyWorkouts
                    .flatMap { workout ->
                        workout.exercises.map { workoutExercise ->
                            workoutExercise.copy(date = workout.date)
                        }
                    }
                    .filter { it.exerciseid == exerciseId } // Filter by the exercise ID


                val reps = arrayListOf<Int>()
                val weights = arrayListOf<Float>()
                val maxes = arrayListOf<Int>()
                val dates = arrayListOf<String>()
                var totalVol = 0
                var totalSets = 0
                var totalReps = 0
                for (workoutExercise in allWorkoutExercises) {
                    totalVol += workoutExercise.volume
                    totalSets += workoutExercise.completedsetscount
                    totalReps += workoutExercise.completedreps
                }
                val maxSortedWorkoutExercises = allWorkoutExercises
                    .sortedByDescending { it.max }.take(15)

                for (workoutExercise in maxSortedWorkoutExercises) {
                    val max = workoutExercise.max
                    val filteredIndices = workoutExercise.sets.indices.filter { index ->
                        workoutExercise.completion.getOrElse(index) { false } &&
                                workoutExercise.sets.getOrElse(index) { "" } != "W"
                    }
                    filteredIndices.forEach { index ->
                        val rep = workoutExercise.reps[index]
                        val weight = workoutExercise.weights[index]
                        val curMax = epleyFormula(weight, rep)
                        if (curMax == max) {
                            reps.add(rep)
                            weights.add(weight)
                            maxes.add(curMax)
                            dates.add(Date(workoutExercise.date).toString())
                            return@forEach
                        }
                    }
                }

                val indices = maxes.indices.toList()

                val sortedIndices = indices.sortedWith(
                    compareByDescending<Int> { maxes[it] }
                        .thenByDescending { reps[it] }
                )
                val sortedMaxes = sortedIndices.map { maxes[it] }
                val sortedReps = sortedIndices.map { reps[it] }
                val sortedWeights = sortedIndices.map {weights[it]}


                val (displayWeight, unit) = when (weightSystem) {
                    "lbs" -> Pair(convertKgToLbs(totalVol), "lbs")
                    "stones" -> Pair(convertKgToStones(totalVol), "st")
                    else -> Pair(totalVol, "kg")
                }

                // Update UI elements
                binding.totalVol.text =
                    getString(R.string.exercise_records_total_vol, "${displayWeight}$unit")
                binding.totalReps.text =
                    getString(R.string.exercise_records_reps_performed, totalReps)
                binding.totalSets.text =
                    getString(R.string.exercise_records_sets_performed, totalSets)

                val convertedWeights = sortedWeights.map { weight ->
                    when (weightSystem) {
                        "lbs" -> convertKgToLbs(ceil(weight).toInt())
                        "stones" -> convertKgToStones(ceil(weight).toInt())
                        else -> ceil(weight).toInt() // Default to kg, no conversion needed
                    }
                }

                val convertedMaxes = sortedMaxes.map { max ->
                    when (weightSystem) {
                        "lbs" -> convertKgToLbs(max) // Need to convert Int to Float for the conversion
                        "stones" -> convertKgToStones(max)
                        else -> max
                    }
                }

                // Initialize RecyclerView with the adapter
                exercise.let {
                    val adapter = ExerciseRecordsAdapter(
                        sortedReps, convertedWeights,
                        convertedMaxes, dates, weightSystem
                    )
                    binding.recordsRecyclerview.layoutManager =
                        LinearLayoutManager(requireContext())
                    binding.recordsRecyclerview.adapter = adapter
                }
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

    private fun convertKgToLbs(weight: Int): Int {
        return ceil(weight * 2.20462f).toInt()
    }

    private fun convertKgToStones(weight: Int): Int {
        return ceil(weight * 0.157473f).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
