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
import com.workoutwrecker.workouttracker.databinding.FragmentVerifyEmailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.airbnb.lottie.LottieDrawable
import com.workoutwrecker.workouttracker.R

class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!
    private val auth = Firebase.auth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val animationView = binding.lottieAnimationView

        enter_verify_email()

        sharedPreferences = requireContext().getSharedPreferences("register_prefs", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("register_email", "NA")
        val password = sharedPreferences.getString("register_password", "NA")
        if (email == null || password == null) {
            findNavController().navigate(R.id.navigation_sign_in)
        }
        else {
            Log.d("VerifyEmailFragment", "Email: $email")
            // Perform sign in and wait for completion
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        // Sign-in successful
                        Log.d("VerifyEmailFragment", "Sign-in successful")
                    } else {
                        // Sign-in failed
                        Toast.makeText(context, "Sign-in failed: ${signInTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        Log.d("VerifyEmailFragment", "Sign-in failed,  ${signInTask.exception?.message}")
                        findNavController().navigate(R.id.navigation_sign_in)
                    }
                }
        }
        val unverifiedAccountCreated = sharedPreferences //Prioritise account is alr created (avoid abuse)
            .getBoolean("unverified_account_created", true)
        if (!unverifiedAccountCreated){binding.unverifiedAccountCreationLink.visibility = View.VISIBLE}

        binding.unverifiedAccountCreationLink.setOnClickListener {
            val action = VerifyEmailFragmentDirections
                .actionNavigationVerifyEmailFragmentToNavigationUnverifiedAccountCreation()
            findNavController().navigate(action)
        }

        with(sharedPreferences.edit()) {
            putString("register_page", "verify")
            putBoolean("sign_out_flag", true)
            apply()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            confirmLeaveFragment()
        }

        animationView.repeatCount = LottieDrawable.INFINITE
        var sentEmail = false

        binding.verifyButton.setOnClickListener {
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (!task.isSuccessful || sentEmail) {
                    Log.d("VerifyEmailFragment", "Email unverified")
                    Toast.makeText(context, "Email not verified. Please check your email.", Toast.LENGTH_SHORT).show()
                    sentEmail = false
                    return@addOnCompleteListener
                }
                Log.d("VerifyEmailFragment", "Task Successful")
                sentEmail = true
                if (auth.currentUser?.isEmailVerified == true) {
                    Log.d("VerifyEmailFragment", "Email verified")
                    Toast.makeText(context, "Email verified successfully.", Toast.LENGTH_SHORT).show()
                    val action = VerifyEmailFragmentDirections.actionNavigationVerifyEmailFragmentToNavigationAccountCreation()
                    findNavController().navigate(action)
                }
                else {
                    Log.d("VerifyEmailFragment", "Task Unsuccessful")
                    Toast.makeText(context, "Failed to reload user: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    sentEmail = false
                }
            }
        }

        binding.resendButton.setOnClickListener {
            Log.d("UserOnly", "${auth.currentUser?.email}")
            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Verification email resent. Please check your email.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to resend verification email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun enter_verify_email(){
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.verification_sent_title)
            .setMessage(R.string.verification_sent)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun deleteUserIfNotVerified() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful && !user.isEmailVerified) {
                user.delete().addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        Log.d("UserDeleted", "User deleted")
                    } else {
                        Log.d("UserDeleted", "User not deleted")
                    }
                }
            }
        }
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
                    deleteUserIfNotVerified()
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
        super.onDestroyView()
        auth.signOut()
        _binding = null
    }
}
