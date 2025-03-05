/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentExerciseInformationBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.workoutwrecker.workouttracker.MainActivity

class ExerciseInformationFragment : Fragment() {

    private var _binding: FragmentExerciseInformationBinding? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val binding get() = _binding!!

    val exerciseInfoArgs: ExerciseInformationFragmentArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        _binding = FragmentExerciseInformationBinding.inflate(inflater, container, false)
        viewPager = binding.viewPager
        tabLayout = binding.tabLayout

        val adapter = ExercisePagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.instructions)
                1 -> getString(R.string.title_visuals)
                2 -> getString(R.string.exercise_records_records)
                else -> null
            }
        }.attach()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exercise = exerciseInfoArgs.exercise
        binding.exercise = exercise
//        MainActivity.setPageTitle(exercise.title)
    }


    // Load animation into ImageView if it's a URL or resource
        // Assuming `exercise.animation` is a URL or resource identifier
        // Uncomment and adjust accordingly if using a specific library like Glide or Lottie
        // Glide.with(this).load(exercise.animation).into(binding.ivAnimation)

        // Example using a placeholder animation, replace with actual loading code
//        binding.ivAnimation.setImageResource(R.drawable.ic_placeholder_animation)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
