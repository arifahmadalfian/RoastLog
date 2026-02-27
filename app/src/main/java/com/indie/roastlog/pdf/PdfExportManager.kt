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

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }

        // Helper function to start a new page
        fun startNewPage(): Canvas {
            currentPage?.let { pdfDocument.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            currentPage = pdfDocument.startPage(pageInfo)
            return currentPage!!.canvas
        }

        // Start first page
        canvas = startNewPage()
        val firstCanvas = canvas!!
        var yPosition = 50f

        // Title
        firstCanvas.drawText("Roast Log Report", 50f, yPosition, titlePaint)
        yPosition += 30f

        // Date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
        firstCanvas.drawText("Tanggal: ${dateFormat.format(data.roastDate)}", 50f, yPosition, paint)
        yPosition += 25f

        // Bean Information Section
        firstCanvas.drawText("Informasi Bean:", 50f, yPosition, headerPaint)
        yPosition += 20f
        firstCanvas.drawText("Jenis Bean: ${data.beanType}", 50f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Kadar Air: ${data.waterContent}%", 50f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Density: ${data.density} kg/l", 50f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Roast Type: ${data.roastType}", 50f, yPosition, paint)
        yPosition += 25f

        // Weight Information
        firstCanvas.drawText("Informasi Berat:", 50f, yPosition, headerPaint)
        yPosition += 20f
        firstCanvas.drawText("Berat Masuk: ${data.weightIn} gram", 50f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Berat Keluar: ${data.weightOut} gram", 50f, yPosition, paint)
        yPosition += 15f
        val weightLoss = if (data.weightIn.isNotEmpty() && data.weightOut.isNotEmpty()) {
            val inWeight = data.weightIn.toFloatOrNull() ?: 0f
            val outWeight = data.weightOut.toFloatOrNull() ?: 0f
            val loss = inWeight - outWeight
            val lossPercent = if (inWeight > 0) (loss / inWeight) * 100 else 0f
            "%.1f g (%.1f%%)".format(loss, lossPercent)
        } else "N/A"
        firstCanvas.drawText("Weight Loss: $weightLoss", 50f, yPosition, paint)
        yPosition += 25f

        // Time & Temperature Section
        firstCanvas.drawText("Time & Temperature:", 50f, yPosition, headerPaint)
        yPosition += 20f
        firstCanvas.drawText("Charge Time: ${data.chargeTimeTemp.ifEmpty { "-" }}°C", 50f, yPosition, paint)
        firstCanvas.drawText("End Time: ${data.endTimeTemp.ifEmpty { "-" }}°C", 250f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Roast Time: ${data.roastTime.ifEmpty { "-" }} menit", 50f, yPosition, paint)
        firstCanvas.drawText("Dev Time: ${data.devTime.ifEmpty { "-" }} menit", 250f, yPosition, paint)
        yPosition += 25f

        // Event Suhu Section
        firstCanvas.drawText("Event Suhu:", 50f, yPosition, headerPaint)
        yPosition += 20f
        firstCanvas.drawText("Turn Point: ${data.turnPoint.ifEmpty { "-" }}°C", 50f, yPosition, paint)
        firstCanvas.drawText("Yellowing: ${data.yellowing.ifEmpty { "-" }}°C", 250f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("First Crack: ${data.firstCrack.ifEmpty { "-" }}°C", 50f, yPosition, paint)
        yPosition += 25f

        // Parameter Mesin Section
        firstCanvas.drawText("Parameter Mesin:", 50f, yPosition, headerPaint)
        yPosition += 20f
        firstCanvas.drawText("Air Flow: ${data.airFlowPower.ifEmpty { "-" }}", 50f, yPosition, paint)
        firstCanvas.drawText("RPM Drum: ${data.rpmDrum.ifEmpty { "-" }}", 250f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Burner Power: ${data.burnerPower.ifEmpty { "-" }}", 50f, yPosition, paint)
        firstCanvas.drawText("ROR: ${data.ror.ifEmpty { "-" }}", 250f, yPosition, paint)
        yPosition += 25f

        // Roast Session Info
        firstCanvas.drawText("Informasi Roasting:", 50f, yPosition, headerPaint)
        yPosition += 20f
        firstCanvas.drawText("Durasi Target: ${data.targetDuration} menit", 50f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Interval Input: ${data.intervalSeconds} detik", 50f, yPosition, paint)
        yPosition += 15f
        firstCanvas.drawText("Suhu Awal: ${data.startTemperature.toInt()}°C", 50f, yPosition, paint)
        yPosition += 25f

        // Temperature Profile (Diagram)
        if (yPosition > 450f) {
            canvas = startNewPage()
            yPosition = 50f
        }

        val chartCanvas = canvas!!
        chartCanvas.drawText("Temperature Profile:", 50f, yPosition, headerPaint)
        yPosition += 20f

        val chartBitmap = createChartBitmap(data)
        val chartHeight = 300
        val chartWidth = 500
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
        val chartWidth = 500
        val chartHeight = 300

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
                textSize = 10f
                labelCount = 18
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                granularity = 1f
                textColor = Color.BLACK
                textSize = 8f
                axisMinimum = 0f
                labelRotationAngle = -45f
                valueFormatter = PdfTimeAxisFormatter(data.intervalSeconds)
            }
        }

        val entries = data.temperatureData.map { (intervalNum, temp) ->
            Entry(intervalNum.toFloat(), temp)
        }

        val dataSet = LineDataSet(entries, "Temperature").apply {
            color = Color.parseColor("#2196F3")
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(Color.parseColor("#2196F3"))
            circleRadius = 3f
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
        val bitmap = Bitmap.createBitmap(chartWidth, chartHeight, Bitmap.Config.ARGB_8888)
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
