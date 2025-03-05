/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises.details

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.databinding.ItemExerciseRecordsBinding

class ExerciseRecordsAdapter(
    private val repsList: List<Int>,
    private val weightsList: List<Int>,
    private val maxList: List<Int>,
    private val dateList: List<String>,
    private val unit: String,
) : RecyclerView.Adapter<ExerciseRecordsAdapter.ExerciseRecordViewHolder>() {

    inner class ExerciseRecordViewHolder(private val binding: ItemExerciseRecordsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reps: Int, weight: Int, estimatedMax: Int, date: String) {
            // Set the details of the set
            binding.set.text = "${weight}$unit x $reps"
            // Display the estimated 1 rep max
            binding.estimatedOneRepMax.text = "${estimatedMax}$unit"
            binding.date.text = date
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseRecordViewHolder {
        val binding = ItemExerciseRecordsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExerciseRecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseRecordViewHolder, position: Int) {
        holder.bind(repsList[position], weightsList[position], maxList[position], dateList[position])
    }

    override fun getItemCount(): Int {
        return repsList.size
    }
}