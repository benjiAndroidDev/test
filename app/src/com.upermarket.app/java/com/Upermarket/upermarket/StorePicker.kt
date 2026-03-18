package com.Upermarket.upermarket

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upermarket.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

// --- MODÈLE DE DONNÉES MAGASIN PRO ---
data class Store(
    val name: String,
    val address: String,
    val zipCode: String,
    val city: String,
    val distance: String,
    val openingHours: String,
    val logoRes: Int,
    val hasDrive: Boolean = true,
    val hasDelivery: Boolean = true,
    val hasWalkIn: Boolean = true
)

// --- SERVICE API ADRESSE (REUTILISABLE) ---
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
    var searchQuery by remember { mutableStateOf("") }
    var addressSuggestions by remember { mutableStateOf(listOf<String>()) }
    var selectedMode by remember { mutableStateOf("Drive") }
    val scope = rememberCoroutineScope()

    val demoStores = listOf(
        Store("Upermarket Avermes", "Avenue des Isles", "03000", "Avermes", "1.2 km", "08:30 - 19:30", R.drawable.lidl_logo_svg),
        Store("Upermarket Moulins", "60 Rue de Bourgogne", "03000", "Moulins", "1.8 km", "08:00 - 20:00", R.drawable.carrefour_logo_1982),
        Store("Upermarket Yzeure", "Route Départementale 12", "03400", "Yzeure", "3.6 km", "08:30 - 19:00", R.drawable.e_leclerc_logo_svg)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f)
            .navigationBarsPadding()
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color(0xFF0066CC))
            }
            Text(
                "Trouver un magasin",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0066CC)
            )
        }

        // --- BARRE DE RECHERCHE VILLE/CP ---
        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    scope.launch { addressSuggestions = searchRealAddressesForStore(it) }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ville, Code postal", color = Color.Gray) },
                trailingIcon = {
                    Icon(Icons.Rounded.MyLocation, null, tint = Color(0xFF0066CC))
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0066CC),
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
        }

        // Affichage des suggestions fluides
        AnimatedVisibility(visible = addressSuggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8F9FA),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column {
                    addressSuggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                searchQuery = suggestion
                                addressSuggestions = emptyList()
                            }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF0066CC), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(text = suggestion, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // --- SÉLECTEUR DE MODE ---
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeChip("Drive", Icons.Rounded.DirectionsCar, selectedMode == "Drive") { selectedMode = "Drive" }
            ModeChip("Piéton", Icons.AutoMirrored.Rounded.DirectionsWalk, selectedMode == "Piéton") { selectedMode = "Piéton" }
            ModeChip("Livraison", Icons.Rounded.LocalShipping, selectedMode == "Livraison") { selectedMode = "Livraison" }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE))

        // --- LISTE DES MAGASINS ---
        Text(
            "Magasins à proximité",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(demoStores) { store ->
                StoreCardPro(store, onSelect = { onStoreSelected(store) })
            }
        }
    }
}

@Composable
fun ModeChip(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF0066CC) else Color(0xFFF5F5F5),
        border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) Color.White else Color.Black)
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun StoreCardPro(store: Store, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                        Image(painter = painterResource(id = store.logoRes), contentDescription = null, contentScale = ContentScale.Fit)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = store.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text("${store.address}, ${store.city}", fontSize = 13.sp, color = Color.Gray)
                }
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                    Text(store.distance, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp), tint = Color(0xFF0066CC))
                Spacer(Modifier.width(8.dp))
                Text("Ouvert", color = Color(0xFF00C853), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(" • Ferme à ${store.openingHours.split("-").last().trim()}", fontSize = 13.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE30613)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Choisir ce magasin", fontWeight = FontWeight.Bold)
            }
        }
    }
}
