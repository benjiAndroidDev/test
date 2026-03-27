package com.Upermarket.upermarket

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.upermarket.R
import kotlinx.coroutines.flow.collectLatest

// Définition de la classe pour les filtres
data class VisualFilter(val name: String, val imageRes: Int)

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

    // --- MOTEUR DE RECONNAISSANCE D'IMAGES ULTRA-PERSONNALISABLE ---
    fun getBestImageFor(name: String): Int {
        val n = name.lowercase()
        return when {
            // RAYON FRAIS & TRAITEUR
            n.contains("traiteur") -> R.drawable.traiteur
            n.contains("sandwich") -> R.drawable.club_sandwich1
            n.contains("plat") -> R.drawable._4486_1640103633
            n.contains("frais") -> R.drawable.produit_frais
            
            // SURGELÉS
            n.contains("pizza") -> R.drawable.regina
            n.contains("glace") || n.contains("sorbet") -> R.drawable.vanille_600x600_1
            n.contains("légume") && n.contains("surgelé") -> R.drawable.front_fr_42_200
            n.contains("surgelé") -> R.drawable.surgele

            // ÉPICERIE / SEL / SUCRE
            n.contains("sel") -> R.drawable.sel
            n.contains("sucre") -> R.drawable.la_baleine_sel_fin_250g
            n.contains("huile") -> R.drawable.fbfc6ec60502_huiles
            n.contains("riz") -> R.drawable.riz
            n.contains("pâte") -> R.drawable.pates
            n.contains("miel") -> R.drawable.miel
            n.contains("céréale") -> R.drawable.istockphoto_118964827_612x612
            n.contains("chocolat") -> R.drawable.chocolat

            // BOISSONS & ALCOOLS
            n.contains("bière") -> R.drawable.bierre
            n.contains("vin") -> R.drawable.vin
            n.contains("jus") -> R.drawable._3045320104127_c1c11_1
            n.contains("soda") || n.contains("coca") -> R.drawable.coca
            n.contains("eau") -> R.drawable.volvic
            n.contains("café") -> R.drawable.caf_

            // VIANDES & CHARCUTERIE
            n.contains("volaille") -> R.drawable.blanc_de_poulet_naturals_300x300
            n.contains("poulet") -> R.drawable.poulet
            n.contains("boeuf") -> R.drawable.cote_de_boeuf
            n.contains("steak") -> R.drawable.steak
            n.contains("jambon") -> R.drawable.jambon
            n.contains("lardon") -> R.drawable.lardons
            n.contains("saucisson") -> R.drawable.saucisse_seche

            // FRUITS & LÉGUMES
            n.contains("pomme de terre") || n.contains("patate") -> R.drawable.white_e1563480325829
            n.contains("pomme") -> R.drawable.pommes
            n.contains("banane") -> R.drawable.__idees_recettes_pour_vos_bananes_mures_scaled
            n.contains("orange") -> R.drawable.csm_orange_yi1yb_fubh8_unsplash_128aea4909
            n.contains("salade") -> R.drawable.large_saladeslaituesavril
            n.contains("tomate") -> R.drawable.tomatoes_5962167_scaled
            n.contains("carotte") -> R.drawable._99729

            // LAITERIE
            n.contains("lait") -> R.drawable.lait
            n.contains("yaourt") -> R.drawable._25151
            n.contains("fromage") -> R.drawable.fromage
            n.contains("oeuf") -> R.drawable.oeuf
            
            else -> R.drawable.bien_consommer_le_beurre_les_bons_gestes_et_erreurs_a_eviter_selon_une_nutritionniste
        }
    }

    val headerImage = remember(selectedFilter, categoryName) {
        val target = if (selectedFilter == "Tous") categoryName else selectedFilter
        getBestImageFor(target)
    }

    val visualFilters = remember(categoryName) {
        val name = categoryName.lowercase()
        val baseImg = getBestImageFor(categoryName)
        
        when {
            name.contains("frais") || name.contains("traiteur") -> listOf(
                VisualFilter("Tous", baseImg),
                VisualFilter("Traiteur", R.drawable.traiteur),
                VisualFilter("Sandwich", R.drawable.club_sandwich1),
                VisualFilter("Plats", R.drawable._4486_1640103633)
            )
            name.contains("surgelé") -> listOf(
                VisualFilter("Tous", baseImg),
                VisualFilter("Pizza", R.drawable.regina),
                VisualFilter("Glace", R.drawable.vanille_600x600_1),
                VisualFilter("Légumes", R.drawable.front_fr_42_200)
            )
            name.contains("viande") || name.contains("charcut") -> listOf(
                VisualFilter("Tous", baseImg),
                VisualFilter("Poulet", R.drawable.poulet),
                VisualFilter("Boeuf", R.drawable.cote_de_boeuf),
                VisualFilter("Volaille", R.drawable.blanc_de_poulet_naturals_300x300)
            )
            else -> listOf(
                VisualFilter("Tous", baseImg),
                VisualFilter("Bio", R.drawable.produkt_biodegradowalny_do_pielegnacji_kwiatow2_1)
            )
        }
    }

    val filteredProducts = remember(products, selectedFilter) {
        if (selectedFilter == "Tous") products
        else {
            products.filter { 
                it.name?.contains(selectedFilter, ignoreCase = true) == true ||
                it.brands?.contains(selectedFilter, ignoreCase = true) == true ||
                it.categories?.contains(selectedFilter, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(categoryTag) {
        isLoading = true
        searchManager.searchProductsFlow(query = categoryName, categoryTag = categoryTag)
            .collectLatest { updatedList: List<Product> ->
                if (updatedList.isNotEmpty()) {
                    products = updatedList
                    isLoading = false
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(categoryName, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(
                            text = if (isLoading && products.isEmpty()) "Arrivage frais en cours..." else "${filteredProducts.size} produits trouvés",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
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
            
            // --- FILTRES ---
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visualFilters) { filter ->
                    VisualFilterItem(
                        filter = filter,
                        isSelected = selectedFilter == filter.name,
                        onClick = { selectedFilter = filter.name }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White)
                        ) {
                            Image(
                                painter = painterResource(id = headerImage),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(24.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF00C853),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "SÉLECTION PREMIUM",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if(selectedFilter == "Tous") categoryName else selectedFilter,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    if (isLoading && products.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF00C853))
                            }
                        }
                    } else {
                        items(filteredProducts) { product ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SearchProductRow(
                                    product = product,
                                    isFavorite = favoritesViewModel.isFavorite(product),
                                    onToggleFavorite = { favoritesViewModel.toggleFavorite(product) },
                                    onClick = { selectedProduct = product }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
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

@Composable
fun VisualFilterItem(
    filter: VisualFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).width(75.dp)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (isSelected) Color(0xFF00C853) else Color.White,
            border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)) else null,
            shadowElevation = if (isSelected) 8.dp else 2.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                Image(
                    painter = painterResource(id = filter.imageRes),
                    contentDescription = filter.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = filter.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) Color(0xFF00C853) else Color.DarkGray,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
