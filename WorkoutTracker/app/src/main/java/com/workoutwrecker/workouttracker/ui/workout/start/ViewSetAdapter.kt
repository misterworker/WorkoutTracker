/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start

import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.workout.shared.SwipeLayout


class ViewSetAdapter(
    val sets: MutableList<Triple<Float, Int, String>>,
    private val completions: MutableList<Boolean>,
) : ListAdapter<Triple<Float, Int, String>, ViewSetAdapter.SetViewHolder>(SetDiffCallback()) {

    inner class SetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val weightInput: EditText = view.findViewById(R.id.weight_input)
        val repsInput: EditText = view.findViewById(R.id.reps_input)
        val setType: TextView = view.findViewById(R.id.set_number)
        val setCheckbox: CheckBox = view.findViewById(R.id.set_checkbox)
        val swipeLayout: SwipeLayout = view.findViewById(R.id.swipe_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        Log.d("SetAdapter", "CreateViewHolder: $sets")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_set, parent, false)
        return SetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.swipeLayout.isEnabledSwipe = false
        val (weight, reps, setType) = sets[holder.bindingAdapterPosition]
        val drawable = AppCompatResources.getDrawable(holder.itemView.context, R.drawable.edit_text_background)
        holder.weightInput.background = drawable
        holder.repsInput.background = drawable
        Log.d("SetAdapter", "onBindViewHolder: $weight, $reps, $setType, $completions")
        holder.setType.text = setType
        holder.weightInput.isEnabled = false
        holder.repsInput.isEnabled = false
        holder.setCheckbox.isEnabled = false
        // Remove existing TextWatchers
        holder.weightInput.tag?.let { watcher ->
            if (watcher is TextWatcher) {
                holder.weightInput.removeTextChangedListener(watcher)
            }
        }
        holder.repsInput.tag?.let { watcher ->
            if (watcher is TextWatcher) {
                holder.repsInput.removeTextChangedListener(watcher)
            }
        }
        holder.weightInput.text.clear()
        holder.repsInput.text.clear()
        holder.weightInput.hint = weight.toString()
        holder.repsInput.hint = reps.toString()
        Log.d("SetAdapter", "weightInput: ${holder.weightInput.text}, ${holder.weightInput.hint}")
        holder.weightInput.filters =
            arrayOf(InputFilter.LengthFilter(6), DecimalDigitsInputFilter(5, 2))
        holder.setCheckbox.setOnCheckedChangeListener(null)
        holder.setCheckbox.isChecked = completions[holder.bindingAdapterPosition]
        val setTypeColour = when (holder.setType.text){
            "D" -> ContextCompat.getColor(holder.setType.context, R.color.set_drop) // Replace with your actual color resource
            "R" -> ContextCompat.getColor(holder.setType.context, R.color.set_rest_pause)
            "F" -> ContextCompat.getColor(holder.setType.context, R.color.set_failure)
            "W" -> ContextCompat.getColor(holder.setType.context, R.color.set_warm_up)
            else -> ContextCompat.getColor(holder.setType.context, R.color.text) // Default color
        }
        holder.setType.setTextColor(setTypeColour)
    }

    override fun getItemCount(): Int {
        return sets.size
    }
}
