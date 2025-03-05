/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.workoutwrecker.workouttracker.databinding.ActivityMainBinding
import com.workoutwrecker.workouttracker.ui.workout.start.WorkoutSummaryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.ui.workout.start.WorkoutStartSepFragment
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
    WorkoutSummaryFragment.OnWorkoutSummaryBackListener,
    MainBillingManager.BillingUpdatesListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var billingManager: MainBillingManager
    private lateinit var billingClient: BillingClient
    private lateinit var securePrefManager: SecurePreferenceManager

    private var isShow = false
    private var isHide = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firestore settings before any Firestore interaction
        val settings = FirebaseFirestoreSettings.Builder()
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()

        // Apply settings to Firestore
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = settings

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themePreference = sharedPreferences.getString("theme_preference", "default")
        val currentTheme = setAppTheme(themePreference)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setThemeBackgrounds(currentTheme)

        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )

        // Initialize BillingClient
        billingClient = BillingClient.newBuilder(this)
            .setListener { _, _ ->
            }
            .enablePendingPurchases()
            .build()

        // Initialize MainBillingManager
        billingManager = MainBillingManager(this, this, billingClient, this)
        billingManager.startConnection()

        securePrefManager = SecurePreferenceManager(applicationContext)
        enablePremium(false)

        // Set up custom toolbar
        val customToolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.custom_toolbar)
        setSupportActionBar(customToolbar)

        val settingsIcon: ImageView = findViewById(R.id.settings_icon)
        val premiumIcon: ImageView = findViewById(R.id.premium_icon)
        val filterIcon: ImageView = findViewById(R.id.filter_icon)
        val pageTitle: TextView = findViewById(R.id.page_title)

        val typedValue = TypedValue()

        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        settingsIcon.setBackgroundResource(typedValue.resourceId)
        premiumIcon.setBackgroundResource(typedValue.resourceId)
        filterIcon.setBackgroundResource(typedValue.resourceId)

        settingsIcon.setOnClickListener {
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            val currentDestinationId = navController.currentDestination?.id

            if (currentDestinationId != R.id.navigation_settings) {
                navController.navigate(R.id.navigation_settings)
            }
        }

//        premiumIcon.setOnClickListener {
//            val navController = findNavController(R.id.nav_host_fragment_activity_main)
//            val currentDestinationId = navController.currentDestination?.id
//
//            if (currentDestinationId != R.id.navigation_premium_container) {
//                navController.navigate(R.id.action_to_premium_container)
//            }
//        }

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_stats, R.id.navigation_workout,
                R.id.navigation_history_workout, R.id.navigation_exercises,
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val label = destination.label

            pageTitle.text = label
        }

        binding.mainContainer.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? WorkoutStartSepFragment
                val addExerciseButton = fragment?.view?.findViewById<FloatingActionButton>(R.id.add_exercise_button)
                val workoutNameButton = fragment?.view?.findViewById<EditText>(R.id.workout_name_input)
                val workoutNotesButton = fragment?.view?.findViewById<EditText>(R.id.workout_notes_input)
                val cancelWorkoutButton = fragment?.view?.findViewById<Button>(R.id.cancel_workout_button)
                val cancelWorkoutButtonCollapsedMode = fragment?.view?.findViewById<Button>(R.id.cancel_workout_button_collapsed_mode)
                addExerciseButton?.isEnabled = false
                workoutNameButton?.isEnabled = false
                workoutNotesButton?.isEnabled = false
                cancelWorkoutButton?.isEnabled = false
                cancelWorkoutButtonCollapsedMode?.isEnabled = false
                Log.e(MainActivity::class.java.simpleName,
                    "change $startId $progress $startId $endId")
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? WorkoutStartSepFragment
                val addExerciseButton = fragment?.view?.findViewById<FloatingActionButton>(R.id.add_exercise_button)
                val workoutNameButton = fragment?.view?.findViewById<EditText>(R.id.workout_name_input)
                val workoutNotesButton = fragment?.view?.findViewById<EditText>(R.id.workout_notes_input)
                val cancelWorkoutButton = fragment?.view?.findViewById<Button>(R.id.cancel_workout_button)
                val cancelWorkoutButtonCollapsedMode = fragment?.view?.findViewById<Button>(R.id.cancel_workout_button_collapsed_mode)
                addExerciseButton?.isEnabled = true
                workoutNameButton?.isEnabled = true
                workoutNotesButton?.isEnabled = true
                cancelWorkoutButton?.isEnabled = true
                cancelWorkoutButtonCollapsedMode?.isEnabled = true
