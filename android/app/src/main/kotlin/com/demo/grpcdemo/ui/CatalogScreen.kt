// android/app/src/main/kotlin/com/demo/grpcdemo/ui/CatalogScreen.kt
package com.demo.grpcdemo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.demo.grpcdemo.domain.model.Product

// ─────────────────────────────────────────────────────────────
// CatalogScreen — receives an already-created ViewModel so the
// Activity (not Compose) owns the factory / lifecycle binding.
// ─────────────────────────────────────────────────────────────
@Composable
fun CatalogScreen(viewModel: CatalogViewModel) {
    val uiState     by viewModel.uiState.collectAsState()
    val priceTicker by viewModel.priceTicker.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadProducts() }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        Text(
            text       = "gRPC Product Catalog",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.padding(bottom = 8.dp)
        )

        // Live price ticker card (demo streaming slide)
        if (priceTicker.isLive) {
            PriceTickerCard(ticker = priceTicker)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Content
        when (val state = uiState) {
            is CatalogUiState.Idle    -> {}
            is CatalogUiState.Loading -> LoadingView()
            is CatalogUiState.Error   -> ErrorView(message = state.message) {
                viewModel.loadProducts()
            }
            is CatalogUiState.Products -> ProductList(
                products          = state.list,
                onWatchPrice      = { viewModel.startWatchingPrice(it) },
                onStopWatch       = { viewModel.stopWatchingPrice() },
                watchingProductId = if (priceTicker.isLive) priceTicker.productId else null,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────
@Composable
fun ProductList(
    products:          List<Product>,
    onWatchPrice:      (String) -> Unit,
    onStopWatch:       () -> Unit,
    watchingProductId: String?,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product      = product,
                onWatchPrice = { onWatchPrice(product.id) },
                onStopWatch  = onStopWatch,
                isWatching   = product.id == watchingProductId,
            )
        }
    }
}

@Composable
fun ProductCard(
    product:      Product,
    onWatchPrice: () -> Unit,
    onStopWatch:  () -> Unit,
    isWatching:   Boolean,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = product.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "₹ ${product.pricePaise / 100}", fontSize = 14.sp, color = Color.Gray)
            Text(
                text  = if (product.inStock) "In Stock" else "Out of Stock",
                color = if (product.inStock) Color(0xFF2EC4B6) else Color.Red,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Stream price button — triggers watchPrice RPC
            Button(
                onClick = { if (isWatching) onStopWatch() else onWatchPrice() },
                colors  = ButtonDefaults.buttonColors(
                    containerColor = if (isWatching) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isWatching) "Stop Live Price" else "Watch Live Price")
            }
        }
    }
}

@Composable
fun PriceTickerCard(ticker: PriceTickerState) {
    // Animate background colour when price changes
    val bgColor by animateColorAsState(
        targetValue = Color(0xFF00B4D8).copy(alpha = 0.12f),
        animationSpec = tween(300),
        label = "ticker_bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4F8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "LIVE PRICE", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = ticker.productId, fontSize = 13.sp)
            }
            Text(
                text       = "${ticker.currency} ${ticker.pricePaise / 100}.${ticker.pricePaise % 100}",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF0D1B2A),
            )
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally,
    ) {
        Text(text = message, color = Color.Red, modifier = Modifier.padding(bottom = 12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
