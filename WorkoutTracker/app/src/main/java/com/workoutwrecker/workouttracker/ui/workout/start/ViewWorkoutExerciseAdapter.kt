/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start

import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import android.text.method.ScrollingMovementMethod
import androidx.core.content.ContextCompat


class ViewWorkoutExerciseAdapter(
    private var selectedExercises: MutableList<WorkoutExercise>
) : RecyclerView.Adapter<ViewWorkoutExerciseAdapter.WorkoutExerciseViewHolder>() {

    private val supersetColors = listOf(
        R.color.superset_1,
        R.color.superset_2,
        R.color.superset_3,
        R.color.superset_4,
        R.color.superset_5,
        R.color.superset_6,
        R.color.superset_7,
        R.color.superset_8,
    )


    private val viewPool = RecyclerView.RecycledViewPool()

    class WorkoutExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.exercise_title)
        val notes: EditText = view.findViewById(R.id.notes_edittext)
        val setsRecyclerView: RecyclerView = view.findViewById(R.id.sets_recycler_view)
        val topVerticalLine: View = view.findViewById(R.id.top_vertical_line)
        val topHorizontalLine: View = view.findViewById(R.id.top_horizontal_line)
        val bottomVerticalLine: View = view.findViewById(R.id.bottom_vertical_line)
        val bottomHorizontalLine: View = view.findViewById(R.id.bottom_horizontal_line)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutExerciseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout_exercise, parent, false)
        return WorkoutExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutExerciseViewHolder, position: Int) {
        // Remove any existing TextWatcher to prevent multiple instances
        holder.notes.removeTextChangedListener(holder.notes.tag as? TextWatcher)

        val workoutExercise = selectedExercises[position]
        holder.title.text = workoutExercise.title
        holder.notes.visibility = if (workoutExercise.isNotesVisible
            || workoutExercise.notes.isNotEmpty()) View.VISIBLE else View.GONE

        holder.notes.isVerticalScrollBarEnabled = true
        holder.notes.movementMethod = ScrollingMovementMethod()

        // Set up the nested RecyclerView for sets
        val sets = workoutExercise.weights.zip(workoutExercise.reps).mapIndexed { index, pair ->
            Triple(pair.first, pair.second, workoutExercise.sets.getOrNull(index) ?: "")
        }.toMutableList()

        if (workoutExercise.completion.size < sets.size) {
            for (i in workoutExercise.completion.size until sets.size) {
                workoutExercise.completion.add(false)
            }
        }

        holder.setsRecyclerView.setRecycledViewPool(viewPool)
        val setAdapter = ViewSetAdapter(
            sets, workoutExercise.completion,
        )

        holder.setsRecyclerView.adapter = setAdapter
        holder.setsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)

        if (workoutExercise.supersetid != null) {
            holder.topVerticalLine.visibility = View.VISIBLE
            holder.bottomVerticalLine.visibility = View.VISIBLE
            holder.topHorizontalLine.visibility = View.VISIBLE
            holder.bottomHorizontalLine.visibility = View.VISIBLE
        }

        val colorResId = getSupersetColor(workoutExercise.supersetid)
        holder.topVerticalLine.setBackgroundColor(ContextCompat.getColor(holder.topVerticalLine.context, colorResId))
        holder.bottomVerticalLine.setBackgroundColor(ContextCompat.getColor(holder.bottomVerticalLine.context, colorResId))
        holder.topHorizontalLine.setBackgroundColor(ContextCompat.getColor(holder.bottomHorizontalLine.context, colorResId))
        holder.bottomHorizontalLine.setBackgroundColor(ContextCompat.getColor(holder.bottomHorizontalLine.context, colorResId))
    }

    private fun getSupersetColor(supersetid: Int?): Int {
        if (supersetid == null) {
            return android.R.color.transparent // Or any default color you want to use for non-superset items
        }
        val index = (supersetid - 1) % supersetColors.size
        return supersetColors[index]
    }

    override fun getItemCount(): Int {
        return selectedExercises.size
    }
}
