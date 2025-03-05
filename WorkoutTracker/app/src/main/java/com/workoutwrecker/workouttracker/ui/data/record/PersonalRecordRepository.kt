/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.data.record

import android.util.Log
import javax.inject.Inject

class PersonalRecordRepository @Inject constructor(
    private val personalRecordDao: PersonalRecordDao,
) {
    private var cachedPersonalRecords: List<PersonalRecord> = emptyList()

    suspend fun updatePersonalRecordInLocalCache(personalRecord: PersonalRecord) {
        personalRecordDao.updatePersonalRecord(personalRecord)
    }

    suspend fun addPersonalRecordToLocalCache(personalRecord: PersonalRecord) {
        personalRecordDao.insertPersonalRecords(listOf(personalRecord))
    }

    suspend fun getAllPersonalRecordsFromCache(): List<PersonalRecord> {
        cachedPersonalRecords = personalRecordDao.getAllPersonalRecords()
        Log.d("PersonalRecordRepository", "Cached records: $cachedPersonalRecords")
        return cachedPersonalRecords
    }

    suspend fun deletePersonalRecordFromLocalCache(personalRecord: PersonalRecord) {
        personalRecordDao.deletePersonalRecord(personalRecord)
    }

    suspend fun clearLocalCache() {
        personalRecordDao.clearPersonalRecords()
        cachedPersonalRecords = emptyList()
    }
}
