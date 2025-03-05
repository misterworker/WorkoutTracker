/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.home.charts.ChartFragment

// For Bottom Sheet Selection
class VisualsAdapter(
    private val visuals: List<String>,
    private val selectedVisuals: List<String>,
    private val onVisualSelected: (String, Boolean) -> Unit
) : RecyclerView.Adapter<VisualsAdapter.VisualViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisualViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bottom_sheet_visuals, parent, false)
        return VisualViewHolder(view)
    }

    override fun onBindViewHolder(holder: VisualViewHolder, position: Int) {
        val visual = visuals[position]
        holder.bind(visual, selectedVisuals.contains(visual))
    }

    override fun getItemCount(): Int = visuals.size

    inner class VisualViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.visualCheckbox)
        private val visualName: TextView = itemView.findViewById(R.id.visualName)

        fun bind(visual: String, isSelected: Boolean) {
            visualName.text = visual
            checkBox.isChecked = isSelected

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onVisualSelected(visual, isChecked)
            }
        }
    }
}

// For Viewpager2
class ChartPagerAdapter(fragment: HomeFragment,
    private val chartList: List<String>) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return chartList.size
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = ChartFragment()
        val args = Bundle()

        when (chartList[position]) {
            "Workouts Done" -> {
                args.putString("argsChart", "Workouts Done")
            }
            "Time Spent" -> {
                args.putString("argsChart", "Time Spent")
            }
            "Radar Chart" -> {
                args.putString("argsChart", "Radar Chart")
            }
            "Chart4" -> {
                args.putString("argsChart", "Chart4")
            }
            else -> {
                args.putString("argsChart", "Unknown Chart")
            }
        }

        fragment.arguments = args
        return fragment
    }
}


