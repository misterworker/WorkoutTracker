/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start


import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.MainActivity
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentWorkoutSummaryBinding
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.workoutwrecker.workouttracker.ui.workout.create.CreateWorkoutViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID


@AndroidEntryPoint
class WorkoutSummaryFragment : Fragment() {

    private val auth = Firebase.auth

    private var _binding: FragmentWorkoutSummaryBinding? = null
    private val binding get() = _binding!!

    private var loadingProgress: ProgressBar? = null
    private val viewModel: CreateWorkoutViewModel by viewModels()
    private lateinit var deadLinearLayout: LinearLayout


    private val args: WorkoutSummaryFragmentArgs by navArgs()

    private var listener: OnWorkoutSummaryBackListener? = null

    interface OnWorkoutSummaryBackListener {
        fun onWorkoutSummaryBackPressed()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutSummaryBinding.inflate(inflater, container, false)
        loadingProgress = binding.loadingProgress
        binding.fragmentToolbar.visibility = View.VISIBLE
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnWorkoutSummaryBackListener) {
            listener = context
        } else {
            throw IllegalStateException("$context must implement OnWorkoutSummaryBackListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deadLinearLayout = requireActivity().findViewById(R.id.dead_linear_layout)

        val title = args.workoutTitle
        val exercises = args.exercises?.toList() ?: emptyList()
        val oldExerciseIds = args.oldExerciseIds

        Log.d("WorkoutSummaryFragment", "Exercises: $exercises")

        val topExercises = exercises
            .map { exercise ->
                // Calculate the total reps where completion is true and set is not "W"
                val totalReps = exercise.reps.indices.sumOf { index ->
                    if (exercise.completion.getOrElse(index) { false } && exercise.sets.getOrElse(index) { "" } != "W") {
                        exercise.reps[index]
                    } else {
                        0
                    }
                }
                exercise.copy(completedreps = totalReps) // Add a totalReps property for sorting
            }
            .sortedByDescending { it.completedreps } // Sort exercises by total reps in descending order
            .take(5) // Take the top 5 exercises


        // Calculate total sets considering both completion and non-"W" conditions
        val totalSets = exercises.sumOf { exercise ->
            exercise.sets.indices.count { index ->
                exercise.sets[index] != "W" && exercise.completion.getOrElse(index) { false }
            }
        }

        // Calculate categorized exercises considering both completion and non-"W" conditions
        val categoryCounts = exercises
            .flatMap { exercise ->
                exercise.sets.indices.filter { index ->
                    exercise.sets[index] != "W" && exercise.completion.getOrElse(index) { false }
                }.map { exercise.category }
            }
            .groupingBy { it }
            .eachCount()

        // Calculate category percentages
        val categoryPercentages = categoryCounts.mapValues { (_, count) ->
            (count.toFloat() / totalSets) * 100
        }


        val pieEntries = categoryPercentages.map { (category, percentage) ->
            PieEntry(percentage, category)
        }

        val pieDataSet = PieDataSet(pieEntries, "Categories")
        pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        pieDataSet.setValueTextColors(context?.let { listOf(it.getColor(R.color.text)) })
        pieDataSet.setValueTextColors(context?.let { listOf(it.getColor(R.color.text)) })

        val pieData = PieData(pieDataSet)

        // Set up the Pie Chart
        val pieChart = binding.donutChart
        setupPieChart(pieChart, pieData)

        // Prepare BarEntries for the chart
        val barEntries = topExercises.mapIndexed { index, exercise ->
            BarEntry(index.toFloat(), exercise.completedreps.toFloat())
        }

        // Create a BarDataSet with the entries
        val barDataSet = BarDataSet(barEntries, "Total Reps")
        barDataSet.colors = ColorTemplate.LIBERTY_COLORS.toList()
        barDataSet.setValueTextColors(context?.let { listOf(it.getColor(R.color.text)) })

        // Create BarData object and set it to the BarChart
        val barData = BarData(barDataSet)

        val barChart = view.findViewById<BarChart>(R.id.top_5_reps_barChart)
        barChart.description.isEnabled = false
        barChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        // Enable touch gestures and highlighting
        barChart.isHighlightPerTapEnabled = true
        barChart.setTouchEnabled(false)
        barChart.data = barData
        barChart.description.text = "Top 5 Exercises by Total Reps"

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(topExercises.map { it.title })
        xAxis.labelRotationAngle = 270f
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        barChart.axisLeft.labelCount = 5
        barChart.axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        barChart.axisRight.isEnabled = false // Hide right y-axis
        barChart.axisRight.textColor = ContextCompat.getColor(requireContext(), R.color.text)
        barChart.invalidate() // Refresh the chart

        val totalMinutes = args.length.toLong()

        binding.workoutTitle.text = title

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        binding.timeTaken.text = String.format(Locale.getDefault(), "Time taken: %dh %dm", hours, minutes)

        val completedSets = exercises
            .filter { exercise ->
                exercise.completedsetscount > 0 // Check if there are completed sets
            }
            .joinToString(separator = "\n") { exercise ->
                "${exercise.completedsetscount} x ${exercise.title}"
            }

        binding.completedSets.text = completedSets.trim()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmLeaveFragment()
        }

        showSaveWorkoutSection()

        val moreIcon = view.findViewById<ImageView>(R.id.more_icon)
        val lessIcon = view.findViewById<ImageView>(R.id.less_icon)
        val additionalCheckboxes = view.findViewById<LinearLayout>(R.id.additional_checkboxes)

        moreIcon.setOnClickListener {
            additionalCheckboxes.visibility = View.VISIBLE
            moreIcon.visibility = View.GONE
            lessIcon.visibility = View.VISIBLE
        }
        lessIcon.setOnClickListener {
            additionalCheckboxes.visibility = View.GONE
            lessIcon.visibility = View.GONE
            moreIcon.visibility = View.VISIBLE
        }

        val checkboxSaveWorkout = view.findViewById<CheckBox>(R.id.checkbox_save_workout)
        val checkboxSaveOrder = view.findViewById<CheckBox>(R.id.checkbox_save_order)
        val checkboxSaveRest = view.findViewById<CheckBox>(R.id.checkbox_save_rest)
        val checkboxSaveValues = view.findViewById<CheckBox>(R.id.checkbox_save_values)
        val checkboxSaveNotes = view.findViewById<CheckBox>(R.id.checkbox_save_notes)
        val checkboxSaveSuperset = view.findViewById<CheckBox>(R.id.checkbox_save_superset)
        val checkboxSaveDetails = view.findViewById<CheckBox>(R.id.checkbox_save_workout_details)
        val buttonConfirm = view.findViewById<Button>(R.id.button_confirm)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)

