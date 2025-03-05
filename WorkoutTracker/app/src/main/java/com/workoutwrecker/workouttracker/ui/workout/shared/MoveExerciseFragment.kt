/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.shared

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import com.google.android.material.button.MaterialButton

class MoveExerciseFragment : Fragment() {
    private val args: MoveExerciseFragmentArgs by navArgs()
    private val viewModel: MoveExerciseViewModel by viewModels()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var linkButton: ImageView
    private lateinit var unlinkButton: ImageView
    private lateinit var circleBackground: View
    private lateinit var recyclerView: RecyclerView

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
    private lateinit var sortedSupersetIds: List<Int>
    private val selectedExerciseIds = mutableSetOf<String>()
    private var originalNullStatus = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_move_exercise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set up the action bar to show the back button
        val activity = activity as? AppCompatActivity
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = view.findViewById(R.id.recycler_view_move_exercises)
        circleBackground = view.findViewById(R.id.circle_background)
        val saveOrderButton: MaterialButton = view.findViewById(R.id.button_save)
        val linkIcon: ImageView = view.findViewById(R.id.link)
        val unlinkIcon: ImageView = view.findViewById(R.id.unlink)

        // Access the list of exercises from arguments
        val originalExercises = args.selectedExercises.toMutableList()

        // Create a copy of the exercises list to work with
        val exercises = originalExercises.map { it.copy() }.toMutableList()


        // Collect superset ids into a set and sort them
        val supersetIdSet = mutableSetOf<Int?>()
        for (exercise in exercises) {
            supersetIdSet.add(exercise.supersetid)
        }
        supersetIdSet.remove(null)

        sortedSupersetIds = supersetIdSet.filterNotNull().sorted()

        val adapter = MoveExerciseAdapter(exercises, onStartDrag = { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }, sortedSupersetIds, supersetColors, onExerciseSelected = { id, isSelected, nullStatus ->
            if (isSelected) {
                if (nullStatus && selectedExerciseIds.isEmpty()) {
                    this.originalNullStatus = true
                }
                if (!nullStatus && selectedExerciseIds.isEmpty()) {
                    this.originalNullStatus = false
                }
                selectedExerciseIds.add(id)
            } else {
                selectedExerciseIds.remove(id)

            }

            if (originalNullStatus) {
                linkButton.visibility = if (selectedExerciseIds.isNotEmpty()) View.VISIBLE else View.GONE
                val upcomingColor = supersetColors[sortedSupersetIds.size % supersetColors.size]
                updateCircleBackgroundColor(upcomingColor, true)
            } else {
                unlinkButton.visibility = if (selectedExerciseIds.isNotEmpty()) View.VISIBLE else View.GONE
                val upcomingColor = supersetColors[sortedSupersetIds.size % supersetColors.size]
                updateCircleBackgroundColor(upcomingColor, false)
            }
            circleBackground.visibility = if (selectedExerciseIds.isNotEmpty()) View.VISIBLE else View.GONE
        })
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Initialize itemTouchHelper with proper implementations for abstract methods
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                adapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe actions needed
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                adapter.notifyDataSetChanged()
            }
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        saveOrderButton.setOnClickListener {
            val updatedExercises = adapter.getExercises().toMutableList()
            viewModel.updateAndSaveExercises(updatedExercises)
            val normalisedExercises = viewModel.getExercises()
            Log.d("MoveExerciseFragment", "Updated Exercises: $normalisedExercises")
            setFragmentResult("moveExerciseRequestKey", Bundle().apply {
                putParcelableArrayList("updatedExercises", ArrayList(normalisedExercises))
            })
            findNavController().popBackStack()
        }
        linkIcon.setOnClickListener {
            val newSupersetId = (sortedSupersetIds.maxOrNull() ?: 0) + 1
            sortedSupersetIds = sortedSupersetIds + newSupersetId

            selectedExerciseIds.forEach { id ->
                val position = exercises.indexOfFirst { it.id == id }
                if (position != -1) {
                    exercises[position].supersetid = newSupersetId
                }
            }

            // Ensure all exercises in the new superset are correctly recolored
            sortedSupersetIds = exercises.mapNotNull { it.supersetid }.distinct().sorted()
            val newSupersetExercises = exercises.filter { it.supersetid == newSupersetId }.map { it.id }
            adapter.updateSupersetColors(newSupersetId, newSupersetExercises)
            adapter.updateSupersetColorMap(sortedSupersetIds)

            adapter.clearSelections()
            selectedExerciseIds.clear()
            linkIcon.visibility = View.GONE
            circleBackground.visibility = View.GONE

            adapter.notifyDataSetChanged()
        }

        unlinkIcon.setOnClickListener {
            selectedExerciseIds.forEach { id ->
                val position = exercises.indexOfFirst { it.id == id }
                if (position != -1) {
                    exercises[position].supersetid = null
                }
            }
            adapter.clearSupersetColors(selectedExerciseIds.toList())
            adapter.clearSelections()
            selectedExerciseIds.clear()
            unlinkIcon.visibility = View.GONE
            circleBackground.visibility = View.GONE
            sortedSupersetIds = exercises.mapNotNull { it.supersetid }.distinct().sorted()

            adapter.updateSupersetColorMap(sortedSupersetIds)
            adapter.notifyDataSetChanged()

        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmLeaveFragment()
        }

        // Register the MenuProvider
        activity?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // No need to inflate a menu resource file since we only need the back button
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        confirmLeaveFragment()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun updateCircleBackgroundColor(colorResId: Int, link: Boolean) {
        if (link) {
            val drawable = circleBackground.background as GradientDrawable
            drawable.setColor(resources.getColor(colorResId, null))
        }
        else {
            val drawable = circleBackground.background as GradientDrawable
            drawable.setColor(android.graphics.Color.TRANSPARENT)
        }
    }

    private fun confirmLeaveFragment() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_superset_move_title)
            .setMessage(R.string.leave_superset_move_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                findNavController().popBackStack()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }
}
