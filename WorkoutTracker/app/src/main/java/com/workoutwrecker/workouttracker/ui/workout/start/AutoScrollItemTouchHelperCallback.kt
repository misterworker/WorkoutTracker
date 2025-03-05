/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AutoScrollItemTouchHelperCallback(
    private val adapter: ItemTouchHelperAdapter,
    private val recyclerView: RecyclerView,
    private val onDragEnd: () -> Unit,
) : ItemTouchHelper.Callback() {

    private val scrollSpeed = 20  // Adjust the scroll speed as needed
    private var longPressDragEnabled = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = if (longPressDragEnabled) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }

    fun setLongPressDragEnabled(enabled: Boolean) {
        longPressDragEnabled = enabled
    }

    override fun isLongPressDragEnabled(): Boolean {
        return longPressDragEnabled
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            viewHolder?.itemView?.alpha = 0.7f
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            viewHolder?.itemView?.alpha = 1.0f
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f
        onDragEnd()
        isLongPressDragEnabled = false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
            autoScroll(viewHolder)
        }
    }

    private fun autoScroll(viewHolder: RecyclerView.ViewHolder) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val viewTop = viewHolder.itemView.top
        val viewBottom = viewHolder.itemView.bottom
        val parentHeight = recyclerView.height

        if (viewTop < 0) {
            recyclerView.scrollBy(0, -scrollSpeed)
        } else if (viewBottom > parentHeight) {
            recyclerView.scrollBy(0, scrollSpeed)
        }
    }
}
