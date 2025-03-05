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
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.ui.data.exercise.Exercise
import com.workoutwrecker.workouttracker.databinding.FragmentCreateExerciseBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID


@AndroidEntryPoint
class CreateExerciseFragment : Fragment() {

    private var _binding: FragmentCreateExerciseBinding? = null
    private val binding get() = _binding!!
    private val auth = Firebase.auth

    private val viewModel: CreateExerciseViewModel by viewModels()

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
        _binding = FragmentCreateExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = activity as? AppCompatActivity
        activity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSpinners()
        setupCreateButton()

        binding.etInstructions.addTextChangedListener(object : TextWatcher {
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
                val cursorPosition = binding.etInstructions.selectionStart
                val currentLineIndex = binding.etInstructions.layout?.getLineForOffset(cursorPosition) ?: 0

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
                binding.etInstructions.setText(formattedText)
                binding.etInstructions.setSelection(formattedText.length) // Move cursor to end
                isFormatting = false
            }
        })


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

        binding.etTypeConcat.setOnClickListener {
            typeAppendInformation()
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
            binding.spinnerType.adapter = adapter
        }
        binding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
            binding.spinnerBodypart.adapter = adapter
        }
        binding.spinnerBodypart.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
            binding.spinnerCategory.adapter = adapter
        }
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        binding.etTypeConcat.setText(typeAbbreviation)
    }

    private fun confirmLeaveFragment() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave_create_exercise_title)
            .setMessage(R.string.leave_create_exercise_message)
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

    private fun setupCreateButton() {
        binding.btnCreateExercise.setOnClickListener {
            val title = binding.etTitle.text.toString()
            val instructions = binding.etInstructions.text.toString().lines().map { line ->
                line.replace(Regex("^\\d+\\.\\s+"), "").trim() // Regex to match the pattern
            }.joinToString("\n")

            if (validateInputs(title, selectedType, selectedBodyPart, selectedCategory, selectedPrivate)) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    viewModel.getAllExercises(onSuccess = { exercisesList ->
                        val userExercises = exercisesList.filter { it.id.startsWith("user_") }
                        if (userExercises.size >= 10) {
                            Toast.makeText(requireContext(), "Max (10) Custom Created Exercises Reached.", Toast.LENGTH_SHORT).show()
                        } else {
                            val isDuplicate = exercisesList.any { it.title == title }
                            if (isDuplicate) {
                                Toast.makeText(requireContext(), "An exercise with this title already exists. Please choose a different title.", Toast.LENGTH_SHORT).show()
                            } else {
                                allTypeOptions = resources.getStringArray(R.array.type_options).toList()
                                val modifiedTitle = if (selectedType != null &&
                                    selectedType !in typeOptions &&
                                    !allTypeOptions.any { title.endsWith((" ($it)")) }) {
                                    "$title ($selectedType)"
                                } else {
                                    title
                                }

                                val newExercise = Exercise(
                                    id = "user_${UUID.randomUUID()}",
                                    title = modifiedTitle,
                                    type = selectedType!!,
                                    bodypart = selectedBodyPart!!,
                                    category = selectedCategory!!,
                                    private_ = selectedPrivate!!,
                                    instructions = instructions,
                                )
                                viewModel.createExercise(newExercise, onSuccess = {
                                    val action = CreateExerciseFragmentDirections.actionCreateExerciseToNavigationExercises()
                                    findNavController().navigate(action)
                                }, onFailure = {
                                    Toast.makeText(context, "Failed: cache create exercise", Toast.LENGTH_SHORT).show()
                                })
                            }
                        }
                    }, onFailure = { exception ->
                        Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Create Exercise Failure", "Error: ${exception.message}")
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
