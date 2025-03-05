/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.history

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkout
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.workout.WorkoutViewModel
import com.workoutwrecker.workouttracker.ui.workout.create.CreateWorkoutViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class HistoryWorkoutFragment : Fragment(), OnDateSelectedListener {

    private val auth = Firebase.auth
    private val historyWorkoutViewModel: HistoryWorkoutViewModel by viewModels()
    private val workoutViewModel: WorkoutViewModel by viewModels()
    private val createWorkoutViewModel: CreateWorkoutViewModel by viewModels()
    private lateinit var historyWorkoutAdapter: HistoryWorkoutAdapter
    private lateinit var weekAdapter: WeekAdapter
    private var dimView: View? = null
    private var loadingProgressBar: View? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    private var currentWeekStartDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history_workout, container, false)
        dimView = view.findViewById(R.id.dimView)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

        // Set up week RecyclerView
        val weekRecyclerView = view.findViewById<RecyclerView>(R.id.week_recycler_view)
        weekAdapter = WeekAdapter(getCurrentWeekDays(emptyList()), requireContext(), this)
        weekRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        weekRecyclerView.adapter = weekAdapter

        // Attach custom touch listener to intercept touch events
        weekRecyclerView.addOnItemTouchListener(RecyclerViewOnItemTouchListener())

        // Set up the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.history_workout_recycler_view)
        historyWorkoutAdapter = HistoryWorkoutAdapter(emptyList(),
            onWorkoutClick = { workout ->
                val action = HistoryWorkoutFragmentDirections.actionNavigationWorkoutToNavigationWorkoutView(
                    selectedExercises = workout.exercises.toTypedArray(), workoutNotes = workout.notes,
                    workoutTitle = workout.title)
                //disables checkboxes and sets visibility of certain features to GONE
                findNavController().navigate(action)
            },
            onWorkoutUpdate = { workout ->
                val action = HistoryWorkoutFragmentDirections.actionNavigationWorkoutToNavigationUpdateHistoryWorkout(
                    selectedExercises = workout.exercises.toTypedArray(), workoutDate = workout.date,
                    historyWorkoutId = workout.id, workoutNotes = workout.notes, workoutTitle = workout.title,
                    checkboxEnabled = true, historyWorkoutLength = workout.length)
                findNavController().navigate(action)
            },
            onWorkoutDelete = { workout ->
                confirmDelete(workout)
            },
            onWorkoutShare = {workout ->
                shareWorkout(workout)

            },
            onWorkoutSave = {workout ->
                saveWorkout(workout)
            })
        recyclerView.adapter = historyWorkoutAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Observe ViewModel
        historyWorkoutViewModel.historyWorkouts.observe(viewLifecycleOwner) { workouts ->
            Log.d("HistoryWorkoutFragment", "Observed workouts: $workouts")
            if (currentWeekStartDate != null) {
                Log.d("HistoryWorkoutFragment", "Filtering workouts for week starting: ${currentWeekStartDate!!.time}")
                val filteredWorkouts = workouts.filter { workout ->
                    val workoutCalendar = Calendar.getInstance()
                    workoutCalendar.timeInMillis = workout.date
                    workoutCalendar.get(Calendar.WEEK_OF_YEAR) == currentWeekStartDate!!.get(Calendar.WEEK_OF_YEAR) &&
                            workoutCalendar.get(Calendar.YEAR) == currentWeekStartDate!!.get(Calendar.YEAR)
                }
                historyWorkoutAdapter.setHistoryWorkouts(filteredWorkouts)
                Log.d("HistoryWorkoutFragment", "Filtered workouts: $filteredWorkouts")
            } else {
                historyWorkoutAdapter.setHistoryWorkouts(workouts)
                Log.d("HistoryWorkoutFragment", "All workouts: $workouts")
            }
            val workoutDates = workouts.map { it.date }
            weekAdapter.updateWeekDays(getCurrentWeekDays(workoutDates)) // Update the week adapter with the workout dates
        }

        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { historyWorkoutViewModel.fetchHistoryWorkouts(userId) },
            )
        }

        return view
    }

    private fun saveWorkout(workout:HistoryWorkout) {
        val workoutName = workout.title
        val workoutNotes = workout.notes
        val workoutExercises = workout.exercises

        val userId = auth.currentUser?.uid
        if (userId != null) {
            createWorkoutViewModel.getAllWorkouts(onSuccess = { workoutsList ->
                val userWorkouts = workoutsList.filter { it.id.startsWith("user_") }
                if (userWorkouts.size >= 5) {
                    Toast.makeText(
                        requireContext(),
                        "Max (5) Custom Created Workouts Reached.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    workoutExercises.forEach { workoutExercise ->
                        workoutExercise.completion =
                            ArrayList(List(workoutExercise.sets.size) { false })
                    }

                    val newWorkout = Workout(
                        id = "user_${UUID.randomUUID()}",
                        title = workoutName,
                        notes = workoutNotes,
                        exercises = workoutExercises
                    )
                    workoutViewModel.createWorkout(userId, newWorkout, onSuccess={}, onFailure={
                        Toast.makeText(requireContext(), "Failed: cache new workout", Toast.LENGTH_SHORT).show()
                    })
                }
            }, onFailure = { exception ->
                Log.d("HistoryWorkoutFragment", "Failed")
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            })
        }
    }

    private fun getCurrentWeekDays(workoutDates: List<Long> = emptyList()): List<WeekDay> {
        val days = mutableListOf<WeekDay>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd-MM", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val formattedWorkoutDates = workoutDates.map { date -> fullDateFormat.format(Date(date)) }

        for (i in 0..6) {
            val dateStr = dateFormat.format(calendar.time)
            val fullDateStr = fullDateFormat.format(calendar.time)
            val hasWorkout = formattedWorkoutDates.contains(fullDateStr)
            days.add(WeekDay(dayFormat.format(calendar.time), dateStr, hasWorkout))
            calendar.add(Calendar.DAY_OF_WEEK, 1)
            Log.d("getCurrentWeekDays", "$fullDateStr has workout: $hasWorkout")
        }

        if (days.size != 7) {
            throw IllegalStateException("getCurrentWeekDays() must return exactly 7 days")
        }

        return days
    }


    override fun onDateSelected(day: Int, month: Int, year: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        calendar.time

        // Update currentWeekStartDate for filtering workouts
        currentWeekStartDate = calendar.clone() as Calendar

        // Force the LiveData to re-emit the current value to trigger filtering
        historyWorkoutViewModel.reEmitHistoryWorkouts()

        val newWeekDays = getWeekDaysForDate(calendar)
        weekAdapter.updateWeekDays(newWeekDays)
    }

    private fun getWeekDaysForDate(calendar: Calendar): List<WeekDay> {
        val days = mutableListOf<WeekDay>()

        // Set the calendar to the start of the week containing the given date
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd-MM", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val workoutDates = historyWorkoutViewModel.historyWorkouts.value?.map { historyWorkout ->
            val dateInMillis = historyWorkout.date
            fullDateFormat.format(Date(dateInMillis))
        } ?: emptyList()
        Log.d("getWeekDaysForDate", "Workout dates: $workoutDates")

        // Add the days of the week to the list
        for (i in 0..6) {
            val dateStr = dateFormat.format(calendar.time)
            val fullDateStr = fullDateFormat.format(calendar.time)
            val hasWorkout = workoutDates.contains(fullDateStr)
            days.add(WeekDay(dayFormat.format(calendar.time), dateStr, hasWorkout))
            calendar.add(Calendar.DAY_OF_WEEK, 1)
            Log.d("getWeekDaysForDate", "$fullDateStr has workout: $hasWorkout")
        }

        if (days.size != 7) {
            throw IllegalStateException("getWeekDaysForDate() must return exactly 7 days")
        }

        return days
    }

    // Custom touch listener to intercept touch events
    private inner class RecyclerViewOnItemTouchListener : RecyclerView.OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Request the RecyclerView to handle the touch event
                    rv.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    }

    private fun confirmDelete(historyWorkout: HistoryWorkout) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_workout_title)
            .setMessage("${getString(R.string.delete_workout_confirmation_message)} ${historyWorkout.title}?")
            .setPositiveButton(R.string.yes) { _, _ ->
                showLoading(true) // Show loading at the beginning
                historyWorkoutViewModel.deleteWorkout(userId, historyWorkout, onSuccess={
                    viewLifecycleOwner.lifecycleScope.launch {
                        awaitAll(
                            async { historyWorkoutViewModel.fetchHistoryWorkouts(userId) }
                        )
                    }
                    showLoading(false)
                }, onFailure={
                    showLoading(false)
                    Toast.makeText(requireContext(),
                        "Failed: cache delete workout", Toast.LENGTH_SHORT).show()
                })
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    fun HistoryWorkout.toJson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

    private fun shareWorkout(historyWorkout: HistoryWorkout) {
        val historyWorkoutJson = historyWorkout.toJson() // Create this method to convert workout to JSON
        val shareUri = "https://www.workoutwrecker.com/workout?data=$historyWorkoutJson"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_workout_text, shareUri))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_workout)))
    }

    private fun showLoading(isLoading: Boolean) {
        dimView?.visibility = if (isLoading) View.VISIBLE else View.GONE
        loadingProgressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

}
