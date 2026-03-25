package com.Upermarket.upermarket

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.upermarket.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
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
    val hasWalkIn: Boolean = true,
    val rating: Float = 4.5f
)

// FONCTION UTILISÉE AUSSI DANS CART_SHEET.KT
suspend fun searchRealAddressesForStore(query: String): List<String> = withContext(Dispatchers.IO) {
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
fun StorePickerSheet(
    onStoreSelected: (Store) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var selectedTab by remember { mutableStateOf("Carte") }
    var selectedStoreForMap by remember { mutableStateOf<Store?>(null) }
    var isMapLoading by remember { mutableStateOf(true) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasLocationPermission = isGranted
    }

    // Configuration OSMDroid ultra-rapide
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Configuration.getInstance().userAgentValue = context.packageName
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        }
        delay(300)
        isMapLoading = false
    }

    val demoStores = remember { listOf(
        Store("Lidl Avermes", "Avenue des Isles", "03000", "Avermes", "1.2 km", "08:30 - 19:30", R.drawable.lidl_logo_svg, LatLng(46.5875, 3.3106), rating = 4.2f),
        Store("Carrefour Moulins", "60 Rue de Bourgogne", "03000", "Moulins", "1.8 km", "08:00 - 20:00", R.drawable.carrefour_logo_1982, LatLng(46.5680, 3.3350), rating = 4.5f),
        Store("Leclerc Yzeure", "Route Départementale 12", "03400", "Yzeure", "3.6 km", "08:30 - 19:00", R.drawable.e_leclerc_logo_svg, LatLng(46.5620, 3.3550), rating = 4.7f),
        Store("Auchan Moulins", "Rue des Docteurs Roux", "03000", "Moulins", "2.1 km", "08:30 - 20:00", R.drawable.logo_auchan_, LatLng(46.5750, 3.3250), hasDelivery = false),
        Store("Intermarché Hyper", "Route de Lyon", "03000", "Moulins", "2.5 km", "08:30 - 19:30", R.drawable.nouveau_logo_intermarche, LatLng(46.5550, 3.3450)),
        Store("Casino Supermarché", "Place d'Allier", "03000", "Moulins", "0.8 km", "08:00 - 20:00", R.drawable.casino_supermarket_logo, LatLng(46.5650, 3.3310), hasDrive = false)
    )}

    val mapView = remember {
        MapView(context).apply {
            // Optimisations de performance
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            setFlingEnabled(true) // Inertie pour le pan
            isVerticalMapRepetitionEnabled = false
            isHorizontalMapRepetitionEnabled = false
            
            // Paramètres de rendu
            setHasTransientState(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            
            // Zoom ultra fluide
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(46.566, 3.333))
            
            // Couleurs de fond
            setBackgroundColor(android.graphics.Color.WHITE)
            
            // Overlay de rotation (pro)
            val rotationGestureOverlay = RotationGestureOverlay(this)
            rotationGestureOverlay.isEnabled = true
            overlays.add(rotationGestureOverlay)
        }
    }

    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
            setDrawAccuracyEnabled(true)
            // Icône de position personnalisée si nécessaire ici
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); locationOverlay.enableMyLocation() }
                Lifecycle.Event.ON_PAUSE -> { mapView.onPause(); locationOverlay.disableMyLocation() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.overlays.add(locationOverlay)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); mapView.onDetach() }
    }

    // Gestion intelligente des marqueurs
    LaunchedEffect(demoStores, selectedStoreForMap) {
        mapView.overlays.removeAll { it is Marker }
        demoStores.forEach { store ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(store.location.latitude, store.location.longitude)
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            val isSelected = selectedStoreForMap == store
            val pinBitmap = createUltraProPin(context, store.logoRes, isSelected = isSelected)
            marker.icon = android.graphics.drawable.BitmapDrawable(context.resources, pinBitmap)
            
            marker.setOnMarkerClickListener { m, _ ->
                selectedStoreForMap = store
                // Animation de caméra plus courte pour plus de réactivité
                mapView.controller.animateTo(m.position, 16.5, 600L)
                true
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
        // HEADER PREMIUM DESIGN 2026
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0xFFF5F5F7)
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.Black, modifier = Modifier.size(20.dp)) 
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Magasins Cali", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = (-0.7).sp)
                    Text("À proximité de Moulins", style = MaterialTheme.typography.labelMedium, color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                }
                Surface(color = Color(0xFFF5F5F7), shape = CircleShape, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(20.dp)) }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // SELECTEUR TAB STYLE IOS
            Surface(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                color = Color(0xFFF2F2F7),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    TabItemPro("Carte", selectedTab == "Carte", Modifier.weight(1f)) { selectedTab = "Carte" }
                    TabItemPro("Liste", selectedTab == "Liste", Modifier.weight(1f)) { selectedTab = "Liste" }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(animationSpec = tween(300)))
                },
                label = "mainView"
            ) { targetTab ->
                if (targetTab == "Liste") {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(demoStores) { store -> 
                            ModernStoreCard(store, onSelect = { 
                                selectedStoreForMap = store
                                selectedTab = "Carte"
                                scope.launch {
                                    delay(400)
                                    mapView.controller.animateTo(GeoPoint(store.location.latitude, store.location.longitude), 16.5, 800L)
                                }
                            }) 
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { mapView }, 
                            modifier = Modifier.fillMaxSize(),
                            update = { /* Pas de mise à jour nécessaire ici */ }
                        )
                        
                        // DEGRADÉ DE HAUT POUR LES CONTROLES
                        Box(Modifier.fillMaxWidth().height(60.dp).background(Brush.verticalGradient(listOf(Color.White.copy(0.3f), Color.Transparent))))

                        if (isMapLoading) {
                            Box(Modifier.fillMaxSize().background(Color.White.copy(0.5f)), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF00C853), strokeWidth = 3.dp)
                            }
                        }

                        // BOUTONS DE CONTRÔLE FLOTTANTS (STYLISÉS)
                        Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FloatingActionBtnPro(Icons.Rounded.MyLocation) {
                                if (hasLocationPermission) {
                                    locationOverlay.myLocation?.let { mapView.controller.animateTo(it, 16.0, 800L) }
                                } else { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                            }
                        }

                        // CARD SÉLECTIONNÉE AVEC ANIMATION DE TRANSLATION
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedStoreForMap != null,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 24.dp),
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
}

