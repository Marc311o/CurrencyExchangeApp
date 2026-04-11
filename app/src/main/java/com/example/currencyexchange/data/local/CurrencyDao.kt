package com.example.currencyexchange.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<CurrencyEntity>)

    @Query("SELECT * FROM currency_rates WHERE dateString = :date")
    suspend fun getRatesForDate(date: String): List<CurrencyEntity>

    @Query("SELECT * FROM currency_rates WHERE currencyCode = :code ORDER BY dateString DESC LIMIT :days")
    suspend fun getHistoryForCurrency(code: String, days: Int): List<CurrencyEntity>

    @Query("DELETE FROM currency_rates WHERE dateString < :limitDate")
    suspend fun deleteOldRates(limitDate: String)
}