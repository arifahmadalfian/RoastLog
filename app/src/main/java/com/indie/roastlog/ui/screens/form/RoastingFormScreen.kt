package com.indie.roastlog.ui.screens.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.ui.components.SmallOutlinedTextField
import com.indie.roastlog.ui.components.IncrementDecrementField
import com.indie.roastlog.ui.components.SmallDropdownField

@OptIn(ExperimentalMaterial3Api::class)
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

            HorizontalDivider()

            Text(
                text = "Parameter Awal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            IncrementDecrementField(
                value = uiState.burnerPower, 
                onValueChange = viewModel::updateBurnerPower, 
                label = "Burner Power", 
                modifier = Modifier.fillMaxWidth()
            )

            IncrementDecrementField(
                value = uiState.airFlowPower, 
                onValueChange = viewModel::updateAirFlowPower, 
                label = "Air Flow Power", 
                modifier = Modifier.fillMaxWidth()
            )
            
            IncrementDecrementField(
                value = uiState.rpmDrum, 
                onValueChange = viewModel::updateRpmDrum, 
                label = "RPM Drum", 
                modifier = Modifier.fillMaxWidth()
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
