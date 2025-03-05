/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.login

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
import com.workoutwrecker.workouttracker.databinding.FragmentRegisterBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.UUID

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val auth = Firebase.auth
    private var signOut = true
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("register_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("sign_out_flag", true)
            putString("register_email", "NA")
            putString("register_password", "NA")
            putString("register_page", "NA")
            apply()
        }

        binding.registerButton.setOnClickListener {
            val email = binding.registerEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                if (isValidEmail(email)) {
                    if (!binding.registerButton.isEnabled) return@setOnClickListener
                    showLoading(true)
                    sendVerificationEmail(email)
                } else {
                    Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please enter an email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun sendVerificationEmail(email: String) {
        val password =  UUID.randomUUID().toString()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("RegisterFragment", "successful account creation")
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Log.d("RegisterFragment", "Verify entered")
                            with(sharedPreferences.edit()) {
                                putString("register_email", email)
                                putString("register_password", password)
                                putBoolean("unverified_account_created", false)
                                apply()
                            }
                            Toast.makeText(context, "Verification email sent. Please check your email.", Toast.LENGTH_SHORT).show()
                            signOut = false
                            val action = RegisterFragmentDirections.actionNavigationRegisterToNavigationVerifyEmail()
                            findNavController().navigate(action)
                        } else {
                            showLoading(false)
                            Toast.makeText(context, "Failed to send verification email: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            Log.d("RegisterFragment", "Verify unentered")
                        }
                    }
                } else {
                    Log.d("RegisterFragment", "unsuccessful account creation")
                    showLoading(false)
                    Toast.makeText(context, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.d("RegisterFragment", "Verify unentered")
                }
            }
        Log.d("RegisterFragment", "sendVerificationEmail: $email")
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
        binding.registerButton.isEnabled = !isLoading

    }

    override fun onDestroyView() {
        if (signOut) {
            auth.signOut()
        }
        super.onDestroyView()
        _binding = null
    }
}
