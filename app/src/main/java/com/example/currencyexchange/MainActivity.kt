package com.example.currencyexchange

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.currencyexchange.data.CurrencyRepository
import com.example.currencyexchange.data.local.AppDatabase
import com.example.currencyexchange.data.remote.RetrofitClient
import com.example.currencyexchange.ui.AppViewModelFactory
import com.example.currencyexchange.ui.theme.CurrencyExchangeTheme
import com.example.currencyexchange.worker.SyncRatesWorker
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.currencyexchange.data.local.DatabaseSeeder
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "secret_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        encryptedPrefs.edit { putString("API_KEY", BuildConfig.API_KEY) }

        val database = AppDatabase.getDatabase(applicationContext)

        val repository = CurrencyRepository(
            api = RetrofitClient.api,
            dao = database.currencyDao(),
            sharedPreferences = encryptedPrefs
        )

        val factory = AppViewModelFactory(repository, encryptedPrefs)

        lifecycleScope.launch {
            DatabaseSeeder.seedDatabase(database.currencyDao())
        }

        setContent {
            CurrencyExchangeTheme {
                MainScreen(factory = factory)
            }
        }

        val syncRequest = PeriodicWorkRequestBuilder<SyncRatesWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyRateSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}