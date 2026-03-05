package com.indie.roastlog.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indie.roastlog.ui.components.RoastingChart
import com.indie.roastlog.ui.components.ScaffoldCustom
import com.indie.roastlog.ui.components.ChartDataPoint
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoastingDetail(
    roastId: String?,
    viewModel: RoastingDetailViewModel = viewModel()
) {
    val session by viewModel.session.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

    LaunchedEffect(roastId) {
        roastId?.let { viewModel.loadSession(it) }
    }

    ScaffoldCustom(
        title = "Detail Roasting"
    ) {
        session?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = s.beanType, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = dateFormat.format(Date(s.date)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoColumn(label = "Roast Type", value = s.roastType, modifier = Modifier.weight(1f))
                            InfoColumn(label = "Berat In", value = "${s.weightIn}g", modifier = Modifier.weight(1f))
                            InfoColumn(label = "Berat Out", value = "${s.weightOut}g", modifier = Modifier.weight(1f))
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Hasil Roasting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoColumn(label = "End Temp", value = "${s.endTimeTemp}°C", modifier = Modifier.weight(1f))
                            InfoColumn(label = "Roast Time", value = s.roastTime, modifier = Modifier.weight(1f))
                            InfoColumn(label = "Dev Time", value = s.devTime, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Chart
                val chartData = remember(s) {
                    val points = s.temperatureData.map { 
                        ChartDataPoint(it.intervalNumber.toFloat(), it.intervalNumber * 60, it.temperature, null)
                    }.toMutableList()
                    
                    listOfNotNull(s.turnPoint, s.yellowing, s.firstCrack, s.endRoast).forEach { ev ->
                        points.add(ChartDataPoint(ev.seconds.toFloat() / 60f, ev.seconds, ev.temperature, null))
                    }
                    points.sortedBy { it.totalSeconds }
                }

                if (chartData.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Grafik Suhu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            RoastingChart(
                                data = chartData,
                                intervalSeconds = 60, // Default to 60
                                modifier = Modifier.fillMaxWidth().height(250.dp)
                            )
                        }
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
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EventRow(label: String, event: com.indie.roastlog.ui.model.RoastingEvent?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = event?.let { "${it.temperature}°C / ${it.time}" } ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
