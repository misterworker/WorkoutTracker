/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.create

import android.app.AlertDialog
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.workoutwrecker.workouttracker.ui.workout.start.WorkoutExerciseAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class CreateWorkoutFragment : Fragment() {
    private val args: CreateWorkoutFragmentArgs by navArgs()
    private var selectedExercises: MutableList<WorkoutExercise> = mutableListOf()
    private val auth = Firebase.auth

    private val viewModel: CreateWorkoutViewModel by viewModels()
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var workoutNameInput: EditText? = null
    private var workoutNotesInput: EditText? = null
    private var addExerciseButton: FloatingActionButton? = null
    private var saveWorkoutButton: ImageButton? = null
    private var loadingProgress: ProgressBar? = null
    private var cancelWorkoutButton: Button? = null

    private var returningFromExerciseSelection = false

    private var hasAddedExercises = false  // Flag to track if exercises have been added


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_workout, container, false)
        workoutNameInput = view.findViewById(R.id.workout_name_input)
        workoutNotesInput = view.findViewById(R.id.workout_notes_input)
        addExerciseButton = view.findViewById(R.id.add_exercise_button)
        saveWorkoutButton = view.findViewById(R.id.save_workout_button)
        cancelWorkoutButton = view.findViewById(R.id.cancel_workout_button)
        loadingProgress = view.findViewById(R.id.loading_progress)

        // Restore the flag from savedInstanceState if it exists
        savedInstanceState?.let {
            hasAddedExercises = it.getBoolean("hasAddedExercises", false)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_workout_exercises)
        val adapter = CreateWorkoutExerciseAdapter(
            selectedExercises,
            onExerciseClick = { workoutExercise ->
                Log.d("CreateWorkoutFragment", "selected Exercises adapter: $selectedExercises")
                lifecycleScope.launch {
                    val exercise = viewModel.getExerciseById(workoutExercise.exerciseid)
                    if (exercise != null){
                        val action = CreateWorkoutFragmentDirections
                            .actionNavigationCreateWorkoutToNavigationExerciseInformation(exercise)
                        findNavController().navigate(action)
                        }
                    else {
                        Toast.makeText(requireContext(), "Exercise not found", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onExerciseDelete = { workoutExercise ->
                confirmDeleteExercise(workoutExercise)
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            },
            onMoveSelected = { workoutExercise ->
                val action = CreateWorkoutFragmentDirections.actionNavigationCreateWorkoutToNavigationMoveExercise(
                    selectedExerciseId = workoutExercise.id,
                    selectedExercises = selectedExercises.toTypedArray())
                findNavController().navigate(action)
            },
            onDeleteSetsSelected = { workoutExercise ->
                confirmDeleteAllSets(workoutExercise)
            },
            context = requireContext(),
        )

        setFragmentResultListener("moveExerciseRequestKey") { key, bundle ->
            val updatedExercises: List<WorkoutExercise>? = if (SDK_INT >= TIRAMISU) {
                bundle.getParcelableArrayList("updatedExercises", WorkoutExercise::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelableArrayList("updatedExercises")
            }
            updatedExercises?.let {
                selectedExercises.clear()
                addExercises(updatedExercises)
                Log.d("CreateWorkoutFragment", "Updated Exercises: $it")
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Initialize itemTouchHelper with custom callback
        val callback = CreateWorkoutAutoScrollCallback(
            adapter, recyclerView, onDragEnd = {
                updateExerciseOrder() // Update exercise order when drag ends
                for (exercise in selectedExercises) {
                    exercise.isSetsVisible = true
                }
                adapter.notifyDataSetChanged()
            })

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        adapter.onStartDrag = { viewHolder ->
            callback.isLongPressDragEnabled = true
            itemTouchHelper.startDrag(viewHolder)
        }

        // Check the flag before adding exercises
        if (!hasAddedExercises) {
            args.selectedExercises?.let {
                addExercises(it.toList())
                hasAddedExercises = true  // Update the flag after adding exercises
            }
            args.workoutTitle?.let {
                workoutNameInput?.setText(it)
            }
            args.workoutNotes?.let {
                workoutNotesInput?.setText(it)
            }
        }

        // Observe selected exercises and update the adapter
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<List<WorkoutExercise>>("selectedExercises")
            ?.observe(viewLifecycleOwner) { exercises ->
                val selectedExSize = selectedExercises.size
                if (returningFromExerciseSelection) {
                    addExercises(exercises)
                    returningFromExerciseSelection = false
                }
                adapter.notifyItemRangeInserted(selectedExSize-1, selectedExSize+exercises.size)
            }

        addExerciseButton?.setOnClickListener {
            val action = CreateWorkoutFragmentDirections.actionNavigationCreateWorkoutToNavigationExerciseSelection(origin = "create")
            findNavController().navigate(action)
            returningFromExerciseSelection = true
        }

        saveWorkoutButton?.setOnClickListener {
            confirmFinish()
        }

        cancelWorkoutButton?.setOnClickListener {
            confirmCancel()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmLeaveFragment()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("workoutName", workoutNameInput?.text.toString())
        outState.putString("workoutNotes", workoutNotesInput?.text.toString())
        outState.putParcelableArrayList("selectedExercises", ArrayList(selectedExercises))
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

    private fun confirmDeleteExercise(exercise: WorkoutExercise) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_exercise_confirmation_title)
            .setMessage("${getString(R.string.delete_exercise_confirmation_message)} ${exercise.title}?")
            .setPositiveButton(R.string.yes) { dialog, _ ->
            onDeleteExercise(exercise)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
        .create().show()
    }

    private fun onDeleteExercise(exercise: WorkoutExercise) {
        // Implement delete logic here
        selectedExercises.remove(exercise)
        updateExerciseOrder()
        val adapter = view?.findViewById<RecyclerView>(R.id.recycler_view_workout_exercises)?.adapter as WorkoutExerciseAdapter
        adapter.notifyDataSetChanged()
    }

    private fun saveWorkout() {
        val workoutId = args.workoutId
        val workoutName = workoutNameInput?.text.toString()
        val workoutNotes = workoutNotesInput?.text.toString()

        if (validateInputs(workoutName)) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                showLoading(true)
                viewModel.getAllWorkouts(onSuccess = { workoutsList ->
                    if (workoutId in workoutsList.map {it.id}) {
                        val updatedWorkout = args.workoutId?.let {
                            Workout(
                                id = it,
                                title = workoutName,
                                notes = workoutNotes,
                                exercises = selectedExercises
                            )
                        }
                        if (updatedWorkout != null) {
                            val originalExerciseIds = args.selectedExercises!!.map {it.id}
                            viewModel.updateWorkout(updatedWorkout, originalExerciseIds, onSuccess = {
                                // Navigate to another fragment or show success message if needed
                                val action = CreateWorkoutFragmentDirections.actionCreateWorkoutToNavigationWorkout()
                                findNavController().navigate(action)
                            }, onFailure = { exception ->
                                Log.d("CreateWorkoutFragment", "Failed to update workout")
                                Toast.makeText(requireContext(), "Failed: cache updated workout", Toast.LENGTH_SHORT).show()
                                showLoading(false)
                            })
                        }
                    }
                    else {
                        val userWorkouts = workoutsList.filter { it.id.startsWith("user_") }
                        if (userWorkouts.size >= 5) {
                            Toast.makeText(
                                requireContext(),
                                "Max (5) Custom Created Workouts Reached.",
                                Toast.LENGTH_SHORT
                            ).show()
                            showLoading(false)
                        } else {
                            val newWorkout = Workout(
                                id = "user_${UUID.randomUUID()}",
                                title = workoutName,
                                notes = workoutNotes,
                                exercises = selectedExercises
                            )
                            viewModel.createWorkout(newWorkout, onSuccess = {
                                val action =
                                    CreateWorkoutFragmentDirections.actionCreateWorkoutToNavigationWorkout()
                                findNavController().navigate(action)
                            }, onFailure = {
                                Toast.makeText(requireContext(), "Failed: cache new workout", Toast.LENGTH_SHORT).show()
                                showLoading(false)
                            })

                        }
                    }
                }, onFailure = { exception ->
                    Log.d("CreateWorkoutFragment", "Failed")
                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                })
            }
        }
    }

    private fun validateInputs(workoutName: String): Boolean {
        return if (workoutName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a workout name", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgress?.visibility = if (isLoading) VISIBLE else GONE
        saveWorkoutButton?.isEnabled = !isLoading
        addExerciseButton?.isEnabled = !isLoading
    }

    private fun confirmDeleteAllSets(exercise: WorkoutExercise) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_sets_title)
            .setMessage("${getString(R.string.clear_sets_message)} ${exercise.title}?")
            .setPositiveButton(R.string.yes) { dialog, _ ->
            deleteAllSets(exercise)
            dialog.dismiss()
        }
            .setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
            .create().show()
    }

    private fun deleteAllSets(exercise: WorkoutExercise) {
        val adapter = view?.findViewById<RecyclerView>(R.id.recycler_view_workout_exercises)?.adapter as WorkoutExerciseAdapter
        exercise.sets.clear()
        exercise.weights.clear()
        exercise.reps.clear()
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "All sets deleted for ${exercise.title}", Toast.LENGTH_SHORT).show()
    }

    private fun confirmLeaveFragment() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_workout_confirmation_title)
            .setMessage(R.string.leave_workout_confirmation_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
            findNavController().popBackStack()
            dialog.dismiss()
        }
            .setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
            .create().show()
    }

    private fun confirmFinish() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.finish_workout_creation_title)
            .setMessage(R.string.finish_workout_creation_confirmation_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                saveWorkout()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun confirmCancel() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cancel_workout_title)
            .setMessage(R.string.cancel_workout_confirmation_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                val action = CreateWorkoutFragmentDirections.actionCreateWorkoutToNavigationWorkout()
                findNavController().navigate(action)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }
}

