/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentCurrentWorkoutBinding
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.workoutwrecker.workouttracker.ui.workout.start.UpdateHistoryWorkoutFragmentArgs
import com.workoutwrecker.workouttracker.ui.workout.start.WorkoutStartSepFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CurrentWorkoutFragment : Fragment() {
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    private val workoutViewModel: WorkoutViewModel by viewModels()

    // Declare binding object
    private var _binding: FragmentCurrentWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: WorkoutSelectionAdapter
    private lateinit var programAdapter: ProgramPagerAdapter
    private var selectedWorkouts: List<Workout> = emptyList()
    private var isPopupVisible: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        _binding = FragmentCurrentWorkoutBinding.inflate(inflater, container, false)
        val view = binding.root

        // Access views using binding object
        val viewPager = binding.viewPagerPrograms
        val programName = binding.programName
        val doneProgram = binding.doneProgram

        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { workoutViewModel.fetchWorkouts(userId) },
            )
        }

        setupRecyclerView(selectedWorkouts)

        workoutViewModel.nonDefaultWorkouts.observe(viewLifecycleOwner) { workouts ->
            adapter.updateWorkouts(workouts)
        }

        workoutViewModel.programs.observe(viewLifecycleOwner) {
            Log.d("CurrentWorkoutFragment", "Programs: $it")
            programAdapter = ProgramPagerAdapter(this, it)
            viewPager.adapter = programAdapter

            updateArrows(viewPager.currentItem, it.size)

            // Handle the left arrow click
            binding.prevProgram.setOnClickListener {
                if (viewPager.currentItem > 0) {
                    viewPager.currentItem -= 1
                }
            }

            // Handle the right arrow click
            binding.nextProgram.setOnClickListener {
                if (viewPager.currentItem < (viewPager.adapter?.itemCount?.minus(1) ?: 0)) {
                    viewPager.currentItem += 1
                }
            }

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateArrows(position, it.size)
                    programName.text = it[position]
                }
            })
        }

        programName.setOnClickListener {
            showPopup(isPopupVisible)
        }

        doneProgram.setOnClickListener {
            val program = binding.newProgramName.text.toString()
            showPopup(isPopupVisible)
            val selectedWorkouts = adapter.getSelectedWorkouts()

            if (selectedWorkouts.isEmpty()){
                return@setOnClickListener
            }
            adapter.clearWorkouts()
            workoutViewModel.updateWorkoutProgram(userId, program, selectedWorkouts, onSuccess = {
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.awaitAll(
                        async { workoutViewModel.fetchWorkouts(userId) },
                    )
                }
            }, onFailure = {
                Toast.makeText(requireContext(), "Failed to update program", Toast.LENGTH_SHORT).show()
            })
        }

        // Add workout button logic
        binding.addWorkoutButton.setOnClickListener {
            val snackbar = Snackbar.make(
                binding.currentWorkoutRoot,
                "In Development",
                Snackbar.LENGTH_SHORT
            )
            snackbar.setAction("Learn More") {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://dictionary.cambridge.org/dictionary/english/development")
                )
                startActivity(browserIntent)
            }
            snackbar.show()
        }

        // Add custom workout button logic
        binding.addCustomWorkout.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_workout_to_navigation_create_workout)
        }

        return view
    }

    private fun updateArrows(currentPosition: Int, itemCount: Int) {
        binding.prevProgram.visibility = if (currentPosition > 0) View.VISIBLE else View.INVISIBLE
        binding.nextProgram.visibility = if (currentPosition < itemCount - 1) View.VISIBLE else View.INVISIBLE
    }

    private fun setupRecyclerView(selectedExercises: List<Workout>) {
        Log.d("ExerciseSelectionFragment", "Pre Selected exercises: $selectedExercises")
        adapter = WorkoutSelectionAdapter(
            items = mutableListOf(),
            onWorkoutClick = { workout ->
                // Handle exercise click
            },
            preSelectedWorkouts = selectedWorkouts
        )

        binding.programRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.programRecyclerView.adapter = adapter
    }

    private fun showPopup(isVisible:Boolean){
        if (isVisible){
            binding.workoutProgramPopup.visibility = View.VISIBLE
            isPopupVisible = false
        }
        else {
            binding.workoutProgramPopup.visibility = View.GONE
            isPopupVisible = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference
    }
}

