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

        // === Informasi Bean ===
        firstCanvas.drawText("Informasi Bean", 50f, yPosition, sectionPaint)
        yPosition += 15f

        firstCanvas.drawText("Jenis Bean", 50f, yPosition, labelPaint)
        firstCanvas.drawText(data.beanType.ifEmpty { "-" }, 50f, yPosition + 10f, valuePaint)
        yPosition += 22f

        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Kadar Air (°)" to data.waterContent.ifEmpty { "-" },
            "Density (kg/L)" to data.density.ifEmpty { "-" },
            "Berat Masuk (gr)" to data.weightIn.ifEmpty { "-" },
            "Berat Keluar (gr)" to data.weightOut.ifEmpty { "-" }
        ))

        val weightLoss = if (data.weightIn.isNotEmpty() && data.weightOut.isNotEmpty()) {
            val inWeight = data.weightIn.toFloatOrNull() ?: 0f
            val outWeight = data.weightOut.toFloatOrNull() ?: 0f
            val loss = inWeight - outWeight
            val lossPercent = if (inWeight > 0) (loss / inWeight) * 100 else 0f
            "%.1f gr (%.1f%%)".format(loss, lossPercent)
        } else "N/A"
        yPosition = drawRow2(firstCanvas, yPosition,
            "Weight Loss", weightLoss,
            "Roasted Type", data.roastType.ifEmpty { "-" }
        )

        // === Time & Temperature ===
        firstCanvas.drawText("Time & Temperature", 50f, yPosition, sectionPaint)
        yPosition += 15f

        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Charge Temp (°C)" to data.chargeTimeTemp.ifEmpty { "-" },
            "End Temp (°C)" to data.endTimeTemp.ifEmpty { "-" },
            "Roast Time" to data.roastTime.ifEmpty { "-" },
            "Dev Time" to data.devTime.ifEmpty { "-" }
        ))

        yPosition += 5f

        // === Event Roasting ===
        firstCanvas.drawText("Event Roasting", 50f, yPosition, sectionPaint)
        yPosition += 15f

        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Turn Point" to data.turnPoint.ifEmpty { "-" },
            "Yellowing" to data.yellowing.ifEmpty { "-" },
            "First Crack" to data.firstCrack.ifEmpty { "-" },
            "End Roasting" to data.endRoasting.ifEmpty { "-" }
        ))

        yPosition += 5f

        // === Machine Log Table ===
        firstCanvas.drawText("Machine Parameters Log", 50f, yPosition, sectionPaint)
        yPosition += 15f

        // Draw Table Header
        val headers = listOf("Time", "Temp", "ROR", "Air", "RPM", "Burner")
        val colWidths = listOf(60f, 60f, 60f, 60f, 60f, 60f)
        var currentX = 50f
        headers.forEachIndexed { i, header ->
            firstCanvas.drawText(header, currentX, yPosition, tableHeaderPaint)
            currentX += colWidths[i]
        }
        yPosition += 12f

        // Draw Table Content (Filter only whole interval points like in the app)
        data.temperatureData.forEach { point ->
            // Check if we need a new page
            if (yPosition > 800f) {
                canvas = startNewPage()
                yPosition = 50f
                // Redraw header on new page if needed, but for simplicity let's just continue
            }
            
            val timeStr = formatTime(point.intervalNumber.toInt(), data.intervalSeconds)
            val tempStr = point.temperature?.toString() ?: "-"
            val rorStr = point.ror?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-"
            
            currentX = 50f
            val rowValues = listOf(timeStr, tempStr, rorStr, point.airFlowPower, point.rpmDrum, point.burnerPower)
            rowValues.forEachIndexed { i, value ->
                canvas!!.drawText(value.ifEmpty { "-" }, currentX, yPosition, tableContentPaint)
                currentX += colWidths[i]
            }
            yPosition += 10f
        }

        yPosition += 15f

        // Temperature Profile (Diagram)
        if (yPosition > 400f) {
            canvas = startNewPage()
            yPosition = 50f
        }

        val chartCanvas = canvas!!
        chartCanvas.drawText("Temperature Profile:", 50f, yPosition, sectionPaint)
        yPosition += 15f

        val chartBitmap = createChartBitmap(data)
        val chartHeight = 240 
        chartCanvas.drawBitmap(chartBitmap, 50f, yPosition, null)
        yPosition += chartHeight + 10f

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

    private fun formatTime(intervalNum: Int, intervalSeconds: Int): String {
        val totalSeconds = intervalNum * intervalSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (intervalSeconds >= 60) "$minutes"
        else String.format(Locale.getDefault(), "%d.%02d", minutes, seconds)
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
                granularity = 1f
                textColor = Color.BLACK
                textSize = 7f 
                axisMinimum = 0f
                axisMaximum = maxX
                val step = maxOf(1, maxIntervals / 8)
                setLabelCount((maxIntervals / step) + 1, true)
                labelRotationAngle = -45f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatTime(value.toInt(), data.intervalSeconds)
                    }
                }
            }
        }

        val entries = data.temperatureData.mapNotNull { point ->
            point.temperature?.let { Entry(point.intervalNumber.toFloat(), it.toFloat()) }
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
}
