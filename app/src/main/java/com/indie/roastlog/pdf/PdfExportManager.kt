package com.indie.roastlog.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.indie.roastlog.ui.components.ChartDataPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt

data class RoastSessionData(
    val beanType: String,
    val waterContent: String,
    val density: String,
    val weightIn: String,
    val weightOut: String,
    val roastType: String,
    // Time & Temperature
    val chargeTimeTemp: String,
    val endTimeTemp: String,
    val roastTime: String,
    val devTime: String,
    // Event Suhu (Formatted as "Temp°C / Time")
    val turnPoint: String,
    val yellowing: String,
    val firstCrack: String,
    val endRoasting: String,
    // Parameter Mesin
    val airFlowPower: String,
    val rpmDrum: String,
    val burnerPower: String,
    val ror: String,
    // Burner Events
    val burnerEvents: List<String>,
    // Plans
    val burnerPlan: List<com.indie.roastlog.ui.model.RoastingEvent>,
    val airFlowPlan: List<com.indie.roastlog.ui.model.RoastingEvent>,
    val rpmPlan: List<com.indie.roastlog.ui.model.RoastingEvent>,
    // Events for ROR calculation
    val turnPointEvent: com.indie.roastlog.ui.model.RoastingEvent?,
    val yellowingEvent: com.indie.roastlog.ui.model.RoastingEvent?,
    val firstCrackEvent: com.indie.roastlog.ui.model.RoastingEvent?,
    val endRoastEvent: com.indie.roastlog.ui.model.RoastingEvent?,
    // Timer & Chart
    val targetDuration: Int,
    val intervalSeconds: Int,
    val startTemperature: Int,
    val temperatureData: List<ChartDataPoint>,
    val roastDate: Date = Date()
)

class PdfExportManager(private val context: Context) {

    fun exportRoastSessionToPdf(data: RoastSessionData): String? {
        val pdfDocument = PdfDocument()
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var pageNumber = 1

        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 7f
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        val sectionPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.BLACK
            textSize = 7f
            typeface = Typeface.DEFAULT_BOLD
        }

        val tableContentPaint = Paint().apply {
            color = Color.BLACK
            textSize = 7f
        }

