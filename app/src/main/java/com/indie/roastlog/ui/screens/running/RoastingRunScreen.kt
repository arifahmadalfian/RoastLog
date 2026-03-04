package com.indie.roastlog.ui.screens.running

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.pdf.PdfExportManager
import com.indie.roastlog.pdf.RoastSessionData
import com.indie.roastlog.speech.VoiceRecognitionState
import com.indie.roastlog.speech.VoiceRecognizerManager
import com.indie.roastlog.ui.components.RoastingChart
import com.indie.roastlog.ui.components.SmallOutlinedTextField
import com.indie.roastlog.ui.screens.form.RoastingFormState
import com.indie.roastlog.ui.model.RoastingEvent
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedRevisionInterval by remember { mutableStateOf<Int?>(null) }
    var revisionTemperatureInput by remember { mutableStateOf("") }
    var revisionDropdownExpanded by remember { mutableStateOf(false) }

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
            burnerEvents = currentSetup.burnerPlan.map { "${it.temperature.toInt()} / ${it.time}" },
            targetDuration = currentSetup.targetDuration.toIntOrNull() ?: 0,
            intervalSeconds = currentSetup.intervalSeconds.toIntOrNull() ?: 60,
            startTemperature = currentSetup.chargeTimeTemp.toFloatOrNull() ?: 70f,
            temperatureData = viewModel.getChartData()
        )
        pdfManager.exportRoastSessionToPdf(roastData)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Roasting: ${uiState.setupData.beanType}") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimerSection(
                elapsedSeconds = uiState.elapsedSeconds,
                isRunning = uiState.isTimerRunning,
                onStartClick = { viewModel.startTimer() },
                onStopClick = { viewModel.stopTimer() },
                onResetClick = { viewModel.resetTimer() }
            )

            // Manual Mark Event Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        val lastTemp = uiState.intervalDataList.lastOrNull()?.temperature ?: 0f
                        viewModel.markYellowing(lastTemp)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B), contentColor = Color.Black)
                ) { Text("Yellowing") }
                
                Button(
                    onClick = { 
                        val lastTemp = uiState.intervalDataList.lastOrNull()?.temperature ?: 0f
                        viewModel.markFirstCrack(lastTemp)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) { Text("1st Crack") }
                
                Button(
                    onClick = { 
                        val lastTemp = uiState.intervalDataList.lastOrNull()?.temperature ?: 0f
                        viewModel.markEndRoast(lastTemp)
                        viewModel.saveToDatabase(context)
                        onFinish() 
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Finish") }
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

            val chartData = viewModel.getChartData()
            val intervalSeconds = uiState.setupData.intervalSeconds.toIntOrNull() ?: 60
            if (chartData.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    RoastingChart(
                        data = chartData,
                        intervalSeconds = intervalSeconds,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showRevisionDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.intervalDataList.isNotEmpty()
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
                    enabled = uiState.intervalDataList.isNotEmpty()
                ) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export PDF")
                }
            }
        }

        // Revision Dialog
        if (showRevisionDialog) {
            val intervalSec = uiState.setupData.intervalSeconds.toIntOrNull() ?: 60
            val sortedIntervals = uiState.intervalDataList.map { it.intervalNumber }.distinct().sorted()

            LaunchedEffect(sortedIntervals) {
                if (selectedRevisionInterval == null && sortedIntervals.isNotEmpty()) {
                    val first = sortedIntervals.first()
                    selectedRevisionInterval = first
                    revisionTemperatureInput = uiState.intervalDataList.find { it.intervalNumber == first }?.temperature?.toString() ?: ""
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
                            val label = selectedRevisionInterval?.let {
                                val ts = it * intervalSec
                                String.format(Locale.getDefault(), "%d.%02d", ts / 60, ts % 60)
                            } ?: "Pilih menit"

                            OutlinedTextField(
                                value = label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Menit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = revisionDropdownExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = revisionDropdownExpanded,
                                onDismissRequest = { revisionDropdownExpanded = false }
                            ) {
                                sortedIntervals.forEach { intervalNum ->
                                    val ts = intervalNum * intervalSec
                                    val l = String.format(Locale.getDefault(), "%d.%02d", ts / 60, ts % 60)
                                    DropdownMenuItem(
                                        text = { Text(l) },
                                        onClick = {
                                            selectedRevisionInterval = intervalNum
                                            revisionTemperatureInput = uiState.intervalDataList.find { it.intervalNumber == intervalNum }?.temperature?.toString() ?: ""
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
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedRevisionInterval?.let { viewModel.updateTemperatureAtInterval(it, revisionTemperatureInput.toFloatOrNull() ?: 0f) }
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
                elapsedTime = formatRunTime(uiState.elapsedSeconds),
                voiceState = voiceState,
                onDismiss = { viewModel.dismissTemperatureDialog() },
                onConfirm = { viewModel.addTemperature(it) }
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
                            text = if (ror != null) String.format(Locale.getDefault(), "%.1f°C", ror) else "-",
                            style = MaterialTheme.typography.displayMedium,
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
            BurnerAlertDialog(
                power = event?.temperature?.toInt()?.toString() ?: "-",
                time = event?.time ?: "-",
                onDismiss = { viewModel.dismissBurnerDialog() }
            )
        }
    }
}

@Composable
fun TimerSection(
    elapsedSeconds: Int,
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
                text = formatRunTime(elapsedSeconds),
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
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onResetClick) {
                Text("Reset")
            }
        }
    }
}

private fun formatRunTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

@Composable
fun TemperatureInputDialog(
    intervalNumber: Int,
    elapsedTime: String,
    voiceState: VoiceRecognitionState,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var input by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Suhu #$intervalNumber ($elapsedTime)") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Suhu (°C)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (voiceState is VoiceRecognitionState.Listening) {
                    Text("Mendengarkan...", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(onClick = { input.toFloatOrNull()?.let { onConfirm(it) } }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = {
                input = ""
                onDismiss()
            }) { Text("Batal") }
        }
    )
}

@Composable
fun BurnerAlertDialog(power: String, time: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Burner") },
        text = { Text("Waktu $time: Ubah burner ke $power") },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}
