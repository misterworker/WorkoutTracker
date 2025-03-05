/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.history

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.google.firebase.firestore.Exclude

@Entity(tableName = "history_workout_table")
data class HistoryWorkout(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val length: String = "",
    var exercises: List<WorkoutExercise> = emptyList(),
) : Parcelable {

    @Exclude
    fun getStability(): Int {
        return 0 // Or any default value
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createTypedArrayList(WorkoutExercise) ?: emptyList(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeLong(date)
        parcel.writeString(notes)
        parcel.writeString(length)
        parcel.writeTypedList(exercises)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<HistoryWorkout> {
        override fun createFromParcel(parcel: Parcel): HistoryWorkout {
            return HistoryWorkout(parcel)
        }

        override fun newArray(size: Int): Array<HistoryWorkout?> {
            return arrayOfNulls(size)
        }
    }

    fun getFormattedDate(): String {
        val dateTime = java.time.Instant.ofEpochMilli(date)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

}

