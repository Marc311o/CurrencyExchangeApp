package com.example.currencyexchange.ui.details

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyexchange.data.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChartPoint(
    val date: String,
    val value: Float
)

data class DetailsUiState(
    val isLoading: Boolean = false,
    val currencyCode: String = "",
    val currencyName: String = "",
    val baseCurrency: String = "PLN",
    val currentRate: String = "0.0000",
    val changeText: String = "0.00 (0.00%)",
    val isUp: Boolean? = null,
    val chartPoints: List<ChartPoint> = emptyList(),
    val lastUpdate: String = "",
    val error: String? = null
)

class DetailsViewModel(
    private val repository: CurrencyRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    fun loadDetails(targetCode: String, days: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, currencyCode = targetCode)

            val userBaseCurrency = sharedPreferences.getString("BASE_CURRENCY", "PLN") ?: "PLN"
            try {
                val targetHistory = repository.dao.getHistoryForCurrency(targetCode, days)
                val baseHistory = repository.dao.getHistoryForCurrency(userBaseCurrency, days)

                val points = targetHistory.mapNotNull { tEntity ->
                    val bEntity = baseHistory.find { it.dateString == tEntity.dateString }
                    if (bEntity != null) {
                        ChartPoint(
                            date = tEntity.dateString,
                            value = (bEntity.rateAgainstUSD / tEntity.rateAgainstUSD).toFloat()
                        )
                    } else null
                }.sortedBy { it.date }

                if (points.isNotEmpty()) {
                    val latest = points.last()
                    val previous = if (points.size > 1) points[points.size - 2] else latest
                    val diff = latest.value - previous.value
                    val percent = if (previous.value != 0f) (diff / previous.value) * 100 else 0.0

                    val fullName = try {
                        java.util.Currency.getInstance(targetCode)
                            .getDisplayName(Locale.getDefault())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    } catch (e: Exception) {
                        targetCode
                    }

                    val latestEntityTime = baseHistory.firstOrNull()?.lastUpdateTime ?: System.currentTimeMillis()
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = latestEntityTime
                    
                    val formattedTime = String.format(
                        Locale.getDefault(),
                        "%04d-%02d-%02d %02d:%02d",
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH) + 1,
                        cal.get(java.util.Calendar.DAY_OF_MONTH),
                        cal.get(java.util.Calendar.HOUR_OF_DAY),
                        cal.get(java.util.Calendar.MINUTE)
                    )

                    val decimalPlaces = sharedPreferences.getInt("DECIMAL_PLACES", 4)
                    val rateFormat = "%.${decimalPlaces}f"
                    val changeFormat = "%+.${decimalPlaces}f %s (%+.2f%%)"

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currencyName = fullName,
                        baseCurrency = userBaseCurrency,
                        currentRate = String.format(Locale.getDefault(), rateFormat, latest.value),
                        changeText = String.format(Locale.getDefault(), changeFormat, diff, userBaseCurrency, percent),
                        isUp = if (diff > 0.000001) true else if (diff < -0.000001) false else null,
                        chartPoints = points,
                        lastUpdate = formattedTime
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Brak danych historycznych w pamięci.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}