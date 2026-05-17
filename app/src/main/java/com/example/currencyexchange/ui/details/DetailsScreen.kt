package com.example.currencyexchange.ui.details

import androidx.compose.foundation.BorderStroke
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
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

import com.example.currencyexchange.ui.theme.TrendDown
import com.example.currencyexchange.ui.theme.TrendUp
import com.example.currencyexchange.ui.theme.Neutral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    currencyCode: String,
    viewModel: DetailsViewModel,
    onBackClick: () -> Unit,
    showBackButton: Boolean = true,
    forceVerticalLayout: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    var selectedRange by remember { mutableIntStateOf(30) }
    val configuration = LocalConfiguration.current
    
    val useTwoColumnLayout = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !forceVerticalLayout

    LaunchedEffect(currencyCode, selectedRange) {
        viewModel.loadDetails(currencyCode, selectedRange)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (useTwoColumnLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    DetailsInfoCard(state, showBackButton, onBackClick)
                    Spacer(modifier = Modifier.height(12.dp))
                    RangeSelectionRow(selectedRange) { selectedRange = it }
                    Spacer(modifier = Modifier.height(12.dp))
                    DataSourceFooter()
                }

                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChartSection(state)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                DetailsInfoCard(state, showBackButton, onBackClick)
                RangeSelectionRow(selectedRange) { selectedRange = it }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(16.dp)
                ) {
                    ChartSection(state)
                }
                DataSourceFooter()
            }
        }
    }
}

@Composable
fun DetailsInfoCard(state: DetailsUiState, showBackButton: Boolean, onBackClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${state.currencyCode} - ${state.currencyName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Wstecz",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(
                "Aktualny kurs",
                "${state.currentRate} ${state.baseCurrency}",
                isBold = true
            )
            InfoRow("Zmiana", state.changeText)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Trend", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                TrendIcon(state.isUp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ostatnia aktualizacja: ${state.lastUpdate}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RangeSelectionRow(selectedRange: Int, onRangeSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RangeChip("7 dni", selectedRange == 7) { onRangeSelected(7) }
        RangeChip("30 dni", selectedRange == 30) { onRangeSelected(30) }
        RangeChip("90 dni", selectedRange == 90) { onRangeSelected(90) }
    }
}

@Composable
fun ChartSection(state: DetailsUiState) {
    if (state.isLoading) {
        CircularProgressIndicator()
    } else if (state.chartPoints.size < 2) {
        Text(
            "Zbyt mało danych do narysowania wykresu.\nHistoria buduje się codziennie.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        if (state.chartPoints.isNotEmpty()) {
            VicoChart(points = state.chartPoints, isUp = state.isUp)
        }
    }
}

@Composable
fun DataSourceFooter() {
    Text(
        text = "Dane pobrane z: exchangerate-api.com",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        textAlign = TextAlign.Center,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun TrendIcon(isUp: Boolean?) {
    val (icon, color) = when (isUp) {
        true -> Icons.Default.TrendingUp to TrendUp
        false -> Icons.Default.TrendingDown to TrendDown
        null -> Icons.Default.TrendingFlat to Neutral
    }
    Icon(imageVector = icon, contentDescription = "Trend", tint = color)
}

@Composable
fun InfoRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
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
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun VicoChart(points: List<ChartPoint>, isUp: Boolean?) {
    if (points.isEmpty()) return

    val model =
        entryModelOf(*points.mapIndexed { index, point -> index to point.value }.toTypedArray())

    val minY = points.minOf { it.value }
    val maxY = points.maxOf { it.value }
    val range = if (maxY == minY) 0.01f else (maxY - minY)
    val padding = range * 0.15f

    val horizontalAxisValueFormatter =
        AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index in points.indices) points[index].date.substring(5) else ""
        }

    val verticalAxisValueFormatter =
        AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Vertical.Start> { value, _ ->
            String.format(java.util.Locale.getDefault(), "%.4f", value)
        }

    val lineColor = when (isUp) {
        true -> TrendUp
        false -> TrendDown
        null -> Neutral
    }

    val axisLabel = textComponent(
        color = MaterialTheme.colorScheme.onSurface,
        textSize = 10.sp
    )

    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(
                    lineColor = lineColor,
                    lineBackgroundShader = verticalGradient(
                        colors = arrayOf(
                            lineColor.copy(alpha = 0.4f),
                            lineColor.copy(alpha = 0.0f)
                        )
                    )
                )
            ),

            axisValuesOverrider = AxisValuesOverrider.fixed(
                minY = minY - padding,
                maxY = maxY + padding
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            label = axisLabel,
            valueFormatter = verticalAxisValueFormatter,
            itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 5),
        ),
        bottomAxis = rememberBottomAxis(
            label = axisLabel,
            valueFormatter = horizontalAxisValueFormatter,
            labelRotationDegrees = -45f,
            itemPlacer = AxisItemPlacer.Horizontal.default(
                spacing = if (points.size > 40) 10 else if (points.size > 10) 5 else 1,
                offset = 0,
                shiftExtremeTicks = true
            )
        ),
        chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = false),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    )
}

