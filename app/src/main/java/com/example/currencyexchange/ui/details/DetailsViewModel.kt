package com.example.currencyexchange.ui.details

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.currencyexchange.data.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChartPoint(
    val date: String,
    val value: Float
)

data class DetailsUiState(
    val isLoading: Boolean = false,
    val currencyCode: String = "",
    val currencyName: String = "",
    val currentRate: String = "0.0000",
    val changeText: String = "0.00 zł (0.00%)",
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

    /**
     * Pobiera i przelicza dane historyczne.
     * @param targetCode (np. "EUR")
     * @param days dni wstecz (np. 7, 30, 90)
     */
    fun loadDetails(targetCode: String, days: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, currencyCode = targetCode)

            val baseCurrency = sharedPreferences.getString("BASE_CURRENCY", "PLN") ?: "PLN"

            try {
                val targetHistory = repository.dao.getHistoryForCurrency(targetCode, days)
                val baseHistory = repository.dao.getHistoryForCurrency(baseCurrency, days)

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

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currencyName = targetCode, //TODO: mapowanie kodu na nazwę
                        currentRate = String.format("%.4f", latest.value),
                        changeText = String.format("%+.2f zł (%+.2f%%)", diff, percent),
                        isUp = if (diff > 0.0001) true else if (diff < -0.0001) false else null,
                        chartPoints = points,
                        lastUpdate = latest.date
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