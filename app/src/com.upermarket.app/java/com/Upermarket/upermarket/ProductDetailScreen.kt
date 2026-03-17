package com.Upermarket.upermarket

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // --- ACTION BAR (FAVORITE) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onToggleFavorite, 
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape, 
                    color = Color(0xFFF5F5F5)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            null, tint = if (isFavorite) Color.Red else Color.DarkGray
                        )
                    }
                }
            }

            // --- IMAGE PRODUIT ---
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // --- INFOS PRINCIPALES ---
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = product.name ?: "Produit inconnu",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                )
                Text(
                    text = "${product.brands ?: "Upermarket"} • ${product.quantity ?: "Format standard"}",
                    style = MaterialTheme.typography.titleMedium, 
                    color = Color.Gray
                )
            }

            // --- SCORES (NUTRI/ECO) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScoreCard("NUTRI-SCORE", product.nutriscore?.uppercase() ?: "?", getNutriColor(product.nutriscore), Modifier.weight(1f))
                ScoreCard("ECO-SCORE", product.ecoscore?.uppercase() ?: "?", getEcoColor(product.ecoscore), Modifier.weight(1f))
            }

            // --- SECTION ANALYSE NUTRITIONNELLE ---
            if (product.levels != null) {
                DetailSectionHeader("Analyse nutritionnelle")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NutrientBadge("Sucre", product.levels.sugars ?: "inconnu")
                    NutrientBadge("Sel", product.levels.salt ?: "inconnu")
                    NutrientBadge("Gras", product.levels.fat ?: "inconnu")
                }
            }

            // --- SECTION INGRÉDIENTS (MODERNE) ---
            DetailSectionHeader("Ingrédients")
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                color = Color(0xFFF8F9FA),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Text(
                    text = product.ingredients ?: "La liste des ingrédients n'est pas disponible pour ce produit.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = Color.DarkGray
                )
            }

            // --- SECTION ADDITIFS ---
            if ((product.additivesCount ?: 0) > 0) {
                DetailSectionHeader("Additifs (${product.additivesCount})")
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    color = if ((product.additivesCount ?: 0) > 3) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if ((product.additivesCount ?: 0) > 3) Icons.Rounded.Warning else Icons.Rounded.Info, 
                            null, 
                            tint = if ((product.additivesCount ?: 0) > 3) Color.Red else Color(0xFF4CAF50)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Ce produit contient ${product.additivesCount} additifs.",
                            fontWeight = FontWeight.Bold,
                            color = if ((product.additivesCount ?: 0) > 3) Color.Red else Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // --- SECTION PRIX & AJOUT ---
            HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), color = Color(0xFFEEEEEE))
            
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { if (it.length <= 7) priceInput = it },
                    label = { Text("Prix constaté (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Icon(Icons.Rounded.EuroSymbol, null, tint = Color(0xFF00C853)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C853),
                        focusedLabelColor = Color(0xFF00C853)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(if (price != null && price > 0) 12.dp else 0.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    )
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AddShoppingCart, null)
                        Spacer(Modifier.width(12.dp))
                        Text("AJOUTER AU PANIER", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun NutrientBadge(label: String, level: String) {
    val color = when(level.lowercase()) {
        "low" -> Color(0xFF4CAF50)
        "moderate" -> Color(0xFFFFB300)
        "high" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ScoreCard(label: String, score: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier, 
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF8F9FA), 
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = score, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color)
        }
    }
}

fun getNutriColor(score: String?): Color = when(score?.lowercase()) {
    "a" -> Color(0xFF038141); "b" -> Color(0xFF85BB2F); "c" -> Color(0xFFFEC902); "d" -> Color(0xFFEE8100); "e" -> Color(0xFFE63E11); else -> Color.Gray
}

fun getEcoColor(score: String?): Color = when(score?.lowercase()) {
    "a" -> Color(0xFF038141); "b" -> Color(0xFF85BB2F); "c" -> Color(0xFFFEC902); "d" -> Color(0xFFEE8100); "e" -> Color(0xFFE63E11); else -> Color.Gray
}
