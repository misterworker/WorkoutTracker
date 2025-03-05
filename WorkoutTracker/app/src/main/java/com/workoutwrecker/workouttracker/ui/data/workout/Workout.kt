/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.workout

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.firestore.Exclude

@Entity(tableName = "workout_table")
data class Workout(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val notes: String = "",
    var programName: String = "Unspecified",
    var exercises: List<WorkoutExercise> = emptyList(),
    var order: Int = 0,
    var type: String = "Weights",


    ) : Parcelable {

    @Exclude
    fun getStability(): Int {
        return 0 // Or any default value
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "Unspecified",
        parcel.createTypedArrayList(WorkoutExercise) ?: emptyList(),
        parcel.readInt(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(notes)
        parcel.writeTypedList(exercises)
        parcel.writeInt(order)
        parcel.writeString(type)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Workout> {
        override fun createFromParcel(parcel: Parcel): Workout {
            return Workout(parcel)
        }

        override fun newArray(size: Int): Array<Workout?> {
            return arrayOfNulls(size)
        }
    }

}

data class WorkoutExercise(
    val id: String = "",
    val exerciseid: String = "",
    var sets: ArrayList<String> = arrayListOf(),
    var reps: ArrayList<Int> = arrayListOf(),
    var weights: ArrayList<Float> = arrayListOf(),
    val title: String = "",
    var order: Int = 0,
    var notes: String = "",
    var completion: ArrayList<Boolean> = arrayListOf(),
    var max: Int = 0,
    var bestPace: Float = 0f,
    var volume: Int = 0,
    var distances: ArrayList<Float> = arrayListOf(),
    var times: ArrayList<Float> = arrayListOf(),
    var restTimer: Long = 0L,
    var supersetid: Int? = null,
    var completedsetscount: Int = 0,
    var completedreps: Int = 0,
    var totalDistance: Float = 0f,
    var totalTime: Float = 0f,
    val category: String = "",
    var isNotesVisible: Boolean = false,
    var isSetsVisible: Boolean = true,
    val date: Long = System.currentTimeMillis(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArray()?.toCollection(ArrayList()) ?: arrayListOf(),
        parcel.createIntArray()?.toCollection(ArrayList()) ?: arrayListOf(),
        parcel.createFloatArray()?.toCollection(ArrayList()) ?: arrayListOf(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.createBooleanArray()?.toCollection(ArrayList()) ?: arrayListOf(),
        parcel.readInt(), // max
        parcel.readFloat(),
        parcel.readInt(), // volume
        parcel.createFloatArray()?.toCollection(ArrayList()) ?: arrayListOf(), // Distance for duration type exercises
        parcel.createFloatArray()?.toCollection(ArrayList()) ?: arrayListOf(), // Time for duration type exercises
        parcel.readLong(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readFloat(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(exerciseid)
        parcel.writeStringList(sets)
        parcel.writeIntArray(reps.toIntArray())
        parcel.writeFloatArray(weights.toFloatArray())
        parcel.writeString(title)
        parcel.writeInt(order)
        parcel.writeString(notes)
        parcel.writeBooleanArray(completion.toBooleanArray())
        parcel.writeInt(max)
        parcel.writeInt(volume)
        parcel.writeFloatArray(distances.toFloatArray())
        parcel.writeFloatArray(times.toFloatArray())
        parcel.writeLong(restTimer)
        parcel.writeValue(supersetid)
        parcel.writeInt(completedsetscount)
        parcel.writeInt(completedreps)
        parcel.writeFloat(totalDistance)
        parcel.writeFloat(totalTime)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WorkoutExercise> {
        override fun createFromParcel(parcel: Parcel): WorkoutExercise {
            return WorkoutExercise(parcel)
        }

        override fun newArray(size: Int): Array<WorkoutExercise?> {
            return arrayOfNulls(size)
        }
    }
}

