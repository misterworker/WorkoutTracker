/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentExercisesBinding
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseDatabase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExercisesFragment : Fragment() {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private var _binding: FragmentExercisesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseAdapter
    private lateinit var repository: ExerciseRepository

    private val args: ExercisesFragmentArgs by navArgs()
    private var selectedTypes: List<String> = emptyList()
    private var selectedBodyParts: List<String> = emptyList()
    private val viewModel: ExerciseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExercisesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val exerciseDao = ExerciseDatabase.getDatabase(requireContext()).exerciseDao()
        repository = ExerciseRepository(exerciseDao, requireContext())

        val searchView: SearchView = requireActivity().findViewById(R.id.search_view)
        val filterIcon: ImageView = requireActivity().findViewById(R.id.filter_icon)

        filterIcon.setOnClickListener {
            val bundle = Bundle().apply {
                putStringArray("selectedTypes", selectedTypes.toTypedArray())
                putStringArray("selectedBodyParts", selectedBodyParts.toTypedArray())
            }
            try {
                findNavController().navigate(
                    R.id.action_navigation_exercise_to_navigation_filter_exercises,
                    bundle
                )
            }
            catch (e: Exception) {
                Log.e("ExercisesFragment", "Error navigating, filter button most likely "+
                        "clicked too fast after changing fragments")
            }
        }

        setupRecyclerView()
//        setupSwipeRefreshLayout()

        viewModel.exercises.observe(viewLifecycleOwner) { exercises ->
            adapter.updateExercises(exercises)
        }

        selectedTypes = args.selectedTypes?.toList() ?: emptyList()
        selectedBodyParts = args.selectedBodyParts?.toList() ?: emptyList()

        fetchExercises()
        // Handle search query changes
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addExerciseButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_exercises_to_navigation_create_exercise)
        }
    }

    private fun setupRecyclerView() {
        adapter = ExerciseAdapter(
            mutableListOf(),
            onExerciseClick = { exercise ->
                val action = ExercisesFragmentDirections.actionNavigationExercisesToNavigationExerciseInformation(exercise)
                findNavController().navigate(action)
            },
            onExerciseUpdate = { exercise ->
               showUpdateExerciseFragment(exercise)
            },
            onExerciseDelete = { exercise ->
                showDeleteConfirmationDialog(requireContext(), exercise)
            }
        )

        binding.recyclerViewExercises.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewExercises.adapter = adapter
    }

    private fun showUpdateExerciseFragment(exercise: Exercise) {
        val action = ExercisesFragmentDirections.actionNavigationExercisesToNavigationUpdateExercise(exercise.id)
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmationDialog(context: Context, exercise: Exercise) {
        AlertDialog.Builder(context)
            .setTitle(R.string.delete_exercise_confirmation_title)
            .setMessage("${getString(R.string.delete_exercise_confirmation_message)} ${exercise.title}?")
            .setPositiveButton("Yes") { dialog, _ ->
                viewModel.deleteExercise(userId, exercise, onSuccess = {
                    dialog.dismiss()
                    fetchExercises()
                }, onFailure = {
                    Toast.makeText(context, "Failed: cache delete exercise", Toast.LENGTH_SHORT).show()
                })
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun fetchExercises() {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { viewModel.fetchExercises(userId, selectedTypes, selectedBodyParts) },
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
