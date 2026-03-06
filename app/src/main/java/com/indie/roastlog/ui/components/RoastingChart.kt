package com.indie.roastlog.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class ChartDataPoint(
    val intervalNumber: Float, // Menggunakan Float agar presisi detik/menit
    val totalSeconds: Int,   
    val temperature: Int?,  // null if not yet input
    val ror: Int?, // kenaikan suhu per interval (Rate of Rise) - tetap Int
    val airFlowPower: String = "",
    val rpmDrum: String = "",
    val burnerPower: String = ""
)

@Composable
fun RoastingChart(
    data: List<ChartDataPoint>,
    intervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(300.dp)
    ) {
        // Chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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

        // Chart area only - no ROR/AirFlow/RPM/Burner here anymore
    }
}

@Composable
fun RoastingChartRor(
    data: List<ChartDataPoint>,
    intervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "ROR (kenaikan suhu bean per menit)",
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            val chartWidth = maxOf(300, data.size * 30)
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setupHorizontalChart()
                        setRorData(data)
                        invalidate()
                    }
                },
                update = { chart ->
                    chart.setRorData(data)
                    chart.invalidate()
                },
                modifier = Modifier
                    .width(chartWidth.dp)
                    .height(40.dp)
            )
        }
    }
}

@Composable
fun RoastingChartAirFlow(
    data: List<ChartDataPoint>,
    intervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "Air Flow Power (besaran buangan asap)",
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            val chartWidth = maxOf(300, data.size * 30)
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setupHorizontalChart()
                        setAirFlowData(data)
                        invalidate()
                    }
                },
                update = { chart ->
                    chart.setAirFlowData(data)
                    chart.invalidate()
                },
                modifier = Modifier
                    .width(chartWidth.dp)
                    .height(40.dp)
            )
        }
    }
}

@Composable
fun RoastingChartRpm(
    data: List<ChartDataPoint>,
    intervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "RPM Drum (kecepatan putaran drum)",
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            val chartWidth = maxOf(300, data.size * 30)
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setupHorizontalChart()
                        setRpmData(data)
                        invalidate()
                    }
                },
                update = { chart ->
                    chart.setRpmData(data)
                    chart.invalidate()
                },
                modifier = Modifier
                    .width(chartWidth.dp)
                    .height(40.dp)
            )
        }
    }
}

@Composable
fun RoastingChartBurner(
    data: List<ChartDataPoint>,
    intervalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = "Burner Power (besaran tekanan api)",
            fontSize = 10.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            val chartWidth = maxOf(300, data.size * 30)
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setupHorizontalChart()
                        setBurnerData(data)
                        invalidate()
                    }
                },
                update = { chart ->
                    chart.setBurnerData(data)
                    chart.invalidate()
                },
                modifier = Modifier
                    .width(chartWidth.dp)
                    .height(40.dp)
            )
        }
    }
}

private fun LineChart.setupHorizontalChart() {
    description.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(false)
    setScaleEnabled(false)
    setPinchZoom(false)
    setDrawGridBackground(false)
    setBackgroundColor(Color.TRANSPARENT)

    xAxis.apply {
        isEnabled = false
        setDrawGridLines(false)
        setDrawLabels(false)
        setDrawAxisLine(false)
    }

    axisLeft.apply {
        isEnabled = false
        setDrawGridLines(false)
        setDrawLabels(false)
        setDrawAxisLine(false)
        axisMinimum = 0f
        axisMaximum = 1f
    }

    axisRight.isEnabled = false
}

private fun LineChart.setRorData(data: List<ChartDataPoint>) {
    if (data.isEmpty()) {
        clear()
        return
    }

    val maxX = data.maxOf { it.intervalNumber }
    xAxis.axisMaximum = maxX

    val entries = data.mapNotNull { point ->
        point.ror?.let { Entry(point.intervalNumber, 0.5f, it.toString()) }
    }

    val dataSet = LineDataSet(entries, "ROR").apply {
        color = "#4CAF50".toColorInt()
        lineWidth = 0f
        setDrawCircles(true)
        setCircleColor("#4CAF50".toColorInt())
        circleRadius = 2f
        setDrawCircleHole(false)
        mode = LineDataSet.Mode.LINEAR
        setDrawValues(true)
        valueTextSize = 10f
        valueTextColor = Color.BLACK
        valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.data?.toString() ?: "-"
            }
        }
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(false)
    }

    this.data = if (entries.isEmpty()) null else LineData(dataSet)
}