        // Helper function to start a new page
        fun startNewPage(): Canvas {
            currentPage?.let { pdfDocument.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            currentPage = pdfDocument.startPage(pageInfo)
            return currentPage!!.canvas
        }

        // Helper to draw a row with 4 fields
        fun drawRow4(canvas: Canvas, y: Float, items: List<Pair<String, String>>): Float {
            val colWidth = 120f
            var x = 50f
            items.forEach { (label, value) ->
                canvas.drawText(label, x, y, labelPaint)
                canvas.drawText(value, x, y + 10f, valuePaint)
                x += colWidth
            }
            return y + 25f
        }

        // Helper to draw a row with 2 fields
        fun drawRow2(canvas: Canvas, y: Float, label1: String, value1: String, label2: String, value2: String): Float {
            val x1 = 50f
            val x2 = 300f
            canvas.drawText(label1, x1, y, labelPaint)
            canvas.drawText(value1, x1, y + 10f, valuePaint)
            canvas.drawText(label2, x2, y, labelPaint)
            canvas.drawText(value2, x2, y + 10f, valuePaint)
            return y + 25f
        }

        // Start first page
        canvas = startNewPage()
        val firstCanvas = canvas!!
        var yPosition = 40f

        // Title
        firstCanvas.drawText("Roast Log Report", 50f, yPosition, titlePaint)
        yPosition += 20f

        // Date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
        firstCanvas.drawText("Tanggal: ${dateFormat.format(data.roastDate)}", 50f, yPosition, labelPaint)
        yPosition += 20f

        // === Informasi Setup Roasting ===
        firstCanvas.drawText("Informasi Setup Roasting", 50f, yPosition, sectionPaint)
        yPosition += 15f

        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Bean" to data.beanType.ifEmpty { "-" },
            "Kadar Air" to "${data.waterContent.ifEmpty { "-" }}%",
            "Density" to "${data.density.ifEmpty { "-" }} kg/L",
            "Roast Type" to data.roastType.ifEmpty { "-" }
        ))

        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Berat Masuk (gr)" to data.weightIn.ifEmpty { "-" },
            "Berat Keluar (gr)" to data.weightOut.ifEmpty { "-" },
            "Charge Temp (°C)" to data.chargeTimeTemp.ifEmpty { "-" },
            "End Temp (°C)" to data.endTimeTemp.ifEmpty { "-" }
        ))

        yPosition += 5f

        // === Plans (Burner, Air Flow, RPM) ===
        firstCanvas.drawText("Rencana Mesin", 50f, yPosition, sectionPaint)
        yPosition += 15f

        // Burner Plan
        if (data.burnerPlan.isNotEmpty()) {
            firstCanvas.drawText("Burner Plan:", 50f, yPosition, labelPaint)
            yPosition += 10f
            val burnerText = data.burnerPlan.joinToString(" | ") { "${it.temperature} @ ${it.time}" }
            firstCanvas.drawText(burnerText, 50f, yPosition, valuePaint)
            yPosition += 15f
        }

        // Air Flow Plan
        if (data.airFlowPlan.isNotEmpty()) {
            firstCanvas.drawText("Air Flow Plan:", 50f, yPosition, labelPaint)
            yPosition += 10f
            val airFlowText = data.airFlowPlan.joinToString(" | ") { "${it.temperature} @ ${it.time}" }
            firstCanvas.drawText(airFlowText, 50f, yPosition, valuePaint)
            yPosition += 15f
        }

        // RPM Plan
        if (data.rpmPlan.isNotEmpty()) {
            firstCanvas.drawText("RPM Plan:", 50f, yPosition, labelPaint)
            yPosition += 10f
            val rpmText = data.rpmPlan.joinToString(" | ") { "${it.temperature} @ ${it.time}" }
            firstCanvas.drawText(rpmText, 50f, yPosition, valuePaint)
            yPosition += 15f
        }

        yPosition += 5f

        // === Hasil Roasting ===
        firstCanvas.drawText("Hasil Roasting", 50f, yPosition, sectionPaint)
        yPosition += 15f

        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Roast Time" to data.roastTime.ifEmpty { "-" },
            "Dev Time" to data.devTime.ifEmpty { "-" },
            "End Temp (°C)" to data.endTimeTemp.ifEmpty { "-" },
            "Weight Loss" to if (data.weightIn.isNotEmpty() && data.weightOut.isNotEmpty()) {
                val inWeight = data.weightIn.toFloatOrNull() ?: 0f
                val outWeight = data.weightOut.toFloatOrNull() ?: 0f
                val loss = inWeight - outWeight
                val lossPercent = if (inWeight > 0) (loss / inWeight) * 100 else 0f
                "%.1f gr (%.1f%%)".format(loss, lossPercent)
            } else "N/A"
        ))

        yPosition += 5f

        // === Milestones ===
        firstCanvas.drawText("Milestones", 50f, yPosition, sectionPaint)
        yPosition += 15f

        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Turn Point" to data.turnPoint.ifEmpty { "-" },
            "Yellowing" to data.yellowing.ifEmpty { "-" },
            "First Crack" to data.firstCrack.ifEmpty { "-" },
            "End Roast" to data.endRoasting.ifEmpty { "-" }
        ))

        yPosition += 15f

        // Temperature Profile Chart
        if (yPosition > 400f) {
            canvas = startNewPage()
            yPosition = 50f
        }

        val chartCanvas = canvas!!
        chartCanvas.drawText("Grafik Temperatur:", 50f, yPosition, sectionPaint)
        yPosition += 15f

        val chartBitmap = createChartBitmap(data)
        val chartHeight = 240 
        chartCanvas.drawBitmap(chartBitmap, 50f, yPosition, null)
        yPosition += chartHeight + 20f

        // ROR Chart
        if (yPosition > 500f) {
            canvas = startNewPage()
            yPosition = 50f
        }

        val rorCanvas = canvas!!
        rorCanvas.drawText("Grafik ROR (Rate of Rise):", 50f, yPosition, sectionPaint)
        yPosition += 15f

        val rorBitmap = createRorChartBitmap(data)
        val rorChartHeight = 120
        rorCanvas.drawBitmap(rorBitmap, 50f, yPosition, null)
        yPosition += rorChartHeight + 20f

        // Finish the last page
        currentPage?.let { pdfDocument.finishPage(it) }

        // Save PDF to Downloads
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "RoastLog_${data.beanType.replace(" ", "_")}_$timestamp.pdf"

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    "Downloads/$fileName"
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                file.absolutePath
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    private fun formatTime(intervalNum: Float, intervalSeconds: Int): String {
        val totalSeconds = (intervalNum * intervalSeconds).roundToInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (seconds == 0) "$minutes"
        else String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    private fun createChartBitmap(data: RoastSessionData): Bitmap {
        val chartWidth = 480
        val chartHeight = 240 

        val totalSeconds = data.targetDuration * 60
        val maxIntervals = if (data.intervalSeconds > 0) totalSeconds / data.intervalSeconds else 0
        val maxX = maxIntervals.toFloat()

        val chart = LineChart(context).apply {
            layoutParams = ViewGroup.LayoutParams(chartWidth, chartHeight)
            setBackgroundColor(Color.WHITE)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 70f
                axisMaximum = 240f
                textColor = Color.BLACK
                textSize = 8f
                labelCount = 18
                setLabelCount(18, true)
                setDrawLabels(true)
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                granularity = 0.1f
                textColor = Color.BLACK
                textSize = 7f 
                axisMinimum = 0f
                axisMaximum = maxX
                val step = maxOf(1, maxIntervals / 8)
                setLabelCount((maxIntervals / step) + 1, true)
                labelRotationAngle = -45f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatTime(value, data.intervalSeconds)
                    }
                }
            }
        }

        val entries = data.temperatureData.mapNotNull { point ->
            point.temperature?.let { Entry(point.intervalNumber, it.toFloat()) }
        }

        val dataSet = LineDataSet(entries, "Temperature").apply {
            color = "#2196F3".toColorInt()
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor("#2196F3".toColorInt())
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        chart.data = LineData(dataSet)

        chart.measure(
            View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
        )
        chart.layout(0, 0, chartWidth, chartHeight)

        val bitmap = createBitmap(chartWidth, chartHeight)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)

        return bitmap
    }

    private fun createRorChartBitmap(data: RoastSessionData): Bitmap {
        val chartWidth = 480
        val chartHeight = 120

        val totalSeconds = data.targetDuration * 60
        val maxIntervals = if (data.intervalSeconds > 0) totalSeconds / data.intervalSeconds else 0
        val maxX = maxIntervals.toFloat()

        // Build ROR data with interpolation like in RoastingDetailScreen
        val rorData = buildRorDataForPdf(data)

        val chart = LineChart(context).apply {
            layoutParams = ViewGroup.LayoutParams(chartWidth, chartHeight)
            setBackgroundColor(Color.WHITE)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(false)
            setScaleEnabled(false)
            setPinchZoom(false)

            axisLeft.apply {
                setDrawGridLines(true)
                setDrawLabels(true)
                textColor = Color.BLACK
                textSize = 8f
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 0.1f
                textColor = Color.BLACK
                textSize = 7f
                axisMinimum = 0f
                axisMaximum = maxX
                val step = maxOf(1, maxIntervals / 8)
                setLabelCount((maxIntervals / step) + 1, true)
                labelRotationAngle = -45f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatTime(value, data.intervalSeconds)
                    }
                }
            }
        }

        val entries = rorData.mapNotNull { point ->
            point.ror?.let { Entry(point.intervalNumber, it.toFloat()) }
        }

        if (entries.isNotEmpty()) {
            val dataSet = LineDataSet(entries, "ROR").apply {
                color = "#4CAF50".toColorInt()
                lineWidth = 2f
                setDrawCircles(true)
                setCircleColor("#4CAF50".toColorInt())
                circleRadius = 2f
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
            }

            chart.data = LineData(dataSet)
            chart.axisLeft.axisMinimum = entries.minOf { it.y } * 0.9f
            chart.axisLeft.axisMaximum = entries.maxOf { it.y } * 1.1f
        }

        chart.measure(
            View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
        )
        chart.layout(0, 0, chartWidth, chartHeight)

        val bitmap = createBitmap(chartWidth, chartHeight)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)

        return bitmap
    }

    // Build ROR data combining interval data and events with interpolation
    private fun buildRorDataForPdf(data: RoastSessionData): List<ChartDataPoint> {
        val interval = data.intervalSeconds
        val totalSeconds = data.targetDuration * 60
        val maxIntervals = totalSeconds / interval

        // Build all data points (interval data + events)
        val allPoints = mutableListOf<Pair<Int, Int>>()

        // Add interval data points
        data.temperatureData.forEach { point ->
            point.temperature?.let { temp ->
                allPoints.add(point.totalSeconds to temp)
            }
        }

        // Add event points
        data.turnPointEvent?.let { allPoints.add(it.seconds to it.temperature) }
        data.yellowingEvent?.let { allPoints.add(it.seconds to it.temperature) }
        data.firstCrackEvent?.let { allPoints.add(it.seconds to it.temperature) }
        data.endRoastEvent?.let { allPoints.add(it.seconds to it.temperature) }

        val sortedPoints = allPoints.sortedBy { it.first }

        // Build positions (intervals only for ROR chart to keep it clean)
        val positions = (0..maxIntervals).map { i ->
            i.toFloat() to (i * interval)
        }

        // Calculate ROR for each position
        return positions.mapNotNull { (intervalNum, seconds) ->
            val rorValue = calculateRor(seconds, sortedPoints)
            rorValue?.let {
                ChartDataPoint(
                    intervalNumber = intervalNum,
                    totalSeconds = seconds,
                    temperature = getTemperatureAtSeconds(seconds, sortedPoints),
                    ror = rorValue
                )
            }
        }
    }

    private fun getTemperatureAtSeconds(seconds: Int, allPoints: List<Pair<Int, Int>>): Int? {
        if (allPoints.isEmpty()) return null

        allPoints.find { it.first == seconds }?.let { return it.second }

        val before = allPoints.lastOrNull { it.first < seconds }
        val after = allPoints.firstOrNull { it.first > seconds }

        return when {
            before != null && after != null -> {
                val ratio = (seconds - before.first).toFloat() / (after.first - before.first)
                (before.second + ratio * (after.second - before.second)).toInt()
            }
            before != null -> before.second
            after != null -> after.second
            else -> null
        }
    }

    private fun calculateRor(seconds: Int, allPoints: List<Pair<Int, Int>>): Int? {
        if (seconds == 0) return 0

        val currentTemp = getTemperatureAtSeconds(seconds, allPoints) ?: return null
        val prevTemp = allPoints.sortedBy { it.first }.lastOrNull { it.first < seconds }?.second ?: return null
        return currentTemp - prevTemp
    }
}
