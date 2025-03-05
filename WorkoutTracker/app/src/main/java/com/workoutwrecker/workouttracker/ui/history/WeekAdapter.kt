/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.history

import android.app.DatePickerDialog
import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.workoutwrecker.workouttracker.R
import java.util.Calendar

data class WeekDay(val day: String, val date: String, val hasWorkout: Boolean = false)

interface OnDateSelectedListener {
    fun onDateSelected(day: Int, month: Int, year: Int)
}

class WeekAdapter(
    private var weekDays: List<WeekDay>,
    private val context: Context,
    private val onDateSelectedListener: OnDateSelectedListener
) : RecyclerView.Adapter<WeekAdapter.WeekViewHolder>() {

    init {
        if (weekDays.size != 7) {
            throw IllegalArgumentException("weekDays list should contain exactly 7 elements")
        }
    }

    class WeekViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val weekDayText: TextView = view.findViewById(R.id.week_day_text)
        val weekDateText: TextView = view.findViewById(R.id.week_date_text)
        val cardView: CardView = view.findViewById(R.id.card_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_week, parent, false)
        return WeekViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        if (position < 0 || position >= weekDays.size) {
            Log.e("WeekAdapter", "Invalid position: $position")
            return
        }

        val weekDay = weekDays[position]
        holder.weekDayText.text = weekDay.day
        holder.weekDateText.text = weekDay.date

        val typedValue = TypedValue()
        val theme = context.theme

        // Retrieve card background color from theme
        theme.resolveAttribute(R.attr.cardBackgroundColor, typedValue, true)
        val defaultColor = typedValue.data

        if (weekDay.hasWorkout) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.special_date_background))
        } else {
            holder.cardView.setCardBackgroundColor(defaultColor)
        }

        holder.cardView.setOnClickListener {
            val calendar = Calendar.getInstance()
            val dateParts = weekDay.date.split("-")
            if (dateParts.size != 2) { // Update size check to 2 for "dd-MM" format
                Log.e("WeekAdapter", "Invalid date format: ${weekDay.date}")
                return@setOnClickListener
            }

            calendar.set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
            calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)

            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    // Handle date selection
                    onDateSelectedListener.onDateSelected(dayOfMonth, month, year)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    override fun getItemCount() = weekDays.size

    fun updateWeekDays(newWeekDays: List<WeekDay>) {
        if (newWeekDays.size != 7) {
            throw IllegalArgumentException("weekDays list should contain exactly 7 elements")
        }
        this.weekDays = newWeekDays
        notifyDataSetChanged()
    }
}
