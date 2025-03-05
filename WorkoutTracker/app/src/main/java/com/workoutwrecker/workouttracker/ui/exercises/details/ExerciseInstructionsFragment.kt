/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises.details

import android.graphics.drawable.Drawable
import android.os.Bundle
import com.bumptech.glide.request.transition.Transition
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.workoutwrecker.workouttracker.MainActivity
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentExerciseInstructionsBinding

class ExerciseInstructionsFragment : Fragment() {

    private var _binding: FragmentExerciseInstructionsBinding? = null
    private val binding get() = _binding!!
    private var gifDrawable: GifDrawable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseInstructionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val exercise = try {
            (parentFragment as ExerciseInformationFragment).exerciseInfoArgs.exercise
        } catch (e: ClassCastException) {
            try {
                (parentFragment as ExerciseInformationSepFragment).exerciseInfoArgs.exercise
            } catch (e: ClassCastException) {
                null // or handle the case when both casts fail
            }
        }

        requireActivity().let {
            (it as MainActivity).setPageTitle(exercise?.title.toString())
        }

        val imageView: ImageView = binding.ivAnimation // Make sure to reference the correct view

        // Dynamically get the resource ID from the exercise id
        val resourceId = resources.getIdentifier(exercise?.id, "raw", requireContext().packageName)

        // Load the GIF using Glide
        if (resourceId != 0) { // Ensure the resource exists
            Glide.with(this)
                .asGif()
                .load(resourceId)
                .into(object : CustomTarget<GifDrawable>() {
                    override fun onResourceReady(resource: GifDrawable, transition: Transition<in GifDrawable>?) {
                        gifDrawable = resource
                        resource.startFromFirstFrame()
                        imageView.setImageDrawable(resource)
                        resource.start()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        imageView.setImageDrawable(placeholder)
                    }
                })
        } else {
            // Handle case where the resource does not exist
            imageView.setImageResource(R.drawable.ic_launcher_foreground) // Placeholder image
        }

        val instructions = exercise?.instructions ?: ""
        val numberedInstructions = instructions.split("\n").mapIndexed { index, line ->
            "${index + 1}. $line"
        }.joinToString("\n\n")

        binding.tvInstructions.text = numberedInstructions

        binding.btnPause.setOnClickListener {
            pauseAnimation()
            binding.btnPause.visibility = View.GONE
            binding.btnPlay.visibility = View.VISIBLE
        }
        binding.btnPlay.setOnClickListener {
            playAnimation()
            binding.btnPause.visibility = View.VISIBLE
            binding.btnPlay.visibility = View.GONE
        }
    }

    private fun pauseAnimation() {
        gifDrawable?.stop()
    }

    private fun playAnimation() {
        gifDrawable?.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}