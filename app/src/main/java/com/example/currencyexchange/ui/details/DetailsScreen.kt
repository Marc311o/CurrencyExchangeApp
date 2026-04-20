package com.example.currencyexchange.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    currencyCode: String,
    viewModel: DetailsViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedRange by remember { mutableIntStateOf(30) }

    LaunchedEffect(currencyCode, selectedRange) {
        viewModel.loadDetails(currencyCode, selectedRange)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Szczegóły waluty") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Gray)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "${state.currencyCode} - ${state.currencyName}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow("Aktualny kurs", "${state.currentRate} zł", isBold = true)
                    InfoRow("Zmiana (poprzedni dzień)", state.changeText)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Trend", fontSize = 14.sp)
                        TrendIcon(state.isUp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ostatnia aktualizacja: ${state.lastUpdate}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RangeChip("7 dni", selectedRange == 7) { selectedRange = 7 }
                RangeChip("30 dni", selectedRange == 30) { selectedRange = 30 }
                RangeChip("90 dni", selectedRange == 90) { selectedRange = 90 }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (state.chartPoints.size < 2) {
                    Text(
                        "Zbyt mało danych do narysowania wykresu.\nHistoria buduje się codziennie.",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                } else {
                    VicoChart(state.chartPoints)
                }
            }

            Text(
                text = "Dane pobrane z: exchangerate-api.com",
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun TrendIcon(isUp: Boolean?) {
    val (icon, color) = when (isUp) {
        true -> Icons.Default.TrendingUp to Color(0xFF4CAF50)
        false -> Icons.Default.TrendingDown to Color(0xFFE53935)
        null -> Icons.Default.TrendingFlat to Color.Gray
    }
    Icon(imageVector = icon, contentDescription = "Trend", tint = color)
}

@Composable
fun InfoRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun VicoChart(points: List<ChartPoint>) {
    val model = entryModelOf(*points.mapIndexed { index, point -> index to point.value }.toTypedArray())

    val horizontalAxisValueFormatter = AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { value, _ ->
        val index = value.toInt()
        if (index in points.indices) points[index].date.substring(5) else ""
    }

    Chart(
        chart = lineChart(),
        model = model,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(valueFormatter = horizontalAxisValueFormatter),
        modifier = Modifier.fillMaxSize()
    )
}

