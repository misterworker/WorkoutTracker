/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.record

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkout
import com.workoutwrecker.workouttracker.ui.data.record.PersonalRecord
import com.workoutwrecker.workouttracker.ui.data.record.PersonalRecordRepository
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class PersonalRecordViewModel @Inject constructor(
    private val repository: PersonalRecordRepository
) : ViewModel() {
    // LiveData to expose the list of personal records to the UI
    private val _personalRecords = MutableLiveData<List<PersonalRecord>>()
    val personalRecords: LiveData<List<PersonalRecord>> = _personalRecords

    // Function to load all personal records
    fun loadAllPersonalRecords() {
        viewModelScope.launch {
            val records = repository.getAllPersonalRecordsFromCache()
            _personalRecords.value = records
        }
    }

    // Function to add a new personal record
    fun addPersonalRecord(personalRecord: PersonalRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addPersonalRecordToLocalCache(personalRecord)
            loadAllPersonalRecords()
        }
    }

    // Function to update a personal record
    fun updatePersonalRecord(personalRecord: PersonalRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePersonalRecordInLocalCache(personalRecord)
            loadAllPersonalRecords()
        }
    }

    // Function to delete a personal record
    fun deletePersonalRecord(personalRecord: PersonalRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePersonalRecordFromLocalCache(personalRecord)
            loadAllPersonalRecords()
        }
    }

    // Function to clear all records
    fun clearAllRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLocalCache()
            _personalRecords.postValue(emptyList())
        }
    }

    fun calculateAllRecords(historyWorkouts: List<HistoryWorkout>) {

        for (historyWorkout in historyWorkouts) {
            for (workoutExercise in historyWorkout.exercises) {
                val filteredIndices = workoutExercise.sets.indices.filter { index ->
                    workoutExercise.completion.getOrElse(index) { false } &&
                            workoutExercise.sets.getOrElse(index) { "" } != "W"
                }

                // Calculate 1RM
                val maxes = calculate1RM(workoutExercise, filteredIndices)
                val maxVal = maxes[0]
                val maxReps = maxes[1]
                val maxVol = maxes[2]


                val curRecords =
                    _personalRecords.value?.filter { it.exerciseId == workoutExercise.exerciseid }
                var existingOneRM = 0
                var existingMaxReps = 0
                var existingMaxVolume = 0
                curRecords?.let {
                    existingOneRM = it.filter { record -> record.prType.contains(0) }
                        .maxOfOrNull { record -> record.maxVal } ?: 0
                    existingMaxReps = it.filter { record -> record.prType.contains(1) }
                        .maxOfOrNull { record -> record.maxVol } ?: 0
                    existingMaxVolume = it.filter { record -> record.prType.contains(2) }
                        .maxOfOrNull { record -> record.maxVol } ?: 0
                }
                val curPrTypes = ArrayList<Int>()
                if (maxVal > existingOneRM) {
                    curPrTypes.add(0)
                }
                if (maxReps > existingMaxReps) {
                    curPrTypes.add(1)
                }
                if (maxVol > existingMaxVolume) {
                    curPrTypes.add(2)
                }

                val newRecord = PersonalRecord(
                    historyWorkoutId = historyWorkout.id,
                    workoutExerciseId = workoutExercise.id,
                    exerciseId = workoutExercise.exerciseid,
                    maxVol = maxVol,
                    maxReps = maxReps,
                    maxVal = maxVal,
                    prType = curPrTypes,
                )

                addPersonalRecord(newRecord)

                Log.d("RecordViewModel", "$newRecord")
            }
        }
    }

    private fun calculate1RM(workoutExercise: WorkoutExercise, filteredIndices: List<Int>): List<Int> {

        var bestMax = 0
        var bestReps = 0
        var totalVolume = 0
        filteredIndices.forEach { index ->
            val reps = workoutExercise.reps.getOrElse(index) { 0 }
            val weight = workoutExercise.weights.getOrElse(index) { 0f }
            val volume = (reps * weight).toInt()
            val curMax = epleyFormula(weight, reps)
            if (curMax > bestMax) {
                bestMax = curMax
            }
            if (reps > bestReps){
                bestReps = reps
            }
            totalVolume += volume
        }
        return listOf(bestMax, bestReps, totalVolume)
    }

    private fun epleyFormula(weight: Float, reps: Int): Int {
        if (reps == 0) {
            return 0
        }
        if (reps == 1) {
            return ceil(weight).toInt()
        }
        return ceil(weight * (1 + reps / 30f)).toInt()
    }

    private fun pace(mode: String, distance: Float, time: Float): Float {
        if (mode == "km/h") {
            return distance * 1000 / time * 3600
        } else {
            return 0f
        }
    }
}

