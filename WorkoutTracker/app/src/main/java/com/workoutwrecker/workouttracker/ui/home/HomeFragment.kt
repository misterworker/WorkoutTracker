/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.ViewAnimator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentHomeBinding
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseDao
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseDatabase
import com.workoutwrecker.workouttracker.ui.data.exercise.ExerciseRepository
import com.workoutwrecker.workouttracker.ui.history.HistoryWorkoutViewModel
import com.workoutwrecker.workouttracker.ui.workout.start.UpdateHistoryWorkoutFragmentArgs
import com.workoutwrecker.workouttracker.ui.workout.start.WorkoutStartSepFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var exerciseRepository: ExerciseRepository
    private val homeViewModel: HomeViewModel by viewModels()
    private val historyWorkoutViewModel: HistoryWorkoutViewModel by viewModels()
    private val sharedPrefs by lazy {
        requireContext().getSharedPreferences("visuals_prefs", Context.MODE_PRIVATE)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize ExerciseDao and ExerciseRepository here
        val exerciseDatabase = ExerciseDatabase.getDatabase(requireContext())
        exerciseDao = exerciseDatabase.exerciseDao()
        exerciseRepository = ExerciseRepository(exerciseDao, requireContext())
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            homeViewModel.uploadImageToFirebase(requireContext(), it)
        }
    }
    private lateinit var adapter: ChartPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val selectedVisuals = getSelectedVisualsFromPrefs().toMutableList()
        if (selectedVisuals.isEmpty()){selectedVisuals.add("Workouts Done")}

        refreshViewPager(selectedVisuals)
        val viewPager = binding.viewPagerCharts

        adapter = ChartPagerAdapter(this, selectedVisuals)
        viewPager.adapter = adapter
        TabLayoutMediator(binding.chartsTabLayout, binding.viewPagerCharts) { _, _ -> }.attach()

        val prevChart = binding.prevChart
        val nextChart = binding.nextChart

        updateArrows(viewPager.currentItem, selectedVisuals.size)

        // Handle the left arrow click
        prevChart.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        // Handle the right arrow click
        nextChart.setOnClickListener {
            if (viewPager.currentItem < (viewPager.adapter?.itemCount?.minus(1) ?: 0)) {
                viewPager.currentItem += 1
            }
        }

        // Register a page change callback to update the chart title dynamically
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val selectedCharts = getSelectedVisualsFromPrefs().toMutableList()
                if (selectedCharts.isEmpty()){selectedCharts.add("Workouts Done")}
                binding.chartTitle.text = getChartTitleForPosition(position, selectedCharts)
                updateArrows(position, selectedCharts.size)
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageView = binding.profileImage

        homeViewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            if (url != null) {
                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.default_profile_picture)
                    .circleCrop()
                    .into(binding.profileImage)
            }
        }

        homeViewModel.loadProfileImage(requireContext())

        binding.addVisualButton.setOnClickListener {showVisualsBottomSheet()}

        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { historyWorkoutViewModel.fetchHistoryWorkouts(userId) }
            )
        }

        historyWorkoutViewModel.historyWorkoutCount.observe(viewLifecycleOwner) { count ->
            binding.workoutsCompletedNumber.text = count.toString()
        }

        val newCardView = binding.homeCategoryNewWorkout
        val newViewAnimator = binding.newWorkoutViewAnimator
        val newOriginalLayout = binding.newWorkoutText
        val newSegment = binding.newWorkoutSegment
        val newButton = binding.newWorkoutButton
        val newIcon = binding.plusIcon
        val communityCardView = binding.homeCategoryCommunity
        val communityViewAnimator = binding.communityViewAnimator
        val communityOriginalLayout = binding.communityText
        val communitySegment = binding.communitySegment
        val communityButton = binding.communityButton
        val communityIcon = binding.communityIcon
        val plateCalcCardView = binding.homeCategoryPlateCalc
        val plateCalcViewAnimator = binding.plateCalcViewAnimator
        val plateCalcOriginalLayout = binding.plateCalcText
        val plateCalcSegment = binding.plateCalcSegment
        val plateCalcButton = binding.plateCalcButton
        val plateCalcIcon = binding.plateCalcIcon

        var isAnimating = false

        var newWorkoutClicked = false
        var communityClicked = false
        var plateCalcClicked = false


        newButton.setOnClickListener {
            startWorkout(System.currentTimeMillis())
        }
        communityButton.setOnClickListener {
            val snackbar = Snackbar.make(
                view.findViewById(R.id.home_root),
                "In Development",
                Snackbar.LENGTH_SHORT
            )
            snackbar.setAction("Learn More") {
                // Handle the click event here, for example, opening a link
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://dictionary.cambridge.org/dictionary/english/development"))
                startActivity(browserIntent)
            }
            snackbar.show()
//            val action = HomeFragmentDirections.actionNavigationHomeToNavigationCommunity()
//            findNavController().navigate(action)
        }
        plateCalcButton.setOnClickListener {
            val snackbar = Snackbar.make(
                view.findViewById(R.id.home_root),
                "In Development",
                Snackbar.LENGTH_SHORT
            )
            snackbar.setAction("Learn More") {
                // Handle the click event here, for example, opening a link
                val browserIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://dictionary.cambridge.org/dictionary/english/development"))
                startActivity(browserIntent)
            }
            snackbar.show()
//            val action = HomeFragmentDirections.actionNavigationHomeToNavigationPlateCalc()
//            findNavController().navigate(action)
        }

        // Define animation methods
        fun animateOutOriginalLayout(icon: View, layout: View) {
            val fadeOut = ObjectAnimator.ofFloat(layout, "alpha", 1f, 0f).setDuration(250)
            fadeOut.start()

            val slideOut = ObjectAnimator.ofFloat(icon, "translationX", 0f, -icon.width.toFloat()).setDuration(250)
            slideOut.start()
        }

        fun animateInNewSegment(segment: View) {
            val slideIn = ObjectAnimator.ofFloat(segment, "translationX", 0f).setDuration(250)
            val fadeIn = ObjectAnimator.ofFloat(segment, "alpha", 0f, 1f).setDuration(250)
            slideIn.start()
            fadeIn.start()
        }

        fun resetCardView(icon: View, segment: View, layout: View, viewAnimator: ViewAnimator) {
            val fadeIn = ObjectAnimator.ofFloat(layout, "alpha", 0f, 1f).setDuration(250)
            val slideIn = ObjectAnimator.ofFloat(icon, "translationX", -icon.width.toFloat(), 0f).setDuration(250)

            fadeIn.start()
            slideIn.start()

            val fadeOut = ObjectAnimator.ofFloat(segment, "alpha", 1f, 0f).setDuration(250)
            fadeOut.start()

            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    viewAnimator.displayedChild = 0 // Show the original layout
                    segment.visibility = View.GONE
                    layout.visibility = View.VISIBLE
                    isAnimating = false // Enable clicks again
                }
            })
        }

        newCardView.setOnClickListener {
            if (isAnimating) return@setOnClickListener

            if (newWorkoutClicked) {
                // Reset the card view
                isAnimating = true
                newWorkoutClicked = false
                resetCardView(newIcon, newSegment, newOriginalLayout, newViewAnimator)

            } else {
                if (communityClicked) {
                    communityClicked = false
                    isAnimating = true
                    resetCardView(communityIcon, communitySegment, communityOriginalLayout, communityViewAnimator)
                }
                if (plateCalcClicked) {
                    plateCalcClicked = false
                    isAnimating = true
                    resetCardView(plateCalcIcon, plateCalcSegment, plateCalcOriginalLayout, plateCalcViewAnimator)
                }
                // Perform animations to switch to the new segment
                newWorkoutClicked = true
                animateOutOriginalLayout(newIcon, newOriginalLayout)
                newViewAnimator.displayedChild = 1 // Show the new segment
                newOriginalLayout.visibility = View.GONE
                newSegment.visibility = View.VISIBLE
                newSegment.alpha = 0f
                newSegment.translationX = newSegment.width.toFloat()
                animateInNewSegment(newSegment)
                return@setOnClickListener
            }
        }
        communityCardView.setOnClickListener {
            if (isAnimating) return@setOnClickListener

            if (communityClicked) {
                // Reset the card view
                isAnimating = true
                communityClicked = false
                resetCardView(communityIcon, communitySegment, communityOriginalLayout, communityViewAnimator)

            } else {
                if (newWorkoutClicked) {
                    newWorkoutClicked = false
                    isAnimating = true
                    resetCardView(newIcon, newSegment, newOriginalLayout, newViewAnimator)
                }
                if (plateCalcClicked) {
                    plateCalcClicked = false
                    isAnimating = true
                    resetCardView(plateCalcIcon, plateCalcSegment, plateCalcOriginalLayout, plateCalcViewAnimator)
                }
                // Perform animations to switch to the new segment
                communityClicked = true
                animateOutOriginalLayout(communityIcon, communityOriginalLayout)
                communityViewAnimator.displayedChild = 1 // Show the new segment
                communityOriginalLayout.visibility = View.GONE
                communitySegment.visibility = View.VISIBLE
                communitySegment.alpha = 0f
                communitySegment.translationX = communitySegment.width.toFloat()
                animateInNewSegment(communitySegment)
                return@setOnClickListener
            }
        }
        plateCalcCardView.setOnClickListener {
            if (isAnimating) return@setOnClickListener

            if (plateCalcClicked) {
                // Reset the card view
                isAnimating = true
                plateCalcClicked = false
                resetCardView(plateCalcIcon, plateCalcSegment, plateCalcOriginalLayout, plateCalcViewAnimator)

            } else {
                if (newWorkoutClicked) {
                    newWorkoutClicked = false
                    isAnimating = true
                    resetCardView(newIcon, newSegment, newOriginalLayout, newViewAnimator)
                }
                if (communityClicked) {
                    communityClicked = false
                    isAnimating = true
                    resetCardView(communityIcon, communitySegment, communityOriginalLayout, communityViewAnimator)
                }
                // Perform animations to switch to the new segment
                plateCalcClicked = true
                animateOutOriginalLayout(plateCalcIcon, plateCalcOriginalLayout)
                plateCalcViewAnimator.displayedChild = 1 // Show the new segment
                plateCalcOriginalLayout.visibility = View.GONE
                plateCalcSegment.visibility = View.VISIBLE
                plateCalcSegment.alpha = 0f
                plateCalcSegment.translationX = plateCalcSegment.width.toFloat()
                animateInNewSegment(plateCalcSegment)
                return@setOnClickListener
            }
        }

        // Set up the profile image click listener
        imageView.setOnClickListener {
            showPopupMenu(it)
        }

    }

    private fun startWorkout(currentDate: Long) {
        // Create the action with arguments using the generated Directions class
        // Manually create the WorkoutStartFragmentArgs
        val args = UpdateHistoryWorkoutFragmentArgs(
            checkboxEnabled = true,
            workoutDate = currentDate,
            historyWorkoutId = null.toString(),
            selectedExercises = emptyArray()
        )

        // Convert args to a Bundle
        val bundle = args.toBundle()

        // Create the fragment and set the arguments
        val fragment = WorkoutStartSepFragment().apply {
            arguments = bundle
        }

        // Replace the current fragment with WorkoutStartFragment
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("MAIN_FRAGMENT_TAG")
            .commit()

    }

    private fun logout() {
        Log.d("HomeFragment", "logout")
        showLoading(true)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            homeViewModel.logout(requireContext())
        }

        homeViewModel.logoutStatus.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut) {
                // Clear the entire back stack and navigate to the sign-in screen
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_home, true) // Use the ID of the home fragment or the root fragment
                    .setLaunchSingleTop(true)
                    .build()

                findNavController().navigate(R.id.navigation_sign_in, null, navOptions)

                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                showLoading(false)
            } else {
                Toast.makeText(context, "Logout failed", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.profile_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_update_pfp -> {
                    homeViewModel.pickImageFromGallery({ pickImageLauncher.launch(it) }, requireContext())
                    true
                }
                R.id.action_settings -> {
                    val action = HomeFragmentDirections.actionNavigationHomeToNavigationSettings()
                    findNavController().navigate(action)
                    true
                }
                R.id.action_logout -> {
                    confirmLogout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                logout()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.dimView.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showVisualsBottomSheet() {
        // Inflate the bottom sheet layout
        val view = layoutInflater.inflate(R.layout.bottom_sheet_visuals, null)
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(view)

        // Initialize RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.visualsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val selectedVisuals = sharedPrefs.getString("selected_visuals", "")
            ?.split(",")?.toList() ?: emptyList()

        // List of available visuals
        val availableVisuals = listOf("Workouts Done", "Time Spent", "Radar Chart", "Chart4")

        // Set up adapter for RecyclerView
        recyclerView.adapter = VisualsAdapter(availableVisuals, selectedVisuals) { visual, isChecked ->
            updateSelectedVisuals(visual, isChecked)
        }

        bottomSheetDialog.show()
    }

    private fun updateSelectedVisuals(visual: String, isChecked: Boolean) {
        // Retrieve the current list from SharedPreferences
        val selectedVisualsList = getSelectedVisualsFromPrefs().toMutableList()

        Log.d("HomeFragment", "Selected Visuals 0: ${selectedVisualsList.size}")

        // Modify the list without altering the original reference
        if (isChecked) {

            if (!selectedVisualsList.contains(visual)) {
                selectedVisualsList.add(visual)
            }
        } else {
            selectedVisualsList.remove(visual)
        }

        Log.d("HomeFragment", "Selected Visuals 1: $selectedVisualsList")

        // Save the new list back to SharedPreferences as a string
        sharedPrefs.edit().putString("selected_visuals", selectedVisualsList.joinToString(",")).apply()
        Log.d("HomeFragment", "Selected Visuals: $selectedVisualsList")
        refreshViewPager(selectedVisualsList)
    }

    private fun refreshViewPager(selectedCharts: List<String>) {
        // Update the adapter with the new set of charts
        val adapter = ChartPagerAdapter(this, selectedCharts)
        binding.viewPagerCharts.adapter = adapter

        // Update TabLayout with new charts
        TabLayoutMediator(binding.chartsTabLayout, binding.viewPagerCharts) { _, _ -> }.attach()
        updateArrows(binding.viewPagerCharts.currentItem, selectedCharts.size)
    }

    // Function to retrieve the selected visuals from SharedPreferences
    private fun getSelectedVisualsFromPrefs(): List<String> {
        val selectedVisuals = sharedPrefs.getString("selected_visuals", "")
        val selectedVisualsList = selectedVisuals?.split(",")
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
        return selectedVisualsList
    }

    private fun getChartTitleForPosition(position: Int, selectedCharts: List<String>): String {
        val string = selectedCharts[position]
        return when (string){
            "Workouts Done" -> getString(R.string.weekly_workout_counts)
            "Time Spent" -> getString(R.string.weekly_workout_time)
            "Radar Chart" -> "Radar Chart"
            "Chart4" -> "Unavailable"
            else -> "Unavailable"
        }
    }

    // Function to update arrow visibility based on current position
    private fun updateArrows(currentPosition: Int, itemCount: Int) {
        Log.d("HomeFragment", "updateArrows: $currentPosition, $itemCount")
        binding.prevChart.visibility = if (currentPosition > 0) View.VISIBLE else View.INVISIBLE
        binding.nextChart.visibility = if (currentPosition < itemCount - 1) View.VISIBLE else View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