        fun checkboxesEnabled(enabled: Boolean) {
            if (!enabled){
                checkboxSaveOrder.isEnabled = false
                checkboxSaveRest.isEnabled = false
                checkboxSaveValues.isEnabled = false
                checkboxSaveNotes.isEnabled = false
                checkboxSaveSuperset.isEnabled = false
                checkboxSaveDetails.isEnabled = false
            }
            else {
                checkboxSaveOrder.isEnabled = true
                checkboxSaveRest.isEnabled = true
                checkboxSaveValues.isEnabled = true
                checkboxSaveNotes.isEnabled = true
                checkboxSaveSuperset.isEnabled = true
                checkboxSaveDetails.isEnabled = true
            }
        }

        if (args.createdWorkoutId == null) {
            checkboxesEnabled(false)
        } else {
            checkboxSaveWorkout.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkboxSaveOrder.isChecked = false
                    checkboxSaveRest.isChecked = false
                    checkboxSaveValues.isChecked = false
                    checkboxSaveNotes.isChecked = false
                    checkboxSaveSuperset.isChecked = false
                    checkboxSaveDetails.isChecked = false

                    checkboxesEnabled(false)
                } else {
                    checkboxesEnabled(true)
                }
            }
        }

        buttonConfirm.setOnClickListener {
            val reorder = checkboxSaveOrder.isChecked
            val saveValues = checkboxSaveValues.isChecked
            val saveRestTimers = checkboxSaveRest.isChecked
            val saveNotes = checkboxSaveNotes.isChecked
            val saveSupersets = checkboxSaveSuperset.isChecked
            val saveDetails = checkboxSaveDetails.isChecked
            val saveWorkout = checkboxSaveWorkout.isChecked

            saveWorkout(
                reorder = reorder, saveValues = saveValues, saveRestTimers = saveRestTimers,
                saveNotes = saveNotes, saveSupersets = saveSupersets, saveDetails = saveDetails,
                saveWorkout = saveWorkout, oldExerciseIds = oldExerciseIds.toList()
            )
            hideSaveWorkoutSection()
        }

        buttonCancel.setOnClickListener {
            hideSaveWorkoutSection()
        }

        val doneAll = view.findViewById<Button>(R.id.done)

        doneAll.setOnClickListener {
            confirmLeaveFragment()
        }
    }

    private fun setupPieChart(pieChart: PieChart, pieData: PieData) {
        pieChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text)
        pieChart.data = pieData
