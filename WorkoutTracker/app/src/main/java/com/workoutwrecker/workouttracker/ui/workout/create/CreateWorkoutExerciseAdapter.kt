/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.create

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

interface ItemTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int)
    fun onItemDismiss(position: Int)
}

class CreateWorkoutExerciseAdapter(
    private var selectedExercises: MutableList<WorkoutExercise>,
    private val onExerciseClick: (WorkoutExercise) -> Unit,
    private val onExerciseDelete: (WorkoutExercise) -> Unit,
    var onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onMoveSelected: (WorkoutExercise) -> Unit,
    private val onDeleteSetsSelected: (WorkoutExercise) -> Unit,
    private val context: Context,
) : ListAdapter<WorkoutExercise, CreateWorkoutExerciseAdapter.WorkoutExerciseViewHolder>
    (WorkoutExerciseDiffCallback()), ItemTouchHelperAdapter {

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
        val addSetButton: Button = view.findViewById(R.id.add_set_button)
        val optionsButton: ImageButton = view.findViewById(R.id.exercise_options_button)
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
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        holder.title.setBackgroundResource(typedValue.resourceId)
        // Remove any existing TextWatcher to prevent multiple instances
        holder.notes.removeTextChangedListener(holder.notes.tag as? TextWatcher)

        val workoutExercise = selectedExercises[position]
        holder.title.text = workoutExercise.title
        holder.notes.visibility = if (workoutExercise.isNotesVisible
            || workoutExercise.notes.isNotEmpty()) View.VISIBLE else View.GONE

        holder.notes.setText(workoutExercise.notes)

        // Attach new TextWatcher
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: ""
                workoutExercise.notes = newText
            }
        }

        holder.notes.addTextChangedListener(textWatcher)
        holder.notes.tag = textWatcher
        holder.notes.isVerticalScrollBarEnabled = true
        holder.notes.movementMethod = ScrollingMovementMethod()
        holder.notes.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (v.hasFocus()) {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    when (event.action and MotionEvent.ACTION_MASK) {
                        MotionEvent.ACTION_UP -> {
                            v.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
                return false
            }
        })

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
        val setAdapter = CreateWorkoutSetAdapter(
            sets, workoutExercise.completion,
            object : CreateWorkoutSetAdapter.OnSetChangeListener {
                override fun onSetChanged(setIndex: Int, weight: Float, reps: Int) {
                    if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        workoutExercise.weights[setIndex] = weight
                        workoutExercise.reps[setIndex] = reps
                    }
                }

                override fun onSetRemoved(updatedSets: List<Triple<Float, Int, String>>) {
                    if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        workoutExercise.sets.clear()
                        workoutExercise.weights.clear()
                        workoutExercise.reps.clear()
                        updatedSets.forEach { (weight, reps, setType) ->
                            workoutExercise.weights.add(weight)
                            workoutExercise.reps.add(reps)
                            workoutExercise.sets.add(setType)
                        }
                    }
                }

                override fun onSetTypeChanged(setIndex: Int, setType: String) {
                    if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        workoutExercise.sets[setIndex] = setType
                    }
                }
            }
        )

        holder.setsRecyclerView.adapter = setAdapter
        holder.setsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)

//        // Enable drag-and-drop and swipe-to-delete for sets
//        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {
//            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
//                return false
//            }
//
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                Log.d("onSwipe", "onSwipe title: ${workoutExercise.title}")
//                val setPosition = viewHolder.bindingAdapterPosition
//                Log.d("ItemTouchHelper", "onSwiped - Position: $setPosition, sets before remove: ${setAdapter.sets}")
//                setAdapter.removeSet(setPosition)
//                Log.d("ItemTouchHelper", "onSwiped - sets after remove: ${setAdapter.sets}")
//            }
//        }
//
//        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)

        Log.d("WorkoutExerciseAdapter", "Workout Exercise title: ${workoutExercise.title}")

