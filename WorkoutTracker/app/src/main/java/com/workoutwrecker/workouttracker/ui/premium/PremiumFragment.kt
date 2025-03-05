/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.premium

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.workoutwrecker.workouttracker.R

class PremiumFragment() : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_premium, container, false)

        val closeButton: Button = view.findViewById(R.id.close_button)
        val subscribeButton: Button = view.findViewById(R.id.subscribe_button)
        val findOutMoreButton: Button = view.findViewById(R.id.explore_all_button)

        closeButton.setOnClickListener {
            findNavController().popBackStack()
        }

        findOutMoreButton.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("page", 1) // Pass the page number as an argument
            }
            findNavController().navigate(R.id.navigation_premium_container, bundle, navOptions {
                // This will pop up to the first instance of PremiumContainerFragment
                popUpTo(R.id.navigation_premium_container) {
                    inclusive = true // This makes sure to remove the current instance
                }
            })
        }

        subscribeButton.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("page", 2) // Pass the page number as an argument
            }
            findNavController().navigate(R.id.navigation_premium_container, bundle, navOptions {
                // This will pop up to the first instance of PremiumContainerFragment
                popUpTo(R.id.navigation_premium_container) {
                    inclusive = true // This makes sure to remove the current instance
                }
            })
        }

        return view
    }

    private fun navigateToPageInContainer(position: Int) {
        // Find the existing instance of PremiumContainerFragment
        val premiumContainerFragment = parentFragmentManager.findFragmentById(R.id.fragment_premium_container)
                as? PremiumContainerFragment

        Log.d("premium container fragment", "$premiumContainerFragment")
        // Call the navigation function only if the fragment exists
        premiumContainerFragment?.navigateToPage(position)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
