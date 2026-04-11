package com.example.currencyexchange.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.currencyexchange.data.CurrencyRepository
import com.example.currencyexchange.data.local.AppDatabase
import com.example.currencyexchange.data.remote.RetrofitClient

class SyncRatesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WORKER", "Rozpoczynam pobieranie w tle...")

            val sharedPrefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = CurrencyRepository(RetrofitClient.api, database.currencyDao(), sharedPrefs)

            val refreshSucceeded = repository.refreshRatesFromApi()

            if (refreshSucceeded) {
                Log.d("WORKER", "Sukces! Pobrane dane zapisano do bazy Room i posprzątano stare.")
                Result.success()
            } else {
                Log.w("WORKER", "Synchronizacja nie powiodła się - brak poprawnej odpowiedzi z API.")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e("WORKER", "Błąd podczas pobierania w tle", e)
            Result.retry()
        }
    }
}