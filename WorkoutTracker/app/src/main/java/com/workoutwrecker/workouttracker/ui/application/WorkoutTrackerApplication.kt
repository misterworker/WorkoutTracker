/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.application

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WorkoutTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Read night mode setting from SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isNightModeEnabled = sharedPreferences.getBoolean("night_mode", false)

        // Apply the night mode setting
        if (isNightModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
