package com.Upermarket.upermarket

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.upermarket.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class Store(
    val name: String,
    val address: String,
    val zipCode: String,
    val city: String,
    val distance: String,
    val openingHours: String,
    val logoRes: Int,
    val location: LatLng,
    val hasDrive: Boolean = true,
    val hasDelivery: Boolean = true,
    val hasWalkIn: Boolean = true
)

suspend fun searchRealAddressesForStore(query: String): List<String> = withContext(Dispatchers.IO) {
    if (query.length < 3) return@withContext emptyList()
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val response = URL("https://api-adresse.data.gouv.fr/search/?q=$encoded&limit=5").readText()
        val json = JSONObject(response)
        val features = json.getJSONArray("features")
        val results = mutableListOf<String>()
        for (i in 0 until features.length()) {
            val properties = features.getJSONObject(i).getJSONObject("properties")
            results.add(properties.getString("label"))
        }
        results
    } catch (e: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorePickerSheet(
    onStoreSelected: (Store) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Liste") }
    var selectedStoreForMap by remember { mutableStateOf<Store?>(null) }
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(48.8566, 2.3522), 6f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasLocationPermission = isGranted
    }

    @SuppressLint("MissingPermission")
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    loc?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val demoStores = listOf(
        Store("Lidl Avermes", "Avenue des Isles", "03000", "Avermes", "1.2 km", "08:30 - 19:30", R.drawable.lidl_logo_svg, LatLng(46.5875, 3.3106)),
        Store("Carrefour Moulins", "60 Rue de Bourgogne", "03000", "Moulins", "1.8 km", "08:00 - 20:00", R.drawable.carrefour_logo_1982, LatLng(46.5680, 3.3350)),
        Store("Leclerc Yzeure", "Route Départementale 12", "03400", "Yzeure", "3.6 km", "08:30 - 19:00", R.drawable.e_leclerc_logo_svg, LatLng(46.5620, 3.3550))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.background(Color(0xFFF1F3F4), CircleShape)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.Black)
            }
            Spacer(Modifier.width(16.dp))
            Text("Choisissez un service", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }

        // --- TABS GOOGLE STYLE ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabButton("Liste", selectedTab == "Liste", Modifier.weight(1f)) { selectedTab = "Liste" }
            TabButton("Carte", selectedTab == "Carte", Modifier.weight(1f)) { selectedTab = "Carte" }
        }

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == "Liste") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        ModernDeliveryBanner(searchQuery.ifBlank { "Ma Position" })
                    }
                    
                    item {
                        Text("Retrait en magasin", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }

                    items(demoStores) { store ->
                        ModernStoreCard(store, onSelect = { onStoreSelected(store) })
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                        uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
                    ) {
                        demoStores.forEach { store ->
                            Marker(
                                state = MarkerState(position = store.location),
                                title = store.name,
                                onClick = {
                                    selectedStoreForMap = store
                                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(store.location, 16f))
                                    false
                                }
                            )
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedStoreForMap != null,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        selectedStoreForMap?.let { StoreMapCard(it, onSelect = { onStoreSelected(it) }) }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernDeliveryBanner(location: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF1A73E8).copy(0.5f)),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1A73E8)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Effet de lumière en arrière-plan
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = size.width / 1.5f,
                    center = Offset(size.width, size.height / 2)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LIVRAISON À DOMICILE", color = Color.White.copy(0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(location, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Color.White.copy(0.2f), shape = RoundedCornerShape(12.dp)) {
                        Text("Express • Gratuit", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                
                // LE BEAUX CAMION
                Image(
                    painter = painterResource(id = R.drawable.truck),
                    contentDescription = null,
                    modifier = Modifier.size(110.dp).offset(x = 15.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun TabButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) Color.Black else Color(0xFFF1F3F4),
        contentColor = if (isSelected) Color.White else Color.Black
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun ModernStoreCard(store: Store, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF8F9FA),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(64.dp), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 4.dp) {
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    Image(painter = painterResource(id = store.logoRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Ouvert • ${store.distance}", color = Color(0xFF00C853), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(store.address, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun StoreMapCard(store: Store, onSelect: (Store) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(32.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFF8F9FA)) {
                    Image(painter = painterResource(id = store.logoRes), contentDescription = null, modifier = Modifier.padding(10.dp), contentScale = ContentScale.Fit)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(store.name, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text(store.address, color = Color.Gray, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSelect(store) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Choisir ce magasin", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}
