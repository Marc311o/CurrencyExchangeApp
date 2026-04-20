package com.example.currencyexchange.ui.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyexchange.data.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


data class CurrencyUiModel(
    val code: String,
    val name: String,
    val rate: Double,
    val changeValue: Double,
    val changePercent: Double,
    val isUp: Boolean?
)

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val isOnline: Boolean,
        val lastUpdateText: String,
        val baseCurrency: String,
        val rates: List<CurrencyUiModel>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: CurrencyRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRates()
    }

    private fun getDateString(daysAgo: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    fun loadRates(showLoadingScreen: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            if (showLoadingScreen) {
                _uiState.value = HomeUiState.Loading
            }

            try {
                val userBaseCurrency = sharedPreferences.getString("BASE_CURRENCY", "PLN") ?: "PLN"
                val today = getDateString(0)
                val yesterday = getDateString(1)

                repository.refreshRatesFromApi()

                val todayRates = repository.getRatesForSpecificDate(today)
                val yesterdayRates = repository.getRatesForSpecificDate(yesterday)

                if (todayRates.isNotEmpty()) {
                    val baseUsdRateToday = todayRates.find { it.currencyCode == userBaseCurrency }?.rateAgainstUSD ?: 1.0
                    val baseUsdRateYesterday = yesterdayRates.find { it.currencyCode == userBaseCurrency }?.rateAgainstUSD ?: baseUsdRateToday

                    val uiModels = todayRates.mapNotNull { todayEntity ->
                        if (todayEntity.currencyCode == userBaseCurrency) return@mapNotNull null

                        val currentRate = baseUsdRateToday / todayEntity.rateAgainstUSD

                        val yesterdayEntity = yesterdayRates.find { it.currencyCode == todayEntity.currencyCode }

                        var changeValue = 0.0
                        var changePercent = 0.0
                        var isUp: Boolean? = null

                        if (yesterdayEntity != null) {
                            val yesterdayRate = baseUsdRateYesterday / yesterdayEntity.rateAgainstUSD
                            changeValue = currentRate - yesterdayRate
                            changePercent = if (yesterdayRate != 0.0) (changeValue / yesterdayRate) * 100 else 0.0

                            isUp = when {
                                changeValue > 0.0001 -> true
                                changeValue < -0.0001 -> false
                                else -> null
                            }
                        }

                        CurrencyUiModel(
                            code = todayEntity.currencyCode,
                            name = todayEntity.currencyCode,
                            rate = currentRate,
                            changeValue = changeValue,
                            changePercent = changePercent,
                            isUp = isUp
                        )
                    }.sortedBy { it.code }

                    val favorites = sharedPreferences.getStringSet("FAVORITES", setOf("EUR", "USD", "GBP", "CHF")) ?: setOf("EUR", "USD", "GBP", "CHF")
                    val filteredUiModels = uiModels.filter { favorites.contains(it.code) }

                    _uiState.value = HomeUiState.Success(
                        isOnline = true,
                        lastUpdateText = "Dane z: $today",
                        baseCurrency = userBaseCurrency,
                        rates = filteredUiModels
                    )
                } else {
                    _uiState.value = HomeUiState.Error("Brak danych offline i brak dostępu do sieci.")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Wystąpił błąd: ${e.message}")
            }
        }
    }
}