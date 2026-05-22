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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

import com.example.currencyexchange.ui.theme.TrendDown
import com.example.currencyexchange.ui.theme.TrendUp
import com.example.currencyexchange.ui.theme.Neutral

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCurrencyClick: (String) -> Unit,
    isOnline: Boolean = true
) {
    val state by viewModel.uiState.collectAsState()
    val decimalPlaces by viewModel.decimalPlaces.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }

    LaunchedEffect(isOnline) {
        viewModel.updateConnectivityStatus(isOnline)
    }

    LaunchedEffect(viewModel.errorEvents) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val currentState = state) {

                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentState.message,
                                color = MaterialTheme.colorScheme.error,
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
                            text = if (isOnline) "Online" else "Offline",
                            color = if (isOnline) TrendUp else TrendDown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentState.lastUpdateText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyCard(
    currency: CurrencyUiModel,
    baseCurrency: String,
    decimalPlaces: Int,
    onClick: () -> Unit
) {

    val dynamicFormat = "%.${decimalPlaces}f %s"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                Text(
                    text = "Aktualny kurs",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                Text(
                    text = "Zmiana",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val sign = if (currency.changeValue > 0) "+" else ""
                Text(
                    text = "${sign}${
                        String.format(
                            Locale.getDefault(),
                            "%.${decimalPlaces}f",
                            currency.changeValue
                        )
                    } $baseCurrency (${sign}${
                        String.format(
                            Locale.getDefault(),
                            "%.2f",
                            currency.changePercent
                        )
                    }%)", fontSize = 14.sp,
                    color = if (currency.isUp == true) TrendUp else if (currency.isUp == false) TrendDown else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trend",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val (icon, color) = when (currency.isUp) {
                    true -> Icons.AutoMirrored.Filled.TrendingUp to TrendUp
                    false -> Icons.AutoMirrored.Filled.TrendingDown to TrendDown
                    null -> Icons.AutoMirrored.Filled.TrendingFlat to Neutral
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
