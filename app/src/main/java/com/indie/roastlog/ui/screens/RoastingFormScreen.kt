package com.indie.roastlog.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.speech.VoiceRecognitionState
import com.indie.roastlog.speech.VoiceRecognizerManager
import com.indie.roastlog.ui.components.RoastingChart
import com.indie.roastlog.ui.components.ChartDataPoint
import com.indie.roastlog.pdf.PdfExportManager
import com.indie.roastlog.pdf.RoastSessionData
import com.indie.roastlog.R
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoastingFormScreen(
    viewModel: RoastingViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val roastTypes = listOf("Light", "Medium", "Dark")

    val voiceRecognizer = remember { VoiceRecognizerManager(context) }
    val voiceState by voiceRecognizer.state.collectAsState()

    // Snackbar for showing export status
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    // Revision dialog state
    var showRevisionDialog by remember { mutableStateOf(false) }
    var selectedRevisionInterval by remember { mutableStateOf<Int?>(null) }
    var revisionTemperatureInput by remember { mutableStateOf("") }
    var revisionDropdownExpanded by remember { mutableStateOf(false) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecognizer.destroy()
        }
    }

    // PDF export function
    val exportToPdf = suspend {
        isExporting = true
        val pdfManager = PdfExportManager(context)
        val roastData = RoastSessionData(
            beanType = uiState.beanType,
            waterContent = uiState.waterContent,
            density = uiState.density,
            weightIn = uiState.weightIn,
            weightOut = uiState.weightOut,
            roastType = uiState.roastType,
            // Time & Temperature
            chargeTimeTemp = uiState.chargeTimeTemp,
            endTimeTemp = uiState.endTimeTemp,
            roastTime = uiState.roastTime,
            devTime = uiState.devTime,
            // Event Suhu
            turnPoint = uiState.turnPoint,
            yellowing = uiState.yellowing,
            firstCrack = uiState.firstCrack,
            // Parameter Mesin
            airFlowPower = uiState.airFlowPower,
            rpmDrum = uiState.rpmDrum,
            burnerPower = uiState.burnerPower,
            ror = uiState.ror,
            // Timer & Chart
            targetDuration = uiState.targetDuration.toIntOrNull() ?: 0,
            intervalSeconds = uiState.intervalSeconds.toIntOrNull() ?: 60,
            burnerIntervalSeconds = uiState.burnerIntervalSeconds.toIntOrNull() ?: 210,
            startTemperature = uiState.chargeTimeTemp.toFloatOrNull() ?: 70f,
            temperatureData = viewModel.getChartData().mapNotNull { point ->
                point.temperature?.let { Pair(point.intervalNumber, it) }
            }
        )

        val result = pdfManager.exportRoastSessionToPdf(roastData)
        isExporting = false
        result
    }

    Scaffold(
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
            Text(
                text = "Roasting Form",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SmallOutlinedTextField(
                value = uiState.beanType,
                onValueChange = { viewModel.updateBeanType(it) },
                label = "Jenis Bean",
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlinedTextField(
                    value = uiState.waterContent,
                    onValueChange = { viewModel.updateWaterContent(it) },
                    label = "Kadar Air (°)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.density,
                    onValueChange = { viewModel.updateDensity(it) },
                    label = "Density (kg/L)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlinedTextField(
                    value = uiState.weightIn,
                    onValueChange = { viewModel.updateWeightIn(it) },
                    label = "Berat Masuk (gr)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.weightOut,
                    onValueChange = { viewModel.updateWeightOut(it) },
                    label = "Berat Keluar (gr)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Roast Type Dropdown - Custom Style
            ExposedDropdownMenuBox(
                expanded = uiState.isRoastTypeExpanded,
                onExpandedChange = { viewModel.toggleRoastTypeExpanded(it) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = if (uiState.isRoastTypeExpanded) 4.dp else 1.dp,
                    tonalElevation = if (uiState.isRoastTypeExpanded) 4.dp else 1.dp,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Roasted Type",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isRoastTypeExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.roastType,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isRoastTypeExpanded)
                    }
                }

                ExposedDropdownMenu(
                    expanded = uiState.isRoastTypeExpanded,
                    onDismissRequest = { viewModel.toggleRoastTypeExpanded(false) }
                ) {
                    roastTypes.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                viewModel.updateRoastType(selectionOption)
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlinedTextField(
                    value = uiState.chargeTimeTemp,
                    onValueChange = { viewModel.updateChargeTimeTemp(it) },
                    label = "Charge Time (°C)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.endTimeTemp,
                    onValueChange = { viewModel.updateEndTimeTemp(it) },
                    label = "End Time (°C)",
                    keyboardType = KeyboardType.Decimal,
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlinedTextField(
                    value = uiState.roastTime,
                    onValueChange = { viewModel.updateRoastTime(it) },
                    label = "Roast Time (m)",
                    keyboardType = KeyboardType.Number,
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.devTime,
                    onValueChange = { viewModel.updateDevTime(it) },
                    label = "Dev Time (m)",
                    keyboardType = KeyboardType.Number,
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlinedTextField(
                    value = uiState.yellowing,
                    onValueChange = { viewModel.updateYellowing(it) },
                    label = "Yellowing (°C)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.firstCrack,
                    onValueChange = { viewModel.updateFirstCrack(it) },
                    label = "First Crack (°C)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Card Turn Point
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pengaturan Turn Point",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    var newTurnPointTemp by remember { mutableStateOf("") }
                    var newTurnPointSeconds by remember { mutableStateOf("") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallOutlinedTextField(
                            value = newTurnPointTemp,
                            onValueChange = { newTurnPointTemp = it.filter { it.isDigit() || it == '.' } },
                            label = "Suhu (°C)",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        SmallOutlinedTextField(
                            value = newTurnPointSeconds,
                            onValueChange = { newTurnPointSeconds = it.filter { it.isDigit() } },
                            label = "Detik (s)",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {


                        // List of turn points
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.turnPoints?.let { event ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text("${event.temperature}°C / ${event.seconds}s") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { viewModel.removeTurnPoint() },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val temp = newTurnPointTemp.toFloatOrNull()
                                val seconds = newTurnPointSeconds.toIntOrNull()
                                if (temp != null && seconds != null) {
                                    viewModel.addTurnPoint(temp, seconds)
                                    newTurnPointTemp = ""
                                    newTurnPointSeconds = ""
                                }
                            },
                            enabled = newTurnPointTemp.toFloatOrNull() != null && newTurnPointSeconds.toIntOrNull() != null
                        ) {
                            Text("Save")
                        }
                    }
                }
            }

            HorizontalDivider()

            // Card with Duration, Interval inputs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pengaturan Timer",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallOutlinedTextField(
                            value = uiState.targetDuration,
                            onValueChange = { viewModel.updateTargetDuration(it) },
                            label = "Durasi (m)",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        SmallOutlinedTextField(
                            value = uiState.intervalSeconds,
                            onValueChange = { viewModel.updateIntervalSeconds(it) },
                            label = "Interval Suhu (s)",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Card Pengaturan Burner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pengaturan Burner",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Burner Value List with Add/Remove
                    var newBurnerValue by remember { mutableStateOf("") }

                    // Add new burner value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallOutlinedTextField(
                            value = newBurnerValue,
                            onValueChange = { newBurnerValue = it.filter { it.isDigit() }  },
                            label ="Burner Baru",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val value = newBurnerValue.toIntOrNull()
                                if (value != null && value > 0) {
                                    viewModel.addBurnerValue(value)
                                    newBurnerValue = ""
                                }
                            },
                            enabled = newBurnerValue.toIntOrNull() != null && newBurnerValue.toIntOrNull()!! > 0
                        ) {
                            Text("Save")
                        }
                    }
                    // List of burner values
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.burnerValues.forEach { value ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(value.toString()) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.removeBurnerValue(value) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    // Burner Interval Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallOutlinedTextField(
                            value = uiState.burnerIntervalSeconds,
                            onValueChange = { viewModel.updateBurnerIntervalSeconds(it) },
                            label = "Interval Burner (s)",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Default: 210s (3m 30s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            IncrementDecrementField(
                value = uiState.airFlowPower,
                onValueChange = { viewModel.updateAirFlowPower(it) },
                label = "Air Flow Power (besaran buangan asap)",
                modifier = Modifier.fillMaxWidth()
            )

            IncrementDecrementField(
                value = uiState.rpmDrum,
                onValueChange = { viewModel.updateRpmDrum(it) },
                label = "RPM Drum (kecepatan putaran drum)",
                modifier = Modifier.fillMaxWidth()
            )

            TimerSection(
                elapsedSeconds = uiState.elapsedSeconds,
                isRunning = uiState.isTimerRunning,
                canStart = uiState.canStartTimer(),
                onStartClick = { viewModel.startTimer() },
                onStopClick = { viewModel.stopTimer() },
                onResetClick = { viewModel.resetTimer() }
            )

            val chartData = viewModel.getChartData()
            val intervalSeconds = uiState.intervalSeconds.toIntOrNull() ?: 60
            if (chartData.isNotEmpty()) {
                ChartSection(
                    data = chartData,
                    intervalSeconds = intervalSeconds,
                )
            }

            val scope = rememberCoroutineScope()

            Row {
                Button(
                    onClick = { showRevisionDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.intervalDataList.isNotEmpty()
                ) {
                    Text("Revisi Suhu")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (!isExporting) {
                            scope.launch {
                                val result = exportToPdf()
                                if (result != null) {
                                    snackbarHostState.showSnackbar(
                                        message = "PDF berhasil diekspor ke: $result",
                                        duration = SnackbarDuration.Long
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = "Gagal mengekspor PDF",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isExporting && uiState.intervalDataList.isNotEmpty()
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mengekspor...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF")
                    }
                }
            }

        }

        if (showRevisionDialog) {
            val intervalSeconds = uiState.intervalSeconds.toIntOrNull() ?: 60
            val sortedIntervals = uiState.intervalDataList.map { it.intervalNumber }.distinct().sorted()

            LaunchedEffect(sortedIntervals) {
                if (selectedRevisionInterval == null && sortedIntervals.isNotEmpty()) {
                    val firstInterval = sortedIntervals.first()
                    selectedRevisionInterval = firstInterval
                    val currentTemp = uiState.intervalDataList.firstOrNull { it.intervalNumber == firstInterval }?.temperature
                    revisionTemperatureInput = currentTemp?.toString() ?: ""
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
                            val selectedLabel = selectedRevisionInterval?.let {
                                formatIntervalLabel(it, intervalSeconds)
                            } ?: "Pilih menit"

                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Menit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = revisionDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = revisionDropdownExpanded,
                                onDismissRequest = { revisionDropdownExpanded = false }
                            ) {
                                sortedIntervals.forEach { interval ->
                                    val label = formatIntervalLabel(interval, intervalSeconds)
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedRevisionInterval = interval
                                            val currentTemp = uiState.intervalDataList.firstOrNull { it.intervalNumber == interval }?.temperature
                                            revisionTemperatureInput = currentTemp?.toString() ?: ""
                                            revisionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = revisionTemperatureInput,
                            onValueChange = { value ->
                                revisionTemperatureInput = value.filter { it.isDigit() || it == '.' }
                            },
                            label = { Text("Suhu (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val interval = selectedRevisionInterval
                            val newTemp = revisionTemperatureInput.toFloatOrNull()
                            if (interval != null && newTemp != null) {
                                viewModel.updateTemperatureAtInterval(interval, newTemp)
                                showRevisionDialog = false
                            }
                        },
                        enabled = selectedRevisionInterval != null && revisionTemperatureInput.toFloatOrNull() != null
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRevisionDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (uiState.showTemperatureDialog) {
            val interval = uiState.intervalSeconds.toIntOrNull() ?: 60
            val currentTimeSeconds = uiState.currentInterval * interval
            val currentMinute = currentTimeSeconds / 60
            val currentSecond = currentTimeSeconds % 60

            TemperatureInputDialog(
                intervalNumber = uiState.currentInterval,
                elapsedTime = String.format(Locale.getDefault(), "%02d:%02d", currentMinute, currentSecond),
                voiceState = voiceState,
                hasAudioPermission = hasAudioPermission,
                onDismiss = {
                    voiceRecognizer.stopListening()
                    viewModel.dismissTemperatureDialog()
                },
                onConfirm = { temperature ->
                    voiceRecognizer.stopListening()
                    viewModel.addTemperature(temperature)
                },
                onStartVoiceInput = {
                    if (hasAudioPermission) {
                        voiceRecognizer.resetState()
                        voiceRecognizer.startListening(context)
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                autoStartVoice = hasAudioPermission,
                onAutoStartVoice = {
                    voiceRecognizer.resetState()
                    voiceRecognizer.startListening(context)
                },
                onAutoConfirm = { temperature ->
                    voiceRecognizer.stopListening()
                    viewModel.addTemperature(temperature)
                }
            )
        }

        // ROR Dialog - shows after temperature input for 3 seconds
        if (uiState.showRorDialog) {
            val rorValue = uiState.lastRorValue

            // Auto dismiss after 3 seconds
            LaunchedEffect(Unit) {
                delay(3000)
                viewModel.dismissRorDialog()
            }

            AlertDialog(
                onDismissRequest = { viewModel.dismissRorDialog() },
                title = {
                    Text(
                        text = "Rate of Rise (ROR)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (rorValue != null) {
                            val rorColor = when {
                                rorValue > 0 -> MaterialTheme.colorScheme.primary
                                rorValue < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            val rorIcon = when {
                                rorValue > 0 -> "↑"
                                rorValue < 0 -> "↓"
                                else -> "→"
                            }

                            Text(
                                text = "$rorIcon ${String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(rorValue))}°C",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = rorColor
                            )
                            Text(
                                text = if (rorValue > 0) "Suhu naik" else if (rorValue < 0) "Suhu turun" else "Suhu stabil",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Data ROR tidak tersedia",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Countdown indicator
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Menutup dalam 3 detik...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissRorDialog() }) {
                        Text("Tutup")
                    }
                }
            )
        }

        // Burner Input Dialog - shows one value at a time with 5s auto-close
        if (uiState.showBurnerDialog) {
            val currentBurnerValue = uiState.burnerValues.getOrNull(uiState.currentBurnerIndex)

            // Update burner power to current value
            LaunchedEffect(currentBurnerValue) {
                currentBurnerValue?.let {
                    viewModel.updateBurnerPower(it.toString())
                }
            }

            // Auto dismiss after 5 seconds
            LaunchedEffect(Unit) {
                delay(5000)
                viewModel.dismissBurnerDialog()
            }

            AlertDialog(
                onDismissRequest = { viewModel.dismissBurnerDialog() },
                title = {
                    Text(
                        text = "Burner Power",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (currentBurnerValue != null) {
                            Text(
                                text = "Atur burner ke:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentBurnerValue.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nilai ke-${uiState.currentBurnerIndex + 1} dari ${uiState.burnerValues.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Tidak ada nilai burner",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Countdown indicator
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Menutup dalam 5 detik...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissBurnerDialog() }) {
                        Text("Tutup")
                    }
                }
            )
        }
    }
}

@Composable
private fun TimerSection(
    elapsedSeconds: Int,
    isRunning: Boolean,
    canStart: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRunning) {
                        Button(
                            onClick = onStopClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop")
                        }
                    } else {
                        Button(
                            onClick = onStartClick,
                            enabled = canStart
                        ) {
                            Text("Start")
                        }
                    }

                    OutlinedButton(onClick = onResetClick) {
                        Text("Reset")
                    }
                }
            }
            if (!canStart && !isRunning) {
                Text(
                    text = "Masukkan durasi, interval, dan Charge Time (70-240°C)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ChartSection(
    data: List<ChartDataPoint>,
    intervalSeconds: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            RoastingChart(
                data = data,
                intervalSeconds = intervalSeconds,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Suppress("UNUSED_VALUE")
@Composable
private fun TemperatureInputDialog(
    intervalNumber: Int,
    elapsedTime: String,
    voiceState: VoiceRecognitionState,
    hasAudioPermission: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    onStartVoiceInput: () -> Unit,
    autoStartVoice: Boolean,
    onAutoStartVoice: () -> Unit,
    onAutoConfirm: (Float) -> Unit
) {
    val context = LocalContext.current
    var temperatureInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasAutoConfirmed by remember { mutableStateOf(false) }

    // Function to play notification sound
    val playNotificationSound = {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.coins)
            mediaPlayer?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                setVolume(1f, 1f)
                setOnCompletionListener { it.release() }
                start()
            }
        } catch (_: Exception) {
            // Ignore sound errors
        }
    }

    // Play sound when dialog first appears
    LaunchedEffect(Unit) {
        playNotificationSound()
    }

    LaunchedEffect(Unit) {
        if (autoStartVoice && !hasAutoConfirmed) {
            delay(600)
            onAutoStartVoice()
        }
    }

    LaunchedEffect(voiceState) {
        when (voiceState) {
            is VoiceRecognitionState.Success -> {
                temperatureInput = voiceState.number.toInt().toString()
                isError = false
                errorMessage = ""
                
                if (!hasAutoConfirmed) {
                    hasAutoConfirmed = true
                    delay(600)
                    onAutoConfirm(voiceState.number)
                }
            }
            is VoiceRecognitionState.Error -> {
                if (voiceState.message != "Tidak ada hasil") {
                    isError = true
                    errorMessage = voiceState.message
                }
                if (autoStartVoice && hasAudioPermission && !hasAutoConfirmed) {
                    delay(800)
                    onAutoStartVoice()
                }
            }
            is VoiceRecognitionState.Listening -> {
                // Play sound when voice recognition restarts (after error or no result)
                playNotificationSound()
            }
            else -> {
                isError = false
                errorMessage = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Input Suhu #$intervalNumber (Menit $elapsedTime)")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (voiceState is VoiceRecognitionState.Listening) {
                    ListeningAnimation()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mendengarkan...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Silakan katakan suhu (contoh: 'seratus lima puluh' atau '150'):",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = temperatureInput,
                    onValueChange = { value ->
                        temperatureInput = value.filter { it.isDigit() || it == '.' }
                        isError = false
                        errorMessage = ""
                    },
                    label = { Text("Suhu (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        when {
                            errorMessage.isNotEmpty() -> Text(errorMessage, color = MaterialTheme.colorScheme.error)
                            voiceState is VoiceRecognitionState.Success -> Text("✓ Suhu terdeteksi: ${temperatureInput}°C", color = MaterialTheme.colorScheme.primary)
                            voiceState is VoiceRecognitionState.Listening -> Text("Katakan suhu sekarang...", color = MaterialTheme.colorScheme.primary)
                            else -> Text("Ketik angka atau gunakan mikrofon")
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = onStartVoiceInput) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Input suara",
                                tint = when {
                                    voiceState is VoiceRecognitionState.Listening ->
                                        MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!hasAudioPermission) {
                    Text(
                        text = "⚠️ Permission mikrofon diperlukan untuk input suara",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val temperature = temperatureInput.toFloatOrNull()
                    if (temperature != null && temperature > 0) {
                        onConfirm(temperature)
                    } else {
                        isError = true
                        errorMessage = "Masukkan angka yang valid"
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private fun formatIntervalLabel(interval: Int, intervalSeconds: Int): String {
    val totalSeconds = interval * intervalSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d.%02d", minutes, seconds)
}

@Composable
private fun SmallOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.FocusInteraction.Focus -> isFocused = true
                is androidx.compose.foundation.interaction.FocusInteraction.Unfocus -> isFocused = false
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isFocused) 4.dp else 1.dp,
        tonalElevation = if (isFocused) 4.dp else 1.dp,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IncrementDecrementField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    step: Int = 1
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.FocusInteraction.Focus -> isFocused = true
                is androidx.compose.foundation.interaction.FocusInteraction.Unfocus -> isFocused = false
            }
        }
    }

    fun increment() {
        val currentValue = value.toIntOrNull() ?: 0
        onValueChange((currentValue + step).toString())
    }

    fun decrement() {
        val currentValue = value.toIntOrNull() ?: 0
        onValueChange((currentValue - step).coerceAtLeast(0).toString())
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TextField area
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = if (isFocused) 4.dp else 1.dp,
            tonalElevation = if (isFocused) 4.dp else 1.dp,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyMedium.merge(
                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // +/- Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { decrement() },
                color = MaterialTheme.colorScheme.secondary,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "-",
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Button +
            Surface(
                onClick = { increment() },
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

        }
    }
}

@Composable
private fun ListeningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "listening")
    
    val scales = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, delayMillis = index * 200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale$index"
        )
    }
    
    val alpha = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        scales.forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(scale.value)
                    .alpha(if (index == 0) alpha.value else 0.3f)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
        
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
