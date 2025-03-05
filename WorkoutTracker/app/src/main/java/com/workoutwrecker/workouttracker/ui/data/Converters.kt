/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data

import android.util.Log
import androidx.room.TypeConverter
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromWorkoutExerciseList(value: List<WorkoutExercise>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWorkoutExerciseList(value: String): List<WorkoutExercise> {
        val listType = object : com.google.common.reflect.TypeToken<List<WorkoutExercise>>() {}.type
        return Gson().fromJson(value, listType)
    }

    // Converters for ArrayList<String>
    @TypeConverter
    fun fromStringList(value: ArrayList<String>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return gson.fromJson(value, listType) ?: arrayListOf()
    }

    // Converters for ArrayList<Int>
    @TypeConverter
    fun fromIntList(value: ArrayList<Int>?): String {
        Log.d("Converters", "fromIntList: $value")
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): ArrayList<Int> {
        Log.d("Converters", "toIntList: $value")
        val listType = object : TypeToken<ArrayList<Int>>() {}.type
        return gson.fromJson(value, listType) ?: arrayListOf()
    }

    // Converters for ArrayList<Float>
    @TypeConverter
    fun fromFloatList(value: ArrayList<Float>?): String {
        Log.d("Converters", "fromFloatList: $value")
        return gson.toJson(value)
    }

    @TypeConverter
    fun toFloatList(value: String): ArrayList<Float> {
        Log.d("Converters", "toFloatList: $value")
        val listType = object : TypeToken<ArrayList<Float>>() {}.type
        return gson.fromJson(value, listType) ?: arrayListOf()
    }

    // Converters for ArrayList<Boolean>
    @TypeConverter
    fun fromBooleanList(value: ArrayList<Boolean>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toBooleanList(value: String): ArrayList<Boolean> {
        val listType = object : TypeToken<ArrayList<Boolean>>() {}.type
        return gson.fromJson(value, listType) ?: arrayListOf()
    }
}
