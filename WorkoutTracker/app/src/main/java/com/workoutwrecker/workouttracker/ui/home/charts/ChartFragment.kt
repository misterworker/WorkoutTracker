/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.home.charts

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.firebase.auth.FirebaseAuth
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.databinding.FragmentHomeChartsBinding
import com.workoutwrecker.workouttracker.ui.history.HistoryWorkoutViewModel
import com.workoutwrecker.workouttracker.ui.record.PersonalRecordViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class ChartFragment : Fragment() {

    private var _binding: FragmentHomeChartsBinding? = null
    private val binding get() = _binding!!
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    private lateinit var chartType: String
    private lateinit var homeVisualFrame: ConstraintLayout

    private val historyWorkoutViewModel: HistoryWorkoutViewModel by viewModels()
    private val personalRecordViewModel: PersonalRecordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeChartsBinding.inflate(inflater, container, false)
        arguments?.let {
            chartType = it.getString("argsChart") ?: "weeklyWorkoutCounts"
        }
        homeVisualFrame = binding.homeVisualFrame

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (chartType){
            "Workouts Done" -> {
                val barChart = binding.weeklyCounts
                barChart.visibility = View.VISIBLE

                fetchHistoryWorkoutsForWeeks(12, "weeklyCounts")

                binding.filterIcon.setOnClickListener{
                    showWeekSelectionPopup(it, "weeklyCounts")
                }

                historyWorkoutViewModel.weeklyWorkoutCounts.observe(viewLifecycleOwner) { weeklyCounts ->
                    val sortedWeeklyCounts = weeklyCounts.sortedBy { it.x } //Need to sort otherwise crash
                    val barDataSet = BarDataSet(sortedWeeklyCounts, getString(R.string.weekly_workouts))

                    barDataSet.colors = ColorTemplate.LIBERTY_COLORS.toList()
                    barDataSet.setValueTextColors(context?.let { listOf(it.getColor(R.color.text)) })

                    val barData = BarData(barDataSet)

                    // Determine the maximum value in the data set
                    val maxValue = sortedWeeklyCounts.maxOfOrNull { it.y } ?: 0f

                    // Set up the bar chart with the max value
                    setupBarChart(barChart, maxValue)

                    barChart.data = barData
                    barChart.invalidate()
                }
            }
            "Time Spent" -> {
                val lineChart = binding.weeklyTimeTaken
                lineChart.visibility = View.VISIBLE
                fetchHistoryWorkoutsForWeeks(12, "timeTaken")
                binding.filterIcon.setOnClickListener{
                    showWeekSelectionPopup(it, "timeTaken")
                }

                historyWorkoutViewModel.weeklyWorkoutTimeTaken.observe(viewLifecycleOwner) { timeTaken ->
                    val sortedTimeTaken = timeTaken.sortedBy { it.x } //Need to sort otherwise crash
                    val lineDataSet = LineDataSet(sortedTimeTaken, "Minutes Spent")

                    lineDataSet.colors = ColorTemplate.LIBERTY_COLORS.toList()
                    lineDataSet.setValueTextColors(context?.let { listOf(it.getColor(R.color.text)) })

                    val lineData = LineData(lineDataSet)
                    Log.d("sortedLineData", "$sortedTimeTaken")

                    // Determine the maximum value in the data set
                    val maxValue = sortedTimeTaken.maxOfOrNull { it.y } ?: 0f

                    setupLineChart(lineChart, maxValue)

                    lineChart.data = lineData
                    lineChart.invalidate()
                }
            }
            "Radar Chart" -> {
                val radarChart = binding.radarChart
                radarChart.visibility = View.VISIBLE

                binding.filterIcon.setOnClickListener{
                    showWeekSelectionPopup(it, "radarChart")
                }

                // Fetch workout data for the radar chart
                fetchHistoryWorkoutsForWeeks(12, "radarChart")
                personalRecordViewModel.loadAllPersonalRecords()
                personalRecordViewModel.personalRecords.observe(viewLifecycleOwner) { personalRecords ->
                    Log.d("ChartFragment", "Personal Records: $personalRecords")
                    historyWorkoutViewModel.updatePersonalRecord(personalRecords)
                }

                // Observe the consistency data from the ViewModel
                historyWorkoutViewModel.performanceMetrics.observe(viewLifecycleOwner) { metrics ->
                    Log.d("ChartFragment", "Metrics: $metrics")
                    val consistency = metrics[0]
                    Log.d("ChartFragment", "Workout Consistency: $consistency")


                    // Dummy data for radar chart
                    val dummyData = listOf(
                        RadarEntry(40f),
                        RadarEntry(50f),
                        RadarEntry(90f),
                        RadarEntry(60f),
                        RadarEntry(consistency.toFloat())
                    )

                    // Create a dataset for the radar chart
                    val radarDataSet = RadarDataSet(dummyData, "Workout Categories")
                    radarDataSet.color = ColorTemplate.COLORFUL_COLORS[0]
                    radarDataSet.fillColor = ColorTemplate.COLORFUL_COLORS[0]
                    radarDataSet.setDrawFilled(true)
                    radarDataSet.lineWidth = 2f
                    radarDataSet.fillAlpha = 80

                    // Create radar data object
                    val radarData = RadarData(radarDataSet)

                    radarChart.data = radarData
                    setupRadarChart(radarChart)
                }
            }
        }
    }

    private fun setupBarChart(barChart: BarChart, maxValue: Float) {
        barChart.description.isEnabled = false

        // Enable touch gestures and highlighting
        barChart.isHighlightPerTapEnabled = false
        barChart.setTouchEnabled(true)
        barChart.isDoubleTapToZoomEnabled = false

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = WeekValueFormatter()
        xAxis.labelRotationAngle = 272f
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f // Set interval of 1
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        val rightAxis = barChart.axisRight
        rightAxis.isEnabled = false
        rightAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        // Add limit lines for each integer value up to the maximum value
        for (i in 1..<maxValue.toInt()) {
            val limitLine = LimitLine(i.toFloat(), i.toString())
            limitLine.lineWidth = 2f
            limitLine.lineColor = ContextCompat.getColor(requireContext(), R.color.screen)
            limitLine.label = null

            leftAxis.addLimitLine(limitLine)
        }

        barChart.animateY(1000)
        barChart.legend.isEnabled = true
        barChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text)
    }

    private fun setupLineChart(lineChart: LineChart, maxValue: Float) {
        Log.d("setupLineChart", "maxValue: $maxValue")
        lineChart.description.isEnabled = false

        // Enable touch gestures and highlighting
        lineChart.isHighlightPerTapEnabled = true
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = WeekValueFormatter()
        xAxis.labelRotationAngle = 272f
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.axisMinimum = 0f
        leftAxis.granularity = 1f // Set interval of 1
        leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false // Typically not needed for line charts, can enable if you have additional data.
        rightAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        lineChart.animateY(1000)
        lineChart.legend.isEnabled = false
        lineChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text)
    }

    private fun setupRadarChart(radarChart: RadarChart) {
        radarChart.setTouchEnabled(true)
        radarChart.description.isEnabled = false

        val yAxis = radarChart.yAxis
        val xAxis = radarChart.xAxis
        // Set maximum value for the Y-axis
        yAxis.axisMaximum = 100f
        yAxis.axisMinimum = 0f
        yAxis.setLabelCount(5, true)
        yAxis.isEnabled = false
        yAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)


        // Set default labels for the radar chart's axes
        xAxis.valueFormatter = object : ValueFormatter() {
            private val categories = arrayOf("Str", "Car", "PRs", "Four", "Con")

            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                return categories[value.toInt() % categories.size] // Simple mapping
            }
        }
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.setLabelCount(5, true)
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text)

        // Show values at each point (optional)
        radarChart.data.setValueFormatter(object : ValueFormatter() {
            fun getPointLabel(entry: RadarEntry): String {
                return entry.y.toInt().toString() // Show the value of each entry
            }
        })

        // Refresh the radar chart to display the data
        radarChart.invalidate()
        radarChart.animateY(1000)

        // Optionally set the legend
        radarChart.legend.isEnabled = true
    }


    // Function to show the popup menu
    private fun showWeekSelectionPopup(view: View, chart: String) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.stats_home_weekly_workouts_count)  // Use your popup_menu.xml

        // Set item click listener
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.last_8_weeks -> fetchHistoryWorkoutsForWeeks(8, chart)
                R.id.last_12_weeks -> fetchHistoryWorkoutsForWeeks(12, chart)
                R.id.last_16_weeks -> fetchHistoryWorkoutsForWeeks(16, chart)
                R.id.last_20_weeks -> fetchHistoryWorkoutsForWeeks(20, chart)
                R.id.last_24_weeks -> fetchHistoryWorkoutsForWeeks(24, chart)
            }
            true
        }
        popupMenu.show()  // Show the popup menu
    }

    // Function to launch coroutines based on the selected number of weeks
    private fun fetchHistoryWorkoutsForWeeks(weeks: Int, chart: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.awaitAll(
                async { historyWorkoutViewModel.fetchHistoryWorkoutsLastWeeks(
                    weeks, chart) },  // Pass selected weeks
                async { historyWorkoutViewModel.fetchHistoryWorkouts(userId) }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CustomMarkerView(context: Context, layoutResource: Int) :

    MarkerView(context, layoutResource) {
    private val textView: TextView = findViewById(R.id.marker_text)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        textView.text = "Week ${e?.x?.toInt()}: ${e?.y?.toInt()} workout(s)"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}

class WeekValueFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    override fun getFormattedValue(value: Float): String {
        val calendar = Calendar.getInstance()

        // Set to the first day of the year
        calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
        calendar.set(Calendar.WEEK_OF_YEAR, value.toInt())
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        return dateFormat.format(calendar.time)
    }
}

class RadarValueFormatter : ValueFormatter() {
    private val categories = arrayOf("Strength", "Endurance", "Flexibility", "Speed", "PRs")

    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
        return categories[value.toInt()]
    }
}