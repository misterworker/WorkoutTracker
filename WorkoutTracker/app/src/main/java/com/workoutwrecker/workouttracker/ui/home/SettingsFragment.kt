/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.workoutwrecker.workouttracker.MainActivity
import com.workoutwrecker.workouttracker.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val isNightModeEnabled = sharedPreferences.getBoolean("night_mode", false)
        val themePreference = sharedPreferences.getString("theme_preference", "default")
        val weightSystemPreference = sharedPreferences.getString("weight_system_preference", "kgs")

        val nightModeSwitcher = view.findViewById<SwitchCompat>(R.id.night_mode_switcher)
        nightModeSwitcher.isChecked = isNightModeEnabled

        val themeSelectionGroup = view.findViewById<RadioGroup>(R.id.theme_selection_group)
        val themeDefault = view.findViewById<RadioButton>(R.id.theme_default)
        val themeIce = view.findViewById<RadioButton>(R.id.theme_ice)
        val themeSilverNight = view.findViewById<RadioButton>(R.id.theme_silver_night)
        val themeNight = view.findViewById<RadioButton>(R.id.theme_night)
        val weightSystemSpinner = view.findViewById<Spinner>(R.id.weight_system_spinner)
        val weightSystems = resources.getStringArray(R.array.weight_systems_entries)

        // Set up ArrayAdapter for the Spinner
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            weightSystems
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weightSystemSpinner.adapter = adapter

        weightSystemSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedWeightSystem = weightSystems[position]
                val editor = sharedPreferences.edit()
                editor.putString("weight_system_preference", selectedWeightSystem)
                editor.apply()
                // Optionally, trigger other updates based on weight system change
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Set initial selection based on saved preference
        val initialPosition = weightSystems.indexOf(weightSystemPreference)
        if (initialPosition >= 0) {
            weightSystemSpinner.setSelection(initialPosition)
        }

        nightModeSwitcher.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPreferences.edit()
            editor.putBoolean("night_mode", isChecked)
            editor.apply()

            // Update the app's night mode immediately without recreating the activity
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putString("theme_preference", "night")
                themeSelectionGroup.check(R.id.theme_night)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putString("theme_preference", "default")
                themeSelectionGroup.check(R.id.theme_default)
            }

            // Preserve theme selection visibility
            if (isChecked) {
                themeDefault.visibility = View.GONE
                themeIce.visibility = View.GONE
                themeSilverNight.visibility = View.VISIBLE
                themeNight.visibility = View.VISIBLE
            } else {
                themeDefault.visibility = View.VISIBLE
                themeIce.visibility = View.VISIBLE
                themeSilverNight.visibility = View.GONE
                themeNight.visibility = View.GONE
            }
        }

        // Set initial visibility based on night mode
        if (isNightModeEnabled) {
            themeDefault.visibility = View.GONE
            themeIce.visibility = View.GONE
            themeSilverNight.visibility = View.VISIBLE
            themeNight.visibility = View.VISIBLE
        } else {
            themeDefault.visibility = View.VISIBLE
            themeIce.visibility = View.VISIBLE
            themeSilverNight.visibility = View.GONE
            themeNight.visibility = View.GONE
        }

        when (themePreference) {
            "silver_night" -> themeSelectionGroup.check(R.id.theme_silver_night)
            "night" -> themeSelectionGroup.check(R.id.theme_night)
            "default" -> themeSelectionGroup.check(R.id.theme_default)
            "ice" -> themeSelectionGroup.check(R.id.theme_ice)
        }

        themeSelectionGroup.setOnCheckedChangeListener { _, checkedId ->
            val editor = sharedPreferences.edit()
            when (checkedId) {
                R.id.theme_silver_night -> {
                    editor.putString("theme_preference", "silver_night")
                }
                R.id.theme_night -> {
                    editor.putString("theme_preference", "night")
                }
                R.id.theme_default -> {
                    editor.putString("theme_preference", "default")
                }
                R.id.theme_ice -> {
                    editor.putString("theme_preference", "ice")
                    Toast.makeText(requireContext(), "Reload the app to view the full theme!", Toast.LENGTH_LONG).show()
                }
            }

            editor.apply()

            // Apply the selected theme and recreate the activity to apply changes
            requireActivity().let {
                (it as MainActivity).setAppTheme(sharedPreferences.getString("theme_preference", "default"))
                it.recreate()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure the fragment does not pop from the back stack
        findNavController().popBackStack(R.id.navigation_settings, false)
    }
}
