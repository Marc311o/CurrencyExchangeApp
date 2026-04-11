package com.example.currencyexchange.data.remote

data class CurrencyResponse(
    val result: String,
    val base_code: String,
    val time_last_update_unix: Long,
    val conversion_rates: Map<String, Double>
)