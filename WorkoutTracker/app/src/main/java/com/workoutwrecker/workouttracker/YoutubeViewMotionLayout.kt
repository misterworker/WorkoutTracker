/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.internal.managers.ViewComponentManager
import kotlin.math.abs


@AndroidEntryPoint
class YoutubeViewMotionLayout(
    context: Context,
    attributeSet: AttributeSet? = null
) : MotionLayout(context, attributeSet) {


    private val viewToDetectTouch by lazy {
        findViewById<View>(R.id.background_view)
    }

    private val viewRect = Rect()
    private var hasTouchStarted = false
    private val transitionListenerList = mutableListOf<TransitionListener?>()


    init {

        super.setTransitionListener(object : TransitionListener {
            override fun onTransitionTrigger(
                p0: MotionLayout?,
                p1: Int,
                p2: Boolean,
                p3: Float
            ) {
            }

            override fun onTransitionStarted(
                p0: MotionLayout?,
                p1: Int,
                p2: Int
            ) {

            }

            override fun onTransitionChange(
                p0: MotionLayout?,
                p1: Int,
                p2: Int,
                p3: Float
            ) {
                transitionListenerList.filterNotNull()
                    .forEach { it.onTransitionChange(p0, p1, p2, p3) }

                if (p1 == p0?.startState &&
                    p2 == p0.endState
                ) {
                }

                val mainActivity = (context as? ViewComponentManager.FragmentContextWrapper)
                    ?.baseContext as? MainActivity
                mainActivity?.findViewById<MotionLayout>(R.id.main_container)?.progress = abs(progress)
                Log.d("youtubeContext", "$mainActivity")

            }

            override fun onTransitionCompleted(
                p0: MotionLayout?,
                p1: Int
            ) {

                transitionListenerList.filterNotNull()
                    .forEach { it.onTransitionCompleted(p0, p1) }

                hasTouchStarted = false
            }
        })

    }

    override fun setTransitionListener(listener: TransitionListener?) {
        addTransitionListener(listener)
    }

    override fun addTransitionListener(listener: TransitionListener?) {
        transitionListenerList += listener
    }

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {

//                val closeRect = Rect()
//                val closeImageView = findViewById<View>(R.id.iv_close)
//                closeImageView.getHitRect(closeRect)
//                val hasTouchCloseImageView =
//                    closeRect.contains(event?.x?.toInt() ?: 0, event?.y?.toInt() ?: 0)
//
                val cancelBtnRect = Rect()
                val cancelBtnView = findViewById<View>(R.id.cancel_workout_button_collapsed_mode)
                cancelBtnView.getHitRect(cancelBtnRect)
                val hasTouchCancelBtnView =
                    cancelBtnRect.contains(event.x.toInt(), event.y.toInt())

//                if (!hasTouchCloseImageView && !hasTouchPlayPauseView) {
//                    transitionToEnd()
//                }

                val recyclerViewRect = Rect()
                val recyclerView = findViewById<View>(R.id.recycler_view_workout_exercises)
                recyclerView.getHitRect(recyclerViewRect)
                val hasTouchRecyclerView =
                    recyclerViewRect.contains(event.x.toInt(), event.y.toInt())

                if (!hasTouchRecyclerView && !hasTouchCancelBtnView) {
                    transitionToEnd()
                }
                return false
            }
        })


    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)   //This ensures the Mini Player is maximised on single tap

        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hasTouchStarted = false
                return super.onTouchEvent(event)
            }
        }
        if (!hasTouchStarted) {
            viewToDetectTouch.getHitRect(viewRect)
            hasTouchStarted = viewRect.contains(event.x.toInt(), event.y.toInt())
        }
        return hasTouchStarted && super.onTouchEvent(event)
    }


    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return !onTouchEvent((event))
    }
}