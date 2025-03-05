/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise

class ExerciseAdapter(
    private var items: List<Pair<String, Exercise?>>,
    private val onExerciseClick: (Exercise) -> Unit,
    private val onExerciseUpdate: (Exercise) -> Unit,
    private val onExerciseDelete: (Exercise) -> Unit,

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val typeLabel = 0
    private val typeExercise = 1

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val labelView: TextView = itemView.findViewById(R.id.label)
    }

    class ExerciseViewHolder(
        itemView: View,
        private val onExerciseClick: (Exercise) -> Unit,
        private val onExerciseUpdate: (Exercise) -> Unit,
        private val onExerciseDelete: (Exercise) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameExerciseView: TextView = itemView.findViewById(R.id.title)
        private val bodyPartExerciseView: TextView = itemView.findViewById(R.id.bodypart)
        private val userCreatedIconView: ImageView = itemView.findViewById(R.id.userCreatedIcon)

        fun bind(exercise: Exercise) {
            nameExerciseView.text = exercise.title
            Log.d("Exercise Adapter", "Binding ${exercise.title}")
            bodyPartExerciseView.text = exercise.bodypart
            itemView.setOnClickListener { onExerciseClick(exercise) }

            userCreatedIconView.visibility = View.GONE //Reset Icon Visibility when live view changes

            if (exercise.id.startsWith("user")) {
                userCreatedIconView.visibility = View.VISIBLE
                userCreatedIconView.setOnClickListener {
                    showUserCreatedPopup(userCreatedIconView.context)
                }
            }
            itemView.setOnLongClickListener {
                // Check if the exercise was created by the current user and is not a default exercise
                if (exercise.id.startsWith("user")) {
                    showPopupMenu(it, exercise)
                }
                true
            }
        }

        private fun showUserCreatedPopup(context: Context) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Created by User")
                .setMessage("Press and hold to update exercise")
                .setPositiveButton("OK", null)
                .show()
        }

        private fun showPopupMenu(view: View, exercise: Exercise) {
            Log.d("ShowPopupMenu", exercise.id)
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.exercise_popup_menu, popup.menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.exercise_update -> {
                        onExerciseUpdate(exercise)
                        true
                    }
                    R.id.exercise_delete -> {
                        onExerciseDelete(exercise)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (items[position].second == null) typeLabel else typeExercise
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == typeLabel) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_label, parent, false)
            LabelViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
            ExerciseViewHolder(view, onExerciseClick, onExerciseUpdate, onExerciseDelete)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == typeLabel) {
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