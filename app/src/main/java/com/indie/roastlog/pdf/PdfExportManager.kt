package com.indie.roastlog.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.indie.roastlog.ui.components.ChartDataPoint
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
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

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

        val smallPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        var yPosition = 50f

        // Title
        canvas.drawText("Roast Log Report", 50f, yPosition, titlePaint)
        yPosition += 30f

        // Date
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))
        canvas.drawText("Tanggal: ${dateFormat.format(data.roastDate)}", 50f, yPosition, paint)
        yPosition += 25f

        // Bean Information Section
        canvas.drawText("Informasi Bean:", 50f, yPosition, headerPaint)
        yPosition += 20f
        canvas.drawText("Jenis Bean: ${data.beanType}", 50f, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Kadar Air: ${data.waterContent}%", 50f, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Density: ${data.density} kg/l", 50f, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Roast Type: ${data.roastType}", 50f, yPosition, paint)
        yPosition += 25f

        // Weight Information
        canvas.drawText("Informasi Berat:", 50f, yPosition, headerPaint)
        yPosition += 20f
        canvas.drawText("Berat Masuk: ${data.weightIn} gram", 50f, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Berat Keluar: ${data.weightOut} gram", 50f, yPosition, paint)
        yPosition += 15f
        val weightLoss = if (data.weightIn.isNotEmpty() && data.weightOut.isNotEmpty()) {
            val inWeight = data.weightIn.toFloatOrNull() ?: 0f
            val outWeight = data.weightOut.toFloatOrNull() ?: 0f
            val loss = inWeight - outWeight
            val lossPercent = if (inWeight > 0) (loss / inWeight) * 100 else 0f
            "%.1f g (%.1f%%)".format(loss, lossPercent)
        } else "N/A"
        canvas.drawText("Weight Loss: $weightLoss", 50f, yPosition, paint)
        yPosition += 25f

        // Time & Temperature Section
        canvas.drawText("Time & Temperature:", 50f, yPosition, headerPaint)
        yPosition += 20f
        
        // Row 1: Charge Time | End Time
        canvas.drawText("Charge Time: ${data.chargeTimeTemp.ifEmpty { "-" }}°C", 50f, yPosition, paint)
        canvas.drawText("End Time: ${data.endTimeTemp.ifEmpty { "-" }}°C", 250f, yPosition, paint)
        yPosition += 15f
        
        // Row 2: Roast Time | Dev Time
        canvas.drawText("Roast Time: ${data.roastTime.ifEmpty { "-" }} menit", 50f, yPosition, paint)
        canvas.drawText("Dev Time: ${data.devTime.ifEmpty { "-" }} menit", 250f, yPosition, paint)
        yPosition += 25f

        // Event Suhu Section
        canvas.drawText("Event Suhu:", 50f, yPosition, headerPaint)
        yPosition += 20f
        
        // Row 1: Turn Point | Yellowing
        canvas.drawText("Turn Point: ${data.turnPoint.ifEmpty { "-" }}°C", 50f, yPosition, paint)
        canvas.drawText("Yellowing: ${data.yellowing.ifEmpty { "-" }}°C", 250f, yPosition, paint)
        yPosition += 15f
        
        // Row 2: First Crack
        canvas.drawText("First Crack: ${data.firstCrack.ifEmpty { "-" }}°C", 50f, yPosition, paint)
        yPosition += 25f

        // Parameter Mesin Section
        canvas.drawText("Parameter Mesin:", 50f, yPosition, headerPaint)
        yPosition += 20f
        
        // Row 1: Air Flow | RPM Drum
        canvas.drawText("Air Flow: ${data.airFlowPower.ifEmpty { "-" }}", 50f, yPosition, paint)
        canvas.drawText("RPM Drum: ${data.rpmDrum.ifEmpty { "-" }}", 250f, yPosition, paint)
        yPosition += 15f
        
        // Row 2: Burner Power | ROR
        canvas.drawText("Burner Power: ${data.burnerPower.ifEmpty { "-" }}", 50f, yPosition, paint)
        canvas.drawText("ROR: ${data.ror.ifEmpty { "-" }}", 250f, yPosition, paint)
        yPosition += 25f

        // Roast Session Info
        canvas.drawText("Informasi Roasting:", 50f, yPosition, headerPaint)
        yPosition += 20f
        canvas.drawText("Durasi Target: ${data.targetDuration} menit", 50f, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Interval Input: ${data.intervalSeconds} detik", 50f, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Suhu Awal: ${data.startTemperature.toInt()}°C", 50f, yPosition, paint)
        yPosition += 30f

        // Check if we need a new page for temperature profile
        if (yPosition > 650f) {
            pdfDocument.finishPage(page)
            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
            val newPage = pdfDocument.startPage(newPageInfo)
            yPosition = 50f
            
            // Draw Temperature Profile on new page
            newPage.canvas.drawText("Temperature Profile:", 50f, yPosition, headerPaint)
            yPosition += 20f
            
            drawTemperatureTable(newPage.canvas, data, yPosition, paint, headerPaint, smallPaint, pdfDocument)
            pdfDocument.finishPage(newPage)
        } else {
            // Draw Temperature Profile on current page
            canvas.drawText("Temperature Profile:", 50f, yPosition, headerPaint)
            yPosition += 20f
            
            drawTemperatureTable(canvas, data, yPosition, paint, headerPaint, smallPaint, pdfDocument)
        }

        // Save PDF to Downloads
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "RoastLog_${data.beanType.replace(" ", "_")}_$timestamp.pdf"

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ - use MediaStore
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
                // Android 9 and below - use direct file access
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

    private fun drawTemperatureTable(
        initialCanvas: android.graphics.Canvas,
        data: RoastSessionData,
        startY: Float,
        paint: Paint,
        headerPaint: Paint,
        smallPaint: Paint,
        pdfDocument: PdfDocument
    ): Float {
        var yPosition = startY
        var currentCanvas = initialCanvas

        // Create temperature table
        if (data.temperatureData.isNotEmpty()) {
            // Table header
            currentCanvas.drawText("Waktu", 50f, yPosition, headerPaint)
            currentCanvas.drawText("Interval", 120f, yPosition, headerPaint)
            currentCanvas.drawText("Suhu (°C)", 190f, yPosition, headerPaint)
            yPosition += 15f

            // Draw line under header
            currentCanvas.drawLine(50f, yPosition - 5f, 250f, yPosition - 5f, paint)

            // Table rows
            data.temperatureData.sortedBy { it.first }.forEach { (intervalNum, temp) ->
                if (yPosition > 800f) {
                    // Finish current page and start new one
                    val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    val newPage = pdfDocument.startPage(newPageInfo)
                    currentCanvas = newPage.canvas
                    yPosition = 50f
                    // Redraw header on new page
                    currentCanvas.drawText("Waktu", 50f, yPosition, headerPaint)
                    currentCanvas.drawText("Interval", 120f, yPosition, headerPaint)
                    currentCanvas.drawText("Suhu (°C)", 190f, yPosition, headerPaint)
                    yPosition += 15f
                    currentCanvas.drawLine(50f, yPosition - 5f, 250f, yPosition - 5f, paint)
                }

                val minutes = (intervalNum * data.intervalSeconds) / 60
                val seconds = (intervalNum * data.intervalSeconds) % 60
                val timeStr = String.format("%d:%02d", minutes, seconds)

                currentCanvas.drawText(timeStr, 50f, yPosition, paint)
                currentCanvas.drawText("#$intervalNum", 120f, yPosition, paint)
                currentCanvas.drawText("${temp.toInt()}°C", 190f, yPosition, paint)
                yPosition += 15f
            }
        } else {
            currentCanvas.drawText("Tidak ada data suhu", 50f, yPosition, paint)
            yPosition += 15f
        }

        return yPosition
    }
}