//                hideActionBar()
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }

        })

        val auth = Firebase.auth
        val sharedPref = getSharedPreferences("register_prefs", Context.MODE_PRIVATE)
        val signOutFlag = sharedPref.getBoolean("sign_out_flag", false)
        val pageFlag = sharedPref.getString("register_page", "NA")
        if (signOutFlag) {
            Firebase.auth.signOut()
            when (pageFlag) {
                "NA" -> navController.navigate(R.id.navigation_sign_in)
                "verify" -> navController.navigate(R.id.navigation_verify_email)
                "new_password" -> navController.navigate(R.id.navigation_account_creation)
            }
        } else {
            // Check if the user is already signed in
            if (auth.currentUser == null) {
                Log.d("MainActivity", "Current user null")
                navController.navigate(R.id.navigation_sign_in)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? WorkoutStartSepFragment
            fun decideDeadLayout(){
                if (fragment != null) {
                    binding.deadLinearLayout.visibility = View.VISIBLE
                    fragment.setFragmentVisibility(true)
                    Log.d("MainActivity", "Dead Linear Layout should be active")
                }
                else {
                    binding.deadLinearLayout.visibility = View.GONE
                }
            }

            fun boilerDestinationActions(){
                showActionBar()
                settingsIcon.visibility = View.VISIBLE
                premiumIcon.visibility = View.VISIBLE
                binding.searchView.visibility = View.GONE
                binding.filterIcon.visibility = View.GONE
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                showBottomBar()
                decideDeadLayout()
            }

            when (destination.id) {
                //Navigation bar fragments
                R.id.navigation_home -> {
                    navController.popBackStack(R.id.navigation_home, false)
                    boilerDestinationActions()
                }
                R.id.navigation_stats -> {
                    navController.popBackStack(R.id.navigation_stats, false)
                    boilerDestinationActions()
                }
                R.id.navigation_workout -> {
                    navController.popBackStack(R.id.navigation_workout, false)
                    boilerDestinationActions()
                }
                R.id.navigation_history_workout -> {
                    navController.popBackStack(R.id.navigation_history_workout, false)
                    boilerDestinationActions()
                }
                R.id.navigation_exercises -> {
                    showActionBar()
                    navController.popBackStack(R.id.navigation_exercises, false)
                    decideDeadLayout()
                    settingsIcon.visibility = View.GONE
                    premiumIcon.visibility = View.GONE
                    binding.searchView.visibility = View.VISIBLE
                    binding.filterIcon.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    showBottomBar()
                }

                // Hide ActionBar and bottom bar for these destinations
                R.id.navigation_premium_container-> {
                    hideActionBar()
                    fragment?.setFragmentVisibility(false)
                    settingsIcon.visibility = View.GONE
                    premiumIcon.visibility = View.GONE
                    hideBottomBar()
                    binding.deadLinearLayout.visibility = View.GONE
                }

                R.id.navigation_settings ->{
                    showActionBar()
                    settingsIcon.visibility = View.GONE
                    premiumIcon.visibility = View.GONE
                    binding.searchView.visibility = View.GONE
                    binding.filterIcon.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    hideBottomBar()
                    try {
                        fragment?.setFragmentVisibility(false)
                        binding.deadLinearLayout.visibility = View.GONE
                    } catch (e: Exception) {
                        Log.e("MainActivity Error", "Error hiding, likely from changing themes")
                        binding.deadLinearLayout.visibility = View.VISIBLE
                    }
                }

                // Hide settingsIcon, premiumIcon, and customize ActionBar for these destinations
                R.id.navigation_sign_in,
                R.id.navigation_register,
                R.id.navigation_verify_email,
                R.id.navigation_account_creation,
                R.id.navigation_unverified_account_creation,
                R.id.navigation_settings -> {
                    showActionBar()
                    settingsIcon.visibility = View.GONE
                    premiumIcon.visibility = View.GONE
                    binding.searchView.visibility = View.GONE
                    binding.filterIcon.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    hideBottomBar()
                }

                R.id.navigation_exercise_selection -> {
                    showActionBar()
                    fragment?.setFragmentVisibility(false)
                    settingsIcon.visibility = View.GONE
                    premiumIcon.visibility = View.GONE
                    binding.searchView.visibility = View.VISIBLE
                    binding.filterIcon.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    decideDeadLayout()
                    hideBottomBar()
                    binding.deadLinearLayout.visibility = View.GONE
                }

                R.id.navigation_exercise_information,
                R.id.navigation_create_exercise,
                R.id.navigation_update_exercise,
                R.id.navigation_filter_exercises, -> {
                    showActionBar()
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    fragment?.setFragmentVisibility(false)
                    settingsIcon.visibility = View.VISIBLE
                    premiumIcon.visibility = View.VISIBLE
                    binding.searchView.visibility = View.GONE
                    binding.filterIcon.visibility = View.GONE
                    hideBottomBar()
                    binding.deadLinearLayout.visibility = View.GONE
                }

                // Hide bottom bar and some elements for these workout-related destinations
                R.id.navigation_workout_view,
                R.id.navigation_workout_summary,
                R.id.navigation_create_workout,
                R.id.navigation_update_history_workout-> {
                    showActionBar()
                    settingsIcon.visibility = View.VISIBLE
                    premiumIcon.visibility = View.VISIBLE
                    binding.searchView.visibility = View.GONE
                    binding.filterIcon.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    fragment?.setFragmentVisibility(false)
                    hideBottomBar()
                    binding.deadLinearLayout.visibility = View.GONE
                }

                // Handle all other destinations (default case)
                else -> {
                    showActionBar()
                    settingsIcon.visibility = View.VISIBLE
                    premiumIcon.visibility = View.VISIBLE
                    binding.searchView.visibility = View.GONE
                    binding.filterIcon.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    fragment?.setFragmentVisibility(true)
                    showBottomBar()
                }
            }
        }

        auth.currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val basePlan = result.claims["base_plan"] as? String ?: ""
            when (basePlan){
                "basic" -> {}
                "fit" -> {}
                "jacked" -> {}
                "elite" -> {}
            }
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun setAppTheme(themePreference: String?): String {
        when (themePreference) {
            "silver_night" -> setTheme(R.style.Theme_WorkoutTracker_SilverNight)
            "default" -> setTheme(R.style.Theme_WorkoutTracker)
            "ice" -> {
                setTheme(R.style.Theme_Ice)
            }
        }
        return themePreference.toString()
    }

    private fun setThemeBackgrounds(theme:String) {
        when (theme) {
            "ice" -> {
                binding.iceBackground.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun showActionBar() {
        if (isShow) return

        isShow = true
        isHide = false
        supportActionBar?.let {
            it.setShowHideAnimationEnabled(false)
            it.show()
        }

    }

    @SuppressLint("RestrictedApi")
    private fun hideActionBar() {
        if (isHide) return

        isShow = false
        isHide = true
        supportActionBar?.let {
            it.setShowHideAnimationEnabled(false)
            it.hide()
        }
    }

    fun closeStartWorkoutFragments() {
        // Get the Top Fragment and Remove All From Back Stack
        val name = supportFragmentManager.getBackStackEntryAt(0).name
        supportFragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        // Reduce the Duration for Navigation Bar Visibility Animation
        binding.mainContainer.setTransitionDuration(200)
        binding.mainContainer.transitionToStart()
        // Navigate To Home Fragment After Finishing
//        binding.navView.selectedItemId = R.id.navigation_home
    }

    override fun onWorkoutSummaryBackPressed() {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Get the Top Fragment and Remove All From Back Stack
        val name = supportFragmentManager.getBackStackEntryAt(0).name
        supportFragmentManager.popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        // Reduce the Duration for Navigation Bar Visibility Animation
        binding.mainContainer.setTransitionDuration(200)
        binding.mainContainer.transitionToStart()
        // Navigate to call onCreateView to re-fetch workouts
        if (binding.navView.selectedItemId == R.id.navigation_workout) {
            navController.navigate(R.id.navigation_workout)
        }
    }

    private fun hideBottomBar(){
        binding.mainContainer.transitionToEnd()
    }

    private fun showBottomBar(){
        binding.mainContainer.transitionToStart()
    }

    fun setPageTitle(name:String){
        binding.pageTitle.text = name
    }

    override fun onRecoverProgress(purchases: List<Purchase>) {
        var unverifiedCount = 0

        for (purchase in purchases) {
            if (purchase.isAcknowledged) {
                continue
            }
            unverifiedCount += 1

            Log.d("MainActivityPurchase", "Unacknowledged Purchase: $purchase")

            val purchaseToken = securePrefManager.getString("purchaseToken") ?: "N/A"

            if (purchaseToken == "N/A") {
                billingManager.getFirebaseBasePlan { firebaseToken ->
                    if (firebaseToken.isEmpty() || firebaseToken != purchase.purchaseToken) {
                        Log.d("MainActivityPurchase", "Invalid data")
                        Toast.makeText(
                            applicationContext, "You have unacknowledged purchases",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@getFirebaseBasePlan
                    } else {
                        verifyPurchase(purchase)
                    }
                }
            } else {
                // valid token
                verifyPurchase(purchase)
            }
        }
        if (unverifiedCount == 0){enablePremium(true)}
    }

    // Separate function to handle verification
    private fun verifyPurchase(purchase: Purchase) {
        billingManager.verifyPurchaseWithFirebase(purchase.purchaseToken) { success ->
            if (!success) {
                Toast.makeText(applicationContext, "Error verifying purchase",
                    Toast.LENGTH_SHORT).show()
                return@verifyPurchaseWithFirebase
            }

            billingManager.handlePurchase(purchase, 1) { handleSuccess ->
                if (!handleSuccess) {
                    Toast.makeText(applicationContext, "Error: Unacknowledged Purchase.",
                        Toast.LENGTH_SHORT).show()
                    return@handlePurchase
                }
                enablePremium(true)
                Toast.makeText(applicationContext, "Your purchase was acknowledged",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun snackBarUnresolved(errorCode: Int) {
        val snackbar = Snackbar.make(
            findViewById(R.id.main_container),
            "You have unacknowledged purchases",
            Snackbar.LENGTH_SHORT
        )
        snackbar.setAction("Learn More") {
            // Handle the click event here, for example, opening a link
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://Google.com"))
            startActivity(browserIntent)
            TODO("Link url to FAQ page and pre enter query into link for why purchases could" +
                    "be unacknowledged (Something went wrong while verifying, network failure after purchase" +
                    "swapping devices with the same underlying google account to a different firebase account" +
                    "where there is an existing unacknowledged purchase")
        }
        snackbar.show()
    }

    private fun enablePremium(enable: Boolean) {
        Log.d("EnablePremium", "$enable")
        if (enable){
            binding.premiumIcon.setOnClickListener {
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                val currentDestinationId = navController.currentDestination?.id

                if (currentDestinationId != R.id.navigation_premium_container) {
                    navController.navigate(R.id.action_to_premium_container)
                }
            }
            return
        }
        binding.premiumIcon.setOnClickListener {
            val snackbar = Snackbar.make(
                findViewById(R.id.main_container),
                "Temporarily disabled",
                Snackbar.LENGTH_SHORT
            )
            snackbar.setAction("Learn More") {
                // Handle the click event here, for example, opening a link
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://Google.com"))
                startActivity(browserIntent)
                TODO("Link url to FAQ page and pre enter query into link for why premium is disabled." +
                        "Disable reason would be that we want to prevent purchases from occuring while" +
                        "there are issues with payments (Typically while attempting to verify unacknowledged" +
                        "purchases.")
            }
            snackbar.show()
        }
    }

//    private fun disableAllViews(viewGroup: ViewGroup) {
//        for (i in 0 until viewGroup.childCount) {
//            val child = viewGroup.getChildAt(i)
//            if (child.id == R.id.dimView || child.id == R.id.loadingProgressBar){ continue }
//            else if (child is ViewGroup) {
//                disableAllViews(child)
//            } else {
//                child.isEnabled = false
//            }
//        }
//    }
//
//    private fun enableAllViews(viewGroup: ViewGroup) {
//        for (i in 0 until viewGroup.childCount) {
//            val child = viewGroup.getChildAt(i)
//            if (child is ViewGroup) {
//                enableAllViews(child) // Recursively enable child ViewGroups
//            } else {
//                child.isEnabled = true // Enable the view here
//            }
//        }
//    }
}