//        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.isRotationEnabled = false
        pieChart.holeRadius = 50f
        pieChart.transparentCircleRadius = 55f
        pieChart.setCenterTextSize(16f)
        pieChart.setEntryLabelTextSize(50f)
        // Set the color of the transparent circle
        pieChart.setHoleColor(ContextCompat.getColor(requireContext(), R.color.screen))
        pieChart.setDrawEntryLabels(false) // You don't want this
        pieChart.invalidate() // Refresh the chart
    }

    private fun showSaveWorkoutSection() {
        binding.dimmingView.visibility = View.VISIBLE
        binding.saveWorkoutSection.visibility = View.VISIBLE
    }

    private fun hideSaveWorkoutSection() {
        binding.dimmingView.visibility = View.GONE
        binding.saveWorkoutSection.visibility = View.GONE
    }

    private fun saveWorkout(reorder: Boolean, saveValues: Boolean, saveRestTimers: Boolean,
                            saveNotes: Boolean, saveSupersets: Boolean, saveDetails: Boolean,
                            saveWorkout: Boolean, oldExerciseIds: List<String>) {
        val title = args.workoutTitle
        val newExercises = args.exercises?.toList()?.map { exercise ->
            exercise.copy(completion = ArrayList(emptyList()))
        } ?: emptyList()
        val notes = args.workoutNotes.toString()
        val userId = auth.currentUser?.uid

        if (userId != null) {
            viewModel.getAllWorkouts(onSuccess = { workoutsList ->
                val existingWorkout = workoutsList.find { it.id == args.createdWorkoutId }
                if (existingWorkout != null) {
                    val oldExercisesMap = existingWorkout.exercises.associateBy { it.id }.toMutableMap()
                    var updatedExercises = existingWorkout.exercises.toMutableList()
                    var updatedTitle = existingWorkout.title
                    var updatedNotes = existingWorkout.notes

                    fun updateExercises(transform: (oldExercise: WorkoutExercise, newExercise: WorkoutExercise?) -> WorkoutExercise) {
                        val transformedExercises = oldExercisesMap.mapValues { (_, oldExercise) ->
                            val newExercise = newExercises.find { it.id == oldExercise.id }
                            transform(oldExercise, newExercise)
                        }.values.toList()

                        // Apply updates
                        transformedExercises.forEach { updatedExercise ->
                            val index = updatedExercises.indexOfFirst { it.id == updatedExercise.id }
                            if (index != -1) {
                                updatedExercises[index] = updatedExercise
                                // Update the map
                                oldExercisesMap[updatedExercise.id] = updatedExercise
                            }
                        }
                    }

                    if (reorder) {
                        val reorderedExercises = newExercises.mapNotNull { oldExercisesMap[it.id]?.copy(order = it.order) }
                        // Normalize the order values to be contiguous
                        reorderedExercises.sortedBy { it.order }.mapIndexed { index, exercise ->
                            exercise.copy(order = index + 1)
                        }.let { reorderedList ->
                            // Apply reordering
                            reorderedList.forEach { reorderedExercise ->
                                val index = updatedExercises.indexOfFirst { it.id == reorderedExercise.id }
                                if (index != -1) {
                                    updatedExercises[index] = reorderedExercise
                                    // Update the map
                                    oldExercisesMap[reorderedExercise.id] = reorderedExercise
                                }
                            }
                        }
                    }

                    if (saveValues) {
                        updateExercises { oldExercise, newExercise ->
                            if (newExercise != null) {
                                oldExercise.copy(
                                    sets = newExercise.sets,
                                    weights = newExercise.weights,
                                    reps = newExercise.reps
                                )
                            } else {
                                oldExercise
                            }
                        }
                    }

                    if (saveRestTimers) {
                        updateExercises { oldExercise, newExercise ->
                            if (newExercise != null) {
                                oldExercise.copy(
                                    restTimer = newExercise.restTimer
                                )
                            } else {
                                oldExercise
                            }
                        }
                    }

                    if (saveNotes) {
                        updateExercises { oldExercise, newExercise ->
                            if (newExercise != null) {
                                oldExercise.copy(
                                    notes = newExercise.notes
                                )
                            } else {
                                oldExercise
                            }
                        }
                    }

                    if (saveSupersets){
                        updateExercises { oldExercise, newExercise ->
                            if (newExercise != null) {
                                oldExercise.copy(
                                    supersetid = newExercise.supersetid
                                )
                            } else {
                                oldExercise
                            }
                        }
                    }

                    if (saveDetails){
                        updatedTitle = title
                        updatedNotes = notes
                    }

                    if (saveWorkout) {
                        updatedTitle = title
                        updatedNotes = notes
                        updatedExercises = newExercises.toMutableList()
                    }

                    val updatedWorkout = Workout(
                        id = existingWorkout.id,
                        title = updatedTitle,
                        notes = updatedNotes,
                        exercises = updatedExercises
                    )

                    viewModel.updateWorkout(updatedWorkout, oldExerciseIds, onSuccess = {}, onFailure = { _ ->
                        Log.e("WorkoutSummaryFragment", "Failed to update workout")
                        Toast.makeText(requireContext(), "Failed: summary cache updated workout", Toast.LENGTH_SHORT).show()
                    })
                } else {
                    if (saveWorkout) {
                        val userWorkouts = workoutsList.filter { it.id.startsWith("user_") }
                        if (userWorkouts.size >= 5) {
                            Toast.makeText(
                                requireContext(),
                                "Max (5) Custom Created Workouts Reached.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val newWorkout = Workout(
                                id = "user_${UUID.randomUUID()}",
                                title = title,
                                notes = notes,
                                exercises = newExercises
                            )
                            viewModel.createWorkout(newWorkout, onSuccess = {}, onFailure = {
                                Log.e("WorkoutSummaryFragment", "Failed to create workout")
                                Toast.makeText(
                                    requireContext(),
                                    "Failed: summary cache new workout",
                                    Toast.LENGTH_SHORT
                                ).show()
                            })
                        }
                    }
                }
            }, onFailure = { exception ->
                Log.e("WorkoutSummaryFragment", "Failed")
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            })
        }
    }


    private fun confirmLeaveFragment() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_summary_title)
            .setMessage(R.string.leave_summary_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
//                val action = WorkoutSummaryFragmentDirections.actionWorkoutStartToNavigationWorkout()
//                findNavController().navigate(action)
                listener?.onWorkoutSummaryBackPressed()
                dialog.dismiss()
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        (activity as? MainActivity)?.closeStartWorkoutFragments()
                        deadLinearLayout.visibility = View.GONE
                    }
                }
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}