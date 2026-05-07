package com.example.currencyexchange.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

val CustomGreen = Color(0xFF2B672C)
val CustomRed = Color(0xFFE53935)
val CustomGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCurrencyClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val decimalPlaces by viewModel.decimalPlaces.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }

    val format = "%.${decimalPlaces}f"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val currentState = state) {

            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CustomGreen)
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentState.message,
                            color = CustomRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.loadRates(true) }) {
                            Text("Spróbuj ponownie")
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                var isRefreshing by remember { mutableStateOf(false) }

                LaunchedEffect(currentState) {
                    isRefreshing = false
                }

                val pullRefreshState = rememberPullRefreshState(
                    refreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.loadRates(showLoadingScreen = false)
                    }
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (currentState.isOnline) "Online" else "Offline",
                        color = if (currentState.isOnline) CustomGreen else CustomRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentState.lastUpdateText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullRefreshState)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(currentState.rates) { currency ->
                                CurrencyCard(
                                    currency = currency,
                                    baseCurrency = currentState.baseCurrency,
                                    decimalPlaces = decimalPlaces,
                                    onClick = { onCurrencyClick(currency.code) }
                                )
                            }
                        }

                        PullRefreshIndicator(
                            refreshing = isRefreshing,
                            state = pullRefreshState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            contentColor = CustomGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyCard(currency: CurrencyUiModel, baseCurrency: String, decimalPlaces: Int, onClick: () -> Unit) {

    val dynamicFormat = "%.${decimalPlaces}f %s"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${currency.code} - ${currency.name}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Aktualny kurs", fontSize = 14.sp)
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        dynamicFormat,
                        currency.rate,
                        baseCurrency
                    ), fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Zmiana", fontSize = 14.sp)
                val sign = if (currency.changeValue > 0) "+" else ""
                Text(
                    text = "${sign}${
                        String.format(
                            Locale.getDefault(),
                            "%.2f",
                            currency.changeValue
                        )
                    } $baseCurrency (${sign}${
                        String.format(
                            Locale.getDefault(),
                            "%.2f",
                            currency.changePercent
                        )
                    }%)", fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Trend", fontSize = 14.sp)
                val (icon, color) = when (currency.isUp) {
                    true -> Icons.AutoMirrored.Filled.TrendingUp to CustomGreen
                    false -> Icons.AutoMirrored.Filled.TrendingDown to CustomRed
                    null -> Icons.AutoMirrored.Filled.TrendingFlat to Color.Gray
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Trend",
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}