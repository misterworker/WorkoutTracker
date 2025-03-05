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
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.workoutwrecker.workouttracker.databinding.FragmentPremiumContainerBinding // Import your binding class

class PremiumContainerFragment : Fragment() {

    private var _binding: FragmentPremiumContainerBinding? = null // Declare the binding
    val binding get() = _binding!! // Non-null reference to the binding

    private lateinit var adapter: PremiumViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumContainerBinding.inflate(inflater, container, false)

        val args: PremiumContainerFragmentArgs? = arguments?.let {
            PremiumContainerFragmentArgs.fromBundle(it)
        }

        val pageNumber = args?.page?:0

        Log.d("args", "$args")

        adapter = PremiumViewPagerAdapter(this)
        binding.premiumViewpager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.premiumViewpager) { _, _ ->}.attach()

        navigateToPage(pageNumber)

        return binding.root
    }

    fun navigateToPage(position: Int) {
        binding.premiumViewpager.post {
            binding.premiumViewpager.setCurrentItem(position, true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class PremiumViewPagerAdapter(fragment: PremiumContainerFragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 3 // Number of fragments
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PremiumFragment()
            1 -> PremiumBenefitsFragment()
            2 -> PremiumPurchaseFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
