package com.example.currencyexchange.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyexchange.data.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit


class SettingsViewModel(
    private val sharedPreferences: SharedPreferences,
    private val repository: CurrencyRepository
) : ViewModel() {
    private val _baseCurrency =
        MutableStateFlow(sharedPreferences.getString("BASE_CURRENCY", "PLN") ?: "PLN")
    val baseCurrency = _baseCurrency.asStateFlow()

    private val _retentionDays = MutableStateFlow(sharedPreferences.getInt("RETENTION_DAYS", 30))
    val retentionDays = _retentionDays.asStateFlow()

    fun updateBaseCurrency(newCurrency: String) {
        sharedPreferences.edit { putString("BASE_CURRENCY", newCurrency) }
        _baseCurrency.value = newCurrency
    }

    fun updateRetentionDays(days: Int) {
        sharedPreferences.edit { putInt("RETENTION_DAYS", days) }
        _retentionDays.value = days
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

    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    val availableCurrencies = listOf("PLN", "USD", "EUR", "GBP", "CHF")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ustawienia") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column {
                Text("Waluta bazowa", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Waluta, do której przeliczane są wszystkie kursy",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = currencyDropdownExpanded,
                    onExpandedChange = { currencyDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentBase,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyDropdownExpanded,
                        onDismissRequest = { currencyDropdownExpanded = false }
                    ) {
                        availableCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    viewModel.updateBaseCurrency(currency)
                                    currencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Column {
                Text("Historia wykresów", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Trzymaj w pamięci dane z ostatnich: $currentRetention dni",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = currentRetention.toFloat(),
                    onValueChange = { viewModel.updateRetentionDays(it.toInt()) },
                    valueRange = 7f..90f,
                    steps = 0
                )
            }

            Button(
                onClick = { viewModel.forceRefresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wymuś pobranie najnowszych kursów")
            }
        }
    }
}