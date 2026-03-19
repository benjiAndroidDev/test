package com.Upermarket.upermarket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    favoritesViewModel: FavoritesViewModel, 
    cartViewModel: CartViewModel,
    initialQuery: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val searchManager = remember { MegaProductSearchManager(context) }
    
    var query by remember { mutableStateOf(initialQuery ?: "") }
    var results by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    
    // Filtres
    var selectedFilter by remember { mutableStateOf("Tous") }
    val filters = listOf("Tous", "Nutri-Score A", "Bio", "Sans Additifs")

    val filteredResults = remember(results, selectedFilter) {
        when (selectedFilter) {
            "Nutri-Score A" -> results.filter { it.nutriscore?.lowercase() == "a" }
            "Bio" -> results.filter { it.name?.contains("bio", ignoreCase = true) == true }
            "Sans Additifs" -> results.filter { (it.additivesCount ?: 0) == 0 }
            else -> results
        }
    }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            query = initialQuery
            isLoading = true
            searchManager.searchProductsFlow(query = query).collectLatest {
                results = it
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 1. Barre de recherche
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Rechercher un produit...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; results = emptyList() }) {
                        Icon(Icons.Rounded.Close, null)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (query.isNotBlank()) {
                    isLoading = true
                    scope.launch {
                        searchManager.searchProductsFlow(query = query).collectLatest {
                            results = it
                            isLoading = false
                        }
                    }
                }
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00C853),
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )

        // 2. Barre de filtres (Chips)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00C853),
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.LightGray.copy(alpha = 0.5f),
                        enabled = true,
                        selected = selectedFilter == filter
                    )
                )
            }
        }

        if (isLoading && results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00C853))
            }
        } else if (filteredResults.isEmpty() && query.isNotEmpty() && !isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun résultat pour \"$selectedFilter\"", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredResults, key = { it.code ?: it.name ?: "" }) { product ->
                    SearchProductRow(
                        product = product,
                        isFavorite = favoritesViewModel.isFavorite(product),
                        onToggleFavorite = { favoritesViewModel.toggleFavorite(product) },
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
fun SearchProductRow(
    product: Product, 
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name ?: "Produit", fontWeight = FontWeight.Bold, maxLines = 1)
                Text(product.brands ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null,
                    tint = if (isFavorite) Color.Red else Color.Gray
                )
            }
        }
    }
}
