/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.create

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.workout.shared.SwipeLayout

class DecimalDigitsInputFilter(digitsBeforeZero: Int, digitsAfterZero: Int) : InputFilter {
    private val pattern = Regex("^[0-9]{0,$digitsBeforeZero}((\\.[0-9]{0,$digitsAfterZero})?)\$")

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        val newString = dest.toString().substring(0, dstart) + source.toString() + dest.toString()
            .substring(dend)
        return if (newString.matches(pattern)) {
            null // Accept the new input
        } else {
            "" // Reject the new input
        }
    }
}

class CreateWorkoutSetAdapter(
    val sets: MutableList<Triple<Float, Int, String>>,
    private val completions: MutableList<Boolean>,
    private val listener: OnSetChangeListener,
) : ListAdapter<Triple<Float, Int, String>, CreateWorkoutSetAdapter.SetViewHolder>(SetDiffCallback()) {

    interface OnSetChangeListener {
        fun onSetChanged(setIndex: Int, weight: Float, reps: Int)
        fun onSetRemoved(updatedSets: List<Triple<Float, Int, String>>)
        fun onSetTypeChanged(setIndex: Int, setType: String)
    }

    inner class SetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val weightInput: EditText = view.findViewById(R.id.weight_input)
        val swipeLayout: SwipeLayout = view.findViewById(R.id.swipe_layout)
        val repsInput: EditText = view.findViewById(R.id.reps_input)
        val setType: TextView = view.findViewById(R.id.set_number)
        val setCheckbox: CheckBox = view.findViewById(R.id.set_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_set, parent, false)
        return SetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        val (weight, reps, setType) = sets[holder.bindingAdapterPosition]
        val weightDrawable = AppCompatResources.getDrawable(holder.itemView.context, R.drawable.edit_text_background)?.mutate()
        val repsDrawable = AppCompatResources.getDrawable(holder.itemView.context, R.drawable.edit_text_background)?.mutate()
        val drawable = AppCompatResources.getDrawable(holder.itemView.context, R.drawable.edit_text_background)

        holder.setCheckbox.isEnabled = false
        holder.weightInput.background = drawable
        holder.repsInput.background = drawable

        Log.d("SetAdapter", "onBindViewHolder: $weight, $reps, $setType, $completions")
        holder.setType.text = setType

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

        holder.weightInput.filters =
            arrayOf(InputFilter.LengthFilter(6), DecimalDigitsInputFilter(5, 2))

        val weightTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newWeight = s.toString().toFloatOrNull() ?: 0.0f
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    sets[pos] = sets[pos].copy(first = newWeight)
                    listener.onSetChanged(pos, newWeight, sets[pos].second)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        holder.weightInput.addTextChangedListener(weightTextWatcher)
        holder.weightInput.tag = weightTextWatcher

        val repsTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newReps = s.toString().toIntOrNull() ?: 0
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    sets[pos] = sets[pos].copy(second = newReps)
                    listener.onSetChanged(pos, sets[pos].first, newReps)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        holder.repsInput.addTextChangedListener(repsTextWatcher)
        holder.repsInput.tag = repsTextWatcher
        holder.weightInput.hint = weight.toString()
        holder.repsInput.hint = reps.toString()

        holder.setType.setOnClickListener {
            showSetTypePopupMenu(holder.setType, holder.bindingAdapterPosition)
        }

        val setTypeColour = when (holder.setType.text){
            "D" -> ContextCompat.getColor(holder.setType.context, R.color.set_drop) // Replace with your actual color resource
            "R" -> ContextCompat.getColor(holder.setType.context, R.color.set_rest_pause)
            "F" -> ContextCompat.getColor(holder.setType.context, R.color.set_failure)
            "W" -> ContextCompat.getColor(holder.setType.context, R.color.set_warm_up)
            else -> ContextCompat.getColor(holder.setType.context, R.color.text) // Default color
        }

        holder.setType.setTextColor(setTypeColour)

        holder.swipeLayout.setOnActionsListener(object : SwipeLayout.SwipeActionsListener {
            override fun onDragStart() {
                val color = ContextCompat.getColor(holder.itemView.context, R.color.set_delete_color)
                val colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
                weightDrawable?.colorFilter = colorFilter
                repsDrawable?.colorFilter = colorFilter
                holder.weightInput.background = weightDrawable
                holder.repsInput.background = repsDrawable
            }
            override fun onOpen(direction: Int, isContinuous: Boolean) {
                if (direction == SwipeLayout.RIGHT) {
                    removeSet(holder.bindingAdapterPosition)
                } else if (direction == SwipeLayout.LEFT) {
                    removeSet(holder.bindingAdapterPosition)
                }
            }

            override fun onClose() {
                weightDrawable?.clearColorFilter()
                repsDrawable?.clearColorFilter()

                // Set the updated background with the appropriate color filter
                holder.weightInput.background = weightDrawable
                holder.repsInput.background = repsDrawable
            }
        })
    }


    private fun showSetTypePopupMenu(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.set_type_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            handleSetTypeMenuItemClick(menuItem, position)
            true
        }
        popup.show()
    }

    private fun handleSetTypeMenuItemClick(item: MenuItem, position: Int) {
        when (item.itemId) {
            R.id.action_drop_set -> toggleSetType(position, "D")
            R.id.action_rest_pause_set -> toggleSetType(position, "R")
            R.id.action_failure_set -> toggleSetType(position, "F")
            R.id.action_warmup_set -> toggleSetType(position, "W")
        }
        notifyItemChanged(position)
    }

    private fun toggleSetType(position: Int, type: String) {
        val currentSetType = sets[position].third
        val newSetType = if (currentSetType == type) (position + 1).toString() else type
        sets[position] = sets[position].copy(third = newSetType)
        listener.onSetTypeChanged(position, newSetType)
        Log.d("SetAdapter", "toggleSetType: $sets")
    }

    override fun getItemCount(): Int {
        return sets.size
    }

    fun removeSet(position: Int) {
        if (position >= 0 && position < sets.size && position < completions.size) {
            sets.removeAt(position)
            completions.removeAt(position) // Remove corresponding completion status
            renumberSets()
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, sets.size)
            listener.onSetRemoved(sets)
        }
    }

    private fun renumberSets() {
        for (i in sets.indices) {
            val newSetType = when (val currentSetType = sets[i].third) {
                "D", "R", "F", "W" -> currentSetType // Preserve special types
                else -> (i + 1).toString() // Convert numeric set types to position-based numbers
            }
            Log.d("SetAdapter", "renumberSets: $sets")
            sets[i] = sets[i].copy(third = newSetType)
            listener.onSetTypeChanged(i, newSetType)
            notifyItemChanged(i)
        }
    }
}

class SetDiffCallback : DiffUtil.ItemCallback<Triple<Float, Int, String>>() {
    override fun areItemsTheSame(
        oldItem: Triple<Float, Int, String>,
        newItem: Triple<Float, Int, String>
    ): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(
        oldItem: Triple<Float, Int, String>,
        newItem: Triple<Float, Int, String>
    ): Boolean {
        return oldItem == newItem
    }
}
