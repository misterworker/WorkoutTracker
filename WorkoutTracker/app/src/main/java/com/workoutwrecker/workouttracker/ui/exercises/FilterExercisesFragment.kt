/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentFilterExercisesBinding

class FilterExercisesFragment : Fragment() {

    private var _binding: FragmentFilterExercisesBinding? = null
    private val binding get() = _binding!!
    private val args: FilterExercisesFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterExercisesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the views
        val barbellCheckBox: CheckBox = binding.checkboxBarbell
        val dumbbellCheckBox: CheckBox = binding.checkboxDumbbell
        val machineCheckBox: CheckBox = binding.checkboxMachine
        val cableCheckBox: CheckBox = binding.checkboxCable
        val kettleBellCheckBox: CheckBox = binding.checkboxKettleBell
        val weightPlateCheckBox: CheckBox = binding.checkboxWeightPlate
        val resistanceBandCheckBox: CheckBox = binding.checkboxResistanceBand
        val calisthenicsCheckBox: CheckBox = binding.checkboxCalisthenics
        val typeOtherCheckBox: CheckBox = binding.checkboxTypeOther
        val armsCheckBox: CheckBox = binding.checkboxArms
        val legsCheckBox: CheckBox = binding.checkboxLegs
        val chestCheckBox: CheckBox = binding.checkboxChest
        val backCheckBox: CheckBox = binding.checkboxBack
        val fullBodyCheckBox: CheckBox = binding.checkboxFullBody
        val shoulderBodyCheckBox: CheckBox = binding.checkboxShoulders
        val absCheckBox: CheckBox = binding.checkboxAbs
        val bodyPartOtherCheckbox: CheckBox = binding.checkboxBodypartOther
        val doneButton = binding.doneButton

        val selectedTypes = args.selectedTypes?.toList() ?: emptyList()
        val selectedBodyParts = args.selectedBodyParts?.toList() ?: emptyList()

        // Set checkbox states
        barbellCheckBox.isChecked = getString(R.string.barbell) in selectedTypes
        dumbbellCheckBox.isChecked = getString(R.string.dumbbell) in selectedTypes
        machineCheckBox.isChecked = getString(R.string.machine) in selectedTypes
        cableCheckBox.isChecked = getString(R.string.cable) in selectedTypes
        kettleBellCheckBox.isChecked = getString(R.string.kettle_bell) in selectedTypes
        weightPlateCheckBox.isChecked = getString(R.string.weight_plate) in selectedTypes
        resistanceBandCheckBox.isChecked = getString(R.string.resistance_band) in selectedTypes
        calisthenicsCheckBox.isChecked = getString(R.string.calisthenics) in selectedTypes
        typeOtherCheckBox.isChecked = getString(R.string.other) in selectedTypes

        armsCheckBox.isChecked = getString(R.string.arms) in selectedBodyParts
        legsCheckBox.isChecked = getString(R.string.legs) in selectedBodyParts
        chestCheckBox.isChecked = getString(R.string.chest) in selectedBodyParts
        backCheckBox.isChecked = getString(R.string.body_back) in selectedBodyParts
        fullBodyCheckBox.isChecked = getString(R.string.full_body) in selectedBodyParts
        shoulderBodyCheckBox.isChecked = getString(R.string.shoulders) in selectedBodyParts
        absCheckBox.isChecked = getString(R.string.abs) in selectedBodyParts
        bodyPartOtherCheckbox.isChecked = getString(R.string.body_other) in selectedBodyParts

        // Set up done button click listener
        doneButton.setOnClickListener {
            val selectedFilterTypes = mutableListOf<String>()
            val selectedFilterBodyParts = mutableListOf<String>()

            if (barbellCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.barbell))
            if (dumbbellCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.dumbbell))
            if (machineCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.machine))
            if (cableCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.cable))
            if (kettleBellCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.kettle_bell))
            if (weightPlateCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.weight_plate))
            if (resistanceBandCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.resistance_band))
            if (calisthenicsCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.calisthenics))
            if (typeOtherCheckBox.isChecked) selectedFilterTypes.add(getString(R.string.other))

            if (armsCheckBox.isChecked) selectedFilterBodyParts.add(getString(R.string.arms))
            if (legsCheckBox.isChecked) selectedFilterBodyParts.add(getString(R.string.legs))
            if (chestCheckBox.isChecked) selectedFilterBodyParts.add(getString(R.string.chest))
            if (backCheckBox.isChecked) selectedFilterBodyParts.add(getString(R.string.body_back))
            if (fullBodyCheckBox.isChecked) selectedFilterBodyParts.add(getString(R.string.full_body))
            if (shoulderBodyCheckBox.isChecked) selectedFilterBodyParts.add(getString(R.string.shoulders))
            if (absCheckBox.isChecked) selectedFilterBodyParts.add(getString(R.string.abs))
            if (bodyPartOtherCheckbox.isChecked) selectedFilterBodyParts.add(getString(R.string.body_other))


            // Handle the selections
            Toast.makeText(
                context,
                "Selected Types: ${selectedFilterTypes.joinToString()}\nSelected Body Parts: ${selectedFilterBodyParts.joinToString()}",
                Toast.LENGTH_LONG
            ).show()
            val action = FilterExercisesFragmentDirections.actionFilterExercisesToNavigationExercises(selectedFilterBodyParts.toTypedArray(), selectedFilterTypes.toTypedArray())
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
