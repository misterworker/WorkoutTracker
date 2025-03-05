/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentFilterExercisesSelectionBinding
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise

class FilterExerciseSelectionSepLaunchFragment : Fragment() {

    private var _binding: FragmentFilterExercisesSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectedTypes: List<String>
    private lateinit var selectedBodyParts: List<String>
    private lateinit var selectedExercises: List<Exercise>

    companion object {
        private const val ARG_SELECTED_TYPES = "selectedTypes"
        private const val ARG_SELECTED_BODY_PARTS = "selectedBodyParts"
        private const val ARG_SELECTED_EXERCISES = "selectedExercises"

        // Factory method to create a new instance of this fragment
        fun newInstance(
            selectedTypes: List<String> = emptyList(),
            selectedBodyParts: List<String> = emptyList(),
            selectedExercises: List<Exercise> = emptyList()  // Default to empty list
        ): FilterExerciseSelectionSepLaunchFragment {
            val fragment = FilterExerciseSelectionSepLaunchFragment()
            val args = Bundle().apply {
                putStringArray(ARG_SELECTED_TYPES, selectedTypes.toTypedArray())
                putStringArray(ARG_SELECTED_BODY_PARTS, selectedBodyParts.toTypedArray())
                putParcelableArrayList(ARG_SELECTED_EXERCISES, ArrayList(selectedExercises))
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register a callback for the back button press
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Custom back press handling
                    if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                        requireActivity().supportFragmentManager.popBackStack(
                            "StartFilterSelection",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            selectedTypes = it.getStringArray(ARG_SELECTED_TYPES)?.toList() ?: emptyList()
            selectedBodyParts = it.getStringArray(ARG_SELECTED_BODY_PARTS)?.toList() ?: emptyList()
            selectedExercises = it.getParcelableArrayList(ARG_SELECTED_EXERCISES) ?: emptyList()
        }

        _binding = FragmentFilterExercisesSelectionBinding.inflate(inflater, container, false)
        binding.fragmentToolbar.visibility = View.VISIBLE

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
            val selectedTypes = mutableListOf<String>()
            val selectedBodyParts = mutableListOf<String>()

            if (barbellCheckBox.isChecked) selectedTypes.add(getString(R.string.barbell))
            if (dumbbellCheckBox.isChecked) selectedTypes.add(getString(R.string.dumbbell))
            if (machineCheckBox.isChecked) selectedTypes.add(getString(R.string.machine))
            if (cableCheckBox.isChecked) selectedTypes.add(getString(R.string.cable))
            if (kettleBellCheckBox.isChecked) selectedTypes.add(getString(R.string.kettle_bell))
            if (weightPlateCheckBox.isChecked) selectedTypes.add(getString(R.string.weight_plate))
            if (resistanceBandCheckBox.isChecked) selectedTypes.add(getString(R.string.resistance_band))
            if (calisthenicsCheckBox.isChecked) selectedTypes.add(getString(R.string.calisthenics))
            if (typeOtherCheckBox.isChecked) selectedTypes.add(getString(R.string.other))

            if (armsCheckBox.isChecked) selectedBodyParts.add(getString(R.string.arms))
            if (legsCheckBox.isChecked) selectedBodyParts.add(getString(R.string.legs))
            if (chestCheckBox.isChecked) selectedBodyParts.add(getString(R.string.chest))
            if (backCheckBox.isChecked) selectedBodyParts.add(getString(R.string.body_back))
            if (fullBodyCheckBox.isChecked) selectedBodyParts.add(getString(R.string.full_body))
            if (shoulderBodyCheckBox.isChecked) selectedBodyParts.add(getString(R.string.shoulders))
            if (absCheckBox.isChecked) selectedBodyParts.add(getString(R.string.abs))
            if (bodyPartOtherCheckbox.isChecked) selectedBodyParts.add(getString(R.string.body_other))


            // Handle the selections
            Toast.makeText(
                context,
                "Selected Types: ${selectedTypes.joinToString()}\nSelected Body Parts: ${selectedBodyParts.joinToString()}",
                Toast.LENGTH_LONG
            ).show()
            //   selectedExercises = selectedExercises?.toList() ?: emptyList()
//            val bundle = Bundle().apply {
//                putParcelableArray("selectedExercises", selectedExercises.toTypedArray())
//                putStringArray("selectedTypes", selectedTypes.toTypedArray())
//                putStringArray("selectedBodyParts", selectedBodyParts.toTypedArray())
//            }

            // findNavController().navigate(R.id.action_filter_exercises_selection_to_navigation_exercise_selection, bundle)


            // Create fragment instance using newInstance method
            val fragment = ExerciseSelectionSepLaunchFragment.newInstance(
                selectedExercises = selectedExercises,
                selectedTypes = selectedTypes,  // Pass empty lists or null if you prefer
                selectedBodyParts = selectedBodyParts
            )

            // Perform fragment transaction to replace current fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    fragment,
                    "StartFilterSelection"
                ) // Replace with your container ID
                .addToBackStack(null)
                .commit()

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}