package com.indie.roastlog.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.RingtoneManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.speech.VoiceRecognitionState
import com.indie.roastlog.speech.VoiceRecognizerManager
import com.indie.roastlog.viewmodel.RoastingViewModel
import com.indie.roastlog.ui.components.RoastingChart
import com.indie.roastlog.ui.components.ChartDataPoint
import com.indie.roastlog.pdf.PdfExportManager
import com.indie.roastlog.pdf.RoastSessionData
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
            startTemperature = uiState.startTemperature.toFloatOrNull() ?: 70f,
            temperatureData = uiState.temperatureData
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

            OutlinedTextField(
                value = uiState.beanType,
                onValueChange = { viewModel.updateBeanType(it) },
                label = { Text("Jenis Bean") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.waterContent,
                onValueChange = { viewModel.updateWaterContent(it) },
                label = { Text("Kadar Air (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.density,
                onValueChange = { viewModel.updateDensity(it) },
                label = { Text("Density (kg/l)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.weightIn,
                onValueChange = { viewModel.updateWeightIn(it) },
                label = { Text("Berat Masuk (gram)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.weightOut,
                onValueChange = { viewModel.updateWeightOut(it) },
                label = { Text("Berat Keluar (gram)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = uiState.isRoastTypeExpanded,
                onExpandedChange = { viewModel.toggleRoastTypeExpanded(it) },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.roastType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Roast Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isRoastTypeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

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

            // Section: Time & Temperature
            Text(
                text = "Time & Temperature",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.chargeTimeTemp,
                    onValueChange = { viewModel.updateChargeTimeTemp(it) },
                    label = { Text("Charge Time (°C)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.endTimeTemp,
                    onValueChange = { viewModel.updateEndTimeTemp(it) },
                    label = { Text("End Time (°C)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.roastTime,
                    onValueChange = { viewModel.updateRoastTime(it) },
                    label = { Text("Roast Time (menit)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.devTime,
                    onValueChange = { viewModel.updateDevTime(it) },
                    label = { Text("Dev Time (menit)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // Section: Event Suhu
            Text(
                text = "Event Suhu",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.turnPoint,
                    onValueChange = { viewModel.updateTurnPoint(it) },
                    label = { Text("Turn Point (°C)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.yellowing,
                    onValueChange = { viewModel.updateYellowing(it) },
                    label = { Text("Yellowing (°C)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = uiState.firstCrack,
                onValueChange = { viewModel.updateFirstCrack(it) },
                label = { Text("First Crack (°C)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Section: Parameter Mesin
            Text(
                text = "Parameter Mesin",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.airFlowPower,
                    onValueChange = { viewModel.updateAirFlowPower(it) },
                    label = { Text("Air Flow Power") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.rpmDrum,
                    onValueChange = { viewModel.updateRpmDrum(it) },
                    label = { Text("RPM Drum") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.burnerPower,
                    onValueChange = { viewModel.updateBurnerPower(it) },
                    label = { Text("Burner Power") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.ror,
                    onValueChange = { viewModel.updateRor(it) },
                    label = { Text("ROR") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = uiState.targetDuration,
                onValueChange = { viewModel.updateTargetDuration(it) },
                label = { Text("Durasi Roasting (menit)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isTimerRunning
            )

            OutlinedTextField(
                value = uiState.intervalSeconds,
                onValueChange = { viewModel.updateIntervalSeconds(it) },
                label = { Text("Interval Input Suhu (detik)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !uiState.isTimerRunning
            )

            OutlinedTextField(
                value = uiState.startTemperature,
                onValueChange = { viewModel.updateStartTemperature(it) },
                label = { Text("Suhu Awal (°C)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !uiState.isTimerRunning,
                supportingText = { Text("Range: 70°C - 240°C") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val chartData = viewModel.getChartData()
            val intervalSeconds = uiState.intervalSeconds.toIntOrNull() ?: 60
            if (chartData.isNotEmpty()) {
                ChartSection(data = chartData, intervalSeconds = intervalSeconds)
                Spacer(modifier = Modifier.height(16.dp))
            }

            TimerSection(
                elapsedSeconds = uiState.elapsedSeconds,
                isRunning = uiState.isTimerRunning,
                canStart = uiState.canStartTimer(),
                targetDuration = uiState.targetDuration.toIntOrNull() ?: 0,
                intervalSeconds = uiState.intervalSeconds.toIntOrNull() ?: 60,
                startTemperature = uiState.startTemperature.toFloatOrNull(),
                onStartClick = { viewModel.startTimer() },
                onStopClick = { viewModel.stopTimer() },
                onResetClick = { viewModel.resetTimer() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val scope = rememberCoroutineScope()
            
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting && uiState.temperatureData.isNotEmpty()
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
    }
}

@Composable
private fun TimerSection(
    elapsedSeconds: Int,
    isRunning: Boolean,
    canStart: Boolean,
    targetDuration: Int,
    intervalSeconds: Int,
    startTemperature: Float?,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val formattedDuration = String.format(Locale.getDefault(), "%02d:00", targetDuration)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Roasting Timer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (targetDuration > 0) {
                Text(
                    text = "Target: $formattedDuration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (intervalSeconds > 0) {
                Text(
                    text = "Input suhu setiap $intervalSeconds detik",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (startTemperature != null) {
                Text(
                    text = "Suhu awal: ${startTemperature.toInt()}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

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

            if (!canStart && !isRunning) {
                Text(
                    text = "Masukkan durasi, interval, dan suhu awal (70-240°C)",
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
                .padding(16.dp)
        ) {
            Text(
                text = "Temperature Profile",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            RoastingChart(
                data = data,
                intervalSeconds = intervalSeconds,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
    var hasPlayedSound by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPlayedSound) {
            hasPlayedSound = true
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notification)
                ringtone?.play()
            } catch (_: Exception) {
                // Ignore sound errors
            }
        }
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