@Composable
fun StoreMapCard(store: Store, onSelect: (Store) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(40.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(0.12f)),
        shape = RoundedCornerShape(30.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) { 
                    Image(painter = painterResource(id = store.logoRes), contentDescription = null, modifier = Modifier.padding(12.dp), contentScale = ContentScale.Fit) 
                }
                
                Spacer(Modifier.width(18.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(store.name, fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = (-0.6).sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Verified, null, tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                    }
                    Text(store.address, color = Color.Gray, fontSize = 14.sp)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD600), modifier = Modifier.size(16.dp))
                            Text(" ${store.rating}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        ServiceBadge(Icons.Rounded.Timer, "Ouvert")
                    }
                }
            }
            
            Spacer(Modifier.height(26.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = { /* Itinéraire */ },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF2F2F7)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Directions, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Itinéraire", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                
                Button(
                    onClick = { onSelect(store) },
                    modifier = Modifier.weight(1.5f).height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Choisir ce drive", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TabItemPro(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor by animateColorAsState(if (isSelected) Color.White else Color.Transparent, label = "tabColor")
    val elevation by animateDpAsState(if (isSelected) 4.dp else 0.dp, label = "tabElevation")
    
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(11.dp),
        color = backgroundColor,
        shadowElevation = elevation,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) { 
            Text(label, fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold, fontSize = 14.sp, color = if (isSelected) Color.Black else Color.Gray) 
        }
    }
}

fun createUltraProPin(context: Context, logoResId: Int, isSelected: Boolean): Bitmap {
    val density = context.resources.displayMetrics.density
    val baseSize = if (isSelected) 72 else 58
    val size = (baseSize * density).toInt()
    val logoSize = (size * 0.72).toInt()
    
    val output = Bitmap.createBitmap(size, (size * 1.3).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // OMBRE DU PIN
    paint.color = 0x25000000
    canvas.drawCircle(size / 2f, size * 0.98f, (if (isSelected) 10 else 7) * density, paint)
    
    // CORPS DU PIN (FORME GOUTTE MODERNE)
    paint.color = if (isSelected) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2.1f, paint)
    
    val path = android.graphics.Path()
    path.moveTo(size / 2f - (12 * density), size / 2f + (15 * density))
    path.lineTo(size / 2f + (12 * density), size / 2f + (15 * density))
    path.lineTo(size / 2f, size * 1.15f)
    path.close()
    canvas.drawPath(path, paint)
    
    if (isSelected) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2.25f, paint)
    }

    // LOGO CENTRAL
    ContextCompat.getDrawable(context, logoResId)?.let {
        val logoBitmap = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(logoBitmap)
        it.setBounds(0, 0, logoSize, logoSize)
        it.draw(logoCanvas)
        val circular = getCircularBitmap(logoBitmap)
        val offset = (size - logoSize) / 2f
        canvas.drawBitmap(circular, offset, offset, null)
    }
    
    return output
}

@Composable
fun ModernStoreCard(store: Store, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(16.dp), color = Color.White) { 
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) { 
                    Image(painter = painterResource(id = store.logoRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) 
                } 
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(store.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.4).sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(store.distance, color = Color(0xFF00C853), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("•", color = Color.LightGray)
                    Spacer(Modifier.width(8.dp))
                    Text(store.openingHours, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun FloatingActionBtnPro(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.Black, modifier = Modifier.size(24.dp)) }
    }
}

@Composable
fun ServiceBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(icon, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

fun getCircularBitmap(bitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return output
}
