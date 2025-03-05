/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class WorkoutViewFragment : Fragment() {
    private val args: WorkoutViewFragmentArgs by navArgs()
    private var selectedExercises: MutableList<WorkoutExercise> = mutableListOf()
    private var workoutNameInput: EditText? = null
    private var workoutNotesInput: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout_view, container, false)
        workoutNameInput = view.findViewById(R.id.workout_name_input)
        workoutNotesInput = view.findViewById(R.id.workout_notes_input)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = args.workoutTitle
        val notes = args.workoutNotes

        workoutNameInput?.setText(title)
        workoutNotesInput?.setText(notes)
        workoutNameInput?.isEnabled = false
        workoutNotesInput?.isEnabled = false

        args.selectedExercises?.let { addExercises(it.toList()) }

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_workout_exercises)
        val adapter = ViewWorkoutExerciseAdapter(
            selectedExercises
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    private fun addExercises(exercises: List<WorkoutExercise>) {
        selectedExercises.addAll(exercises)
        updateExerciseOrder()
    }

    private fun updateExerciseOrder() {
        selectedExercises.forEachIndexed { index, exercise ->
            exercise.order = index + 1
            Log.d("updateExerciseOrder", "$exercise")
        }
    }
}
