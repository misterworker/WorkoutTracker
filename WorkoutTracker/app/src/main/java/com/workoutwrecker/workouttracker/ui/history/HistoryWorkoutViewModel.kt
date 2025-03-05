/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.history

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.BarEntry
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkout
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkoutRepository
import com.workoutwrecker.workouttracker.ui.data.record.PersonalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.count
import kotlin.collections.eachCount
import kotlin.collections.emptyList
import kotlin.collections.filter
import kotlin.collections.groupBy
import kotlin.collections.groupingBy
import kotlin.collections.iterator
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.maxByOrNull
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.sortedBy


@HiltViewModel
class HistoryWorkoutViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val repository: HistoryWorkoutRepository,
    @ApplicationContext private val context: Context,) : ViewModel() {

    private val sharedPreferences = context.getSharedPreferences("history_workout_prefs", Context.MODE_PRIVATE)
    private val _historyWorkouts = MutableLiveData<List<HistoryWorkout>>()
    val historyWorkouts: LiveData<List<HistoryWorkout>> get() = _historyWorkouts
    val historyWorkoutCount: LiveData<Int> = _historyWorkouts.map { it.size }
    private val _weeklyWorkoutCounts = MutableLiveData<List<BarEntry>>()
    private val _weeklyWorkoutTimeTaken = MutableLiveData<List<BarEntry>>()
    private val _workoutConsistency = MutableLiveData<Double>()
    private val _performanceMetrics = MutableLiveData<List<Double>>()
    private val _personalRecords = MutableLiveData<List<PersonalRecord>>()
    val personalRecords: LiveData<List<PersonalRecord>> get() = _personalRecords

    val weeklyWorkoutCounts: LiveData<List<BarEntry>> get() = _weeklyWorkoutCounts
    val weeklyWorkoutTimeTaken: LiveData<List<BarEntry>> get() = _weeklyWorkoutTimeTaken

    val performanceMetrics: LiveData<List<Double>> get() = _performanceMetrics

    companion object {
        var isFetched = false
    }

    init {
        isFetched = sharedPreferences.getBoolean("isFetched", false)
    }

    suspend fun fetchHistoryWorkouts(userId: String) {
        if (!isFetched) {
            Log.d("HistoryWorkoutViewModel", "Fetching from Firestore")
            val result = repository.getAllHistoryWorkouts(userId)
            result.onSuccess { historyWorkouts ->
                val sortedCachedHistoryWorkouts = historyWorkouts.sortedByDescending { workout ->
                    workout.date
                }
                _historyWorkouts.value = sortedCachedHistoryWorkouts
                isFetched = true
                sharedPreferences.edit().putBoolean("isFetched", true).apply()
            }.onFailure { exception ->
                Log.e("HistoryWorkoutViewModel", "Error fetching workouts", exception)
                isFetched = false
                sharedPreferences.edit().putBoolean("isFetched", false).apply()
            }
        } else {
            Log.d("HistoryWorkoutViewModel", "Fetching from Cache")
            try {
                val cachedHistoryWorkouts = withContext(Dispatchers.IO) {
                    repository.getAllHistoryWorkoutsFromCache()
                }
                val sortedCachedHistoryWorkouts = cachedHistoryWorkouts.sortedByDescending { workout ->
                    workout.date
                }
                _historyWorkouts.value = sortedCachedHistoryWorkouts
            } catch (e: Exception) {
                Log.e("HistoryWorkoutViewModel", "Error fetching cached workouts", e)
            }
        }
    }

    // Method to re-emit the current value
    fun reEmitHistoryWorkouts() {
        _historyWorkouts.value = _historyWorkouts.value
    }

//    fun filterWorkoutsByDate(startDate: Long, endDate: Long) {
//        val allWorkouts = _historyWorkouts.value ?: return
//        val filteredWorkouts = allWorkouts.filter {
//            val workoutDate = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).parse(it.date)!!.time
//            workoutDate in startDate..endDate
//        }
//        _historyWorkouts.value = filteredWorkouts
//    }

    fun deleteWorkout(userId: String, workout: HistoryWorkout, onSuccess: () -> Unit,
                      onFailure: (Exception) -> Unit) {
        viewModelScope.launch {
            repository.deleteWorkout(userId, workout, onSuccess, onFailure)
        }
    }

    fun updateHistoryWorkout(historyWorkout: HistoryWorkout) {
        viewModelScope.launch {
            repository.updateHistoryWorkoutInLocalCache(historyWorkout)
        }
    }

    private fun aggregateWorkoutsByWeek(workouts: List<HistoryWorkout>) {
        val calendar = Calendar.getInstance()
        val weeklyCounts = mutableMapOf<Int, MutableList<HistoryWorkout>>()

        for (workout in workouts) {
            val workoutDate = workout.date
            calendar.timeInMillis = workoutDate
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            if (weeklyCounts[weekOfYear] == null) {
                weeklyCounts[weekOfYear] = mutableListOf()
            }
            weeklyCounts[weekOfYear]!!.add(workout)
        }

        val entries = ArrayList<BarEntry>()
        for ((week, workoutsInWeek) in weeklyCounts) {
            entries.add(BarEntry(week.toFloat(), workoutsInWeek.size.toFloat()))
        }

        _weeklyWorkoutCounts.value = entries
        Log.d("HistoryWorkoutViewModel", "Weekly workout counts: $entries")
    }

    private fun getTimeTakenByWeek(workouts: List<HistoryWorkout>) {
        val calendar = Calendar.getInstance()
        val weeklyTimeTaken = mutableMapOf<Int, Float>()

        for (workout in workouts) {
            val workoutDate = workout.date
            calendar.timeInMillis = workoutDate
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)

            if (weeklyTimeTaken[weekOfYear] == null) {
                weeklyTimeTaken[weekOfYear] = 0f
            }

            weeklyTimeTaken[weekOfYear] = weeklyTimeTaken[weekOfYear]!! + workout.length.toFloat()
        }

        val entries = ArrayList<BarEntry>()
        for ((week, totalMinutes) in weeklyTimeTaken) {
            entries.add(BarEntry(week.toFloat(), totalMinutes))
        }

        _weeklyWorkoutTimeTaken.value = entries
        Log.d("HistoryWorkoutViewModel", "Weekly workout time taken: $entries")
    }

    private fun getAllMetrics(workouts: List<HistoryWorkout>, records: List<PersonalRecord>){
        // Get consistency, strength, cardio, progress
        val consistency = getConsistency(workouts)
        val progress = getProgress(records)


        _performanceMetrics.value = listOf(consistency)
    }

    private fun getProgress(records: List<PersonalRecord>): Double{
        // Calculating progress: Find the maxes of each workout exercise, and if they
        // have an increase in max for more than 3 exercises per week, then the user
        // has obtained a 100% progress rate. If not, it decreases with each instance of exercises
        // not progressed. So if 3/4 exercises get progress a week, there will be a 75% progress
        // rate. The max number of prs will be 4 * number of weeks.

        // Step 1: Group workouts by week
        // Step 2: Get PRs from workout Exercise (max weight, max volume)
        // Step 3: Get total PR count from history workouts.


        val totalPrCountList = ArrayList<Int>()



        val averagePrCount = totalPrCountList.average()
        val progressPercent = averagePrCount/4

        return progressPercent
    }

    private fun getConsistency(workouts: List<HistoryWorkout>): Double {
        // Calculating consistency: Find the mode of the number of rest days user usually takes.
        // To get the list of groups of rest days, get the rest days before any active day,
        // excluding the first active day of the workouts.

        // Create consistency list: With the list of number of rest days, any number above the
        // mode is considered inconsistent.

        // Calculate: Say there are 10 groups of rest days and the mode is 1 (1 rest day in between
        // workout routines), but there are 3 instances of taking a rest day of >1 day, the
        // consistency rating will be (10-3/10) = 70%.
        if (workouts.size < 2) {
            // If there are fewer than 2 workouts, consistency can't really be calculated
            _workoutConsistency.value = 100.0 // Full consistency since there's no data to analyze
            return 100.0
        }
        // Step 1: Extract and sort the workout dates
        val workoutDates = workouts.map {
            Calendar.getInstance().apply { timeInMillis = it.date }
        }.sortedBy { it.timeInMillis }

        // Step 2: Calculate rest days between workout sessions using DAY_OF_YEAR
        val restDays = mutableListOf<Int>()

        for (i in 0 until workoutDates.size - 1) {
            val currentDay = workoutDates[i]
            val nextDay = workoutDates[i + 1]

            // Calculate the difference in days using DAY_OF_YEAR
            val currentDayOfYear = currentDay.get(Calendar.DAY_OF_YEAR)
            val nextDayOfYear = nextDay.get(Calendar.DAY_OF_YEAR)

            // Calculate the actual difference in days
            val diffInDays = if (nextDay.get(Calendar.YEAR) == currentDay.get(Calendar.YEAR)) {
                nextDayOfYear - currentDayOfYear
            } else {
                // When the years are different, we need to add the remaining days in the current year
                val daysInCurrentYear = currentDay.getActualMaximum(Calendar.DAY_OF_YEAR)
                (daysInCurrentYear - currentDayOfYear) + nextDayOfYear
            }

            // If there's more than 1 day between workouts, count it as a rest period
            if (diffInDays > 1) {
                restDays.add(diffInDays - 1) // Subtract 1 to exclude the workout day itself
            }
        }

        // Get mode
        val modeRestDays = restDays.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 1
        val inconsistentRestDaysCount = restDays.count { it > modeRestDays }
        val totalRestPeriods = restDays.size
        Log.d("HistoryWorkoutViewModel", "Total - $totalRestPeriods")

        val consistency: Double = if (totalRestPeriods == 0) {
            100.0 // If no rest periods exist, assume full consistency
        } else {
            ((totalRestPeriods - inconsistentRestDaysCount).toDouble() / totalRestPeriods) * 100
        }

        _workoutConsistency.value = consistency
        return consistency
    }

    fun fetchHistoryWorkoutsLastWeeks(weeksToView: Int, chart: String) {
        viewModelScope.launch {
            // Calculate the start timestamp for the desired weeks ago (e.g., 12 weeks ago)
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // Set to the start of the week
            calendar.add(Calendar.WEEK_OF_YEAR, -weeksToView + 1)
            val startTimestamp = calendar.timeInMillis

            // Fetch all history workouts from cache
            val cachedHistoryWorkouts = withContext(Dispatchers.IO) {
                repository.getAllHistoryWorkoutsFromCache()
            }

            // Filter workouts to include only those within the specified weeks
            val filteredWorkouts = cachedHistoryWorkouts.filter {
                it.date >= startTimestamp // Only include workouts from the desired weeks
            }
            val filteredPersonalRecords = personalRecords.value?.filter {
                it.date >= startTimestamp
            } ?: emptyList()

            // Process the filtered workouts based on the specified chart type
            when (chart) {
                "weeklyCounts" -> aggregateWorkoutsByWeek(filteredWorkouts)
                "timeTaken" -> getTimeTakenByWeek(filteredWorkouts)
                "radarChart" -> getAllMetrics(filteredWorkouts, filteredPersonalRecords)
            }
        }
    }

    suspend fun fetchExercises(userId: String): List<Exercise> {
        try {
            val cachedExercises = withContext(Dispatchers.IO) {
                exerciseRepository.getAllExercisesFromCache()
            }

            return cachedExercises
        } catch (e: Exception) {
            // Handle error
            return emptyList()
        }
    }

    fun getExerciseMax(exerciseId: String): Int {
        val allHistoryWorkouts = _historyWorkouts.value?: emptyList()
        var curMax = 0
        val allWorkoutExercises = allHistoryWorkouts
            .flatMap { workout -> workout.exercises } // Flatten the exercises
            .sortedBy { it.exerciseid } // Sort by the exercise ID
        for (workoutExercise in allWorkoutExercises){
            if (workoutExercise.max > curMax){
                curMax = workoutExercise.max
            }
        }
        return curMax
    }

    fun updatePersonalRecord(personalRecords:List<PersonalRecord>) {
        _personalRecords.value = personalRecords
    }
}