private fun LineChart.setAirFlowData(data: List<ChartDataPoint>) {
    if (data.isEmpty()) {
        clear()
        return
    }

    val maxX = data.maxOf { it.intervalNumber }
    xAxis.axisMaximum = maxX

    val entries = data.map { point ->
        val displayValue = point.airFlowPower.ifEmpty { "-" }
        Entry(point.intervalNumber, 0.5f, displayValue)
    }

    val dataSet = LineDataSet(entries, "AirFlow").apply {
        color = "#2196F3".toColorInt()
        lineWidth = 0f
        setDrawCircles(true)
        setCircleColor("#2196F3".toColorInt())
        circleRadius = 2f
        setDrawCircleHole(false)
        mode = LineDataSet.Mode.LINEAR
        setDrawValues(true)
        valueTextSize = 10f
        valueTextColor = Color.BLACK
        valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.data?.toString() ?: "-"
            }
        }
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(false)
    }

    this.data = if (entries.isEmpty()) null else LineData(dataSet)
}

private fun LineChart.setRpmData(data: List<ChartDataPoint>) {
    if (data.isEmpty()) {
        clear()
        return
    }

    val maxX = data.maxOf { it.intervalNumber }
    xAxis.axisMaximum = maxX

    val entries = data.map { point ->
        val displayValue = point.rpmDrum.ifEmpty { "-" }
        Entry(point.intervalNumber, 0.5f, displayValue)
    }

    val dataSet = LineDataSet(entries, "RPM").apply {
        color = "#FF9800".toColorInt()
        lineWidth = 0f
        setDrawCircles(true)
        setCircleColor("#FF9800".toColorInt())
        circleRadius = 2f
        setDrawCircleHole(false)
        mode = LineDataSet.Mode.LINEAR
        setDrawValues(true)
        valueTextSize = 10f
        valueTextColor = Color.BLACK
        valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.data?.toString() ?: "-"
            }
        }
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(false)
    }

    this.data = if (entries.isEmpty()) null else LineData(dataSet)
}

private fun LineChart.setBurnerData(data: List<ChartDataPoint>) {
    if (data.isEmpty()) {
        clear()
        return
    }

    val maxX = data.maxOf { it.intervalNumber }
    xAxis.axisMaximum = maxX

    val entries = data.map { point ->
        val displayValue = point.burnerPower.ifEmpty { "-" }
        Entry(point.intervalNumber, 0.5f, displayValue)
    }

    val dataSet = LineDataSet(entries, "Burner").apply {
        color = "#F44336".toColorInt()
        lineWidth = 0f
        setDrawCircles(true)
        setCircleColor("#F44336".toColorInt())
        circleRadius = 2f
        setDrawCircleHole(false)
        mode = LineDataSet.Mode.LINEAR
        setDrawValues(true)
        valueTextSize = 10f
        valueTextColor = Color.BLACK
        valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return entry?.data?.toString() ?: "-"
            }
        }
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(false)
    }

    this.data = if (entries.isEmpty()) null else LineData(dataSet)
}

private fun LineChart.setupChart() {
    description.isEnabled = false
    legend.isEnabled = false
    setTouchEnabled(true)
    setScaleEnabled(true)
    setPinchZoom(false)
    setDrawGridBackground(false)
    setBackgroundColor(Color.TRANSPARENT)

    xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        setDrawGridLines(true)
        granularity = 0.1f 
        textColor = Color.BLACK
        textSize = 11f
        axisMinimum = 0f
        labelRotationAngle = -45f
    }

    axisLeft.apply {
        setDrawGridLines(true)
        axisMinimum = 70f
        axisMaximum = 240f
        textColor = Color.BLACK
        textSize = 12f
        labelCount = 18
    }

    axisRight.isEnabled = false
}

private fun LineChart.setData(data: List<ChartDataPoint>, intervalSeconds: Int) {
    if (data.isEmpty()) {
        clear()
        return
    }

    val maxX = data.maxOf { it.intervalNumber }
    xAxis.axisMaximum = maxX
    xAxis.valueFormatter = TimeAxisFormatter(intervalSeconds)
    
    // Tampilkan label secukupnya agar tidak bertumpuk
    xAxis.setLabelCount(minOf(data.size, 15), false)

    val entries = data.mapNotNull { point ->
        point.temperature?.let { Entry(point.intervalNumber, it.toFloat()) }
    }

    val dataSet = LineDataSet(entries, "Temperature").apply {
        color = "#2196F3".toColorInt()
        lineWidth = 2f
        setDrawCircles(true)
        setCircleColor("#2196F3".toColorInt())
        circleRadius = 3f
        setDrawCircleHole(false)
        mode = LineDataSet.Mode.LINEAR
        setDrawValues(false)
        
        // Hapus garis kuning (highlight indicator) yang muncul saat titik dipilih
        setDrawHorizontalHighlightIndicator(false)
        setDrawVerticalHighlightIndicator(false)
    }

    this.data = if (entries.isEmpty()) null else LineData(dataSet)
}

private class TimeAxisFormatter(private val intervalSeconds: Int) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val totalSeconds = (value * intervalSeconds).roundToInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
