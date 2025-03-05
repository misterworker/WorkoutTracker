/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.record

import androidx.room.Entity
import androidx.room.PrimaryKey


// How Personal Record works:
// 1. It will not be used in exercise details page because top maxes are only taken into
// account chronologically

@Entity(tableName = "pr_table")
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val historyWorkoutId: String,
    val workoutExerciseId: String,
    val exerciseId: String,
    val date: Long = System.currentTimeMillis(),
    val prType: ArrayList<Int>, // See legend
    var maxVol: Int,
    var maxReps: Int,
    var maxVal: Int,
)

//Legend: 0 - Max, 1 - Most reps, 2 - Most Vol