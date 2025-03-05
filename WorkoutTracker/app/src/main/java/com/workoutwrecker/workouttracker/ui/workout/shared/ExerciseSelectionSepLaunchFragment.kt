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
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentWorkoutExercisesBinding
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseDatabase
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.workoutwrecker.workouttracker.ui.exercises.ExerciseViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.UUID


@AndroidEntryPoint
class ExerciseSelectionSepLaunchFragment : Fragment() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private var _binding: FragmentWorkoutExercisesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseSelectionAdapter

    //    private val args: ExerciseSelectionFragmentArgs by navArgs()
    private var selectedExercises: List<Exercise> = emptyList()
    private var selectedTypes: List<String> = emptyList()
    private var selectedBodyParts: List<String> = emptyList()
    private val viewModel: ExerciseViewModel by viewModels()

    companion object {
        private const val ARG_SELECTED_EXERCISES = "selectedExercises"
        private const val ARG_SELECTED_TYPES = "selectedTypes"
        private const val ARG_SELECTED_BODY_PARTS = "selectedBodyParts"

        fun newInstance(
            selectedExercises: List<Exercise> = emptyList(),  // Default to empty list
            selectedTypes: List<String> = emptyList(),        // Default to empty list
            selectedBodyParts: List<String> = emptyList()     // Default to empty list
        ): ExerciseSelectionSepLaunchFragment {
            val fragment = ExerciseSelectionSepLaunchFragment()
            val args = Bundle().apply {
                putParcelableArrayList(ARG_SELECTED_EXERCISES, ArrayList(selectedExercises))
                putStringArray(ARG_SELECTED_TYPES, selectedTypes.toTypedArray())
                putStringArray(ARG_SELECTED_BODY_PARTS, selectedBodyParts.toTypedArray())
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register a callback for the back button press
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Custom back press handling
                    if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                        requireActivity().supportFragmentManager.popBackStack("EXERCISE_SELECTION_BACK_STACK",FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            selectedExercises = it.getParcelableArrayList(ARG_SELECTED_EXERCISES) ?: emptyList()
            selectedTypes = it.getStringArray(ARG_SELECTED_TYPES)?.toList() ?: emptyList()
            selectedBodyParts = it.getStringArray(ARG_SELECTED_BODY_PARTS)?.toList() ?: emptyList()
        }
        _binding = FragmentWorkoutExercisesBinding.inflate(inflater, container, false)


        val root: View = binding.root
        binding.fragmentToolbar.visibility = View.VISIBLE
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

        return root
    }

    private fun setupRecyclerView(selectedExercises: List<Exercise>) {
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
                    title = exercise.title
                )
            }

            Log.d("ExerciseSelectionFragment", "Selected exercises: $workoutExercises")

            val resultBundle = Bundle().apply {
                putParcelableArrayList("selectedExercises", ArrayList(workoutExercises))
            }

            requireActivity().supportFragmentManager.setFragmentResult("requestKey", resultBundle)

            requireActivity().supportFragmentManager.popBackStack("EXERCISE_SELECTION_BACK_STACK",
                FragmentManager.POP_BACK_STACK_INCLUSIVE)
            val fmManager = activity?.supportFragmentManager
            Log.e("Total Back stack Entry: ", fmManager?.backStackEntryCount.toString() + "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}