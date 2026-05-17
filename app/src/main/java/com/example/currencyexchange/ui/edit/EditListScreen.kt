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
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val filteredCurrencies = viewModel.allCurrencies.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Szukaj waluty...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Wybrane waluty: ${favorites.size}", fontWeight = FontWeight.Bold)
                            Text("Będą one widoczne na ekranie głównym.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1.5f).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    CurrencyList(
                        currencies = filteredCurrencies,
                        favorites = favorites,
                        onToggle = { viewModel.toggleFavorite(it) }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    placeholder = { Text("Szukaj waluty...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Card(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    CurrencyList(
                        currencies = filteredCurrencies,
                        favorites = favorites,
                        onToggle = { viewModel.toggleFavorite(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CurrencyList(
    currencies: List<String>,
    favorites: Set<String>,
    onToggle: (String) -> Unit
) {
    LazyColumn {
        items(currencies) { code ->
            val isFav = favorites.contains(code)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(code) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = code, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Ulubione",
                    tint = if (isFav) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}