@AndroidEntryPoint
class WorkoutsFragment : Fragment() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    private lateinit var workoutAdapter: WorkoutAdapter
    private val workoutViewModel: WorkoutViewModel by viewModels()
    private lateinit var workoutType: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        arguments?.let {
            workoutType = it.getString("curProgram") ?: ""
        }
        Log.d("workoutType", workoutType)
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_workouts, container, false)

        // Set up workouts RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.workout_recycler_view)
        workoutAdapter = WorkoutAdapter(
            onStartWorkoutClick = { workout ->
                startCurrentWorkout(
                    workoutTitle = workout.title,
                    workoutId = if (workout.id.startsWith("user")) workout.id else null,
                    workoutNotes = workout.notes,
                    workoutDate = System.currentTimeMillis(),
                    selectedExercises = workout.exercises,
                    checkboxEnabled = true
                )
            },
            onWorkoutUpdate = { workout ->
                val action = CurrentWorkoutFragmentDirections.actionNavigationWorkoutToNavigationCreateWorkout(
                    selectedExercises = workout.exercises.toTypedArray(), workoutNotes = workout.notes,
                    workoutTitle = workout.title, workoutId = workout.id
                )
                findNavController().navigate(action)
            },
            onWorkoutDelete = { workout ->
                confirmDelete(workout)
            },
            onWorkoutShare = { workout ->
                shareWorkout(workout)
            }
        )

        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { workoutViewModel.fetchFilteredWorkouts(userId, workoutType) },
            )
        }

        recyclerView.adapter = workoutAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Observe ViewModel
        workoutViewModel.workouts.observe(viewLifecycleOwner) {
            workoutAdapter.setWorkouts(it)
        }
        return view
    }

    fun startCurrentWorkout(
        workoutTitle: String,
        workoutId: String?,
        workoutNotes: String?,
        workoutDate: Long,
        selectedExercises: List<WorkoutExercise>,
        checkboxEnabled: Boolean
    ) {
        // Create the WorkoutStartFragmentArgs
        val args = UpdateHistoryWorkoutFragmentArgs(
            workoutTitle = workoutTitle,
            workoutId = workoutId,
            workoutNotes = workoutNotes,
            workoutDate = workoutDate,
            selectedExercises = selectedExercises.toTypedArray(),
            checkboxEnabled = checkboxEnabled,
            historyWorkoutId = null.toString()
        )

        // Convert args to a Bundle
        val bundle = args.toBundle()

        // Create the fragment and set the arguments
        val fragment = WorkoutStartSepFragment().apply {
            arguments = bundle
        }

        // Replace the current fragment with WorkoutStartFragment
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("MAIN_FRAGMENT_TAG")
            .commit()

    }

    private fun confirmDelete(workout: Workout) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_workout_title)
            .setMessage("${getString(R.string.delete_workout_confirmation_message)} ${workout.title}?")
            .setPositiveButton(R.string.yes) { dialog, _ ->
                workoutViewModel.deleteWorkout(userId, workout, onSuccess = {
                    dialog.dismiss()
                    viewLifecycleOwner.lifecycleScope.launch {
                        kotlinx.coroutines.awaitAll(
                            async { workoutViewModel.fetchWorkouts(userId) },
                        )
                    }
                }, onFailure = {
                    Toast.makeText(requireContext(), "Failed: cache delete workout", Toast.LENGTH_SHORT).show()
                })
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    // Workout.kt
    private fun Workout.toJson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

    private fun shareWorkout(workout: Workout) {
        val workoutJson = workout.toJson() // Create this method to convert workout to JSON
        val shareUri = "https://www.example.com/workout?data=$workoutJson"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_workout_text, shareUri))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_workout)))
    }
}



