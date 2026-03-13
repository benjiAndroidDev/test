package com.Upermarket.upermarket

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryProductsScreen(
    categoryName: String,
    categoryTag: String,
    favoritesViewModel: FavoritesViewModel,
    cartViewModel: CartViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val searchManager = remember { MegaProductSearchManager(context) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedFilter by remember { mutableStateOf("Tous") }

    // Définition des filtres par catégorie
    val subFilters = remember(categoryName) {
        when (categoryName) {
            "Viandes" -> listOf("Tous", "Poulet", "Boeuf", "Dinde", "Steak", "Saucisse", "Jambon")
            "Boissons" -> listOf("Tous", "Eau", "Jus", "Soda", "Lait", "Café", "Thé")
            "Laiterie" -> listOf("Tous", "Yaourt", "Fromage", "Lait", "Beurre", "Crème")
            "Épicerie" -> listOf("Tous", "Pâtes", "Riz", "Sauce", "Conserve", "Gâteaux")
            "Légumes" -> listOf("Tous", "Carotte", "Salade", "Tomate", "Pomme de terre", "Oignon")
            "Fruits" -> listOf("Tous", "Pomme", "Banane", "Orange", "Fraise", "Raisin")
            else -> listOf("Tous", "Bio", "Nutri-Score A")
        }
    }

    // Filtrage local des produits
    val filteredProducts = remember(products, selectedFilter) {
        if (selectedFilter == "Tous") products
        else {
            products.filter { 
                it.name?.contains(selectedFilter, ignoreCase = true) == true ||
                it.categories?.contains(selectedFilter, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(categoryTag) {
        isLoading = true
        products = searchManager.searchProducts(query = categoryName, categoryTag = categoryTag)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(categoryName, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("${filteredProducts.size} produits", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {
            
            // Barre de filtres horizontale (Chips)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subFilters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00C853),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedFilter == filter) Color.Transparent else Color.LightGray.copy(alpha = 0.5f),
                            enabled = true,
                            selected = selectedFilter == filter
                        )
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF00C853))
                } else if (filteredProducts.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aucun produit trouvé", color = Color.Gray)
                        if (selectedFilter != "Tous") {
                            TextButton(onClick = { selectedFilter = "Tous" }) {
                                Text("Réinitialiser le filtre", color = Color(0xFF00C853))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts) { product ->
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
