package com.indie.roastlog.ui.screens.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.ui.model.RoastingEvent
import com.indie.roastlog.ui.components.SmallOutlinedTextField
import com.indie.roastlog.ui.components.IncrementDecrementField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoastingFormScreen(
    viewModel: RoastingFormViewModel = viewModel(),
    onStartRoast: (RoastingFormState) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val roastTypes = listOf("Light", "Medium", "Dark")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Setup Roasting") })
        }
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
                text = "Informasi Bean",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
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

            SmallOutlinedTextField(
                value = uiState.weightIn,
                onValueChange = { viewModel.updateWeightIn(it) },
                label = "Berat Masuk (gr)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
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
                    label = { Text("Roasted Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isRoastTypeExpanded) },
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
                            onClick = { viewModel.updateRoastType(selectionOption) }
                        )
                    }
                }
            }

            HorizontalDivider()

            SmallOutlinedTextField(
                value = uiState.chargeTimeTemp,
                onValueChange = { viewModel.updateChargeTimeTemp(it) },
                label = "Charge Temp (°C)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Card Events
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Pengaturan Event Roasting", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    EventInputSection(title = "Turn Point", currentEvent = uiState.setupTurnPoint, onAdd = viewModel::addTurnPoint, onRemove = viewModel::removeTurnPoint)
                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    EventInputSection(title = "Yellowing", currentEvent = uiState.setupYellowing, onAdd = viewModel::addYellowing, onRemove = viewModel::removeYellowing)
                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    EventInputSection(title = "First Crack", currentEvent = uiState.setupFirstCrack, onAdd = viewModel::addFirstCrack, onRemove = viewModel::removeFirstCrack)
                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                    EventInputSection(title = "End Roasting", currentEvent = uiState.setupEndRoast, onAdd = viewModel::addEndRoast, onRemove = viewModel::removeEndRoast)
                }
            }

            // Burner Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Pengaturan Burner Plan", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    var bPower by remember { mutableStateOf("") }
                    var bMin by remember { mutableStateOf("0") }
                    var bSec by remember { mutableStateOf("0") }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SmallOutlinedTextField(value = bPower, onValueChange = { input -> bPower = input.filter { it.isDigit() } }, label = "Power", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            SmallOutlinedTextField(value = bMin, onValueChange = { input -> bMin = input.filter { it.isDigit() }.take(2) }, label = "Min", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                            Text(":", fontWeight = FontWeight.Bold)
                            SmallOutlinedTextField(value = bSec, onValueChange = { input -> bSec = input.filter { it.isDigit() }.take(2) }, label = "Sec", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FlowRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.burnerPlan.forEach { event ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text("${event.temperature.toInt()} / ${event.time}") },
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.removeBurnerEvent(event) }, modifier = Modifier.size(16.dp)) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val p = bPower.toFloatOrNull()
                                val s = (bMin.toIntOrNull() ?: 0) * 60 + (bSec.toIntOrNull() ?: 0)
                                if (p != null && s > 0) {
                                    viewModel.addBurnerEvent(p, s)
                                    bPower = ""; bMin = "0"; bSec = "0"
                                }
                            },
                            enabled = bPower.isNotEmpty() && ((bMin.toIntOrNull() ?: 0) * 60 + (bSec.toIntOrNull() ?: 0)) > 0
                        ) { Text("Save") }
                    }
                }
            }

            IncrementDecrementField(value = uiState.airFlowPower, onValueChange = viewModel::updateAirFlowPower, label = "Air Flow Power", modifier = Modifier.fillMaxWidth())
            IncrementDecrementField(value = uiState.rpmDrum, onValueChange = viewModel::updateRpmDrum, label = "RPM Drum", modifier = Modifier.fillMaxWidth())

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Pengaturan Timer", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallOutlinedTextField(value = uiState.targetDuration, onValueChange = viewModel::updateTargetDuration, label = "Durasi (m)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                        SmallOutlinedTextField(value = uiState.intervalSeconds, onValueChange = viewModel::updateIntervalSeconds, label = "Interval (s)", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    }
                }
            }

            Button(
                onClick = { onStartRoast(uiState) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.beanType.isNotEmpty() && uiState.weightIn.isNotEmpty()
            ) {
                Text("Start Roasting")
            }
        }
    }
}

@Composable
private fun EventInputSection(
    title: String,
    currentEvent: RoastingEvent?,
    onAdd: (Float, Int) -> Unit,
    onRemove: () -> Unit
) {
    var tempInput by remember { mutableStateOf("") }
    var minInput by remember { mutableStateOf("0") }
    var secInput by remember { mutableStateOf("0") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallOutlinedTextField(value = tempInput, onValueChange = { input -> tempInput = input.filter { it.isDigit() || it == '.' } }, label = "Suhu (°C)", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallOutlinedTextField(value = minInput, onValueChange = { input -> minInput = input.filter { it.isDigit() }.take(2) }, label = "Min", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                Text(":", fontWeight = FontWeight.Bold)
                SmallOutlinedTextField(value = secInput, onValueChange = { input -> secInput = input.filter { it.isDigit() }.take(2) }, label = "Sec", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (currentEvent != null) {
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text("${currentEvent.temperature}°C / ${currentEvent.time}") },
                        trailingIcon = {
                            IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(12.dp))
                            }
                        }
                    )
                }
            }
            Button(
                onClick = {
                    val t = tempInput.toFloatOrNull()
                    val s = (minInput.toIntOrNull() ?: 0) * 60 + (secInput.toIntOrNull() ?: 0)
                    if (t != null && s > 0) {
                        onAdd(t, s)
                        tempInput = ""; minInput = "0"; secInput = "0"
                    }
                },
                enabled = tempInput.isNotEmpty() && ((minInput.toIntOrNull() ?: 0) * 60 + (secInput.toIntOrNull() ?: 0)) > 0
            ) { Text("Save") }
        }
    }
}
