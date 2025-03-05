/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.login

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentLoginCreateAccountBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.SecurePreferenceManager

class AccountCreationFragment : Fragment() {

    private var _binding: FragmentLoginCreateAccountBinding? = null
    private val binding get() = _binding!!
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private var signOut = true
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var securePrefManager: SecurePreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("register_prefs", Context.MODE_PRIVATE)
        securePrefManager = SecurePreferenceManager(requireContext())

        with(sharedPreferences.edit()) {
            putBoolean("sign_out_flag", true)
            putString("register_page", "new_password")
            apply()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmLeaveFragment()
        }

        binding.registerUsername.setText(securePrefManager.getString("unverified_username"))
        binding.registerPassword.setText(securePrefManager.getString("unverified_password"))
        binding.reRegisterPassword.setText(securePrefManager.getString("unverified_password"))

        binding.createButton.setOnClickListener {
            binding.passwordRequirementText.visibility = View.GONE
            val password = binding.registerPassword.text.toString()
            val username = binding.registerUsername.text.toString()
            val rePassword = binding.reRegisterPassword.text.toString()
            if (password.isNotEmpty() && username.isNotEmpty()) {
                if (password == rePassword) {
                    if (isPasswordStrong(password)) {
                        if (password != username) {
                            if (!binding.createButton.isEnabled) return@setOnClickListener
                            updatePasswordAndUsername(password, username)
                        } else {
                            Toast.makeText(context, "Password cannot be the same as the username", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        binding.passwordRequirementText.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please enter both password and username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        // Password should be at least 8 characters long and include a mix of letters, numbers, and special characters
        val passwordPattern = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@\$!%*?&#])[A-Za-z\\d@\$!%*?&#]{8,}\$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun updatePasswordAndUsername(password: String, username: String){
        showLoading(true)
        val registerEmail = sharedPreferences.getString("register_email", "NA")
        val registerPassword = sharedPreferences.getString("register_password", "NA")
        if (registerEmail == null || registerPassword == null) {
            return
        }
        auth.signInWithEmailAndPassword(registerEmail, registerPassword).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    val userId = it.uid
                    db.collection("users")
                        .whereEqualTo("username", username)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {// Username already exists
                                Toast.makeText(context, "Username already taken, please choose another", Toast.LENGTH_SHORT).show()
                                showLoading(false)
                                return@addOnSuccessListener
                            }
                            user.updatePassword(password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userInfo = hashMapOf(
                                        "username" to username,
                                        "userId" to userId
                                    )
                                    db.collection("users").document(userId)
                                        .set(userInfo)
                                        .addOnSuccessListener {
                                            val action =
                                                AccountCreationFragmentDirections.actionAccountCreationFragmentToNavigationSignIn()
                                            Toast.makeText(
                                                context,
                                                "User registered successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            signOut = false
                                            findNavController().navigate(action)
                                            showLoading(false)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                context,
                                                "Failed to store user info: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showLoading(false)
                                        }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to update password: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showLoading(false)
                                }
                            }.addOnFailureListener { exception ->
                                Toast.makeText(
                                    context,
                                    "Failed to update password: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showLoading(false)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error checking username: ${e.message}", Toast.LENGTH_SHORT).show()
                            showLoading(false)
                        }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isLoading) {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                findNavController().popBackStack()
            }
        }
        else {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}
        }
        binding.dimView.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.createButton.isEnabled = !isLoading
    }

    private fun confirmLeaveFragment() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.return_login_title)
            .setMessage(R.string.return_login_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                if (!findNavController().popBackStack(R.id.navigation_sign_in, false)){
                    with(sharedPreferences.edit()) {
                        putString("register_page", "NA")
                        apply()
                    }
                    findNavController().navigate(R.id.navigation_sign_in)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    override fun onDestroyView() {
        if (signOut) {
            auth.signOut()
        }
        super.onDestroyView()
        _binding = null
    }
}
