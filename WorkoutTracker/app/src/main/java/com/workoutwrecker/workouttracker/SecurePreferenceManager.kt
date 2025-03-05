/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.SharedPreferences

class SecurePreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences = createEncryptedSharedPreferences(context)

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        // Create or retrieve the master key
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs", // File name for encrypted shared preferences
            masterKey, // Master key for encryption/decryption
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, // Key encryption scheme
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM // Value encryption scheme
        )
    }

    // Store a string value securely
    fun storeString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    // Retrieve a string value securely
    fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    // Store an integer value securely
    fun storeInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    // Retrieve an integer value securely
    fun getInt(key: String): Int {
        return sharedPreferences.getInt(key, -1) // Default value is -1
    }

    fun clearData() {
        sharedPreferences.edit().clear().apply()
    }

    // Additional methods can be created for other data types (boolean, float, etc.)
}
