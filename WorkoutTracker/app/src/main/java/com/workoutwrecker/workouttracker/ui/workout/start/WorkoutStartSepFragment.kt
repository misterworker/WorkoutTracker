/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.workout.start

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.workoutwrecker.workouttracker.MainActivity
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.YoutubeViewMotionLayout
import com.workoutwrecker.workouttracker.ui.data.history.HistoryWorkout
import com.workoutwrecker.workouttracker.ui.data.workout.WorkoutExercise
import com.workoutwrecker.workouttracker.ui.exercises.details.ExerciseInformationFragmentArgs
import com.workoutwrecker.workouttracker.ui.exercises.details.ExerciseInformationSepFragment
import com.workoutwrecker.workouttracker.ui.workout.shared.ExerciseSelectionSepLaunchFragment
import com.workoutwrecker.workouttracker.ui.workout.shared.MoveExerciseFragmentArgs
import com.workoutwrecker.workouttracker.ui.workout.shared.MoveExerciseSepFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.ceil


@AndroidEntryPoint
class WorkoutStartSepFragment : Fragment() {
    private val args: UpdateHistoryWorkoutFragmentArgs by navArgs()
    private var selectedExercises: MutableList<WorkoutExercise> = mutableListOf()
    private val auth = Firebase.auth

    private val viewModel: WorkoutStartViewModel by viewModels()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var deadLinearLayout: LinearLayout

    private var workoutNameInput: EditText? = null
    private var workoutNotesInput: EditText? = null
    private var addExerciseButton: FloatingActionButton? = null
    private var finishWorkoutButton: Button? = null
    private var cancelWorkoutButton: Button? = null
    private var loadingProgress: ProgressBar? = null

    private var initial_exercises_added = false
    private var returningFromExerciseSelection = false

