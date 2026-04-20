package com.example.currencyexchange.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.currencyexchange.data.CurrencyRepository
import com.example.currencyexchange.ui.details.DetailsViewModel
import com.example.currencyexchange.ui.settings.SettingsViewModel
import com.example.currencyexchange.ui.edit.EditListViewModel
import com.example.currencyexchange.ui.home.HomeViewModel

class AppViewModelFactory(
    private val repository: CurrencyRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repository, sharedPreferences) as T

            modelClass.isAssignableFrom(DetailsViewModel::class.java) ->
                DetailsViewModel(repository, sharedPreferences) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(sharedPreferences, repository) as T

            modelClass.isAssignableFrom(EditListViewModel::class.java) ->
                EditListViewModel(sharedPreferences) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}