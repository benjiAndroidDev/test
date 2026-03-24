package com.Upermarket.upermarket

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.upermarket.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class Brand(val name: String, val logoRes: Int, val storeUrl: String)

suspend fun searchHomeAddresses(query: String): List<String> = withContext(Dispatchers.IO) {
    if (query.length < 3) return@withContext emptyList()
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val response = URL("https://api-adresse.data.gouv.fr/search/?q=$encoded&limit=5").readText()
        val features = JSONObject(response).getJSONArray("features")
        val results = mutableListOf<String>()
        for (i in 0 until features.length()) {
            results.add(features.getJSONObject(i).getJSONObject("properties").getString("label"))
        }
        results
    } catch (e: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    favoritesViewModel: FavoritesViewModel,
    cartViewModel: CartViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var currentAddress by remember { mutableStateOf("12 Rue de la Paix, Paris") }
    var showAddressSheet by remember { mutableStateOf(false) }
    var showStorePicker by remember { mutableStateOf(false) }

    val categories = listOf(
        Category("Fruits", R.drawable.fruits, 0xFFFFE0E0L, "en:fruits"),
        Category("Légumes", R.drawable.legumes, 0xFFE0FFE0L, "en:vegetables"),
        Category("Viandes", R.drawable.viandes, 0xFFFFE0B2L, "en:meats"),
        Category("Boissons", R.drawable.boissons, 0xFFE1F5FEL, "en:beverages"),
        Category("Épicerie", R.drawable.epicerie, 0xFFF3E5F5L, "en:groceries"),
        Category("Surgelés", R.drawable.surgele, 0xFFE0F7FAL, "en:frozen-foods"),
        Category("Frais", R.drawable.produit_frais, 0xFFFFF9C4L, "en:fresh-foods"),
        Category("Laiterie", R.drawable.produits_laitiers, 0xFFF5F5F5L, "en:dairies"),
        Category("Charcut.", R.drawable.charcuterie, 0xFFFBE9E7L, "en:charcuteries")
    )

    val allBrands = listOf(
        Brand("Lidl", R.drawable.lidl_logo_svg, "https://www.lidl.fr"),
        Brand("Carrefour", R.drawable.carrefour_logo_1982, "https://www.carrefour.fr"),
        Brand("E.Leclerc", R.drawable.e_leclerc_logo_svg, "https://www.e.leclerc"),
        Brand("Auchan", R.drawable.logo_auchan_, "https://www.auchan.fr"),
        Brand("Intermarché", R.drawable.breve29741, "https://www.intermarche.com"),
        Brand("Casino", R.drawable.casino_supermarket_logo, "https://www.casinosupermarches.fr"),
        Brand("Netto", R.drawable.french_netto_logo_2019_svg, "https://www.netto.fr"),
        Brand("Monoprix", R.drawable._00625192, "https://www.monoprix.fr"),
        Brand("Franprix", R.drawable.franprix_2026, "https://www.franprix.fr")
    )

    // AUTO-SCROLL LOGIC
    val brandListState = rememberLazyListState()
    LaunchedEffect(Unit) {
        while (true) {
            brandListState.animateScrollBy(
                value = 500f,
                animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Upermarket", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                // --- MODERN SEARCH BAR PRO ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(12.dp, RoundedCornerShape(30.dp), ambientColor = Color(0xFF00C853).copy(alpha = 0.2f), spotColor = Color(0xFF00C853).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ICON MAP DANS LA SEARCH BAR
                            Surface(
                                onClick = { showStorePicker = true },
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = Color(0xFFF5F5F5)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = R.drawable.map),
                                        contentDescription = "Carte",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            // TEXT INTERACTIF ADRESSE
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showAddressSheet = true }
                            ) {
                                Text(
                                    "Livraison à",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    currentAddress,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                            }
                            
                            // BOUTON RECHERCHE ICONE
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = Color(0xFF00C853)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Nos Enseignes")
                
                LazyRow(
                    state = brandListState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(allBrands + allBrands + allBrands) { brand ->
                        BrandCard(brand) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(brand.storeUrl))
                            context.startActivity(intent)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader("Rayons")
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    categories.chunked(3).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { cat ->
                                ModernCategoryItem(category = cat, modifier = Modifier.weight(1f), onClick = { navController.navigate("category_products/${cat.name}/${cat.apiTag}") })
                            }
                            if (rowItems.size < 3) { repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) } }
                        }
                    }
                }
            }
        }

        if (showAddressSheet) {
            ModalBottomSheet(onDismissRequest = { showAddressSheet = false }, containerColor = Color.White) {
                AddressSearchContent(onAddressSelected = { currentAddress = it; showAddressSheet = false })
            }
        }

        if (showStorePicker) {
            ModalBottomSheet(
                onDismissRequest = { showStorePicker = false },
                containerColor = Color.White,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                StorePickerSheet(
                    onStoreSelected = { store ->
                        currentAddress = store.name
                        showStorePicker = false
                    },
                    onDismiss = { showStorePicker = false }
                )
            }
        }
    }
}

@Composable
fun AddressSearchContent(onAddressSelected: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()) {
        Text("Où souhaitez-vous être livré ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; scope.launch { suggestions = searchHomeAddresses(it) } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tapez une ville ou une adresse...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFF00C853)) },
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(20.dp))
        suggestions.forEach { suggestion ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onAddressSelected(suggestion) }.padding(16.dp)) {
                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF00C853))
                Spacer(Modifier.width(16.dp))
                Text(suggestion)
            }
        }
    }
}

@Composable
fun BrandCard(brand: Brand, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(90.dp).padding(4.dp).shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.3f)),
        shape = CircleShape,
        color = Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize().clickable(onClick = onClick).padding(16.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = brand.logoRes), contentDescription = brand.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun ModernCategoryItem(category: Category, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.padding(4.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(75.dp).background(category.composeColor, CircleShape).padding(15.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = category.imageRes), contentDescription = category.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = category.name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
}
