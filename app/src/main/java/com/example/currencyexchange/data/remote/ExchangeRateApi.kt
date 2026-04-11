package com.example.currencyexchange.data.remote

import com.example.currencyexchange.BuildConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {

    @GET("v6/{apiKey}/latest/{baseCurrency}")
    suspend fun getLatestRates(
        @Path("apiKey") apiKey: String = BuildConfig.API_KEY,
        @Path("baseCurrency") baseCurrency: String
    ): Response<CurrencyResponse>
}