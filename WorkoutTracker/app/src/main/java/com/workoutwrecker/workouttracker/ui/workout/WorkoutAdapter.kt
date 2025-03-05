/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.ui.data.workout.Workout
import com.workoutwrecker.workouttracker.ui.home.HomeFragment
import com.workoutwrecker.workouttracker.ui.home.charts.ChartFragment

class WorkoutAdapter(
    private val onStartWorkoutClick: (Workout) -> Unit,
    private val onWorkoutUpdate: (Workout) -> Unit,
    private val onWorkoutDelete: (Workout) -> Unit,
    private val onWorkoutShare: (Workout) -> Unit
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    private var workouts: List<Workout> = listOf()
    private val expandedPositions = mutableSetOf<Int>()

    class WorkoutViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.workout_title)
        val details: LinearLayout = view.findViewById(R.id.workout_details)
        val workouts: TextView = view.findViewById(R.id.workout_exercises)
        val startWorkoutButton: Button = view.findViewById(R.id.start_workout_button)
        val menuIcon: ImageView = itemView.findViewById(R.id.menu_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = workouts[position]
        holder.title.text = workout.title

        workout.exercises = workout.exercises.sortedBy { it.order }
        // Build a string of exercises
        val exercisesText = workout.exercises.joinToString(separator = "\n") { exercise ->
            "- ${exercise.title}"
        }

        val notesText = workout.notes
        val formattedNotesLabel = "Notes:"

        val combinedText = SpannableStringBuilder()
        combinedText.append(exercisesText)
        if (notesText.isNotEmpty()) {
            combinedText.append("\n\n")

            // Append and style the formattedNotesLabel
            val notesLabelStart = combinedText.length
            combinedText.append(formattedNotesLabel)
            combinedText.setSpan(StyleSpan(Typeface.BOLD_ITALIC), notesLabelStart, combinedText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Append and style the notesText
            combinedText.append("\n")
            val notesStart = combinedText.length
            combinedText.append(notesText)
            combinedText.setSpan(StyleSpan(Typeface.ITALIC), notesStart, combinedText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Set the combined text to the TextView
        holder.workouts.text = combinedText


        Log.d("WorkoutAdapter", "Binding workout: $workout")
        Log.d("WorkoutAdapter", "Expanded positions: $expandedPositions")
        Log.d("WorkoutAdapter", "AdditionalText: $combinedText")

        val isExpanded = expandedPositions.contains(position)
        holder.details.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.startWorkoutButton.visibility = if (isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            if (isExpanded) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
        }

        if (workout.id.startsWith("user")) {
            Log.d("WorkoutAdapter", "Setting ${workout.title} to VISIBLE")
            holder.menuIcon.visibility = View.VISIBLE
        } else {
            Log.d("WorkoutAdapter", "Setting ${workout.title} to GONE")
            holder.menuIcon.visibility = View.GONE
        }

        holder.menuIcon.setOnClickListener {
            showPopupMenu(it, workout)
        }

        // Disable the button to prevent multiple clicks
        holder.startWorkoutButton.setOnClickListener {
            Log.d("WorkoutAdapterClick", "${workout.exercises}")
            onStartWorkoutClick(workout)
        }
    }

    private fun showPopupMenu(view: View, workout: Workout) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.workout_popup_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.workout_update -> {
                    onWorkoutUpdate(workout)
                    true
                }
                R.id.workout_delete -> {
                    onWorkoutDelete(workout)
                    true
                }
                R.id.workout_share -> {
                    onWorkoutShare(workout)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun getItemCount(): Int = workouts.size

    fun setWorkouts(newWorkouts: List<Workout>) {
        workouts = newWorkouts
        notifyDataSetChanged()
    }
}

// For Viewpager2
class ProgramPagerAdapter(fragment: CurrentWorkoutFragment,
                        private val programList: List<String>) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return programList.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = WorkoutsFragment()
        val args = Bundle()
        val curProgram = programList[position]

        args.putString("curProgram", curProgram)
        fragment.arguments = args
        return fragment
    }
}

class WorkoutSelectionAdapter(
    private var items: List<Workout>,
    private val onWorkoutClick: (Workout) -> Unit,
    private val preSelectedWorkouts: List<Workout>

) : RecyclerView.Adapter<WorkoutSelectionAdapter.WorkoutViewHolder>() {

    private val selectedWorkouts = preSelectedWorkouts.toMutableSet()

    // ViewHolder class to bind the workout item
    class WorkoutViewHolder(
        itemView: View,
        private val onWorkoutClick: (Workout) -> Unit,
        private val selectedWorkouts: MutableSet<Workout>
    ) : RecyclerView.ViewHolder(itemView) {

        private lateinit var currentWorkout: Workout
        private val nameWorkoutView: TextView = itemView.findViewById(R.id.title)
        private val bodypartWorkoutView: TextView = itemView.findViewById(R.id.bodypart)
        private val checkBox: CheckBox = itemView.findViewById(R.id.workout_checkbox)

        fun bind(workout: Workout) {
            currentWorkout = workout

            nameWorkoutView.text = workout.title
            bodypartWorkoutView.text = workout.type

            // Set checkbox state based on selected workouts
            checkBox.isChecked = selectedWorkouts.contains(workout)
            Log.d("WorkoutSelectionAdapter", "${selectedWorkouts.contains(workout)}")

            itemView.setOnClickListener {
                toggleSelection()
            }

            checkBox.setOnClickListener {
                toggleSelection()
            }
        }

        private fun toggleSelection() {
            if (selectedWorkouts.contains(currentWorkout)) {
                selectedWorkouts.remove(currentWorkout)
                checkBox.isChecked = false
            } else {
                selectedWorkouts.add(currentWorkout)
                checkBox.isChecked = true
            }

            // Notify the listener
            onWorkoutClick(currentWorkout)
        }
    }

    // Return the list of selected workouts
    fun getSelectedWorkouts(): List<Workout> {
        Log.d("WorkoutSelectionAdapter", "getSelectedWorkouts: $selectedWorkouts")
        return selectedWorkouts.toList()
    }

    // Create a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_selection, parent, false)
        return WorkoutViewHolder(view, onWorkoutClick, selectedWorkouts)
    }

    // Bind the ViewHolder with data
    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val workout = items[position]
        holder.bind(workout) // Use the bind method to populate the ViewHolder
    }

    // Return the size of the dataset
    override fun getItemCount(): Int = items.size

    // Update the list of workouts
    fun updateWorkouts(newItems: List<Workout>) {
        items = newItems
        notifyDataSetChanged() // Notify that the dataset has changed
    }

    fun clearWorkouts(){
        selectedWorkouts.clear()
        notifyDataSetChanged()
    }
}


