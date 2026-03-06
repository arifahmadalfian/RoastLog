package com.indie.roastlog.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import com.indie.roastlog.ui.components.ChartDataPoint
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

        val chartBitmap = createTemperatureChartBitmap(data)
        val chartHeight = 200
        val availableWidth = 495f // Page width (595) - margins (100)
        
        // Scale chart to fit width
        val chartScale = availableWidth / chartBitmap.width
        
        val destRect = android.graphics.RectF(
            50f, 
            yPosition, 
            50f + chartBitmap.width * chartScale,
            yPosition + chartHeight * chartScale
        )
        chartCanvas.drawBitmap(chartBitmap, null, destRect, null)
        yPosition += chartHeight * chartScale + 20f

        // ROR Chart
        if (yPosition > 500f) {
            canvas = startNewPage()
            yPosition = 50f
        }

        val rorCanvas = canvas!!
        rorCanvas.drawText("Grafik ROR (Rate of Rise):", 50f, yPosition, sectionPaint)
        yPosition += 15f

        val rorBitmap = createRorChartBitmap(data)
        val rorChartHeight = 80
        
        // Scale ROR chart to fit width
        val rorScale = availableWidth / rorBitmap.width
        
        val rorDestRect = android.graphics.RectF(
            50f,
            yPosition,
            50f + rorBitmap.width * rorScale,
            yPosition + rorChartHeight * rorScale
        )
        rorCanvas.drawBitmap(rorBitmap, null, rorDestRect, null)
        yPosition += rorChartHeight * rorScale + 20f

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

    // Create temperature chart using native Canvas (fixed width for PDF)
    private fun createTemperatureChartBitmap(data: RoastSessionData): Bitmap {
        val totalSeconds = data.targetDuration * 60
        val maxIntervals = if (data.intervalSeconds > 0) totalSeconds / data.intervalSeconds else 0
        
        // Fixed width for PDF - all 20 intervals in one chart
        val chartWidth = 800 // Large width to accommodate all data points
        val chartHeight = 500
        
        val bitmap = createBitmap(chartWidth, chartHeight)
        val canvas = Canvas(bitmap)
        
        // Background
        canvas.drawColor(Color.WHITE)
        
        val paddingLeft = 60f
        val paddingRight = 300f
        val paddingTop = 20f
        val paddingBottom = 40f
        
        val graphWidth = chartWidth - paddingLeft - paddingRight
        val graphHeight = chartHeight - paddingTop - paddingBottom
        
        // Paints
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        val axisPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }
        
        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        
        val yLabelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            textAlign = Paint.Align.RIGHT
        }
        
        val linePaint = Paint().apply {
            color = "#2196F3".toColorInt()
            strokeWidth = 3f
            isAntiAlias = true
        }
        
        val pointPaint = Paint().apply {
            color = "#2196F3".toColorInt()
            style = Paint.Style.FILL
        }
        
        // Y-axis range (70 to 240)
        val yMin = 70f
        val yMax = 240f
        val yRange = yMax - yMin
        
        // Draw Y-axis grid lines and labels (every 10 degrees: 70, 80, 90... 240)
        val yStep = 10f
        var y = yMin
        while (y <= yMax) {
            val yPos = paddingTop + graphHeight - ((y - yMin) / yRange * graphHeight)
            canvas.drawLine(paddingLeft, yPos, paddingLeft + graphWidth, yPos, gridPaint)
            canvas.drawText(y.toInt().toString(), paddingLeft - 10f, yPos + 5f, yLabelPaint)
            y += yStep
        }
        
        // Draw X-axis grid lines and labels (every 1 minute: 0, 1, 2... 20)
        for (i in 0..maxIntervals) {
            val xPos = paddingLeft + (i.toFloat() / maxIntervals * graphWidth)
            canvas.drawLine(xPos, paddingTop, xPos, paddingTop + graphHeight, gridPaint)
            val timeStr = formatTime(i.toFloat(), data.intervalSeconds)
            canvas.drawText(timeStr, xPos, paddingTop + graphHeight + 25f, labelPaint)
        }
        
        // Draw axes
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + graphHeight, axisPaint)
        canvas.drawLine(paddingLeft, paddingTop + graphHeight, paddingLeft + graphWidth, paddingTop + graphHeight, axisPaint)
        
        // Draw temperature line
        if (data.temperatureData.isNotEmpty()) {
            val path = Path()
            var firstPoint = true
            
            data.temperatureData.forEach { point ->
                point.temperature?.let { temp ->
                    val x = paddingLeft + (point.intervalNumber / maxIntervals * graphWidth)
                    val y = paddingTop + graphHeight - ((temp - yMin) / yRange * graphHeight)
                    
                    if (firstPoint) {
                        path.moveTo(x, y)
                        firstPoint = false
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    // Draw point
                    canvas.drawCircle(x, y, 6f, pointPaint)
                }
            }
            
            canvas.drawPath(path, linePaint)
        }
        
        return bitmap
    }

    // Create ROR chart using native Canvas
    private fun createRorChartBitmap(data: RoastSessionData): Bitmap {
        val totalSeconds = data.targetDuration * 60
        val maxIntervals = if (data.intervalSeconds > 0) totalSeconds / data.intervalSeconds else 0
        
        val chartWidth = 1000 // Same width as temperature chart
        val chartHeight = 80
        
        val bitmap = createBitmap(chartWidth, chartHeight)
        val canvas = Canvas(bitmap)
        
        // Background
        canvas.drawColor(Color.WHITE)
        
        val paddingLeft = 60f
        val paddingRight = 20f
        val paddingTop = 10f
        val paddingBottom = 30f
        
        val graphWidth = chartWidth - paddingLeft - paddingRight
        val graphHeight = chartHeight - paddingTop - paddingBottom
        
        // Paints
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        val labelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            textAlign = Paint.Align.CENTER
        }
        
        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val pointPaint = Paint().apply {
            color = "#4CAF50".toColorInt()
            style = Paint.Style.FILL
        }
        
        // Build ROR data
        val rorData = buildRorDataForPdf(data)
        
        // Draw X-axis grid lines (every 1 minute: 0, 1, 2... 20)
        for (i in 0..maxIntervals) {
            val xPos = paddingLeft + (i.toFloat() / maxIntervals * graphWidth)
            canvas.drawLine(xPos, paddingTop, xPos, paddingTop + graphHeight, gridPaint)
            val timeStr = formatTime(i.toFloat(), data.intervalSeconds)
            canvas.drawText(timeStr, xPos, paddingTop + graphHeight + 20f, labelPaint)
        }
        
        // Draw horizontal grid lines
        canvas.drawLine(paddingLeft, paddingTop + graphHeight / 2, paddingLeft + graphWidth, paddingTop + graphHeight / 2, gridPaint)
        
        // Draw ROR points with values (intervals + events)
        rorData.forEach { point ->
            point.ror?.let { ror ->
                val x = paddingLeft + (point.intervalNumber / maxIntervals * graphWidth)
                val y = paddingTop + graphHeight / 2 // Center line
                
                // Draw point
                canvas.drawCircle(x, y, 5f, pointPaint)
                
                // Draw value above/below point
                val valueY = if (ror >= 0) y - 15f else y + 25f
                canvas.drawText(ror.toString(), x, valueY, valuePaint)
            }
        }
        
        return bitmap
    }

    // Build ROR data combining interval data and events with interpolation
    private fun buildRorDataForPdf(data: RoastSessionData): List<ChartDataPoint> {
        val interval = data.intervalSeconds
        val totalSeconds = data.targetDuration * 60
        val maxIntervals = totalSeconds / interval

        // Build all data points (charge temp + interval data + events)
        val allPoints = mutableListOf<Pair<Int, Int>>()

        // Add charge temperature at second 0
        allPoints.add(0 to data.startTemperature)

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

        // Remove duplicates and sort
        val sortedPoints = allPoints.distinctBy { it.first }.sortedBy { it.first }

        val result = mutableListOf<ChartDataPoint>()
        
        // Calculate ROR for each interval
        for (i in 0..maxIntervals) {
            val seconds = i * interval
            val rorValue = calculateRor(seconds, sortedPoints)
            rorValue?.let {
                result.add(
                    ChartDataPoint(
                        intervalNumber = i.toFloat(),
                        totalSeconds = seconds,
                        temperature = null,
                        ror = rorValue
                    )
                )
            }
        }
        
        // Add ROR for events at their exact positions (fractional interval numbers)
        data.turnPointEvent?.let { event ->
            calculateRor(event.seconds, sortedPoints)?.let { ror ->
                val intervalNum = event.seconds.toFloat() / interval
                result.add(
                    ChartDataPoint(
                        intervalNumber = intervalNum,
                        totalSeconds = event.seconds,
                        temperature = null,
                        ror = ror
                    )
                )
            }
        }
        data.yellowingEvent?.let { event ->
            calculateRor(event.seconds, sortedPoints)?.let { ror ->
                val intervalNum = event.seconds.toFloat() / interval
                result.add(
                    ChartDataPoint(
                        intervalNumber = intervalNum,
                        totalSeconds = event.seconds,
                        temperature = null,
                        ror = ror
                    )
                )
            }
        }
        data.firstCrackEvent?.let { event ->
            calculateRor(event.seconds, sortedPoints)?.let { ror ->
                val intervalNum = event.seconds.toFloat() / interval
                result.add(
                    ChartDataPoint(
                        intervalNumber = intervalNum,
                        totalSeconds = event.seconds,
                        temperature = null,
                        ror = ror
                    )
                )
            }
        }
        data.endRoastEvent?.let { event ->
            calculateRor(event.seconds, sortedPoints)?.let { ror ->
                val intervalNum = event.seconds.toFloat() / interval
                result.add(
                    ChartDataPoint(
                        intervalNumber = intervalNum,
                        totalSeconds = event.seconds,
                        temperature = null,
                        ror = ror
                    )
                )
            }
        }
        
        return result.sortedBy { it.intervalNumber }
    }

    private fun calculateRor(seconds: Int, allPoints: List<Pair<Int, Int>>): Int? {
        if (seconds == 0) return 0

        val currentTemp = getTemperatureAtSeconds(seconds, allPoints) ?: return null
        val prevTemp = allPoints.sortedBy { it.first }.lastOrNull { it.first < seconds }?.second ?: return null
        return currentTemp - prevTemp
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
}
