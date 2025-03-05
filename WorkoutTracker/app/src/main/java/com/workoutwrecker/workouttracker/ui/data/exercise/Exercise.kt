/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.exercise

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String = "",
    var title: String = "",
    val type: String = "",
    val bodypart: String = "",
    val instructions: String = "",
    val category: String = "None",
    val private_: Boolean = false,
) : Parcelable {
    @Exclude
    fun getStability(): Int {
        return 0 // Or any default value
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(type)
        parcel.writeString(bodypart)
        parcel.writeString(instructions)
        parcel.writeString(category)
        parcel.writeByte(if (private_) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Exercise> {
        override fun createFromParcel(parcel: Parcel): Exercise {
            return Exercise(parcel)
        }

        override fun newArray(size: Int): Array<Exercise?> {
            return arrayOfNulls(size)
        }
    }
}
