package com.Upermarket.upermarket

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.upermarket.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// --- SERVICE API ADRESSE FRANCE ---
suspend fun searchRealAddresses(query: String): List<String> = withContext(Dispatchers.IO) {
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
fun CartSheet(cartViewModel: CartViewModel, favoritesViewModel: FavoritesViewModel) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showCheckoutOptions by remember { mutableStateOf(false) }
    var showDrivePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.95f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- HEADER ---
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Column {
                Text("Mon Panier", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp), color = Color.Black)
                Text("${cartViewModel.itemCount} articles sélectionnés", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }

        if (cartViewModel.cartItems.isEmpty()) {
            EmptyCartView()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartViewModel.cartItems) { item ->
                    ModernCartProductCard(item = item, cartViewModel = cartViewModel, onProductClick = { selectedProduct = item.product })
                }
            }

            // --- RÉSUMÉ & COMMANDE ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                shadowElevation = 24.dp
            ) {
                Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                    // SÉLECTEUR DRIVE INTERACTIF
                    Surface(
                        onClick = { showCheckoutOptions = true },
                        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF9F9F9),
                        border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(56.dp), shadowElevation = 2.dp) {
                                Box(contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = if(cartViewModel.selectedDrive != "Drive") cartViewModel.selectedDriveLogo else R.drawable.truck),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if(cartViewModel.selectedDrive == "Drive") "Passer la commande" else "Drive sélectionné", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text(cartViewModel.selectedDrive, color = Color(0xFF00C853), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Rounded.ExpandCircleDown, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total Estimé", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                            Text("${String.format(Locale.FRANCE, "%.2f", cartViewModel.totalPrice)} €", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = Color.Black)
                        }
                        Button(
                            onClick = { showCheckoutOptions = true },
                            modifier = Modifier.height(60.dp).widthIn(min = 180.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Text("Commander", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGUE DE COMMANDE ---
    if (showCheckoutOptions) {
        ModalBottomSheet(onDismissRequest = { showCheckoutOptions = false }) {
            CheckoutOptionsSheet(
                totalPrice = cartViewModel.totalPrice,
                onDriveClick = { showCheckoutOptions = false; showDrivePicker = true },
                onConfirm = { showCheckoutOptions = false }
            )
        }
    }

    // --- SÉLECTEUR DE DRIVE AVEC API MAPS ---
    if (showDrivePicker) {
        ModalBottomSheet(
            onDismissRequest = { showDrivePicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            DrivePickerWithMapsSheet(
                onDriveSelected = { name, logo ->
                    cartViewModel.selectedDrive = name
                    cartViewModel.selectedDriveLogo = logo
                    showDrivePicker = false
                }
            )
        }
    }

    if (selectedProduct != null) {
        ProductDetailSheet(
            product = selectedProduct!!,
            isFavorite = favoritesViewModel.isFavorite(selectedProduct!!),
            onToggleFavorite = { favoritesViewModel.toggleFavorite(selectedProduct!!) },
            onAddToCart = { price -> cartViewModel.addToCart(selectedProduct!!, price); selectedProduct = null },
            onDismiss = { selectedProduct = null }
        )
    }
}

@Composable
fun DrivePickerWithMapsSheet(onDriveSelected: (String, Int) -> Unit) {
    var addressQuery by remember { mutableStateOf("") }
    var addressSuggestions by remember { mutableStateOf(listOf<String>()) }
    var selectedAddress by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val drives = listOf(
        Pair("Lidl Drive", R.drawable.lidl_logo_svg),
        Pair("Carrefour Drive", R.drawable.carrefour_logo_1982),
        Pair("E.Leclerc Drive", R.drawable.e_leclerc_logo_svg),
        Pair("Auchan Drive", R.drawable.logo_auchan__2015__svg),
        Pair("Intermarché Drive", R.drawable.nouveau_logo_intermarche),
        Pair("Casino Drive", R.drawable.casino_supermarket_logo)
    )

    Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.85f).navigationBarsPadding()) {
        Text("Trouver un Drive", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Entrez votre adresse pour localiser les magasins", color = Color.Gray, modifier = Modifier.padding(bottom = 20.dp))

        // Barre de recherche d'adresse avec API réelle
        OutlinedTextField(
            value = addressQuery,
            onValueChange = { 
                addressQuery = it
                scope.launch { addressSuggestions = searchRealAddresses(it) }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tapez votre adresse...") },
            leadingIcon = { Icon(Icons.Rounded.LocationSearching, null, tint = Color(0xFF00C853)) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00C853))
        )

        // Affichage des suggestions fluides
        AnimatedVisibility(visible = addressSuggestions.isNotEmpty() && selectedAddress.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFF5F5F5)) {
                Column {
                    addressSuggestions.forEach { suggestion ->
                        Text(text = suggestion, modifier = Modifier.fillMaxWidth().clickable { 
                            selectedAddress = suggestion
                            addressQuery = suggestion
                            addressSuggestions = emptyList()
                        }.padding(16.dp), fontSize = 14.sp)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(if(selectedAddress.isEmpty()) "Magasins recommandés" else "Magasins près de $selectedAddress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(drives) { drive ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onDriveSelected(drive.first, drive.second) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F5F5),
                    border = BorderStroke(1.dp, if(selectedAddress.isNotEmpty()) Color(0xFF00C853).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                Image(painter = painterResource(id = drive.second), contentDescription = null, contentScale = ContentScale.Fit)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(drive.first, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if(selectedAddress.isEmpty()) "Ouvert" else "Ouvert • À ${(1..5).random()},${(1..9).random()} km", color = Color(0xFF00C853), fontSize = 12.sp)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutOptionsSheet(totalPrice: Float, onDriveClick: () -> Unit, onConfirm: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
        Text("Choisir votre service", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Sélectionnez comment vous souhaitez recevoir vos articles", color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

        CheckoutOptionItem(title = "Livraison Express", subtitle = "Chez vous dans 20-35 min", icon = Icons.Rounded.Bolt, color = Color(0xFFFFB300), price = "2,99 €", onClick = onConfirm)
        Spacer(Modifier.height(12.dp))
        CheckoutOptionItem(title = "Drive & Collect", subtitle = "Choisissez votre magasin favori", icon = Icons.Rounded.Storefront, color = Color(0xFF1976D2), price = "Gratuit", onClick = onDriveClick)

        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total à régler", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${String.format(Locale.FRANCE, "%.2f", totalPrice)} €", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF00C853))
        }
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
            Text("Confirmer ma commande", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun CheckoutOptionItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, price: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color) }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Text(price, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}

@Composable
fun ModernCartProductCard(item: CartItem, cartViewModel: CartViewModel, onProductClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onProductClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF9F9F9)), contentAlignment = Alignment.Center) {
                Image(painter = rememberAsyncImagePainter(item.product.imageUrl), contentDescription = null, modifier = Modifier.size(70.dp), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.product.name ?: "Produit", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = item.product.brands ?: "Upermarket", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text(text = "${String.format(Locale.FRANCE, "%.2f", item.price)} €", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF00C853))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)).padding(vertical = 4.dp)) {
                IconButton(onClick = { cartViewModel.updateQuantity(item.product, item.quantity + 1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp), tint = Color.Black) }
                Text(text = "${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 2.dp))
                IconButton(onClick = { cartViewModel.updateQuantity(item.product, item.quantity - 1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(18.dp), tint = Color.Black) }
            }
        }
    }
}

@Composable
fun EmptyCartView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = Color(0xFFF5F5F5)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.ShoppingBag, null, modifier = Modifier.size(50.dp), tint = Color.LightGray) }
            }
            Spacer(Modifier.height(24.dp))
            Text("Votre panier est vide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Commencez vos achats maintenant", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}
