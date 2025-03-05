/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises.details

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter


// ExercisePagerAdapter.kt
class ExercisePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 3 // Number of tabs
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ExerciseInstructionsFragment()
            1 -> ExercisesStatsFragment()
            2 -> ExerciseRecordsFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}
