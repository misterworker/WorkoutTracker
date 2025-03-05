/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.record

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface PersonalRecordDao {
    @Query("SELECT * FROM pr_table")
    suspend fun getAllPersonalRecords(): List<PersonalRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecords(personalRecord: List<PersonalRecord>)

    @Update
    suspend fun updatePersonalRecord(personalRecord: PersonalRecord)

    @Query("DELETE FROM pr_table")
    suspend fun clearPersonalRecords()

    @Delete
    suspend fun deletePersonalRecord(personalRecord: PersonalRecord)

    @Query("SELECT * FROM pr_table WHERE id = :recordId")
    suspend fun getPersonalRecordById(recordId: String): PersonalRecord

}
