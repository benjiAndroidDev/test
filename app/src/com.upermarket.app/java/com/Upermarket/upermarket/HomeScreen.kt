package com.Upermarket.upermarket

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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

// --- SERVICE API ADRESSE (RECHERCHE PRO) ---
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
        Brand("Auchan", R.drawable.logo_auchan__2015__svg, "https://www.auchan.fr"),
        Brand("Intermarché", R.drawable.nouveau_logo_intermarche, "https://www.intermarche.com"),
        Brand("Casino", R.drawable.casino_supermarket_logo, "https://www.casinosupermarches.fr"),
        Brand("Netto", R.drawable.french_netto_logo_2019_svg, "https://www.netto.fr"),
        Brand("Monoprix", R.drawable.monoprix, "https://www.monoprix.fr"),
        Brand("Franprix", R.drawable.franprix, "https://www.franprix.fr")
    )

    val filteredBrands = remember(searchQuery) {
        if (searchQuery.isEmpty()) allBrands
        else allBrands.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { /* Profil */ }) { }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(6.dp, RoundedCornerShape(27.dp)),
                        placeholder = { 
                            Text(
                                text = "🏠 $currentAddress", 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp,
                                color = Color.Gray
                            ) 
                        },
                        leadingIcon = { 
                            IconButton(onClick = { showAddressSheet = true }) {
                                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF00C853), modifier = Modifier.size(22.dp))
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) { 
                                        Icon(Icons.Rounded.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) 
                                    }
                                }
                                VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                IconButton(onClick = { /* Filtres */ }, modifier = Modifier.size(32.dp)) { 
                                    Icon(Icons.Rounded.Tune, null, tint = Color.DarkGray, modifier = Modifier.size(18.dp)) 
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(27.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.Black
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(if (searchQuery.isEmpty()) "Nos Enseignes" else "Enseignes trouvées")
                
                if (filteredBrands.isEmpty()) {
                    Text("Aucune enseigne trouvée", modifier = Modifier.padding(24.dp).fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray)
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredBrands) { brand ->
                            BrandCard(brand) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(brand.storeUrl))
                                context.startActivity(intent)
                            }
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
            ModalBottomSheet(
                onDismissRequest = { showAddressSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AddressSearchContent(onAddressSelected = { currentAddress = it; showAddressSheet = false })
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
        Text("Saisissez votre adresse pour localiser les enseignes", color = Color.Gray, modifier = Modifier.padding(bottom = 20.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; scope.launch { suggestions = searchHomeAddresses(it) } },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tapez une ville ou une adresse...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFF00C853)) },
            trailingIcon = { if(query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Cancel, null, tint = Color.LightGray) } },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00C853))
        )

        Spacer(Modifier.height(20.dp))

        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFF8F9FA), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                Column {
                    suggestions.forEach { suggestion ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onAddressSelected(suggestion) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(text = suggestion, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }

        if (suggestions.isEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.GpsFixed, null, tint = Color(0xFF00C853))
                Spacer(Modifier.width(16.dp))
                Text("Utiliser ma position actuelle", fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun BrandCard(brand: Brand, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(100.dp).padding(4.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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
        Text(text = category.name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 0.5.sp), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onBackground)
}
