package com.indie.roastlog.ui.components

import android.graphics.Color
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale

data class ChartDataPoint(
    val intervalNumber: Int, // 0, 1, 2, 3...
    val totalSeconds: Int,   // 0, 30, 60, 90...
    val temperature: Float?  // null if not yet input
)

@Composable
fun RoastingChart(
    data: List<ChartDataPoint>,
    intervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.horizontalScroll(scrollState)
    ) {
        // Calculate width: min 300dp, or 30dp per data point (smaller spacing for X axis)
        val chartWidth = maxOf(300, data.size * 30)

        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    setupChart()
                    setData(data, intervalSeconds)
                    invalidate()
                }
            },
            update = { chart ->
                chart.setupChart()
                chart.setData(data, intervalSeconds)
                chart.invalidate()
            },
            modifier = Modifier
                .width(chartWidth.dp)
                .fillMaxHeight()
        )
    }
}

private fun LineChart.setupChart() {
    description.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    setScaleEnabled(true)
    setPinchZoom(false) // Disable pinch zoom for horizontal scroll
    setDrawGridBackground(false)
    setBackgroundColor(Color.TRANSPARENT)

    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(true)
        granularity = 1f
        textColor = Color.BLACK
        textSize = 11f
        axisMinimum = 0f
        labelRotationAngle = -45f // Rotate labels for better fit
    }

    axisLeft.apply {
        setDrawGridLines(true)
        axisMinimum = 70f
        axisMaximum = 240f
        textColor = Color.BLACK
        textSize = 12f
        labelCount = 18 // Show all values: 70, 80, 90, 100, 110, ... 240
    }

    axisRight.isEnabled = false
}

private fun LineChart.setData(data: List<ChartDataPoint>, intervalSeconds: Int) {
    if (data.isEmpty()) {
        clear()
        return
    }

    // Set X axis maximum and formatter
    val maxX = (data.size - 1).toFloat()
    xAxis.axisMaximum = maxX
    xAxis.valueFormatter = TimeAxisFormatter(intervalSeconds)
    
    // Force show all labels (set label count to data size and don't force)
    xAxis.setLabelCount(data.size, false)
    xAxis.granularity = 1f

    // Create entries - use interval number as X value
    val entries = data.mapNotNull { point ->
        point.temperature?.let { Entry(point.intervalNumber.toFloat(), it) }
    }

    // Always create dataset even if empty to show axis
    val dataSet = LineDataSet(entries, "Temperature").apply {
        color = "#2196F3".toColorInt()
        lineWidth = 2f
        setDrawCircles(true)
        setCircleColor("#2196F3".toColorInt())
        circleRadius = 5f
        setDrawCircleHole(false)
        mode = LineDataSet.Mode.LINEAR // Linear for clearer data points
        setDrawValues(false)
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(false)

        // If no entries, create invisible placeholder to keep axis visible
        if (entries.isEmpty()) {
            addEntry(Entry(0f, 70f))
            clear() // Clear but axis remains
        }
    }

    this.data = if (entries.isEmpty()) null else LineData(dataSet)
}

private class TimeAxisFormatter(private val intervalSeconds: Int) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val intervalNum = value.toInt()
        val totalSeconds = intervalNum * intervalSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return when {
            intervalSeconds >= 60 -> {
                // Show whole minutes: 0, 1, 2, 3... (showing interval point in minutes)
                "$minutes"
            }
            else -> {
                // For intervals less than 60 seconds, show minutes.decimalSeconds
                // e.g., interval 10s: 0.00, 0.10, 0.20, 0.30, 0.40, 0.50, 1.00, 1.10...
                // e.g., interval 30s: 0.00, 0.30, 1.00, 1.30, 2.00...
                String.format(Locale.getDefault(), "%d.%02d", minutes, seconds)
            }
        }
    }
}
