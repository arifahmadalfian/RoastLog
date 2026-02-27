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
    // Event Suhu
    val turnPoint: String,
    val yellowing: String,
    val firstCrack: String,
    // Parameter Mesin
    val airFlowPower: String,
    val rpmDrum: String,
    val burnerPower: String,
    val ror: String,
    // Timer & Chart
    val targetDuration: Int,
    val intervalSeconds: Int,
    val startTemperature: Float,
    val temperatureData: List<Pair<Int, Float>>, // intervalNumber, temperature
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

        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Helper function to start a new page
        fun startNewPage(): Canvas {
            currentPage?.let { pdfDocument.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            currentPage = pdfDocument.startPage(pageInfo)
            return currentPage!!.canvas
        }

        // Helper to draw horizontal divider
        fun drawDivider(canvas: Canvas, y: Float): Float {
            canvas.drawLine(50f, y, 545f, y, dividerPaint)
            return y + 10f
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

        // Helper to draw a row with 3 fields
        fun drawRow3(canvas: Canvas, y: Float, items: List<Pair<String, String>>): Float {
            val colWidth = 160f
            var x = 50f
            items.forEach { (label, value) ->
                canvas.drawText(label, x, y, labelPaint)
                canvas.drawText(value, x, y + 10f, valuePaint)
                x += colWidth
            }
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

        // Jenis Bean
        firstCanvas.drawText("Jenis Bean", 50f, yPosition, labelPaint)
        firstCanvas.drawText(data.beanType.ifEmpty { "-" }, 50f, yPosition + 10f, valuePaint)
        yPosition += 22f

        // Row: Kadar Air | Density | Berat Masuk | Berat Keluar
        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Kadar Air (°)" to data.waterContent.ifEmpty { "-" },
            "Density (kg/L)" to data.density.ifEmpty { "-" },
            "Berat Masuk (gr)" to data.weightIn.ifEmpty { "-" },
            "Berat Keluar (gr)" to data.weightOut.ifEmpty { "-" }
        ))

        // Row: Weight Loss | Roasted Type
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

        // Row: Charge Time | End Time | Roast Time | Dev Time
        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Charge Time (°C)" to data.chargeTimeTemp.ifEmpty { "-" },
            "End Time (°C)" to data.endTimeTemp.ifEmpty { "-" },
            "Roast Time (menit)" to data.roastTime.ifEmpty { "-" },
            "Dev Time (menit)" to data.devTime.ifEmpty { "-" }
        ))

        yPosition += 5f

        // === Event Suhu ===
        firstCanvas.drawText("Event Suhu", 50f, yPosition, sectionPaint)
        yPosition += 15f

        // Row: Turn Point | Yellowing | First Crack
        yPosition = drawRow3(firstCanvas, yPosition, listOf(
            "Turn Point (°C)" to data.turnPoint.ifEmpty { "-" },
            "Yellowing (°C)" to data.yellowing.ifEmpty { "-" },
            "First Crack (°C)" to data.firstCrack.ifEmpty { "-" }
        ))

        yPosition += 5f

        // === Parameter Mesin ===
        firstCanvas.drawText("Parameter Mesin", 50f, yPosition, sectionPaint)
        yPosition += 15f

        // Row: Air Flow | RPM Drum | Burner Power | ROR
        yPosition = drawRow4(firstCanvas, yPosition, listOf(
            "Air Flow Power" to data.airFlowPower.ifEmpty { "-" },
            "RPM Drum" to data.rpmDrum.ifEmpty { "-" },
            "Burner Power" to data.burnerPower.ifEmpty { "-" },
            "ROR" to data.ror.ifEmpty { "-" }
        ))

        yPosition += 5f

        // === Pengaturan Timer ===
        firstCanvas.drawText("Pengaturan Timer", 50f, yPosition, sectionPaint)
        yPosition += 15f

        // Row: Durasi | Interval | Suhu Awal
        yPosition = drawRow3(firstCanvas, yPosition, listOf(
            "Durasi (menit)" to "${data.targetDuration}",
            "Interval (detik)" to "${data.intervalSeconds}",
            "Suhu Awal (°C)" to "${data.startTemperature.toInt()}"
        ))

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

    private fun createChartBitmap(data: RoastSessionData): Bitmap {
        val chartWidth = 480
        val chartHeight = 240 // Lebih tinggi untuk jarak antar label suhu lebih renggang

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
                // Force show all labels from 240 to 70
                labelCount = 9
                setLabelCount(9, true)
                setDrawLabels(true)
                // Custom formatter to show 240, 230, 220...70
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Round to nearest 10
                        val rounded = (value / 10).toInt() * 10
                        return if (rounded in 70..240) rounded.toString() else ""
                    }
                }
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                granularity = 1f
                textColor = Color.BLACK
                textSize = 7f // Ukuran lebih besar
                axisMinimum = 0f
                axisMaximum = maxX
                // Batasi jumlah label agar tidak berdempetan
                // Tampilkan sekitar 8-10 label saja
                val step = maxOf(1, maxIntervals / 8)
                setLabelCount((maxIntervals / step) + 1, true)
                labelRotationAngle = -45f
                valueFormatter = PdfTimeAxisFormatter(data.intervalSeconds)
            }
        }

        val entries = data.temperatureData.map { (intervalNum, temp) ->
            Entry(intervalNum.toFloat(), temp)
        }

        val dataSet = LineDataSet(entries, "Temperature").apply {
            color = "#6D4C41".toColorInt() // Coffee brown color
            lineWidth = 2f // Garis lebih tebal
            setDrawCircles(true) // Tampilkan titik
            setCircleColor("#6D4C41".toColorInt())
            circleRadius = 3f // Titik lebih besar
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        chart.data = LineData(dataSet)

        // Measure and layout
        chart.measure(
            View.MeasureSpec.makeMeasureSpec(chartWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(chartHeight, View.MeasureSpec.EXACTLY)
        )
        chart.layout(0, 0, chartWidth, chartHeight)

        // Draw to bitmap
        val bitmap = createBitmap(chartWidth, chartHeight)
        val canvas = Canvas(bitmap)
        chart.draw(canvas)

        return bitmap
    }

    private class PdfTimeAxisFormatter(private val intervalSeconds: Int) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val intervalNum = value.toInt()
            val totalSeconds = intervalNum * intervalSeconds
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            return when {
                intervalSeconds >= 60 -> "$minutes"
                else -> String.format(Locale.getDefault(), "%d.%02d", minutes, seconds)
            }
        }
    }
}