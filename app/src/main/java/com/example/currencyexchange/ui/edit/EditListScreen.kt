package com.example.currencyexchange.ui.edit

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditListViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {

    private val _favorites = MutableStateFlow(
        sharedPreferences.getStringSet("FAVORITES", setOf("EUR", "USD", "GBP", "CHF")) ?: setOf("EUR", "USD", "GBP", "CHF")
    )
    val favorites = _favorites.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val allCurrencies = listOf("EUR", "USD", "GBP", "CHF", "AUD", "CAD", "JPY", "CZK", "NOK", "SEK")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(currencyCode: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(currencyCode)) {
            current.remove(currencyCode)
        } else {
            current.add(currencyCode)
        }

        sharedPreferences.edit().putStringSet("FAVORITES", current).apply()
        _favorites.value = current
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListScreen(viewModel: EditListViewModel) {
    val favorites by viewModel.favorites.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredCurrencies = viewModel.allCurrencies.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zarządzaj walutami", color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Szukaj waluty...") },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Card(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(16.dp)
            ) {
                LazyColumn {
                    items(filteredCurrencies) { code ->
                        val isFav = favorites.contains(code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleFavorite(code) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = code, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Ulubione",
                                tint = if (isFav) Color(0xFFFFC107) else Color.Gray
                            )
                        }
                        HorizontalDivider(
                            color = Color(0xFFE0E0E0),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}