package com.example.currencyexchange

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.currencyexchange.ui.theme.CurrencyExchangeTheme
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.currencyexchange.data.remote.RetrofitClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            CurrencyExchangeTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
        lifecycleScope.launch {
            try {
                Log.d("API_TEST", "Rozpoczynam pobieranie danych...")

                val response = RetrofitClient.api.getLatestRates(baseCurrency = "PLN")

                if (response.isSuccessful) {
                    val body = response.body()
                    val eurRate = body?.conversion_rates?.get("EUR")
                    val usdRate = body?.conversion_rates?.get("USD")

                    Log.d("API_TEST", "Sukces! Kurs EUR: $eurRate, Kurs USD: $usdRate")
                    Log.d("API_TEST", "Czas aktualizacji (UNIX): ${body?.time_last_update_unix}")
                } else {
                    Log.e("API_TEST", "Błąd serwera. Kod błędu: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API_TEST", "Błąd sieci lub konwersji: ${e.message}")
            }
        }
    }
}