package com.example.currencyexchange.ui.settings

import android.content.SharedPreferences
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
import com.example.currencyexchange.data.CurrencyRepository
import com.example.currencyexchange.ui.home.CustomGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
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
fun SettingsScreen(viewModel: SettingsViewModel) {
    val currentBase by viewModel.baseCurrency.collectAsState()
    val currentRetention by viewModel.retentionDays.collectAsState()
    val currentInterval by viewModel.refreshInterval.collectAsState()
    val currentDecimals by viewModel.decimalPlaces.collectAsState()

    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val availableCurrencies = listOf("PLN", "USD", "EUR", "GBP", "CHF", "JPY")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia", fontWeight = FontWeight.Bold, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsGroup(title = "Waluta bazowa") {
                Box {
                    OutlinedButton(
                        onClick = { currencyDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Text("Aktualna: $currentBase", color = Color.Black)
                    }
                    DropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        availableCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency, color = Color.Black) },
                                onClick = {
                                    viewModel.updateBaseCurrency(currency)
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            SettingsGroup(title = "Częstotliwość odświeżania") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 12, 24).forEach { hours ->
                        FilterChip(
                            selected = currentInterval == hours,
                            onClick = { viewModel.updateRefreshInterval(hours) },
                            label = { Text("${hours}h") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CustomGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            SettingsGroup(title = "Miejsca po przecinku") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 4).forEach { places ->
                        FilterChip(
                            selected = currentDecimals == places,
                            onClick = { viewModel.updateDecimalPlaces(places) },
                            label = { Text(if (places == 2) "0.00" else "0.0000") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CustomGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            SettingsGroup(title = "Historia wykresów") {
                Column {
                    Text(
                        "Przechowuj dane z ostatnich: $currentRetention dni",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Slider(
                        value = currentRetention.toFloat(),
                        onValueChange = { viewModel.updateRetentionDays(it.toInt()) },
                        valueRange = 7f..90f,
                        colors = SliderDefaults.colors(
                            thumbColor = CustomGreen,
                            activeTrackColor = CustomGreen,
                            inactiveTrackColor = Color(0xFFE0E0E0)
                        )
                    )
                }
            }

            Button(
                onClick = { viewModel.forceRefresh() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CustomGreen, contentColor = Color.White)
            ) {
                Text("Wymuś pobranie kursów", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Text(
                text = "Currency Exchange v1.0",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}