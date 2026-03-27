package com.Upermarket.upermarket

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.upermarket.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

// Modèle de données magasin
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
    
    var selectedStore by remember { mutableStateOf<Store?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isMapLoading by remember { mutableStateOf(true) }

    var hasLocationPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasLocationPermission = isGranted
    }

    val demoStores = remember { listOf(
        Store("Lidl Avermes", "Avenue des Isles", "03000", "Avermes", "1.2 km", "Ouvre à 08:30", R.drawable.lidl_logo_svg, LatLng(46.5875, 3.3106)),
        Store("Carrefour Moulins", "60 Rue de Bourgogne", "03000", "Moulins", "1.8 km", "Ouvre à 08:00", R.drawable.carrefour_logo_1982, LatLng(46.5680, 3.3350)),
        Store("Leclerc Yzeure", "Route Départementale 12", "03400", "Yzeure", "3.6 km", "Ouvre à 08:30", R.drawable.e_leclerc_logo_svg, LatLng(46.5620, 3.3550)),
        Store("Auchan Moulins", "Rue des Docteurs Roux", "03000", "Moulins", "2.1 km", "Ouvre à 08:30", R.drawable.logo_auchan_, LatLng(46.5750, 3.3250)),
        Store("Intermarché Hyper", "Route de Lyon", "03000", "Moulins", "2.5 km", "Ouvre à 08:30", R.drawable.nouveau_logo_intermarche, LatLng(46.5550, 3.3450)),
        Store("Casino Supermarché", "Place d'Allier", "03000", "Moulins", "0.8 km", "Ouvre à 08:00", R.drawable.casino_supermarket_logo, LatLng(46.5650, 3.3310))
    )}

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder()
            .target(LatLng(46.566, 3.333))
            .zoom(14f)
            .tilt(45f)
            .build()
    }

    LaunchedEffect(Unit) {
        delay(600)
        isMapLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false, 
                myLocationButtonEnabled = false,
                tiltGesturesEnabled = true
            ),
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            )
        ) {
            demoStores.forEach { store ->
                val icon = remember(store.logoRes, selectedStore) {
                    BitmapDescriptorFactory.fromBitmap(
                        createUltraProPin(context, store.logoRes, isSelected = selectedStore == store)
                    )
                }
                Marker(
                    state = MarkerState(position = store.location),
                    icon = icon,
                    onClick = {
                        selectedStore = store
                        scope.launch { 
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.builder()
                                        .target(store.location)
                                        .zoom(17f)
                                        .tilt(60f)
                                        .bearing(0f)
                                        .build()
                                ),
                                1000
                            )
                        }
                        true
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Trouver un magasin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(27.dp),
                color = Color(0xFFF2F2F7),
                shadowElevation = 2.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("Ville, département", color = Color.Gray, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

        Surface(
            onClick = {
                if (hasLocationPermission) {
                    // Recentrage géré par le SDK
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = 100.dp)
                .size(48.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MyLocation, null, tint = Color(0xFF1976D2), modifier = Modifier.size(22.dp))
            }
        }

        AnimatedVisibility(
            visible = selectedStore != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            selectedStore?.let { store ->
                StoreMapCardPro(store, onSelect = { onStoreSelected(it) })
            }
        }
        
        if (isMapLoading) {
            Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00C853))
            }
        }
    }
}

@Composable
fun StoreMapCardPro(store: Store, onSelect: (Store) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(25.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(0.2f)),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8F9FA),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) { 
                    Image(
                        painter = painterResource(id = store.logoRes),
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        contentScale = ContentScale.Fit
                    ) 
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(store.name, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = (-0.5).sp)
                    Text(store.address, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF00C853), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(store.openingHours, color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(store.distance, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onSelect(store) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("Choisir ce", fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                OutlinedButton(
                    onClick = { /* Detail */ },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5EA))
                ) {
                    Text("Voir le détail", fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            }
        }
    }
}

fun createUltraProPin(context: Context, logoResId: Int, isSelected: Boolean): Bitmap {
    val density = context.resources.displayMetrics.density
    val baseSize = if (isSelected) 60 else 48
    val size = (baseSize * density).toInt()
    val logoSize = (size * 0.6).toInt()
    val output = Bitmap.createBitmap(size, (size * 1.2).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = 0x30000000
    canvas.drawCircle(size / 2f, size * 0.9f, 5 * density, paint)
    paint.color = if (isSelected) android.graphics.Color.parseColor("#007AFF") else android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2.1f, paint)
    val path = android.graphics.Path()
    path.moveTo(size / 2f - (8 * density), size / 2f + (10 * density))
    path.lineTo(size / 2f + (8 * density), size / 2f + (10 * density))
    path.lineTo(size / 2f, size * 1.0f)
    path.close()
    canvas.drawPath(path, paint)
    if (isSelected) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, size / 2.3f, paint)
    }
    ContextCompat.getDrawable(context, logoResId)?.let { drawable ->
        val logoBitmap = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(logoBitmap)
        drawable.setBounds(0, 0, logoSize, logoSize)
        drawable.draw(logoCanvas)
        val circular = getCircularBitmap(logoBitmap)
        canvas.drawBitmap(circular, (size - logoSize) / 2f, (size - logoSize) / 2f, null)
    }
    return output
}

fun getCircularBitmap(bitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), Rect(0, 0, bitmap.width, bitmap.height), paint)
    return output
}
