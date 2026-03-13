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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onToggleFavorite, modifier = Modifier.size(48.dp),
                    shape = CircleShape, color = Color(0xFFF5F5F5)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            null, tint = if (isFavorite) Color.Red else Color.DarkGray
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = product.name ?: "Produit inconnu",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            )
            Text(
                text = "${product.brands ?: "Marque inconnue"} • ${product.quantity ?: "Format NC"}",
                style = MaterialTheme.typography.titleMedium, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ScoreCard("NUTRI-SCORE", product.nutriscore?.uppercase() ?: "?", getNutriColor(product.nutriscore), Modifier.weight(1f))
                ScoreCard("ECO-SCORE", product.ecoscore?.uppercase() ?: "?", getEcoColor(product.ecoscore), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = priceInput,
                onValueChange = { if (it.length <= 7) priceInput = it },
                label = { Text("Prix du produit (€)") },
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

            // Bouton Ajouter au Panier - COULEUR DE TEXTE CORRIGÉE (WHITE)
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
                    .height(60.dp)
                    .shadow(if (price != null && price > 0) 8.dp else 0.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853),
                    contentColor = Color.White, // Texte en blanc pur pour une visibilité maximale
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color.Gray
                )
            ) {
                if (isAdding) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.AddShoppingCart, null, tint = Color.White)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "AJOUTER AU PANIER", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Black,
                        color = Color.White // Forçage de la couleur blanche
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ScoreCard(label: String, score: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier, shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF8F9FA), border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
