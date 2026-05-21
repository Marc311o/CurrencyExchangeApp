package com.example.currencyexchange.ui.settings

import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.platform.LocalConfiguration
import com.example.currencyexchange.data.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.Context
import com.example.currencyexchange.worker.WorkManagerScheduler

class SettingsViewModel(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val repository: CurrencyRepository
) : ViewModel() {

    private val _baseCurrency = MutableStateFlow(sharedPreferences.getString("BASE_CURRENCY", "PLN") ?: "PLN")
    val baseCurrency = _baseCurrency.asStateFlow()

    private val _retentionDays = MutableStateFlow(sharedPreferences.getInt("RETENTION_DAYS", 30))
    val retentionDays = _retentionDays.asStateFlow()

    private val _refreshInterval = MutableStateFlow(sharedPreferences.getInt("REFRESH_INTERVAL", 12))
    val refreshInterval = _refreshInterval.asStateFlow()

    private val _decimalPlaces = MutableStateFlow(sharedPreferences.getInt("DECIMAL_PLACES", 4))
    val decimalPlaces = _decimalPlaces.asStateFlow()

    fun updateBaseCurrency(newCurrency: String) {
        sharedPreferences.edit().putString("BASE_CURRENCY", newCurrency).apply()
        _baseCurrency.value = newCurrency
    }

    fun updateRetentionDays(days: Int) {
        sharedPreferences.edit().putInt("RETENTION_DAYS", days).apply()
        _retentionDays.value = days
    }

    fun updateRefreshInterval(hours: Int) {
        sharedPreferences.edit().putInt("REFRESH_INTERVAL", hours).apply()
        _refreshInterval.value = hours
        WorkManagerScheduler.schedule(context, hours)
    }

    fun updateDecimalPlaces(places: Int) {
        sharedPreferences.edit().putInt("DECIMAL_PLACES", places).apply()
        _decimalPlaces.value = places
    }

    fun forceRefresh() {
        viewModelScope.launch {
            repository.refreshRatesFromApi()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, isOnline: Boolean = true) {
    val currentBase by viewModel.baseCurrency.collectAsState()
    val currentRetention by viewModel.retentionDays.collectAsState()
    val currentInterval by viewModel.refreshInterval.collectAsState()
    val currentDecimals by viewModel.decimalPlaces.collectAsState()

    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val availableCurrencies = listOf("PLN", "USD", "EUR", "GBP", "CHF", "JPY")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsGroup(title = "Waluta bazowa") {
                            BaseCurrencySelector(currentBase, availableCurrencies) { viewModel.updateBaseCurrency(it) }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsGroup(title = "Historia wykresów") {
                            RetentionSlider(currentRetention) { viewModel.updateRetentionDays(it) }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsGroup(title = "Miejsca po przecinku") {
                            DecimalPlacesSelector(currentDecimals) { viewModel.updateDecimalPlaces(it) }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsGroup(title = "Częstotliwość odświeżania") {
                            RefreshIntervalSelector(currentInterval) { viewModel.updateRefreshInterval(it) }
                        }
                    }
                }
            } else {
                SettingsGroup(title = "Waluta bazowa") {
                    BaseCurrencySelector(currentBase, availableCurrencies) { viewModel.updateBaseCurrency(it) }
                }

                SettingsGroup(title = "Częstotliwość odświeżania") {
                    RefreshIntervalSelector(currentInterval) { viewModel.updateRefreshInterval(it) }
                }

                SettingsGroup(title = "Miejsca po przecinku") {
                    DecimalPlacesSelector(currentDecimals) { viewModel.updateDecimalPlaces(it) }
                }

                SettingsGroup(title = "Historia wykresów") {
                    RetentionSlider(currentRetention) { viewModel.updateRetentionDays(it) }
                }
            }

            Button(
                onClick = { viewModel.forceRefresh() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Wymuś pobranie kursów", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Text(
                text = "Currency Exchange v1.0",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun BaseCurrencySelector(currentBase: String, availableCurrencies: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Text("Aktualna: $currentBase", color = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            availableCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshIntervalSelector(currentInterval: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(1, 12, 24).forEach { hours ->
            FilterChip(
                selected = currentInterval == hours,
                onClick = { onSelect(hours) },
                label = { Text("${hours}h") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecimalPlacesSelector(currentDecimals: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(2, 4).forEach { places ->
            FilterChip(
                selected = currentDecimals == places,
                onClick = { onSelect(places) },
                label = { Text(if (places == 2) "0.00" else "0.0000") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun RetentionSlider(currentRetention: Int, onValueChange: (Int) -> Unit) {
    Column {
        Text(
            "Dane z ostatnich: $currentRetention dni",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = currentRetention.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 7f..90f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}