//        itemTouchHelper.attachToRecyclerView(holder.setsRecyclerView)

        holder.addSetButton.setOnClickListener {
            workoutExercise.weights.add(0.0f)//default value
            workoutExercise.reps.add(0)
            val setNumber = (workoutExercise.sets.size + 1).toString()
            workoutExercise.sets.add(setNumber)
            workoutExercise.completion.add(false)
            sets.add(Triple(0.0f, 0, setNumber))
            setAdapter.notifyItemInserted(sets.size - 1)
        }

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

        holder.title.setOnClickListener {
            onExerciseClick(workoutExercise)  // Pass the Exercise object
        }

        holder.optionsButton.setOnClickListener { view ->
            showPopupMenu(view, holder.bindingAdapterPosition)
        }

        holder.setsRecyclerView.visibility = if (workoutExercise.isSetsVisible) View.VISIBLE else View.GONE
        // Set long click listener to start dragging
        holder.title.setOnLongClickListener {
            holder.setsRecyclerView.visibility = View.GONE
            toggleAllSetsVisibility(false)

            // Notify items before and after the current one
            val startPosition = 0
            val endPosition = itemCount - 1

            // Update the range before the current item
            if (position > startPosition) {
                notifyItemRangeChanged(startPosition, position)
            }

            // Update the range after the current item
            if (position < endPosition) {
                notifyItemRangeChanged(position + 1, endPosition - position)
            }

            onStartDrag(holder)
            true
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.workout_exercise_popup_menu, popup.menu)

        val exerciseWithNotes = selectedExercises[position]
        val createNotesItem = popup.menu.findItem(R.id.action_create_notes)

        val exerciseWithRestTimer = selectedExercises[position]
        val currentRestTimerInSeconds = exerciseWithRestTimer.restTimer
        val currentRestTimerInMinutes = (currentRestTimerInSeconds / 60).toInt()
        val currentRestTimerRemainingSeconds = (currentRestTimerInSeconds % 60).toInt()
        val restTimerText = "${currentRestTimerInMinutes}m ${currentRestTimerRemainingSeconds}s"

        val autoRestTimerItem = popup.menu.findItem(R.id.action_auto_rest_timer)
        autoRestTimerItem.title = "Rest Timer: $restTimerText"

        if (!exerciseWithNotes.isNotesVisible) {
            // If notes are blank, show "Create Notes"
            createNotesItem.setTitle(R.string.create_notes)
        } else {
            // If notes are not blank, show "Delete Notes"
            createNotesItem.setTitle(R.string.delete_notes)
        }

        popup.setOnMenuItemClickListener { menuItem ->

            handleMenuItemClick(menuItem, position)
            true
        }
        popup.show()
    }

    private fun handleMenuItemClick(item: MenuItem, position: Int) {
        when (item.itemId) {
            R.id.action_delete_exercise -> {
                val exerciseToDelete = selectedExercises[position]
                onExerciseDelete(exerciseToDelete)
            }
            R.id.action_create_superset -> {
                val exerciseToMove = selectedExercises[position]
                onMoveSelected(exerciseToMove)
            }
            R.id.action_create_notes -> {
                createNotes(position)
            }
            R.id.action_auto_rest_timer -> {
                showRestTimerDialog(position)
            }
            R.id.action_delete_all_sets -> {
                val exerciseToDeleteSets = selectedExercises[position]
                onDeleteSetsSelected(exerciseToDeleteSets)
            }
        }
    }

    private fun toggleAllSetsVisibility(isVisible: Boolean) {
        for (exercise in selectedExercises) {
            exercise.isSetsVisible = isVisible
        }
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

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Log.d("ItemTouchHelper", "onItemMove - fromPosition: $fromPosition, toPosition: $toPosition")
        val fromExercise = selectedExercises.removeAt(fromPosition)
        selectedExercises.add(toPosition, fromExercise)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        selectedExercises.removeAt(position)
        notifyItemRemoved(position)
    }

    private fun createNotes(position: Int){
        val exercise = selectedExercises[position]
        if (!exercise.isNotesVisible) {
            // Handle create notes
            exercise.isNotesVisible = true
            notifyItemChanged(position)
        } else {
            // Handle delete notes
            showDeleteNotesConfirmationDialog(position)
        }
    }

    private fun showDeleteNotesConfirmationDialog(position: Int) {
        val exercise = selectedExercises[position]
        AlertDialog.Builder(context)
            .setTitle(R.string.delete_notes_confirmation_title)
            .setMessage(R.string.delete_notes_confirmation_message)
            .setPositiveButton(R.string.delete_notes) { dialog, which ->
                exercise.notes = ""
                exercise.isNotesVisible = false
                notifyItemChanged(position)
            }
            .setNegativeButton(R.string.cancel) { dialog, which ->
                // Cancelled deletion
            }
            .show()
    }

    private fun showRestTimerDialog(position: Int) {
        val exercise = selectedExercises[position]
        val currentRestTimer = exercise.restTimer
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rest_timer, null)
        val minutesPicker = dialogView.findViewById<NumberPicker>(R.id.rest_timer_minutes_picker)
        val secondsPicker = dialogView.findViewById<NumberPicker>(R.id.rest_timer_seconds_picker)

        // Configure minutes picker
        minutesPicker.minValue = 0
        minutesPicker.maxValue = 30
        minutesPicker.value = (currentRestTimer / 60).toInt()
        minutesPicker.wrapSelectorWheel = true

        // Configure seconds picker with intervals of 5
        val displayedValues = arrayOf("00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55")
        secondsPicker.minValue = 0
        secondsPicker.maxValue = displayedValues.size - 1
        secondsPicker.displayedValues = displayedValues
        secondsPicker.value = ((currentRestTimer % 60) / 5).toInt()
        secondsPicker.wrapSelectorWheel = true

        AlertDialog.Builder(context)
            .setTitle(R.string.set_rest_timer)
            .setView(dialogView)
            .setPositiveButton(R.string.set) { dialog, which ->
                val restTimeInMinutes = minutesPicker.value
                val restTimeInSeconds = secondsPicker.value * 5
                val totalRestTimeInSeconds = (restTimeInMinutes * 60) + restTimeInSeconds
                exercise.restTimer = totalRestTimeInSeconds.toLong()
            }
            .setNegativeButton(R.string.cancel) { dialog, which ->
                // Cancelled rest timer setting
            }
            .show()
    }

    class WorkoutExerciseDiffCallback : DiffUtil.ItemCallback<WorkoutExercise>() {
        override fun areItemsTheSame(oldItem: WorkoutExercise, newItem: WorkoutExercise): Boolean {
            // Assuming WorkoutExercise has a unique identifier
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutExercise, newItem: WorkoutExercise): Boolean {
            return oldItem == newItem
        }
    }
}
