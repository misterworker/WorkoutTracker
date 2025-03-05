/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.home.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CommunityViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is plate calculator fragment"
    }
    val text: LiveData<String> = _text
}