    // Timer variables
    private lateinit var timerText: TextView
    private lateinit var timerLayout: LinearLayout
    private lateinit var timerView: TimerView
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private var remainingTime: Long = 60000 // Default to 1 minute in milliseconds
    private var startTime: Long = 0L
    private var dimmingView: View? = null
    private var add30SecondsButton: Button? = null
    private var subtract30SecondsButton: Button? = null
    private lateinit var originalTimerText: TextView
    private lateinit var viewContainer: YoutubeViewMotionLayout
    private lateinit var miniViewCancelButton: Button
    private lateinit var miniViewTimerText: TextView
    private lateinit var miniViewTimerView: TimerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register a callback for the back button press
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    collapseTimerView()
                    // Custom back press handling
                    if (viewContainer.currentState == viewContainer.endState) {
                        Log.d("WorkoutStartSepFragment", "Back pressed 2")
                        // The MotionLayout is in the minimized state
                        viewContainer.transitionToStart()


                    } else if (viewContainer.currentState == viewContainer.startState) {
                        Log.d("WorkoutStartSepFragment", "Back pressed 3")
                        // The MotionLayout is in the expanded state

                    } else {
                        Log.d("WorkoutStartSepFragment", "Back pressed 4")
                        // If in another state, do something else or call the default back press behavior

                    }
                }
            }
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout_start_sep, container, false)
        workoutNameInput = view.findViewById(R.id.workout_name_input)
        workoutNotesInput = view.findViewById(R.id.workout_notes_input)
        addExerciseButton = view.findViewById(R.id.add_exercise_button)
        finishWorkoutButton = view.findViewById(R.id.finish_workout_button)
        cancelWorkoutButton = view.findViewById(R.id.cancel_workout_button)
        loadingProgress = view.findViewById(R.id.loading_progress)
        dimmingView = view.findViewById(R.id.dimming_view)
        viewContainer = view.findViewById(R.id.view_container)
        miniViewCancelButton = view.findViewById(R.id.cancel_workout_button_collapsed_mode)
        miniViewTimerText = view.findViewById(R.id.timer_text_collapsed_mode)
        miniViewTimerView = view.findViewById(R.id.timer_view_collapsed_mode)
        // Ensure MotionLayout
        view.post {
            viewContainer.transitionToEnd()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deadLinearLayout = requireActivity().findViewById(R.id.dead_linear_layout)
        val title = args.workoutTitle
        val notes = args.workoutNotes
        val checkboxEnabled = args.checkboxEnabled

        workoutNameInput?.setText(title)
        workoutNotesInput?.setText(notes)
        workoutNameInput?.isEnabled = checkboxEnabled
        workoutNotesInput?.isEnabled = checkboxEnabled

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_workout_exercises)
        val adapter = WorkoutExerciseAdapter(
            selectedExercises,
            onExerciseClick = { workoutExercise ->
                lifecycleScope.launch {
                    val exercise = viewModel.getExerciseById(workoutExercise.exerciseid)
                    if (exercise != null) {
                        val args = ExerciseInformationFragmentArgs(
                            exercise = exercise,
                        )

                        // Convert args to a Bundle
                        val bundle = args.toBundle()

                        // Create the fragment and set the arguments
                        val fragment = ExerciseInformationSepFragment().apply {
                            arguments = bundle
                        }

                        // Replace the current fragment with WorkoutStartFragment
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack("EXERCISE_INFORMATION")
                            .commit()
                        setFragmentVisibility(false)
                    }
                    else {
                        Toast.makeText(requireContext(), "Exercise not found", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onExerciseDelete = { workoutExercise ->
                confirmDeleteExercise(workoutExercise)
            },
            onStartDrag = { viewHolder ->
                itemTouchHelper.startDrag(viewHolder)
            },
            onMoveSelected = { workoutExercise ->
//                val action =
//                    WorkoutStartFragmentDirections.actionNavigationWorkoutStartToNavigationMoveExercise(
//                        selectedExerciseId = workoutExercise.id,
//                        selectedExercises = selectedExercises.toTypedArray()
//                    )
//                findNavController().navigate(action)
                val args = MoveExerciseFragmentArgs(
                    selectedExerciseId = workoutExercise.id,
                    selectedExercises = selectedExercises.toTypedArray()
                )

                val bundle = args.toBundle()

                val moveExerciseFragment = MoveExerciseSepFragment().apply {
                    arguments = bundle
                }

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, moveExerciseFragment)
                    .addToBackStack("StartMoveExerciseFragment")
                    .commit()

            },
            onDeleteSetsSelected = { workoutExercise ->
                confirmDeleteAllSets(workoutExercise)
            },
            context = requireContext(),
            onCheckboxChecked = { workoutExercise ->
                startTimer(workoutExercise.restTimer * 1000) // Convert seconds to milliseconds
            },
        )

        setFragmentResultListener("moveExerciseRequestKey") { key, bundle ->
            val updatedExercises: List<WorkoutExercise>? = if (SDK_INT >= TIRAMISU) {
                bundle.getParcelableArrayList("updatedExercises", WorkoutExercise::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelableArrayList("updatedExercises")
            }
            updatedExercises?.let {
                selectedExercises.clear()
                addExercises(updatedExercises)
                Log.d("WorkoutStartFragment", "Updated Exercises: $it")
            }
        }

        Log.d("WorkoutStartFragment", "selected exercises: $selectedExercises")
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Initialize itemTouchHelper with custom callback
        val callback = AutoScrollItemTouchHelperCallback(
            adapter, recyclerView, onDragEnd = {
                updateExerciseOrder() // Update exercise order when drag ends
                for (exercise in selectedExercises) {
                    exercise.isSetsVisible = true
                }
                adapter.notifyDataSetChanged()
            }
        )

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        adapter.onStartDrag = { viewHolder ->
            callback.isLongPressDragEnabled = true
            itemTouchHelper.startDrag(viewHolder)
        }

        if (!initial_exercises_added) {
            args.selectedExercises?.let { addExercises(it.toList()) }
            initial_exercises_added = true
        }

        // Observe selected exercises and update the adapter
//        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<List<WorkoutExercise>>(
//            "selectedExercises"
//        )
//            ?.observe(viewLifecycleOwner) { exercises ->
//                val selectedExSize = selectedExercises.size
//                if (returningFromExerciseSelection) {
//                    addExercises(exercises)
//                    returningFromExerciseSelection = false
//                }
//                adapter.notifyItemRangeInserted(selectedExSize - 1, selectedExSize + exercises.size)
//            }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "requestKey",
            this
        ) { requestKey, bundle ->
            val selectedExercises: List<WorkoutExercise>? =
                bundle.getParcelableArrayList("selectedExercises")
            if (selectedExercises != null) {
                val selectedExSize = selectedExercises.size
                if (returningFromExerciseSelection) {
                    addExercises(selectedExercises)
                    returningFromExerciseSelection = false
                }
                adapter.notifyItemRangeInserted(
                    selectedExSize - 1,
                    selectedExSize + selectedExercises.size
                )
            }
        }

        addExerciseButton?.setOnClickListener {

            // Create fragment instance using newInstance method
            val fragment = ExerciseSelectionSepLaunchFragment.newInstance(
                selectedExercises = emptyList(),
                selectedTypes = emptyList(),  // Pass empty lists or null if you prefer
                selectedBodyParts = emptyList()
            )


            // Begin a FragmentTransaction to add ExerciseSelectionFragment on top of the current fragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment) // Use the correct container ID
                .addToBackStack("EXERCISE_SELECTION_BACK_STACK") // Add this transaction to the back stack so pressing back returns to WorkoutStartFragment
                .commit()

            returningFromExerciseSelection = true
        }

        if (!checkboxEnabled) {
            finishWorkoutButton?.visibility = GONE
            cancelWorkoutButton?.visibility = GONE
            addExerciseButton?.visibility = GONE
        }

        finishWorkoutButton?.setOnClickListener {
            if (selectedExercises.size == 0 ||
                selectedExercises.all { it.completion.all { isComplete -> !isComplete } }){
                Toast.makeText(requireContext(), "Complete at least 1 set", Toast.LENGTH_SHORT).show()
            }
            else {
                confirmFinish()
            }
        }

        cancelWorkoutButton?.setOnClickListener {
            confirmCancel()
        }

        miniViewCancelButton.setOnClickListener {
            confirmCancel()
        }

        timerLayout = view.findViewById(R.id.timer_layout)
        timerText = view.findViewById(R.id.timer_text)
        timerHandler = Handler(Looper.getMainLooper())
        timerView = view.findViewById(R.id.timer_view)
        add30SecondsButton = view.findViewById(R.id.add_30_seconds_button)
        subtract30SecondsButton = view.findViewById(R.id.subtract_30_seconds_button)
        originalTimerText = view.findViewById(R.id.original_timer_text)

        timerLayout.setOnClickListener {
            expandTimerView()
        }

        add30SecondsButton?.setOnClickListener {
            addTimeToTimer(30000) // Add 30 seconds
        }

        subtract30SecondsButton?.setOnClickListener {
            subtractTimeFromTimer(30000) // Subtract 30 seconds
        }

        deadLinearLayout.visibility = VISIBLE
    }

    fun setFragmentVisibility(visible: Boolean) {
        viewContainer.visibility = if (visible) VISIBLE else GONE
    }

    private fun expandTimerView() {
        val dimView = dimmingView ?: return
        val timerLayout = timerLayout ?: return

        add30SecondsButton?.visibility = VISIBLE
        subtract30SecondsButton?.visibility = VISIBLE
        originalTimerText.visibility = VISIBLE

        dimView.visibility = VISIBLE
        dimView.alpha = 0f
        dimView.animate().alpha(0.5f).setDuration(300).setListener(null)

        timerLayout.pivotX = (timerLayout.width / 2).toFloat()
        timerLayout.pivotY = (timerLayout.height / 2).toFloat()

        timerLayout.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    timerLayout.setOnClickListener {
                        collapseTimerView()
                    }
                    dimView.setOnClickListener {
                        collapseTimerView()
                    }
                }
            })

    }

    private fun collapseTimerView() {
        val dimView = dimmingView ?: return
        val timerLayout = timerLayout ?: return

        add30SecondsButton?.visibility = GONE
        subtract30SecondsButton?.visibility = GONE
        originalTimerText.visibility = GONE

        dimView.animate().alpha(0f).setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {

                    dimView.visibility = View.GONE
                }
            })

        timerLayout.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Toggle back on click again
                    timerLayout.setOnClickListener {
                        expandTimerView()
                    }
                }
            })
    }

    private fun startTimer(duration: Long) {
        if (duration > 0) {
            clearAndEndTimer()
        }
        remainingTime = duration
        startTime = System.currentTimeMillis()
        val ogSeconds = remainingTime / 1000
        val ogMinutes = ogSeconds / 60
        val ogHours = ogMinutes / 60
        originalTimerText.text = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            ogHours,
            ogMinutes % 60,
            ogSeconds % 60
        )

        timerRunnable = object : Runnable {
            override fun run() {
                val millis = remainingTime - (System.currentTimeMillis() - startTime)
                if (millis > 0) {
                    val seconds = millis / 1000
                    val minutes = seconds / 60
                    val hours = minutes / 60
                    val time = String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d",
                        hours,
                        minutes % 60,
                        seconds % 60
                    )
                    timerText.text = time
                    miniViewTimerText.text = time

                    // Update the TimerView progress
                    val progress = millis.toFloat() / duration
                    timerView.setProgress(progress)
                    miniViewTimerView.setProgress(progress)

                    timerHandler?.postDelayed(this, 250)
                } else {
                    timerText.text = "00:00:00"
                    miniViewTimerText.text = "00:00:00"
                    originalTimerText.text = "00:00:00"
                    timerView.setProgress(0f)
                    miniViewTimerView.setProgress(0f)
                    stopTimer()
                }
            }
        }
        timerHandler?.postDelayed(timerRunnable!!, 0)
    }


    private fun stopTimer() {
        try {
            timerHandler?.removeCallbacks(timerRunnable!!)
        } catch (e: Exception) {
            Log.e("WorkoutStartFragment", "Error stopping timer: ${e.message}")
        }
    }

    private fun clearAndEndTimer() {
        stopTimer() // Stop the timer runnable
        remainingTime = 0 // Reset remaining time
        timerText.text = "00:00:00" // Reset timer text
        miniViewTimerText.text = "00:00:00" // Reset timer text
        originalTimerText.text = "00:00:00" // Reset original timer text
        timerView.setProgress(0f) // Reset timer view progress
        miniViewTimerView.setProgress(0f)
    }

    private fun addTimeToTimer(timeToAdd: Long) {
        if (timerText.text == "00:00:00") {
            // Special case when timer is at 00:00:00
            startTimer(timeToAdd) // Start timer with the added time
        } else {
            remainingTime += timeToAdd // Normal case: add time to remainingTime
        }
    }

    private fun subtractTimeFromTimer(timeToSubtract: Long) {
        remainingTime = maxOf(remainingTime - timeToSubtract, 0)
        if (remainingTime == 0L) {
            timerText.text = "00:00:00"
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("workoutName", workoutNameInput?.text.toString())
        outState.putString("workoutNotes", workoutNotesInput?.text.toString())
        outState.putParcelableArrayList("selectedExercises", ArrayList(selectedExercises))
    }

    private fun addExercises(exercises: List<WorkoutExercise>) {
        selectedExercises.addAll(exercises)
        updateExerciseOrder()
    }

    private fun updateExerciseOrder() {
        selectedExercises.forEachIndexed { index, exercise ->
            exercise.order = index + 1
            Log.d("updateExerciseOrder", "$exercise")
        }
    }

    private fun confirmDeleteExercise(exercise: WorkoutExercise) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_exercise_confirmation_title)
            .setMessage("${getString(R.string.delete_exercise_confirmation_message)} ${exercise.title}?")
            .setPositiveButton(R.string.yes) { dialog, _ ->
                onDeleteExercise(exercise)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }


    private fun onDeleteExercise(exercise: WorkoutExercise) {
        val position = selectedExercises.indexOf(exercise)
        if (position >= 0) {
            selectedExercises.removeAt(position)
            val adapter =
                view?.findViewById<RecyclerView>(R.id.recycler_view_workout_exercises)?.adapter as WorkoutExerciseAdapter
            adapter.notifyItemRemoved(position)
            updateExerciseOrder()
            Log.d("selectedExercises", "Selected aft delete: $selectedExercises")
        }
    }

    private fun saveWorkoutToHistory() {
        val workoutName = workoutNameInput?.text.toString()
        val workoutNotes = workoutNotesInput?.text.toString()
        val workoutDate = args.workoutDate
        val createdWorkoutId = args.workoutId

        // Calculate the time taken
        val currentTime = System.currentTimeMillis()
        val workoutStartTime = Date(workoutDate).time
        val timeTakenMillis = currentTime - workoutStartTime
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(timeTakenMillis)

        if (validateInputs(workoutName)) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                showLoading(true)

                lifecycleScope.launch {
                    // Fetch exercise categories and update exercises
                    val setsPopulatedExercises = selectedExercises.map { workoutExercise ->
                        val exercise = viewModel.getExerciseById(workoutExercise.exerciseid)

                        if (exercise != null) {
                            val filteredIndices = workoutExercise.sets.indices.filter { index ->
                                workoutExercise.completion.getOrElse(index) { false } &&
                                        workoutExercise.sets.getOrElse(index) { "" } != "W"
                            }
                            updateWorkoutExercise(filteredIndices, workoutExercise, exercise.category)
                        }
                        else {
                            workoutExercise
                        }
                    }

                    val historyWorkout = HistoryWorkout(
                        id = UUID.randomUUID().toString(),
                        title = workoutName,
                        notes = workoutNotes,
                        date = workoutDate,
                        exercises = setsPopulatedExercises,
                        length = totalMinutes.toString()
                    )
                    viewModel.saveWorkoutToHistory(historyWorkout, onSuccess={
                        Log.d("WorkoutStartFragment", "Saving Workout to History")
                        val action =
                            UpdateHistoryWorkoutFragmentDirections.actionUpdateHistoryWorkoutToNavigationWorkoutSummary(
                                workoutTitle = workoutName,
                                exercises = setsPopulatedExercises.toTypedArray(),
                                workoutNotes = historyWorkout.notes,
                                length = totalMinutes.toString(),
                                createdWorkoutId = createdWorkoutId,
                                oldExerciseIds = args.selectedExercises.map {it.id}.toTypedArray()
                            )
                        // Get the Bundle from the NavArgs
                        val bundle = action.arguments

                        val workoutSummaryFragment = WorkoutSummaryFragment()
                        workoutSummaryFragment.arguments = bundle

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, workoutSummaryFragment)
                            .addToBackStack("StartWorkoutSummary")  // Optional: add to back stack
                            .commit()

                        Toast.makeText(requireContext(), "workout saved to history",
                            Toast.LENGTH_SHORT).show()
                    }, onFailure={
                        if (isNetworkAvailable()) {
                            Log.e("WorkoutStartFragment", "$it")
                            Toast.makeText(
                                context, "Failed: save history workout",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else {
                            Toast.makeText(
                                context, "Come online to save to cloud",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }
            }
        }
    }

    private fun updateWorkoutExercise(filteredIndices: List<Int>,
                                      workoutExercise: WorkoutExercise,
                                      category: String): WorkoutExercise {

        // Sets already calculated
        if (category == "Compound" || category == "Isolation") {
            var totalRepsCount = 0
            var totalVolume = 0
            var bestMax = 0

            filteredIndices.forEach { index ->
                val reps = workoutExercise.reps.getOrElse(index) { 0 }
                val weight = workoutExercise.weights.getOrElse(index) { 0f }
                val curMax = epleyFormula(weight, reps)
                if (curMax>bestMax){
                    bestMax = curMax
                }
                totalRepsCount += reps
                totalVolume += ceil(weight * reps).toInt()
            }

            return workoutExercise.copy(
                max = bestMax,
                completedsetscount = filteredIndices.size,
                completedreps = totalRepsCount,
                volume = totalVolume
            )
        }

        else if (category == "Cardio") {
            var bestPace = 0f
            var totalDistance = 0f
            var totalTime = 0f

            filteredIndices.forEach { index ->
                val distance = workoutExercise.distances.getOrElse(index) { 0f } * 1000
                val time = workoutExercise.times.getOrElse(index) { 0f } * 3600
                val curPace = pace("km/h", distance, time)
                if (curPace>bestPace){
                    bestPace = curPace
                }
                totalDistance += distance
                totalTime += time
            }

            return workoutExercise.copy(
                bestPace = bestPace,
                completedsetscount = filteredIndices.size,
                totalDistance = totalDistance,
                totalTime = totalTime
            )
        }

        else if (category == "Duration"){
            TODO("Sigma")
            return workoutExercise
        }
        else {
            return workoutExercise
        }
    }

    private fun epleyFormula(weight: Float, reps: Int): Int {
        if (reps == 0) {
            return 0
        }
        if (reps == 1){
            return ceil(weight).toInt()
        }
        return ceil(weight * (1 + reps / 30f)).toInt()
    }

    private fun pace(mode: String, distance: Float, time: Float): Float {
        if (mode == "km/h"){
            return distance*1000/time*3600
        }
        else {
            return 0f
        }
    }

    private fun validateInputs(workoutName: String): Boolean {
        return if (workoutName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a workout name", Toast.LENGTH_SHORT)
                .show()
            false
        } else {
            true
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgress?.visibility = if (isLoading) VISIBLE else GONE
        finishWorkoutButton?.isEnabled = !isLoading
    }

    private fun confirmDeleteAllSets(exercise: WorkoutExercise) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_sets_title)
            .setMessage("${getString(R.string.clear_sets_message)} ${exercise.title}?")
            .setPositiveButton(R.string.yes) { dialog, _ ->
                deleteAllSets(exercise)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun deleteAllSets(exercise: WorkoutExercise) {
        val position = selectedExercises.indexOf(exercise)
        if (position >= 0) {
            exercise.sets.clear()
            exercise.weights.clear()
            exercise.reps.clear()
            val adapter =
                view?.findViewById<RecyclerView>(R.id.recycler_view_workout_exercises)?.adapter as WorkoutExerciseAdapter
            adapter.notifyItemChanged(position)
            Toast.makeText(
                requireContext(),
                "All sets deleted for ${exercise.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun confirmFinish() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.finish_workout_title)
            .setMessage(R.string.finish_workout_confirmation_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                saveWorkoutToHistory()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun confirmCancel() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cancel_workout_title)
            .setMessage(R.string.cancel_workout_confirmation_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
//                val action = WorkoutStartFragmentDirections.actionWorkoutStartToNavigationWorkout()
//                findNavController().navigate(action)
                dialog.dismiss()
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        viewContainer.transitionToStart()
                        delay(300)
                        (activity as? MainActivity)?.closeStartWorkoutFragments()
                        deadLinearLayout.visibility = GONE
                    }
                }
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    // Check if network is available
    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}