/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.exercises

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentUpdateExerciseBinding
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class UpdateExerciseFragment : Fragment() {

    private var _binding: FragmentUpdateExerciseBinding? = null
    private val binding get() = _binding!!
    private val auth = Firebase.auth
    private val args: UpdateExerciseFragmentArgs by navArgs()
    private val id: String by lazy { args.id }

    private val viewModel: UpdateExerciseViewModel by viewModels()

    private var selectedType: String? = null
    private var selectedBodyPart: String? = null
    private var selectedCategory: String? = null
    private var selectedPrivate: Boolean? = false

    private val typeOptions: List<String> = listOf("Other", "Calisthenics", "Resistance Band")
    private lateinit var allTypeOptions: List<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as? AppCompatActivity
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadExerciseDetails(id)
        setupSpinners()
        setupUpdateButton()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmLeaveFragment()
        }
        // Register the MenuProvider
        activity?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // No need to inflate a menu resource file since we only need the back button
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        confirmLeaveFragment()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.etUpdateTypeConcat.setOnClickListener {
            typeAppendInformation()
        }

        binding.privateQuestion.setOnClickListener{
            privateQuestion()
        }

        binding.etUpdateInstructions.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false // To prevent recursive calls
            private var lastCharWasNewline = false // Track if the last character was a newline

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Check if the last character is a newline
                if (count > 0 && s?.get(start) == '\n') {
                    lastCharWasNewline = true
                } else {
                    lastCharWasNewline = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || !lastCharWasNewline) return // Prevent recursion and check for newline

                val text = s.toString()
                val lines = text.split("\n").map { it.trim() }
                // Get the current cursor position
                val cursorPosition = binding.etUpdateInstructions.selectionStart
                val currentLineIndex = binding.etUpdateInstructions.layout?.getLineForOffset(cursorPosition) ?: 0

                isFormatting = true

                // Format lines with numbering, only if they are not empty
                val formattedText = lines.mapIndexed { index, line ->
                    if (line.isNotEmpty() && currentLineIndex-1 == index &&
                        !line.matches(Regex("^\\d+\\.\\s*.*$")))
                    {
                        "${index + 1}. $line"
                    }
                    else line
                }.joinToString("\n") // Preserve the last empty line

                // Set the formatted text back to the EditText
                binding.etUpdateInstructions.setText(formattedText)
                binding.etUpdateInstructions.setSelection(formattedText.length) // Move cursor to end
                isFormatting = false
            }
        })
    }

    private fun confirmLeaveFragment() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_update_exercise_title)
            .setMessage(R.string.leave_update_exercise_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                findNavController().popBackStack()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun privateQuestion() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.private_question_title)
            .setMessage(R.string.private_question_message)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun typeAppendInformation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.type_text)
            .setMessage(R.string.type_text_message)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun loadExerciseDetails(exerciseId: String) {
        viewModel.getExerciseById(exerciseId, onSuccess = { exercise ->
            binding.etUpdateTitle.setText(exercise.title)
            val allInstructions = exercise.instructions
            val lines = allInstructions.split("\n").map { it.trim() }
            val newInstructions = lines.mapIndexed { index, line ->
                if (line.isNotEmpty() &&
                    !line.matches(Regex("^\\d+\\.\\s*.*$"))) {
                    "${index + 1}. $line" // Add numbering to the line
                } else {
                    line // Keep the original line
                }
            }.joinToString("\n") // Combine the lines back into a single string
            binding.etUpdateInstructions.setText(newInstructions)
            setSelectedSpinnerItem(binding.updateSpinnerType, R.array.type_options, exercise.type)
            setSelectedSpinnerItem(binding.updateSpinnerBodypart, R.array.bodypart_options, exercise.bodypart)
            setSelectedSpinnerItem(binding.updateSpinnerCategory, R.array.category_options, exercise.category)
            val private = if (exercise.private_) {
                getString(R.string.true_)
            } else {
                getString(R.string.false_)
            }
            Log.d("UpdateExerciseFragment", private)
        }, onFailure = {
            Toast.makeText(requireContext(), "Failed to load exercise details", Toast.LENGTH_SHORT).show()
            Log.e("UpdateExerciseFragment", "Failed to load exercise details", it)
        })
    }

    private fun setSelectedSpinnerItem(spinner: Spinner, arrayRes: Int, value: String) {
        val adapter = ArrayAdapter.createFromResource(requireContext(), arrayRes, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        val position = adapter.getPosition(value)
        if (position >= 0) {
            spinner.setSelection(position)
        }
    }

    private fun setupSpinners() {
        // Set up type spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.type_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.updateSpinnerType.adapter = adapter
        }
        binding.updateSpinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedType = if (position == 0) null else parent.getItemAtPosition(position) as String
                updateTypeConcat()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedType = null
                updateTypeConcat()
            }
        }

        // Set up body part spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.bodypart_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.updateSpinnerBodypart.adapter = adapter
        }
        binding.updateSpinnerBodypart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedBodyPart = if (position == 0) null else parent.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedBodyPart = null
            }
        }

        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.category_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.updateSpinnerCategory.adapter = adapter
        }
        binding.updateSpinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = if (position == 0) null else parent.getItemAtPosition(position) as String
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCategory = null
            }
        }

        binding.switchPrivate.setOnCheckedChangeListener { _, isChecked ->
            selectedPrivate = isChecked
        }

        binding.privateQuestion.setOnClickListener{
            privateQuestion()
        }
    }

    private fun updateTypeConcat() {
        val typeAbbreviation = if (selectedType != null && selectedType !in typeOptions) {
            ("($selectedType)")
        } else {
            ""
        }
        binding.etUpdateTypeConcat.setText(typeAbbreviation)
    }

    private fun setupUpdateButton() {
        binding.btnUpdateExercise.setOnClickListener {
            val title = binding.etUpdateTitle.text.toString()
            val instructions = binding.etUpdateInstructions.text.toString().lines().map { line ->
                line.replace(Regex("^\\d+\\.\\s+"), "").trim() // Regex to match the pattern
            }.joinToString("\n")

            if (validateInputs(title, selectedType, selectedBodyPart, selectedCategory, selectedPrivate)) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    viewModel.getAllExercises(onSuccess = { exercisesList ->
                        val isDuplicate = exercisesList.any { it.title == title && it.id != id }
                        val currentExercise = exercisesList.find { it.id == id }
                        if (isDuplicate) {
                            Toast.makeText(requireContext(), "An exercise with this title already exists. Please choose a different title.", Toast.LENGTH_SHORT).show()
                        }
                        // Check if there are any changes
                        else if (currentExercise != null && currentExercise.title == title
                            && currentExercise.instructions == instructions
                            && currentExercise.type == selectedType
                            && currentExercise.bodypart == selectedBodyPart
                            && currentExercise.category == selectedCategory
                            && currentExercise.private_ == selectedPrivate){
                            Toast.makeText(requireContext(), "No changes to update", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            allTypeOptions = resources.getStringArray(R.array.type_options).toList()
                            // Remove any existing type abbreviation from the title
                            val cleanTitle = title.replace(Regex("\\s*\\(.*\\)\$"), "")

                            val modifiedTitle = if (selectedType != null &&
                                selectedType !in typeOptions &&
                                !allTypeOptions.any { cleanTitle.endsWith((" ($it)")) }) {
                                "$cleanTitle ($selectedType)"
                            } else {
                                cleanTitle
                            }

                            val updatedExercise = Exercise(
                                id = id,
                                title = modifiedTitle,
                                type = selectedType!!,
                                bodypart = selectedBodyPart!!,
                                category = selectedCategory!!,
                                private_ = selectedPrivate!!,
                                instructions = instructions)
                            viewModel.updateExercise(updatedExercise, onSuccess = {
                                val action = UpdateExerciseFragmentDirections.actionNavigationUpdateExerciseToNavigationExercises()
                                findNavController().navigate(action)
                            }, onFailure = {
                                Toast.makeText(context, "Failed: cache update exercise", Toast.LENGTH_SHORT).show()
                            })
                        }

                    }, onFailure = { exception ->
                        Toast.makeText(requireContext(), "Error updating exercise", Toast.LENGTH_SHORT).show()
                        Log.e("Update Exercise Failure", "Error: ${exception.message}")
                    })
                }
            } else {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(title: String, type: String?, bodyPart: String?, category: String?, private: Boolean?): Boolean {

        return !TextUtils.isEmpty(title) && type != null && bodyPart!= null && category != null && private != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
