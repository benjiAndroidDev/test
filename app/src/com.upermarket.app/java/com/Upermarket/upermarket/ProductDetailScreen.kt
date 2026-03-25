package com.Upermarket.upermarket

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToCart: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var priceInput by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val price = priceInput.replace(",", ".").toFloatOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = null // On gère notre propre drag handle pour plus de style
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
        ) {
            // --- HEADER AVEC IMAGE ET OVERLAY ---
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                // Background Gradient léger
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color.White))
                ))
                
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    contentScale = ContentScale.Fit
                )

                // Drag Handle Custom
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(40.dp, 4.dp)
                        .background(Color.LightGray.copy(0.5f), CircleShape)
                        .align(Alignment.TopCenter)
                )

                // Actions flottantes (Back & Favorite)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.White.copy(0.9f), CircleShape).size(44.dp)
                    ) {
                        Icon(Icons.Rounded.Close, null)
                    }
                    
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.background(Color.White.copy(0.9f), CircleShape).size(44.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            null, 
                            tint = if (isFavorite) Color.Red else Color.Black
                        )
                    }
                }
            }

            // --- CONTENU PRINCIPAL ---
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // Titre et Marque
                Text(
                    text = product.brands?.uppercase() ?: "UPERMARKET",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1A73E8),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.name ?: "Produit inconnu",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 32.sp
                    )
                )
                Text(
                    text = product.quantity ?: "Format standard",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )

                Spacer(Modifier.height(24.dp))

                // --- SCORES EN ROW MODERNISÉ ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScoreCardPro("NUTRI-SCORE", product.nutriscore?.uppercase() ?: "?", getNutriColor(product.nutriscore), Modifier.weight(1f))
                    ScoreCardPro("ECO-SCORE", product.ecoscore?.uppercase() ?: "?", getEcoColor(product.ecoscore), Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))

                // --- ANALYSE NUTRITIONNELLE ---
                if (product.levels != null) {
                    Text("Analyse nutritionnelle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NutrientBadgePro("Sucre", product.levels.sugars ?: "inconnu", Modifier.weight(1f))
                        NutrientBadgePro("Sel", product.levels.salt ?: "inconnu", Modifier.weight(1f))
                        NutrientBadgePro("Gras", product.levels.fat ?: "inconnu", Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(32.dp))

                // --- INGRÉDIENTS & ADDITIFS (DESIGN ÉPURÉ) ---
                Text("Détails du produit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                
                DetailTile(
                    icon = Icons.Rounded.RestaurantMenu,
                    title = "Ingrédients",
                    content = product.ingredients ?: "Non disponibles",
                    color = Color(0xFF1A73E8)
                )
                
                if ((product.additivesCount ?: 0) > 0) {
                    Spacer(Modifier.height(12.dp))
                    DetailTile(
                        icon = Icons.Rounded.WarningAmber,
                        title = "Additifs",
                        content = "Contient ${product.additivesCount} additifs identifiés",
                        color = if ((product.additivesCount ?: 0) > 3) Color(0xFFD32F2F) else Color(0xFFF57C00)
                    )
                }

                Spacer(Modifier.height(40.dp))

                // --- FOOTER FIXE : PRIX & BOUTON ---
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF8F9FA),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Input Prix
                        Box(modifier = Modifier.width(80.dp)) {
                            BasicTextField(
                                value = priceInput,
                                onValueChange = { if (it.length <= 6) priceInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                ),
                                decorationBox = { innerTextField ->
                                    if (priceInput.isEmpty()) Text("0.00", color = Color.LightGray, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                    innerTextField()
                                }
                            )
                        }
                        
                        Text("€", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        
                        Spacer(Modifier.width(12.dp))

                        // Bouton Ajouter
                        Button(
                            onClick = { 
                                if (price != null && price > 0 && !isAdding) {
                                    isAdding = true
                                    scope.launch {
                                        onAddToCart(price)
                                        isAdding = false
                                    }
                                }
                            },
                            enabled = price != null && price > 0 && !isAdding,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            if (isAdding) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.AddShoppingCart, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("AJOUTER", fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreCardPro(label: String, score: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier, 
        shape = RoundedCornerShape(24.dp),
        color = color.copy(alpha = 0.05f), 
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(text = score, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun NutrientBadgePro(label: String, level: String, modifier: Modifier = Modifier) {
    val color = when(level.lowercase()) {
        "low" -> Color(0xFF00C853)
        "moderate" -> Color(0xFFFFB300)
        "high" -> Color(0xFFFF5252)
        else -> Color.Gray
    }
    Surface(
        modifier = modifier,
        color = Color(0xFFF8F9FA),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(
                text = when(level.lowercase()) { "low" -> "Faible"; "moderate" -> "Moyen"; "high" -> "Élevé"; else -> "?" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun DetailTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = color.copy(alpha = 0.1f)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                lineHeight = 20.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getNutriColor(score: String?): Color = when(score?.lowercase()) {
    "a" -> Color(0xFF038141); "b" -> Color(0xFF85BB2F); "c" -> Color(0xFFFEC902); "d" -> Color(0xFFEE8100); "e" -> Color(0xFFE63E11); else -> Color.Gray
}

fun getEcoColor(score: String?): Color = when(score?.lowercase()) {
    "a" -> Color(0xFF038141); "b" -> Color(0xFF85BB2F); "c" -> Color(0xFFFEC902); "d" -> Color(0xFFEE8100); "e" -> Color(0xFFE63E11); else -> Color.Gray
}
