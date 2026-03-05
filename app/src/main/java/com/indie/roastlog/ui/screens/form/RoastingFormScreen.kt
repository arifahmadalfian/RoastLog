package com.indie.roastlog.ui.screens.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.ui.components.ScaffoldCustom
import com.indie.roastlog.ui.model.RoastingEvent
import com.indie.roastlog.ui.components.SmallOutlinedTextField
import com.indie.roastlog.ui.components.SmallDropdownField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoastingFormScreen(
    viewModel: RoastingFormViewModel = viewModel(),
    onStartRoast: (RoastingFormState) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val roastTypes = listOf("Light", "Medium", "Dark")

    ScaffoldCustom(
        title = "Setup Roasting"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Jenis Bean & Kadar Air
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlinedTextField(
                    value = uiState.beanType,
                    onValueChange = { viewModel.updateBeanType(it) },
                    label = "Jenis Bean",
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.waterContent,
                    onValueChange = { viewModel.updateWaterContent(it) },
                    label = "Kadar Air (%)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Density & Berat Masuk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallOutlinedTextField(
                    value = uiState.density,
                    onValueChange = { viewModel.updateDensity(it) },
                    label = "Density (kg/L)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                SmallOutlinedTextField(
                    value = uiState.weightIn,
                    onValueChange = { viewModel.updateWeightIn(it) },
                    label = "Berat Masuk (gr)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Roasted Type & Charge Temp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallDropdownField(
                    value = uiState.roastType,
                    label = "Roasted Type",
                    expanded = uiState.isRoastTypeExpanded,
                    onExpandedChange = { viewModel.toggleRoastTypeExpanded(it) },
                    modifier = Modifier.weight(1f)
                ) {
                    roastTypes.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = { viewModel.updateRoastType(selectionOption) }
                        )
                    }
                }

                SmallOutlinedTextField(
                    value = uiState.chargeTimeTemp,
                    onValueChange = { viewModel.updateChargeTimeTemp(it) },
                    label = "Charge Temp (°C)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Burner Plan Section
            PlanInputSection(
                icon = Icons.Default.Whatshot,
                iconTint = Color(0xFFE64A19), // Orange/Red for Heat
                title = "Burner Power",
                labelValue = "Power",
                fixedSeconds = 30,
                plan = uiState.burnerPlan,
                onAdd = viewModel::addBurnerEvent,
                onRemove = viewModel::removeBurnerEvent
            )

            // Air Flow Plan Section
            PlanInputSection(
                icon = Icons.Default.Air,
                iconTint = Color(0xFF0288D1), // Blue for Air
                title = "Air Flow Power",
                labelValue = "Power",
                fixedSeconds = 35,
                plan = uiState.airFlowPlan,
                onAdd = viewModel::addAirFlowEvent,
                onRemove = viewModel::removeAirFlowEvent
            )

            // RPM Drum Plan Section
            PlanInputSection(
                icon = Icons.Default.Sync,
                iconTint = Color(0xFF43A047), // Green for Rotation
                title = "RPM Drum",
                labelValue = "RPM",
                fixedSeconds = 40,
                plan = uiState.rpmPlan,
                onAdd = viewModel::addRpmEvent,
                onRemove = viewModel::removeRpmEvent
            )

            Spacer(modifier = Modifier.height(16.dp))

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanInputSection(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    labelValue: String,
    fixedSeconds: Int,
    plan: List<RoastingEvent>,
    onAdd: (Int, Int) -> Unit,
    onRemove: (RoastingEvent) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }
    var inputMinute by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            iconTint.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    )
                )
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.medium)
            ) {
                val drawSize = size
                when {
                    title.contains("Burner", ignoreCase = true) -> {
                        // Subtle flame glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(iconTint.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(drawSize.width * 0.9f, drawSize.height * 0.2f),
                                radius = drawSize.width * 0.5f
                            ),
                            center = Offset(drawSize.width * 0.9f, drawSize.height * 0.2f),
                            radius = drawSize.width * 0.5f
                        )
                    }
                    title.contains("Air", ignoreCase = true) -> {
                        // Breeze lines
                        for (i in 0..2) {
                            val yOffset = i * 30f
                            drawArc(
                                color = iconTint.copy(alpha = 0.1f),
                                startAngle = 180f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(drawSize.width * 0.7f, -40f + yOffset),
                                size = Size(drawSize.width * 0.4f, 120f),
                                style = Stroke(width = 8f)
                            )
                        }
                    }
                    title.contains("RPM", ignoreCase = true) -> {
                        // Concentric rotation lines
                        drawCircle(
                            color = iconTint.copy(alpha = 0.08f),
                            center = Offset(drawSize.width * 1f, drawSize.height * 0.5f),
                            radius = drawSize.minDimension * 0.6f,
                            style = Stroke(width = 12f)
                        )
                        drawCircle(
                            color = iconTint.copy(alpha = 0.05f),
                            center = Offset(drawSize.width * 1f, drawSize.height * 0.5f),
                            radius = drawSize.minDimension * 0.4f,
                            style = Stroke(width = 8f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallOutlinedTextField(
                        value = inputValue,
                        onValueChange = { newValue -> inputValue = newValue.filter { it.isDigit() || it == '.' } },
                        label = labelValue,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    SmallOutlinedTextField(
                        value = inputMinute,
                        onValueChange = { newValue -> inputMinute = newValue.filter { it.isDigit() } },
                        label = "Minute",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            val v = inputValue.toIntOrNull()
                            val m = inputMinute.toIntOrNull() ?: 0
                            if (v != null) {
                                val totalSeconds = m * 60 + fixedSeconds
                                val isDuplicate = plan.any { it.seconds == totalSeconds }
                                if (!isDuplicate) {
                                    onAdd(v, totalSeconds)
                                    inputValue = ""
                                    inputMinute = ""
                                }
                            }
                        },
                        enabled = inputValue.isNotEmpty() && inputMinute.isNotEmpty(),
                        border = BorderStroke(
                            1.dp,
                            if (inputValue.isNotEmpty() && inputMinute.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Add")
                        }
                    }
                }

                if (plan.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plan.forEach { event ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text("${event.temperature.toInt()} | ${event.time}") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onRemove(event) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
