package com.Upermarket.upermarket

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ScanHistorySheet(
    scanHistoryManager: ScanHistoryManager,
    favoritesViewModel: FavoritesViewModel,
    cartViewModel: CartViewModel
) {
    // Utilisation de remember pour éviter de re-fetch l'historique à chaque micro-changement
    val historyItems by remember { mutableStateOf(scanHistoryManager.getHistory()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header fixe pour éviter le lag au scroll
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = Color(0xFF00C853))
                    Spacer(Modifier.width(12.dp))
                    Text("Historique", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                if (historyItems.isNotEmpty()) {
                    TextButton(onClick = { 
                        scanHistoryManager.clearHistory()
                        // On ferme la sheet pour forcer le rafraîchissement
                    }) {
                        Text("Vider", color = Color.Red)
                    }
                }
            }
        }

        if (historyItems.isEmpty()) {
            EmptyHistoryView()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyItems, key = { it.code ?: it.name ?: "" }) { product ->
                    HistoryRow(
                        product = product,
                        onClick = { selectedProduct = product }
                    )
                }
            }
        }
    }

    if (selectedProduct != null) {
        ProductDetailSheet(
            product = selectedProduct!!,
            isFavorite = favoritesViewModel.isFavorite(selectedProduct!!),
            onToggleFavorite = { favoritesViewModel.toggleFavorite(selectedProduct!!) },
            onAddToCart = { price -> 
                cartViewModel.addToCart(selectedProduct!!, price)
                selectedProduct = null
            },
            onDismiss = { selectedProduct = null }
        )
    }
}

@Composable
fun HistoryRow(product: Product, onClick: () -> Unit) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optimisation Coil : AsyncImage est plus fluide que rememberAsyncImagePainter
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(product.imageUrl)
                    .crossfade(true)
                    .size(150) // On limite la taille de l'image en mémoire
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(55.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF5F5F5)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name ?: "Produit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(product.brands ?: "Inconnu", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun EmptyHistoryView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.History, null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
            Spacer(Modifier.height(12.dp))
            Text("Votre historique est vide", color = Color.Gray)
        }
    }
}
