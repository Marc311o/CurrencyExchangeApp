package com.example.currencyexchange.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object DatabaseSeeder {


    suspend fun seedDatabase(dao: CurrencyDao) {
        withContext(Dispatchers.IO) {
            if (dao.getHistoryForCurrency("EUR", 30).size < 10) {
                val calendar = Calendar.getInstance()
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fakeEntities = mutableListOf<CurrencyEntity>()

                val baseRates = mapOf("PLN" to 4.0, "EUR" to 0.92, "GBP" to 0.78, "CHF" to 0.90, "USD" to 1.0)

                for (i in 1..30) {
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, -i)
                    val dateStr = formatter.format(calendar.time)

                    baseRates.forEach { (code, baseRate) ->
                        val noise = 1.0 + Random.nextDouble(-0.02, 0.02)
                        val fakeRate = baseRate * noise

                        fakeEntities.add(
                            CurrencyEntity(
                                currencyCode = code,
                                dateString = dateStr,
                                rateAgainstUSD = fakeRate,
                                lastUpdateTime = System.currentTimeMillis()
                            )
                        )
                    }
                }
                dao.insertRates(fakeEntities)
            }
        }
    }
}