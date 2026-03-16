package com.Upermarket.upermarket

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.upermarket.R
import java.util.Locale

@Composable
fun CartSheet(cartViewModel: CartViewModel, favoritesViewModel: FavoritesViewModel) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.95f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- HEADER MODERNE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    "Mon Panier",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.Black
                )
                Text(
                    "${cartViewModel.itemCount} articles sélectionnés",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
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
                    ModernCartProductCard(
                        item = item,
                        cartViewModel = cartViewModel,
                        onProductClick = { selectedProduct = item.product }
                    )
                }
            }

            // --- SECTION RÉSUMÉ & LIVRAISON ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    // SÉLECTEUR DE LIVRAISON (Nouvelle place stratégique)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable { /* Action Livraison */ }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(64.dp),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Utilisation d'une icône Material au lieu d'une ressource manquante
                                Icon(
                                    imageVector = Icons.Rounded.LocalShipping,
                                    contentDescription = "Delivery",
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF00C853)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Option de livraison", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Rapide & Sécurisée", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray)
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Total à régler", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${String.format(Locale.FRANCE, "%.2f", cartViewModel.totalPrice)} €",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.Black
                            )
                        }
                        
                        Button(
                            onClick = { /* Checkout */ },
                            modifier = Modifier
                                .height(60.dp)
                                .widthIn(min = 160.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text("Payer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    if (selectedProduct != null) {
        ProductDetailSheet(
            product = selectedProduct!!,
            isFavorite = favoritesViewModel.isFavorite(selectedProduct!!),
            onToggleFavorite = { favoritesViewModel.toggleFavorite(selectedProduct!!) },
            onAddToCart = { price -> 
                cartViewModel.addToCart(selectedProduct!!, price)
                selectedProduct = null
            },
            onDismiss = { selectedProduct = null }
        )
    }
}

@Composable
fun ModernCartProductCard(
    item: CartItem,
    cartViewModel: CartViewModel,
    onProductClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProductClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Produit
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF9F9F9)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(item.product.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.width(16.dp))

            // Infos Produit
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name ?: "Produit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.product.brands ?: "Upermarket",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${String.format(Locale.FRANCE, "%.2f", item.price)} €",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF00C853)
                )
            }

            // Sélecteur Quantité Vertical / Moderne
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp)
            ) {
                IconButton(
                    onClick = { cartViewModel.updateQuantity(item.product, item.quantity + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                }
                
                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                
                IconButton(
                    onClick = { cartViewModel.updateQuantity(item.product, item.quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                }
            }
        }
    }
}

@Composable
fun EmptyCartView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color(0xFFF5F5F5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.ShoppingBag,
                        null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.LightGray
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Votre panier est vide",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Commencez vos achats maintenant",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
