/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.workoutwrecker.workouttracker.R

class TimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private var progress: Float = 1f

    init {
        // Retrieve the custom attribute
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TimerView)
        val progressColor = typedArray.getColor(R.styleable.TimerView_timerFillColor,
            ContextCompat.getColor(context, R.color.timer_color))
        typedArray.recycle()

        paint.color = progressColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width * progress
        canvas.drawRect(0f, 0f, width, height.toFloat(), paint)
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }
}
