package com.indie.roastlog.ui.components

import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.nativeCanvas
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
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.height(600.dp)
    ) {
        // Chart area
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
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

        // ROR, Air Flow, RPM, Burner labels row - aligned with chart
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .fillMaxWidth()
        ) {
            val columnWidth = 30f // Same as chart's column width
            val textColor = ComposeColor(MaterialTheme.colorScheme.onSurface.hashCode())

            Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                // ROR Row Label
                Text(
                    text = "ROR (kenaikan suhu bean per menit)",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                // ROR Row Canvas
                Canvas(
                    modifier = Modifier
                        .width(maxOf(300, data.size * 30).dp)
                        .height(20.dp)
                ) {
                    val paint = android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    
                    data.forEach { point ->
                        val x = (point.intervalNumber * columnWidth) + (columnWidth / 2)
                        val rorText = when {
                            point.ror == null -> "-"
                            else -> point.ror.toString()
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            rorText,
                            x,
                            14f, // baseline y position
                            paint
                        )
                    }
                }

                // Air Flow Power Row Label
                Text(
                    text = "Air Flow Power (besaran buangan asap)",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
                // Air Flow Power Row Canvas
                Canvas(
                    modifier = Modifier
                        .width(maxOf(300, data.size * 30).dp)
                        .height(20.dp)
                ) {
                    val paint = android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    
                    data.forEach { point ->
                        val x = (point.intervalNumber * columnWidth) + (columnWidth / 2)
                        val airFlowText = point.airFlowPower.ifEmpty { "-" }
                        drawContext.canvas.nativeCanvas.drawText(
                            airFlowText,
                            x,
                            14f,
                            paint
                        )
                    }
                }

                // RPM Drum Row Label
                Text(
                    text = "RPM Drum (kecepatan putaran drum)",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
                // RPM Drum Row Canvas
                Canvas(
                    modifier = Modifier
                        .width(maxOf(300, data.size * 30).dp)
                        .height(20.dp)
                ) {
                    val paint = android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    
                    data.forEach { point ->
                        val x = (point.intervalNumber * columnWidth) + (columnWidth / 2)
                        val rpmText = point.rpmDrum.ifEmpty { "-" }
                        drawContext.canvas.nativeCanvas.drawText(
                            rpmText,
                            x,
                            14f,
                            paint
                        )
                    }
                }

                // Burner Power Row Label
                Text(
                    text = "Burner Power (besaran tekanan api)",
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
                // Burner Power Row Canvas
                Canvas(
                    modifier = Modifier
                        .width(maxOf(300, data.size * 30).dp)
                        .height(20.dp)
                ) {
                    val paint = android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    
                    data.forEach { point ->
                        val x = (point.intervalNumber * columnWidth) + (columnWidth / 2)
                        val burnerText = point.burnerPower.ifEmpty { "-" }
                        drawContext.canvas.nativeCanvas.drawText(
                            burnerText,
                            x,
                            14f,
                            paint
                        )
                    }
                }
            }
        }
    }
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
        circleRadius = 5f
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
