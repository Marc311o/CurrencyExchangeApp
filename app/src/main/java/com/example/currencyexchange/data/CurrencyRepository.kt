package com.example.currencyexchange.data

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.util.Log
import com.example.currencyexchange.data.local.CurrencyDao
import com.example.currencyexchange.data.local.CurrencyEntity
import com.example.currencyexchange.data.remote.ExchangeRateApi


class CurrencyRepository(
    private val api: ExchangeRateApi,
    private val dao: CurrencyDao,
    private val sharedPreferences: SharedPreferences) {

    private fun getToday(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    suspend fun refreshRatesFromApi(): Boolean {
        return try {
            val response = api.getLatestRates(baseCurrency = "USD")

            if (response.isSuccessful) {
                val body = response.body() ?: return false

                val today = getToday()
                val entities = body.conversion_rates.map { (code, rate) ->
                    CurrencyEntity(code, today, rate, body.time_last_update_unix)
                }

                dao.insertRates(entities)
                cleanUpOldData()

                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("REPO", "Błąd sieci podczas pobierania danych z API", e)
            false
        }
    }

    suspend fun getRawRatesFromDatabase(): List<CurrencyEntity> {
        return dao.getRatesForDate(getToday())
    }

    suspend fun cleanUpOldData() {
        val daysToKeep = sharedPreferences.getInt("RETENTION_DAYS", 30)

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val limitDate = formatter.format(calendar.time)

        dao.deleteOldRates(limitDate)
        Log.d("REPO", "Usunięto dane starsze niż: $limitDate (Zatrzymano $daysToKeep dni)")
    }
}