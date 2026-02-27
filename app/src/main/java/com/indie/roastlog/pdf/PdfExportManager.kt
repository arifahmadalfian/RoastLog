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
            textSize = 10f
        }

        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }

        val sectionPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }

        // Helper function to start a new page
        fun startNewPage(): Canvas {
            currentPage?.let { pdfDocument.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create()
            currentPage = pdfDocument.startPage(pageInfo)
            return currentPage!!.canvas
        }

        // Helper to draw a row with 2 fields
        fun drawRow(canvas: Canvas, y: Float, label1: String, value1: String, label2: String, value2: String, x1: Float = 50f, x2: Float = 300f): Float {
            canvas.drawText(label1, x1, y, labelPaint)
            canvas.drawText(value1, x1, y + 12f, valuePaint)
            canvas.drawText(label2, x2, y, labelPaint)
            canvas.drawText(value2, x2, y + 12f, valuePaint)
            return y + 30f
        }

        // Helper to draw a row with 3 fields
        fun drawRow3(canvas: Canvas, y: Float, label1: String, value1: String, label2: String, value2: String, label3: String, value3: String): Float {
            val x1 = 50f
            val x2 = 220f
            val x3 = 390f
            canvas.drawText(label1, x1, y, labelPaint)
            canvas.drawText(value1, x1, y + 12f, valuePaint)
            canvas.drawText(label2, x2, y, labelPaint)
            canvas.drawText(value2, x2, y + 12f, valuePaint)
            canvas.drawText(label3, x3, y, labelPaint)
            canvas.drawText(value3, x3, y + 12f, valuePaint)
            return y + 30f
        }

        // Start first page
        canvas = startNewPage()
        val firstCanvas = canvas!!
        var yPosition = 40f

        // Title
        firstCanvas.drawText("Roast Log Report", 50f, yPosition, titlePaint)
        yPosition += 25f

        // Date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
        firstCanvas.drawText("Tanggal: ${dateFormat.format(data.roastDate)}", 50f, yPosition, paint)
        yPosition += 25f

        // === Bean Information Section ===
        firstCanvas.drawText("Informasi Bean", 50f, yPosition, sectionPaint)
        yPosition += 5f
        // Jenis Bean (full width)
        firstCanvas.drawText("Jenis Bean", 50f, yPosition, labelPaint)
        firstCanvas.drawText(data.beanType.ifEmpty { "-" }, 50f, yPosition + 12f, valuePaint)
        yPosition += 28f

        // Row: Kadar Air + Density
        yPosition = drawRow(firstCanvas, yPosition,
            "Kadar Air (°)", data.waterContent.ifEmpty { "-" },
            "Density (kg/L)", data.density.ifEmpty { "-" }
        )

        // Row: Berat Masuk + Berat Keluar
        yPosition = drawRow(firstCanvas, yPosition,
            "Berat Masuk (gr)", data.weightIn.ifEmpty { "-" },
            "Berat Keluar (gr)", data.weightOut.ifEmpty { "-" }
        )

        // Weight Loss (single line)
        val weightLoss = if (data.weightIn.isNotEmpty() && data.weightOut.isNotEmpty()) {
            val inWeight = data.weightIn.toFloatOrNull() ?: 0f
            val outWeight = data.weightOut.toFloatOrNull() ?: 0f
            val loss = inWeight - outWeight
            val lossPercent = if (inWeight > 0) (loss / inWeight) * 100 else 0f
            "%.1f gr (%.1f%%)".format(loss, lossPercent)
        } else "N/A"
        firstCanvas.drawText("Weight Loss", 50f, yPosition, labelPaint)
        firstCanvas.drawText(weightLoss, 50f, yPosition + 12f, valuePaint)
        yPosition += 28f

        // Roasted Type (full width)
        firstCanvas.drawText("Roasted Type", 50f, yPosition, labelPaint)
        firstCanvas.drawText(data.roastType, 50f, yPosition + 12f, valuePaint)
        yPosition += 28f

        yPosition += 5f

        // === Time & Temperature Section ===
        firstCanvas.drawText("Time & Temperature", 50f, yPosition, sectionPaint)
        yPosition += 5f

        // Row: Charge Time + End Time
        yPosition = drawRow(firstCanvas, yPosition,
            "Charge Time (°C)", data.chargeTimeTemp.ifEmpty { "-" },
            "End Time (°C)", data.endTimeTemp.ifEmpty { "-" }
        )

        // Row: Roast Time + Dev Time
        yPosition = drawRow(firstCanvas, yPosition,
            "Roast Time (menit)", data.roastTime.ifEmpty { "-" },
            "Dev Time (menit)", data.devTime.ifEmpty { "-" }
        )

        yPosition += 5f

        // === Event Suhu Section ===
        firstCanvas.drawText("Event Suhu", 50f, yPosition, sectionPaint)
        yPosition += 5f

        // Row: Turn Point + Yellowing + First Crack
        yPosition = drawRow3(firstCanvas, yPosition,
            "Turn Point (°C)", data.turnPoint.ifEmpty { "-" },
            "Yellowing (°C)", data.yellowing.ifEmpty { "-" },
            "First Crack (°C)", data.firstCrack.ifEmpty { "-" }
        )

        yPosition += 5f

        // === Parameter Mesin Section ===
        firstCanvas.drawText("Parameter Mesin", 50f, yPosition, sectionPaint)
        yPosition += 5f

        // Row: Air Flow + RPM Drum
        yPosition = drawRow(firstCanvas, yPosition,
            "Air Flow Power", data.airFlowPower.ifEmpty { "-" },
            "RPM Drum", data.rpmDrum.ifEmpty { "-" }
        )

        // Row: Burner Power + ROR
        yPosition = drawRow(firstCanvas, yPosition,
            "Burner Power", data.burnerPower.ifEmpty { "-" },
            "ROR", data.ror.ifEmpty { "-" }
        )

        yPosition += 5f

        // === Roast Session Info ===
        firstCanvas.drawText("Pengaturan Timer", 50f, yPosition, sectionPaint)
        yPosition += 5f

        // Row: Durasi + Interval + Suhu Awal
        yPosition = drawRow3(firstCanvas, yPosition,
            "Durasi (menit)", "${data.targetDuration}",
            "Interval (detik)", "${data.intervalSeconds}",
            "Suhu Awal (°C)", "${data.startTemperature.toInt()}"
        )

        yPosition += 15f

        // Temperature Profile (Diagram)
        if (yPosition > 450f) {
            canvas = startNewPage()
            yPosition = 50f
        }

        val chartCanvas = canvas!!
        chartCanvas.drawText("Temperature Profile:", 50f, yPosition, sectionPaint)
        yPosition += 20f

        val chartBitmap = createChartBitmap(data)
        val chartHeight = 200
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
        val chartWidth = 420
        val chartHeight = 200

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
                textSize = 7f
                labelCount = 18
                setLabelCount(18, true)
            }

            axisRight.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                granularity = 1f
                textColor = Color.BLACK
                textSize = 6f
                axisMinimum = 0f
                axisMaximum = maxX
                setLabelCount(maxIntervals + 1, false)
                labelRotationAngle = -45f
                valueFormatter = PdfTimeAxisFormatter(data.intervalSeconds)
            }
        }

        val entries = data.temperatureData.map { (intervalNum, temp) ->
            Entry(intervalNum.toFloat(), temp)
        }

        val dataSet = LineDataSet(entries, "Temperature").apply {
            color = Color.parseColor("#2196F3")
            lineWidth = 1.5f
            setDrawCircles(true)
            setCircleColor(Color.parseColor("#2196F3"))
            circleRadius = 2.5f
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