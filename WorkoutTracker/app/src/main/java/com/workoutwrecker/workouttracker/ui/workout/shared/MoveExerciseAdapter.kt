/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.shared

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise

class MoveExerciseAdapter(
    private val exercises: MutableList<WorkoutExercise>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val sortedSupersetIds: List<Int>,
    private val supersetColors: List<Int>,
    private val onExerciseSelected: (String, Boolean, Boolean) -> Unit
) : RecyclerView.Adapter<MoveExerciseAdapter.MoveExerciseViewHolder>() {

    private val supersetIdToColorMap = mutableMapOf<Int, Int>()
    private val selectedIds = mutableSetOf<String>()
    private var isListenerEnabled = true

    init {
        updateSupersetColorMap(sortedSupersetIds)
    }

    fun updateSupersetColorMap(sortedSupersetIds: List<Int>) {
        sortedSupersetIds.forEachIndexed { index, supersetId ->
            Log.d("MoveExerciseAdapter", "UpdateSuperSetColorMap: $sortedSupersetIds")
            supersetIdToColorMap[supersetId] = supersetColors[index % supersetColors.size]
            Log.d("MoveExerciseAdapter", "UpdateSuperSetColorMap: $supersetIdToColorMap")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveExerciseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_move_exercise, parent, false)
        return MoveExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveExerciseViewHolder, position: Int) {
        val exercise = exercises[position]
        holder.bind(exercise)

        Log.d("MoveExerciseAdapter", "Superset id for ${exercise.title}: ${exercise.supersetid}")
        holder.itemView.setOnLongClickListener {
            onStartDrag(holder)
            true
        }

        holder.itemView.setOnClickListener {
            holder.supersetCheckbox.isChecked = !holder.supersetCheckbox.isChecked
        }

        isListenerEnabled = false
        holder.supersetCheckbox.isChecked = selectedIds.contains(exercise.id)
        isListenerEnabled = true

        holder.supersetCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isListenerEnabled) {
                onExerciseSelected(exercise.id, isChecked, exercise.supersetid == null)
                if (isChecked) {
                    selectedIds.add(exercise.id)
                } else {
                    selectedIds.remove(exercise.id)
                }
            }
        }

        // Set background color based on superset id
        val supersetId = exercise.supersetid
        if (supersetId != null && supersetIdToColorMap.containsKey(supersetId)) {
            holder.itemView.setBackgroundColor(holder.itemView.context.resources.getColor(supersetIdToColorMap[supersetId]!!))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int {
        return exercises.size
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        // Ensure the positions are within the valid range
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= exercises.size || toPosition >= exercises.size) {
            return
        }

        // Move the item in the list
        val movedExercise = exercises.removeAt(fromPosition)
        exercises.add(toPosition, movedExercise)
        notifyItemMoved(fromPosition, toPosition)
        exercises.forEachIndexed { index, exercise ->
            Log.d("MoveExerciseAdapter", "Position $index: ${exercise.title}")
        }
    }

    fun getExercises(): List<WorkoutExercise> {
        Log.d("MoveExerciseAdapter", "exercises: $exercises")
        return exercises
    }

    fun updateSupersetColors(newSupersetId: Int, ids: List<String>) {
        supersetIdToColorMap[newSupersetId] = supersetColors[sortedSupersetIds.size % supersetColors.size]
        ids.forEach { id ->
            Log.d("MoveExerciseAdapter", "Update id: $id")
            Log.d("MoveExerciseAdapter", "SupersetIdToColorMap: $supersetIdToColorMap")
            val position = exercises.indexOfFirst { it.id == id }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }
    }

    fun clearSelections() {
        selectedIds.clear()
        notifyDataSetChanged()
    }

    fun clearSupersetColors(ids: List<String>) {
        ids.forEach { id ->
            val position = exercises.indexOfFirst { it.id == id }
            if (position != -1) {
                exercises[position].supersetid = null
                notifyItemChanged(position)
            }
        }
    }

    inner class MoveExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val exerciseTitle: TextView = itemView.findViewById(R.id.exerciseTitle)
        private val dragHandle: ImageView = itemView.findViewById(R.id.dragHandle)
        val supersetCheckbox: CheckBox = itemView.findViewById(R.id.supersetCheckbox) // New CheckBox

        init {
            dragHandle.setOnTouchListener { _, _ ->
                onStartDrag(this)
                true
            }
        }

        fun bind(workoutExercise: WorkoutExercise) {
            exerciseTitle.text = workoutExercise.title
        }
    }
}
