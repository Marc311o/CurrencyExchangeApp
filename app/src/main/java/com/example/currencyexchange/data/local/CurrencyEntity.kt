package com.example.currencyexchange.data.local

import androidx.room.Entity

@Entity(
    tableName = "currency_rates",
    primaryKeys = ["currencyCode", "dateString"]
)
data class CurrencyEntity(
    val currencyCode: String,
    val dateString: String,
    val rateAgainstUSD: Double,
    val lastUpdateTime: Long
)

