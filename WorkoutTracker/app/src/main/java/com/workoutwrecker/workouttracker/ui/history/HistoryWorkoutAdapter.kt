/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryWorkoutAdapter(
    private var historyWorkouts: List<HistoryWorkout>,
    private val onWorkoutClick: (HistoryWorkout) -> Unit,
    private val onWorkoutUpdate: (HistoryWorkout) -> Unit,
    private val onWorkoutDelete: (HistoryWorkout) -> Unit,
    private val onWorkoutShare: (HistoryWorkout) -> Unit,
    private val onWorkoutSave: (HistoryWorkout) -> Unit):
    RecyclerView.Adapter<HistoryWorkoutAdapter.HistoryWorkoutViewHolder>() {

    class HistoryWorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.history_workout_title)
        val date: TextView = view.findViewById(R.id.history_workout_date)
        val timeTaken: TextView = view.findViewById(R.id.timeTaken)
        val exercises: TextView = view.findViewById(R.id.history_workout_exercises)
        val menuIcon: ImageView = itemView.findViewById(R.id.history_workout_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryWorkoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_workout, parent, false)
        return HistoryWorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryWorkoutViewHolder, position: Int) {
        val historyWorkout = historyWorkouts[position]
        historyWorkout.exercises = historyWorkout.exercises.sortedBy { it.order }
        holder.title.text = historyWorkout.title
        val dateInMillis: Long = historyWorkout.date
        val date = Date(dateInMillis)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

        holder.date.text = dateFormat.format(date)
        holder.timeTaken.text = historyWorkout.length+"mins"

        val completedSets = historyWorkout.exercises
            .filter { exercise ->
                exercise.completedsetscount > 0 // Check if there are completed sets
            }
            .joinToString(separator = "\n") { exercise ->
                "${exercise.completedsetscount} x ${exercise.title}"
            }

        holder.exercises.text = completedSets
        holder.menuIcon.setOnClickListener { view ->
            showPopupMenu(view, historyWorkout)
        }
        holder.itemView.setOnClickListener {
            onWorkoutClick(historyWorkout)
        }
    }

    private fun showPopupMenu(view: View, workout: HistoryWorkout) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.history_workout_popup_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.history_workout_update -> {
                    onWorkoutUpdate(workout)
                    true
                }
                R.id.history_workout_delete -> {
                    onWorkoutDelete(workout)
                    true
                }
                R.id.history_workout_share -> {
                    onWorkoutShare(workout)
                    true
                }
                R.id.history_workout_save -> {
                    onWorkoutSave(workout)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun getItemCount(): Int {
        return historyWorkouts.size
    }

    fun setHistoryWorkouts(historyWorkouts: List<HistoryWorkout>) {
        this.historyWorkouts = historyWorkouts ?: emptyList()
        notifyDataSetChanged()
    }
}
