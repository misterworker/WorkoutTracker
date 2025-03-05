/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.shared

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentWorkoutExercisesBinding
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.workoutwrecker.workouttracker.ui.exercises.ExerciseViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.UUID


@AndroidEntryPoint
class ExerciseSelectionFragment : Fragment() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private var _binding: FragmentWorkoutExercisesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseSelectionAdapter

    private val args: ExerciseSelectionFragmentArgs by navArgs()
    private var selectedExercises: List<Exercise> = emptyList()
    private var selectedTypes: List<String> = emptyList()
    private var selectedBodyParts: List<String> = emptyList()
    private var origin: String = ""
    private val viewModel: ExerciseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        selectedTypes = args.selectedTypes?.toList() ?: emptyList()
        selectedBodyParts = args.selectedBodyParts?.toList() ?: emptyList()
        selectedExercises = args.selectedExercises?.toList() ?: emptyList()
        origin = args.origin.toString()
        _binding = FragmentWorkoutExercisesBinding.inflate(inflater, container, false)

        val root: View = binding.root

        setupRecyclerView(selectedExercises)

        viewModel.exercises.observe(viewLifecycleOwner) { exercises ->
            adapter.updateExercises(exercises)
        }

        fetchExercises()

        binding.filterIcon.setOnClickListener {
            val selectedExercises = adapter.getSelectedExercises()

            val selectedTypes = selectedTypes
            val selectedBodyParts = selectedBodyParts

            // Create fragment instance with arguments
            val fragment = FilterExerciseSelectionSepLaunchFragment.newInstance(
                selectedTypes = selectedTypes,
                selectedBodyParts = selectedBodyParts,
                selectedExercises = selectedExercises
            )

            // Perform fragment transaction to replace current fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // Replace with your container ID
                .addToBackStack("StartFilterSelection")
                .commit()
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.searchExercises(it)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    viewModel.searchExercises(it)
                }
                return false
            }
        })

        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = findNavController()
                if (!navController.popBackStack(R.id.navigation_create_workout, false)) {
                    navController.popBackStack(R.id.navigation_update_history_workout, false)
                    navController.getBackStackEntry(R.id.navigation_update_history_workout).savedStateHandle["selectedExercises"] =
                        emptyList<WorkoutExercise>()
                } else {
                    navController.getBackStackEntry(R.id.navigation_create_workout).savedStateHandle["selectedExercises"] =
                        emptyList<WorkoutExercise>()
                }
            }
        })

        return root
    }

    private fun setupRecyclerView(selectedExercises : List<Exercise>) {
        Log.d("ExerciseSelectionFragment", "Pre Selected exercises: $selectedExercises")
        adapter = ExerciseSelectionAdapter(
            items = mutableListOf(),
            onExerciseClick = { exercise ->
                // Handle exercise click
            },
            preSelectedExercises = selectedExercises
        )

        binding.recyclerViewWorkoutExercises.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewWorkoutExercises.adapter = adapter
    }

    private fun fetchExercises() {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { viewModel.fetchExercises(userId, selectedTypes, selectedBodyParts) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAddExerciseButton()
    }

    private fun setupAddExerciseButton() {
        binding.addExerciseButton.setOnClickListener {
            val selectedExercises = adapter.getSelectedExercises()

            val workoutExercises = selectedExercises.map { exercise ->
                WorkoutExercise(
                    id = "user_${UUID.randomUUID()}",  // Replace with your ID generation logic
                    exerciseid = exercise.id,
                    sets = arrayListOf(),  // Initialize with default or input values
                    reps = arrayListOf(),  // Initialize with default or input values
                    weights = arrayListOf(),  // Initialize with default or input values
                    title = exercise.title,
                )
            }

            Log.d("ExerciseSelectionFragment", "Selected exercises: $workoutExercises")
            val navController = findNavController()

            if (origin == "create") {
                navController.popBackStack(R.id.navigation_create_workout, false)
                navController.getBackStackEntry(R.id.navigation_create_workout)
                    .savedStateHandle["selectedExercises"] = workoutExercises
            }
            else if (origin == "start") {
                navController.popBackStack(R.id.navigation_update_history_workout, false)
                navController.getBackStackEntry(R.id.navigation_update_history_workout)
                    .savedStateHandle["selectedExercises"] = workoutExercises
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
