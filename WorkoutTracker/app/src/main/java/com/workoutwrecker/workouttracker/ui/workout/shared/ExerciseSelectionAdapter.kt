/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.shared

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise

class ExerciseSelectionAdapter(
    private var items: List<Pair<String, Exercise?>>,
    private val onExerciseClick: (Exercise) -> Unit,
    private val preSelectedExercises: List<Exercise>

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val selectedExercises = preSelectedExercises.toMutableSet()
    private val TYPE_LABEL = 0
    private val TYPE_EXERCISE = 1

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelView: TextView = itemView.findViewById(R.id.label)
    }

    class ExerciseViewHolder(
        itemView: View,
        private val onExerciseClick: (Exercise) -> Unit,
        private val selectedExercises: MutableSet<Exercise>
    ) : RecyclerView.ViewHolder(itemView) {

        private lateinit var currentExercise: Exercise
        private val nameExerciseView: TextView = itemView.findViewById(R.id.title)
        private val bodypartExerciseView: TextView = itemView.findViewById(R.id.bodypart)
        private val checkBox: CheckBox = itemView.findViewById(R.id.exercise_checkbox)

        fun bind(exercise: Exercise) {
            currentExercise = exercise

            nameExerciseView.text = exercise.title
            bodypartExerciseView.text = exercise.bodypart

            // Set checkbox state based on selected exercises
            checkBox.isChecked = selectedExercises.contains(exercise)

            itemView.setOnClickListener {
                toggleSelection()
            }

            checkBox.setOnClickListener {
                toggleSelection()
            }
        }

        private fun toggleSelection() {
            if (selectedExercises.contains(currentExercise)) {
                selectedExercises.remove(currentExercise)
                checkBox.isChecked = false
            } else {
                selectedExercises.add(currentExercise)
                checkBox.isChecked = true
            }

            onExerciseClick(currentExercise)

        }
    }

    fun getSelectedExercises(): List<Exercise> {
        return selectedExercises.toList()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].second == null) TYPE_LABEL else TYPE_EXERCISE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_LABEL) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_label, parent, false)
            LabelViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise_selection, parent, false)
            ExerciseViewHolder(view, onExerciseClick, selectedExercises)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_LABEL) {
            (holder as LabelViewHolder).labelView.text = items[position].first
        } else {
            (holder as ExerciseViewHolder).bind(items[position].second!!)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateExercises(newItems: List<Pair<String, Exercise?>>) {
        items = newItems
        notifyDataSetChanged()
    }
}
