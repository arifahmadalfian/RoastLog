package com.indie.roastlog.ui.screens.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.pdf.PdfExportManager
import com.indie.roastlog.pdf.RoastSessionData
import com.indie.roastlog.ui.components.ChartDataPoint
import com.indie.roastlog.ui.components.RoastingChart
import com.indie.roastlog.ui.components.RoastingChartRor
import com.indie.roastlog.ui.components.ScaffoldCustom
import com.indie.roastlog.ui.model.RoastingEvent
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoastingDetail(
    roastId: String?,
    onBack: () -> Unit,
    viewModel: RoastingDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val session by viewModel.session.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(roastId) {
        roastId?.let { viewModel.loadSession(it) }
    }

    val exportToPdf = suspend {
        session?.let { s ->
            val pdfManager = PdfExportManager(context)
            val chartData = buildChartData(s)
            val roastData = RoastSessionData(
                beanType = s.beanType,
                waterContent = s.waterContent,
                density = s.density,
                weightIn = s.weightIn,
                weightOut = s.weightOut,
                roastType = s.roastType,
                chargeTimeTemp = "-",
                endTimeTemp = s.endTimeTemp,
                roastTime = s.roastTime,
                devTime = s.devTime,
                turnPoint = s.turnPoint?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
                yellowing = s.yellowing?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
                firstCrack = s.firstCrack?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
                endRoasting = s.endRoast?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
                airFlowPower = "-",
                rpmDrum = "-",
                burnerPower = "-",
                ror = "-",
                burnerEvents = s.burnerPlan.map { "${it.temperature} / ${it.time}" },
                targetDuration = s.targetDuration,
                intervalSeconds = s.intervalSeconds,
                startTemperature = 70,
                temperatureData = chartData,
                roastDate = Date(s.date)
            )
            pdfManager.exportRoastSessionToPdf(roastData)
        }
    }

    ScaffoldCustom(
        title = session?.let { "Roasting: ${it.beanType}" } ?: "Detail Roasting",
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        session?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Setup Info Display (Read-only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Informasi Setup Roasting", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoItem(label = "Bean", value = s.beanType, modifier = Modifier.weight(1f))
                            InfoItem(label = "Kadar Air", value = "${s.waterContent}%", modifier = Modifier.weight(1f))
                            InfoItem(label = "Density", value = "${s.density} kg/L", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoItem(label = "Roast Type", value = s.roastType, modifier = Modifier.weight(1f))
                            InfoItem(label = "Berat Masuk", value = "${s.weightIn} gr", modifier = Modifier.weight(1f))
                            InfoItem(label = "Charge Temp", value = "-°C", modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                        // Display Plans (Burner, Air Flow, RPM)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlanSummarySection(title = "Burner Plan", events = s.burnerPlan)
                            PlanSummarySection(title = "Air Flow Plan", events = s.airFlowPlan)
                            PlanSummarySection(title = "RPM Plan", events = s.rpmPlan)
                        }
                    }
                }

                // Results Info Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Hasil Roasting", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoItem(label = "Berat Keluar", value = "${s.weightOut} gr", modifier = Modifier.weight(1f))
                            InfoItem(label = "End Temp", value = "${s.endTimeTemp}°C", modifier = Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoItem(label = "Roast Time", value = s.roastTime, modifier = Modifier.weight(1f))
                            InfoItem(label = "Dev Time", value = s.devTime, modifier = Modifier.weight(1f))
                        }
                    }
                }

                val scrollState = rememberScrollState()
                val chartData = buildChartData(s)
                val chartRorData = buildRorData(s)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    if (chartData.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            RoastingChart(
                                data = chartData,
                                intervalSeconds = s.intervalSeconds,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // ROR chart
                    if (chartRorData.isNotEmpty()) {
                        RoastingChartRor(
                            data = chartRorData,
                        )
                    }
                }

                // Event Highlights
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        EventRow(label = "Turn Point", event = s.turnPoint)
                        EventRow(label = "Yellowing", event = s.yellowing)
                        EventRow(label = "1st Crack", event = s.firstCrack)
                        EventRow(label = "End Roast", event = s.endRoast)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val result = exportToPdf()
                                if (result != null) {
                                    snackbarHostState.showSnackbar("PDF exported: $result")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export PDF")
                    }

                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hapus")
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Data") },
            text = { Text("Apakah anda yakin ingin menghapus data roasting ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession {
                            showDeleteDialog = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun buildChartData(s: com.indie.roastlog.data.RoastSessionEntity): List<ChartDataPoint> {
    val interval = s.intervalSeconds
    val dataMap = s.temperatureData.associateBy { it.intervalNumber }
    
    // Calculate max intervals based on target duration (same as RoastingRunScreen)
    val totalSeconds = s.targetDuration * 60
    val maxIntervals = totalSeconds / interval

    // Build regular interval points for the full duration (0 to maxIntervals)
    val points = (0..maxIntervals).map { intervalNum ->
        val secondsAtThisInterval = intervalNum * interval
        val intervalData = dataMap[intervalNum]
        val currentTemp = intervalData?.temperature
        val prevTemp = if (intervalNum > 0) dataMap[intervalNum - 1]?.temperature else null
        val rorValue = if (intervalNum > 0 && currentTemp != null && prevTemp != null) currentTemp - prevTemp else null

        val burnerPower = s.burnerPlan.find { it.seconds == secondsAtThisInterval }?.temperature?.toString() ?: ""

        ChartDataPoint(
            intervalNumber = intervalNum.toFloat(),
            totalSeconds = secondsAtThisInterval,
            temperature = currentTemp,
            ror = rorValue,
            burnerPower = burnerPower
        )
    }.toMutableList()

    // Add event marks at their exact positions (between intervals)
    listOfNotNull(s.turnPoint, s.yellowing, s.firstCrack, s.endRoast).forEach { ev ->
        val eventIntervalNum = ev.seconds.toFloat() / interval
        // Only add if not already in list (avoid duplicates)
        if (points.none { it.totalSeconds == ev.seconds }) {
            points.add(ChartDataPoint(
                intervalNumber = eventIntervalNum,
                totalSeconds = ev.seconds,
                temperature = ev.temperature,
                ror = null
            ))
        }
    }
    return points.sortedBy { it.totalSeconds }
}

private fun buildRorData(s: com.indie.roastlog.data.RoastSessionEntity): List<ChartDataPoint> {
    val interval = s.intervalSeconds
    
    // Calculate max intervals based on target duration
    val totalSeconds = s.targetDuration * 60
    val maxIntervals = totalSeconds / interval
    
    // Build all positions (intervals + events) like getAllChartPositions() in ViewModel
    val positions = mutableListOf<Pair<Float, Int>>()
    
    // Add regular interval positions
    for (i in 0..maxIntervals) {
        positions.add(i.toFloat() to (i * interval))
    }
    
    // Add event mark positions (fractional positions)
    listOfNotNull(s.turnPoint, s.yellowing, s.firstCrack, s.endRoast).forEach { ev ->
        val eventIntervalNum = ev.seconds.toFloat() / interval
        // Only add if not already in list
        if (positions.none { it.second == ev.seconds }) {
            positions.add(eventIntervalNum to ev.seconds)
        }
    }
    
    val sortedPositions = positions.sortedBy { it.first }
    
    // Build all data points (interval data + events) for interpolation
    val allDataPoints = buildAllDataPoints(s)
    
    // Calculate ROR for each position with interpolation (like getChartRor in ViewModel)
    return sortedPositions.mapNotNull { (intervalNum, seconds) ->
        val rorValue = calculateRorWithInterpolation(seconds, allDataPoints)
        
        rorValue?.let {
            ChartDataPoint(
                intervalNumber = intervalNum,
                totalSeconds = seconds,
                temperature = getTemperatureAtSeconds(seconds, allDataPoints),
                ror = rorValue
            )
        }
    }
}

// Build all data points combining interval data and events (like in ViewModel)
private fun buildAllDataPoints(s: com.indie.roastlog.data.RoastSessionEntity): List<Pair<Int, Int>> {
    val interval = s.intervalSeconds
    val points = mutableListOf<Pair<Int, Int>>()
    
    // Add interval data points
    s.temperatureData.forEach { data ->
        points.add(data.intervalNumber * interval to data.temperature)
    }
    
    // Add event mark points
    listOfNotNull(s.turnPoint, s.yellowing, s.firstCrack, s.endRoast).forEach { ev ->
        points.add(ev.seconds to ev.temperature)
    }
    
    return points.sortedBy { it.first }
}

// Get temperature at specific seconds with interpolation (like getTemperatureAtSeconds in ViewModel)
private fun getTemperatureAtSeconds(seconds: Int, allPoints: List<Pair<Int, Int>>): Int? {
    if (allPoints.isEmpty()) return null
    
    // Check for exact match
    allPoints.find { it.first == seconds }?.let { return it.second }
    
    // Find surrounding points for interpolation
    val before = allPoints.lastOrNull { it.first < seconds }
    val after = allPoints.firstOrNull { it.first > seconds }
    
    return when {
        before != null && after != null -> {
            // Linear interpolation
            val ratio = (seconds - before.first).toFloat() / (after.first - before.first)
            (before.second + ratio * (after.second - before.second)).toInt()
        }
        before != null -> before.second
        after != null -> after.second
        else -> null
    }
}

// Get previous temperature before specific seconds
private fun getPreviousTemperature(seconds: Int, allPoints: List<Pair<Int, Int>>): Int? {
    return allPoints.sortedBy { it.first }.lastOrNull { it.first < seconds }?.second
}

// Calculate ROR with interpolation (like getRorValueForChart in ViewModel)
private fun calculateRorWithInterpolation(seconds: Int, allPoints: List<Pair<Int, Int>>): Int? {
    // At second 0, RoR is 0 (starting point, no previous data)
    if (seconds == 0) return 0
    
    val currentTemp = getTemperatureAtSeconds(seconds, allPoints) ?: return null
    val prevTemp = getPreviousTemperature(seconds, allPoints) ?: return null
    return currentTemp - prevTemp
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RowScope.PlanSummarySection(title: String, events: List<RoastingEvent>) {
    if (events.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        events.forEach { event ->
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = "${event.temperature}|${event.time}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EventRow(label: String, event: RoastingEvent?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = event?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
