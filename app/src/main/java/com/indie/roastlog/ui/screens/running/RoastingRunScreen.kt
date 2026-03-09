package com.indie.roastlog.ui.screens.running

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.pdf.PdfExportManager
import com.indie.roastlog.pdf.RoastSessionData
import com.indie.roastlog.speech.VoiceRecognitionState
import com.indie.roastlog.speech.VoiceRecognizerManager
import com.indie.roastlog.ui.components.RoastingChart
import com.indie.roastlog.ui.components.RoastingChartRor
import com.indie.roastlog.ui.components.ScaffoldCustom
import com.indie.roastlog.ui.components.SmallOutlinedTextField
import com.indie.roastlog.ui.screens.form.RoastingFormState
import com.indie.roastlog.ui.model.IntervalData
import com.indie.roastlog.ui.model.RoastingEvent
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EventMarkDialogData(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onConfirm: (Int) -> Unit,
    val isNonDismissible: Boolean = true
)

// Wrapper class for revision items (interval data or event marks)
private sealed class RevisionItem {
    abstract val seconds: Int
    abstract val temperature: Int
    
    data class Interval(val data: IntervalData) : RevisionItem() {
        override val seconds: Int get() = data.actualSeconds
        override val temperature: Int get() = data.temperature
    }
    
    data class Event(val data: RoastingEvent, val eventName: String = "Event") : RevisionItem() {
        override val seconds: Int get() = data.seconds
        override val temperature: Int get() = data.temperature
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoastingRunScreen(
    viewModel: RoastingRunViewModel = viewModel(),
    formState: RoastingFormState,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val voiceRecognizer = remember { VoiceRecognizerManager(context) }
    val voiceState by voiceRecognizer.state.collectAsState()

    var showRevisionDialog by remember { mutableStateOf(false) }
    var selectedRevisionIndex by remember { mutableStateOf<Int?>(null) }
    var revisionTemperatureInput by remember { mutableStateOf("") }
    var revisionDropdownExpanded by remember { mutableStateOf(false) }
    
    var eventMarkDialogData by remember { mutableStateOf<EventMarkDialogData?>(null) }

    // Initialize ViewModel with passed data
    LaunchedEffect(formState) {
        viewModel.init(formState)
    }

    val exportToPdf = suspend {
        val pdfManager = PdfExportManager(context)
        val currentSetup = uiState.setupData
        val roastData = RoastSessionData(
            beanType = currentSetup.beanType,
            waterContent = currentSetup.waterContent,
            density = currentSetup.density,
            weightIn = currentSetup.weightIn,
            weightOut = uiState.weightOut,
            roastType = currentSetup.roastType,
            chargeTimeTemp = currentSetup.chargeTimeTemp,
            endTimeTemp = uiState.endTimeTemp,
            roastTime = uiState.roastTime,
            devTime = uiState.devTime,
            turnPoint = uiState.actualTurnPoint?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
            yellowing = uiState.actualYellowing?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
            firstCrack = uiState.actualFirstCrack?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
            endRoasting = uiState.actualEndRoast?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
            airFlowPower = currentSetup.airFlowPower,
            rpmDrum = currentSetup.rpmDrum,
            burnerPower = currentSetup.burnerPower,
            ror = "-",
            burnerEvents = currentSetup.burnerPlan.map { "${it.temperature} / ${it.time}" },
            // Plans for display
            burnerPlan = currentSetup.burnerPlan,
            airFlowPlan = currentSetup.airFlowPlan,
            rpmPlan = currentSetup.rpmPlan,
            // Event objects for ROR calculation
            turnPointEvent = uiState.actualTurnPoint,
            yellowingEvent = uiState.actualYellowing,
            firstCrackEvent = uiState.actualFirstCrack,
            endRoastEvent = uiState.actualEndRoast,
            targetDuration = currentSetup.targetDuration.toIntOrNull() ?: 0,
            intervalSeconds = currentSetup.intervalSeconds.toIntOrNull() ?: 60,
            startTemperature = currentSetup.chargeTimeTemp.toIntOrNull() ?: 70,
            temperatureData = viewModel.getChartData()
        )
        pdfManager.exportRoastSessionToPdf(roastData)
    }

    val scrollState = rememberScrollState()

    ScaffoldCustom(
        title = "Roasting: ${uiState.setupData.beanType}",
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
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
                        InfoItem(label = "Bean", value = uiState.setupData.beanType, modifier = Modifier.weight(1f))
                        InfoItem(label = "Kadar Air", value = "${uiState.setupData.waterContent}%", modifier = Modifier.weight(1f))
                        InfoItem(label = "Density", value = "${uiState.setupData.density} kg/L", modifier = Modifier.weight(1f))
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        InfoItem(label = "Roast Type", value = uiState.setupData.roastType, modifier = Modifier.weight(1f))
                        InfoItem(label = "Berat Masuk", value = "${uiState.setupData.weightIn} gr", modifier = Modifier.weight(1f))
                        InfoItem(label = "Charge Temp", value = "${uiState.setupData.chargeTimeTemp}°C", modifier = Modifier.weight(1f))
                    }

                    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // Display Plans (Burner, Air Flow, RPM)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlanSummarySection(title = "Burner Plan", events = uiState.setupData.burnerPlan)
                        PlanSummarySection(title = "Air Flow Plan", events = uiState.setupData.airFlowPlan)
                        PlanSummarySection(title = "RPM Drum Plan", events = uiState.setupData.rpmPlan)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlinedTextField(
                    value = uiState.weightOut,
                    onValueChange = { viewModel.updateWeightOut(it) },
                    label = "Berat Keluar (gr)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.endTimeTemp,
                    onValueChange = {},
                    label = "End Temp (°C)",
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallOutlinedTextField(
                    value = uiState.roastTime,
                    onValueChange = {},
                    label = "Roast Time",
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.devTime,
                    onValueChange = {},
                    label = "Dev Time",
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }

            TimerSection(
                elapsedMillis = uiState.elapsedMillis,
                isRunning = uiState.isTimerRunning,
                onStartClick = { viewModel.startTimer() },
                onStopClick = { viewModel.stopTimer() },
                onResetClick = { viewModel.resetTimer() }
            )

            // Manual Mark Event Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EventMarkButton(
                    title = "Turn P",
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconColor = Color(0xFF2196F3),
                    containerColor = Color(0xFF2196F3),
                    enabled = uiState.isTimerRunning,
                    onClick = {
                        eventMarkDialogData = EventMarkDialogData(
                            title = "Turn P",
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            iconColor = Color(0xFF2196F3),
                            onConfirm = { 
                                viewModel.markTurnPoint(it)
                                eventMarkDialogData = null
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                EventMarkButton(
                    title = "Yellow",
                    icon = Icons.Default.WbSunny,
                    iconColor = Color(0xFFFFEB3B),
                    containerColor = Color(0xFFFFEB3B),
                    contentColor = Color.Black,
                    enabled = uiState.isTimerRunning,
                    onClick = {
                        eventMarkDialogData = EventMarkDialogData(
                            title = "Yellow",
                            icon = Icons.Default.WbSunny,
                            iconColor = Color(0xFFFFEB3B),
                            onConfirm = { 
                                viewModel.markYellowing(it)
                                eventMarkDialogData = null
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                
                EventMarkButton(
                    title = "1st Crack",
                    icon = Icons.Default.Whatshot,
                    iconColor = Color(0xFFFF9800),
                    containerColor = Color(0xFFFF9800),
                    enabled = uiState.isTimerRunning,
                    onClick = {
                        eventMarkDialogData = EventMarkDialogData(
                            title = "1st Crack",
                            icon = Icons.Default.Whatshot,
                            iconColor = Color(0xFFFF9800),
                            onConfirm = { 
                                viewModel.markFirstCrack(it)
                                eventMarkDialogData = null
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                
                EventMarkButton(
                    title = "Finish",
                    icon = Icons.Default.Flag,
                    iconColor = Color(0xFFF44336),
                    containerColor = Color(0xFFF44336),
                    enabled = uiState.isTimerRunning,
                    onClick = {
                        eventMarkDialogData = EventMarkDialogData(
                            title = "Finish",
                            icon = Icons.Default.Flag,
                            iconColor = Color(0xFFF44336),
                            onConfirm = { 
                                viewModel.markEndRoast(it)
                                eventMarkDialogData = null
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                val chartData = viewModel.getChartData()
                val chartRor = viewModel.getChartRor()
                val chartAirFlow = viewModel.getChartAirFlow()
                val chartRpm = viewModel.getChartRpm()
                val chartBurner = viewModel.getChartBurner()
                val intervalSeconds = uiState.setupData.intervalSeconds.toIntOrNull() ?: 60

                if (chartData.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        RoastingChart(
                            data = chartData,
                            intervalSeconds = intervalSeconds,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ROR chart
                if (chartRor.isNotEmpty()) {
                    RoastingChartRor(
                        data = chartRor,
                    )
                }

//                // Air Flow chart
//                if (chartAirFlow.isNotEmpty()) {
//                    RoastingChartAirFlow(
//                        data = chartAirFlow
//                    )
//                }
//
//                // RPM chart
//                if (chartRpm.isNotEmpty()) {
//                    RoastingChartRpm(
//                        data = chartRpm
//                    )
//                }
//
//                // Burner chart
//                if (chartBurner.isNotEmpty()) {
//                    RoastingChartBurner(
//                        data = chartBurner
//                    )
//                }
            }

            val isEnable = uiState.actualEndRoast != null && uiState.weightOut.isNotEmpty() && uiState.intervalDataList.isNotEmpty()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        selectedRevisionIndex = null // Reset selection saat buka dialog
                        showRevisionDialog = true 
                    },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.intervalDataList.isNotEmpty() || uiState.eventMarks.isNotEmpty()
                ) {
                    Text("Revisi Suhu")
                }

                Button(
                    onClick = {
                        scope.launch {
                            val result = exportToPdf()
                            if (result != null) {
                                snackbarHostState.showSnackbar("PDF exported: $result")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isEnable
                ) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export PDF")
                }
            }


            // Save Button
            Button(
                onClick = {
                    viewModel.saveToDatabase(context)
                    scope.launch {
                        snackbarHostState.showSnackbar("Data berhasil disimpan ke database")
                    }
                    onFinish()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = isEnable
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Simpan Roasting")
            }
        }

        // Event Mark Dialog (Turn P, Yellow, 1st Crack, Finish) - menggunakan TemperatureInputDialog
        eventMarkDialogData?.let { data ->
            TemperatureInputDialog(
                eventMarkData = data,
                onDismiss = { eventMarkDialogData = null },
                onConfirm = { 
                    data.onConfirm(it)
                    eventMarkDialogData = null
                }
            )
        }

        // Revision Dialog
        if (showRevisionDialog) {
            // Combine interval data and event marks with event names
            val revisionItems = buildList {
                // Add interval data
                uiState.intervalDataList.forEach { data ->
                    add(RevisionItem.Interval(data))
                }
                // Add event marks with proper names
                uiState.eventMarks.forEach { event ->
                    val eventName = when {
                        uiState.actualTurnPoint?.seconds == event.seconds -> "Turn Point"
                        uiState.actualYellowing?.seconds == event.seconds -> "Yellowing"
                        uiState.actualFirstCrack?.seconds == event.seconds -> "First Crack"
                        uiState.actualEndRoast?.seconds == event.seconds -> "End Roast"
                        else -> "Event"
                    }
                    add(RevisionItem.Event(event, eventName))
                }
            }.sortedBy { it.seconds }

            LaunchedEffect(showRevisionDialog, revisionItems) {
                if (selectedRevisionIndex == null && revisionItems.isNotEmpty()) {
                    val lastIndex = revisionItems.lastIndex
                    selectedRevisionIndex = lastIndex
                    revisionTemperatureInput = revisionItems.getOrNull(lastIndex)?.temperature?.toString() ?: ""
                }
            }

            AlertDialog(
                onDismissRequest = { showRevisionDialog = false },
                title = { Text("Revisi Suhu") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = revisionDropdownExpanded,
                            onExpandedChange = { revisionDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val label = selectedRevisionIndex?.let { idx ->
                                revisionItems.getOrNull(idx)?.let { item ->
                                    val m = item.seconds / 60
                                    val s = item.seconds % 60
                                    String.format(Locale.getDefault(), "%02d.%02d", m, s)
                                }
                            } ?: "Pilih waktu"

                            OutlinedTextField(
                                value = label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Waktu") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = revisionDropdownExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = revisionDropdownExpanded,
                                onDismissRequest = { revisionDropdownExpanded = false }
                            ) {
                                revisionItems.forEachIndexed { index, item ->
                                    val m = item.seconds / 60
                                    val s = item.seconds % 60
                                    val timeLabel = String.format(Locale.getDefault(), "%02d.%02d", m, s)
                                    val prefix = when(item) {
                                        is RevisionItem.Event -> "[${item.eventName}] "
                                        is RevisionItem.Interval -> ""
                                    }
                                    DropdownMenuItem(
                                        text = { 
                                            Text("$prefix$timeLabel - ${item.temperature}°C") 
                                        },
                                        onClick = {
                                            selectedRevisionIndex = index
                                            revisionTemperatureInput = item.temperature.toString()
                                            revisionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = revisionTemperatureInput,
                            onValueChange = { revisionTemperatureInput = it.filter { char -> char.isDigit() || char == '.' } },
                            label = { Text("Suhu (°C)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (revisionTemperatureInput.isNotEmpty()) {
                                        selectedRevisionIndex?.let { idx ->
                                            val item = revisionItems.getOrNull(idx)
                                            item?.let { 
                                                when(it) {
                                                    is RevisionItem.Interval -> viewModel.updateTemperatureAtInterval(it.data.intervalNumber, revisionTemperatureInput.toIntOrNull() ?: 0)
                                                    is RevisionItem.Event -> viewModel.updateEventTemperature(it.data, revisionTemperatureInput.toIntOrNull() ?: 0)
                                                }
                                            }
                                        }
                                        showRevisionDialog = false
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedRevisionIndex?.let { idx ->
                            val item = revisionItems.getOrNull(idx)
                            item?.let { 
                                when(it) {
                                    is RevisionItem.Interval -> viewModel.updateTemperatureAtInterval(it.data.intervalNumber, revisionTemperatureInput.toIntOrNull() ?: 0)
                                    is RevisionItem.Event -> viewModel.updateEventTemperature(it.data, revisionTemperatureInput.toIntOrNull() ?: 0)
                                }
                            }
                        }
                        showRevisionDialog = false
                    }) { Text("Submit") }
                },
                dismissButton = {
                    TextButton(onClick = { showRevisionDialog = false }) { Text("Batal") }
                }
            )
        }

        // Popups during run
        if (uiState.showTemperatureDialog) {
            TemperatureInputDialog(
                intervalNumber = uiState.currentInterval,
                elapsedTime = formatRunTime(uiState.elapsedMillis),
                voiceState = voiceState,
                onVoiceClick = { voiceRecognizer.startListening(context) },
                onDismiss = { 
                    voiceRecognizer.stopListening()
                    voiceRecognizer.resetState()
                    viewModel.dismissTemperatureDialog() 
                },
                onConfirm = { 
                    voiceRecognizer.stopListening()
                    voiceRecognizer.resetState()
                    viewModel.addTemperature(it) 
                }
            )
        }

        if (uiState.showRorDialog) {
            val ror = uiState.lastRorValue
            LaunchedEffect(Unit) {
                delay(3000)
                viewModel.dismissRorDialog()
            }
            AlertDialog(
                onDismissRequest = { viewModel.dismissRorDialog() },
                title = { Text("Rate of Rise (ROR)") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (ror != null) "${ror}°C" else "-",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (ror != null && ror > 0) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    }
                },
                confirmButton = { TextButton(onClick = { viewModel.dismissRorDialog() }) { Text("Tutup") } }
            )
        }

        if (uiState.showBurnerDialog) {
            val event = uiState.setupData.burnerPlan.getOrNull(uiState.currentBurnerIndex)
            AutoCloseAlertDialog(
                icon = Icons.Default.Whatshot,
                iconTint = Color(0xFFE64A19),
                title = "Burner Power",
                value = event?.temperature?.toString() ?: "-",
                time = event?.time ?: "-",
                backgroundColor = Color(0xFFE64A19),
                onDismiss = { viewModel.dismissBurnerDialog() }
            )
        }

        if (uiState.showAirFlowDialog) {
            val event = uiState.setupData.airFlowPlan.getOrNull(uiState.currentAirFlowIndex)
            AutoCloseAlertDialog(
                icon = Icons.Default.Air,
                iconTint = Color(0xFF0288D1),
                title = "Air Flow Power",
                value = event?.temperature?.toString() ?: "-",
                time = event?.time ?: "-",
                backgroundColor = Color(0xFF0288D1),
                onDismiss = { viewModel.dismissAirFlowDialog() }
            )
        }

        if (uiState.showRpmDialog) {
            val event = uiState.setupData.rpmPlan.getOrNull(uiState.currentRpmIndex)
            AutoCloseAlertDialog(
                icon = Icons.Default.Sync,
                iconTint = Color(0xFF43A047),
                title = "RPM Drum Speed",
                value = event?.temperature?.toString() ?: "-",
                time = event?.time ?: "-",
                backgroundColor = Color(0xFF43A047),
                onDismiss = { viewModel.dismissRpmDialog() }
            )
        }
    }
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
                    text = "${event.temperature}  -  ${event.time}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun AutoCloseAlertDialog(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    time: String,
    backgroundColor: Color,
    onDismiss: () -> Unit
) {
    LaunchedEffect(time) {
        delay(5000)
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background canvas with gradient
                Canvas(modifier = Modifier.matchParentSize()) {
                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.25f),
                            backgroundColor.copy(alpha = 0.05f)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                    drawRect(brush = gradientBrush)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                    Text(title)
                }
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background canvas with subtle color
                Canvas(modifier = Modifier.matchParentSize()) {
                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.05f),
                            backgroundColor.copy(alpha = 0.15f)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                    drawRect(brush = gradientBrush)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = iconTint
                    )
                    Text(
                        text = "At time: $time",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TimerSection(
    elapsedMillis: Long,
    isRunning: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatRunTime(elapsedMillis),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            if (isRunning) {
                Button(onClick = onStopClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Stop")
                }
            } else {
                Button(onClick = onStartClick) {
                    Text("Start")
                }
            }
//            Spacer(Modifier.width(8.dp))
//            OutlinedButton(onClick = onResetClick) {
//                Text("Reset")
//            }
        }
    }
}

private fun formatRunTime(totalMillis: Long): String {
    val totalSeconds = totalMillis / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val ds = (totalMillis % 1000) / 100
    
    return String.format(Locale.getDefault(), "%02d:%02d.%d", m, s, ds)
}

@Composable
fun TemperatureInputDialog(
    intervalNumber: Int? = null,
    elapsedTime: String? = null,
    eventMarkData: EventMarkDialogData? = null,
    isEventMarkMode: Boolean = eventMarkData != null,
    voiceState: VoiceRecognitionState = VoiceRecognitionState.Idle,
    onVoiceClick: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(voiceState) {
        if (voiceState is VoiceRecognitionState.Success) {
            input = voiceState.number.toString()
        }
    }
    
    AlertDialog(
        onDismissRequest = { if (!isEventMarkMode) onDismiss() },
        properties = if (isEventMarkMode) {
            DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        } else {
            DialogProperties()
        },
        icon = eventMarkData?.let { 
            { Icon(it.icon, contentDescription = null, tint = it.iconColor, modifier = Modifier.size(32.dp)) }
        },
        title = { 
            if (isEventMarkMode) {
                Text(eventMarkData?.title ?: "-")
            } else {
                Text("Input Suhu (${elapsedTime ?: ""})")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Suhu (°C)") },
                    trailingIcon = if (!isEventMarkMode) {
                        {
                            IconButton(onClick = onVoiceClick) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = if (voiceState is VoiceRecognitionState.Listening) 
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (input.isNotEmpty()) {
                                input.toIntOrNull()?.let { onConfirm(it) }
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                
                LaunchedEffect(Unit) {
                    delay(300)
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }

                if (!isEventMarkMode) {
                    if (voiceState is VoiceRecognitionState.Listening) {
                        Text("Mendengarkan...", color = MaterialTheme.colorScheme.primary)
                    }
                    if (voiceState is VoiceRecognitionState.Error) {
                        Text(voiceState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { input.toIntOrNull()?.let { onConfirm(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotEmpty()
            ) { Text("Submit") }
        },
        dismissButton = if (isEventMarkMode) {
            { TextButton(onClick = onDismiss) { Text("Batal") } }
        } else null
    )
}

@Composable
fun EventMarkButton(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    contentColor: Color = Color.Unspecified,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor.copy(alpha = if (enabled) 0.3f else 0.1f),
            contentColor = if (contentColor != Color.Unspecified) contentColor else LocalContentColor.current
        ),
        enabled = enabled
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconColor)
            Text(title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
fun AutoCloseAlertDialogBurnerPreview() {
    MaterialTheme {
        AutoCloseAlertDialog(
            icon = Icons.Default.Whatshot,
            iconTint = Color(0xFFE64A19),
            title = "Burner Power",
            value = "85",
            time = "03:45",
            backgroundColor = Color(0xFFE64A19),
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AutoCloseAlertDialogAirFlowPreview() {
    MaterialTheme {
        AutoCloseAlertDialog(
            icon = Icons.Default.Air,
            iconTint = Color(0xFF0288D1),
            title = "Air Flow Power",
            value = "75",
            time = "04:20",
            backgroundColor = Color(0xFF0288D1),
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AutoCloseAlertDialogRpmPreview() {
    MaterialTheme {
        AutoCloseAlertDialog(
            icon = Icons.Default.Sync,
            iconTint = Color(0xFF43A047),
            title = "RPM Drum Speed",
            value = "60",
            time = "05:10",
            backgroundColor = Color(0xFF43A047),
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TemperatureInputDialogPreview() {
    MaterialTheme {
        TemperatureInputDialog(
            intervalNumber = 5,
            elapsedTime = "03:45",
            voiceState = VoiceRecognitionState.Idle,
            onVoiceClick = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TemperatureInputDialogListeningPreview() {
    MaterialTheme {
        TemperatureInputDialog(
            intervalNumber = 3,
            elapsedTime = "02:20",
            voiceState = VoiceRecognitionState.Listening,
            onVoiceClick = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TemperatureInputDialogEventMarkPreview() {
    MaterialTheme {
        TemperatureInputDialog(
            eventMarkData = EventMarkDialogData(
                title = "First Crack",
                icon = Icons.Default.Flag,
                iconColor = Color(0xFFE91E63),
                onConfirm = {}
            ),
            voiceState = VoiceRecognitionState.Idle,
            onVoiceClick = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RorDialogPreview() {
    MaterialTheme {
        Box{
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Rate of Rise (ROR)") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "12°C",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    }
                },
                confirmButton = { TextButton(onClick = {}) { Text("Tutup") } }
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun RorDialogNegativePreview() {
    MaterialTheme {
        Box{
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Rate of Rise (ROR)") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "-3°C",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    }
                },
                confirmButton = { TextButton(onClick = {}) { Text("Tutup") } }
            )
        }

    }
}
