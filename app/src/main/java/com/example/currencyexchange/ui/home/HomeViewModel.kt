package com.example.currencyexchange.ui.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyexchange.data.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class CurrencyUiModel(
    val code: String,
    val name: String,
    val rate: Double,
    val changeValue: Double,
    val changePercent: Double,
    val isUp: Boolean?
)

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val isOnline: Boolean,
        val lastUpdateText: String,
        val baseCurrency: String,
        val rates: List<CurrencyUiModel>
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: CurrencyRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _decimalPlaces = MutableStateFlow(sharedPreferences.getInt("DECIMAL_PLACES", 4))
    val decimalPlaces = _decimalPlaces.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var isCurrentlyOnline: Boolean = true

    init {
        loadRates()
    }

    fun updateConnectivityStatus(isOnline: Boolean) {
        isCurrentlyOnline = isOnline
    }

    fun refreshSettings() {
        _decimalPlaces.value = sharedPreferences.getInt("DECIMAL_PLACES", 4)
    }

    fun loadRates(showLoadingScreen: Boolean = true) {
        viewModelScope.launch {
            if (showLoadingScreen) {
                _uiState.value = HomeUiState.Loading
            }

            if (!isCurrentlyOnline && !showLoadingScreen) {
                _errorEvents.emit("Brak połączenia z internetem. Nie można odświeżyć kursów.")
            }

            try {
                val userBaseCurrency = sharedPreferences.getString("BASE_CURRENCY", "PLN") ?: "PLN"

                if (isCurrentlyOnline) {
                    repository.refreshRatesFromApi()
                }

                val baseHistory = repository.dao.getHistoryForCurrency(userBaseCurrency, 2)

                
                if (baseHistory.isNotEmpty()) {
                    val latestDate = baseHistory[0].dateString
                    val previousDate = if (baseHistory.size > 1) baseHistory[1].dateString else latestDate

                    val latestRates = repository.getRatesForSpecificDate(latestDate)
                    val previousRates = repository.getRatesForSpecificDate(previousDate)

                    val baseUsdRateLatest = baseHistory[0].rateAgainstUSD
                    val baseUsdRatePrevious = if (baseHistory.size > 1) baseHistory[1].rateAgainstUSD else baseUsdRateLatest

                    val uiModels = latestRates.mapNotNull { latestEntity ->
                        if (latestEntity.currencyCode == userBaseCurrency) return@mapNotNull null

                        val currentRate = baseUsdRateLatest / latestEntity.rateAgainstUSD

                        val previousEntity = previousRates.find { it.currencyCode == latestEntity.currencyCode }

                        var changeValue = 0.0
                        var changePercent = 0.0
                        var isUp: Boolean? = null

                        if (previousEntity != null) {
                            val previousRate = baseUsdRatePrevious / previousEntity.rateAgainstUSD
                            changeValue = currentRate - previousRate
                            changePercent = if (previousRate != 0.0) (changeValue / previousRate) * 100 else 0.0

                            isUp = when {
                                changeValue > 0.000001 -> true
                                changeValue < -0.000001 -> false
                                else -> null
                            }
                        }

                        val currencyFullName = try {
                            val currency = java.util.Currency.getInstance(latestEntity.currencyCode)
                            val displayName = currency.getDisplayName(Locale.getDefault())
                            displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        } catch (e: Exception) {
                            "Nieznana waluta"
                        }

                        CurrencyUiModel(
                            code = latestEntity.currencyCode,
                            name = currencyFullName,
                            rate = currentRate,
                            changeValue = changeValue,
                            changePercent = changePercent,
                            isUp = isUp
                        )
                    }.sortedBy { it.code }

                    val favorites = sharedPreferences.getStringSet("FAVORITES", setOf("EUR", "USD", "GBP", "CHF")) ?: setOf("EUR", "USD", "GBP", "CHF")
                    val filteredUiModels = uiModels.filter { favorites.contains(it.code) }

                    val timestamp = latestRates.firstOrNull()?.lastUpdateTime ?: System.currentTimeMillis()
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = timestamp
                    
                    val formattedTime = String.format(
                        Locale.getDefault(),
                        "%04d-%02d-%02d %02d:%02d:%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH),
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND)
                    )

                    _uiState.value = HomeUiState.Success(
                        isOnline = true,
                        lastUpdateText = "Dane z: $formattedTime",
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
