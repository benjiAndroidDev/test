package com.example.upermarket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==================== TEST BARCODE SIMULATOR ====================

data class TestProduct(
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String
)

object TestBarcodes {
    val products = listOf(
        TestProduct("5000112548136", "Coca-Cola Original", "Coca-Cola", "Boissons"),
        TestProduct("3033710065967", "Danette Vanille", "Danone", "Desserts"),
        TestProduct("3228857000906", "Pain de mie American sandwich", "Harry's", "Boulangerie"),
        TestProduct("3017624010701", "Nutella", "Ferrero", "Petit déjeuner"),
        TestProduct("3068320055503", "Evian Eau minérale naturelle", "Evian", "Boissons"),
        TestProduct("80176756", "Kinder Bueno", "Ferrero", "Confiserie"),
        TestProduct("3228021170008", "Beurre doux", "Président", "Frais"),
        // Codes supplémentaires pour tester la rapidité
        TestProduct("3274080005003", "Président Emmental", "Président", "Frais"),
        TestProduct("3029330003533", "Badoit", "Badoit", "Boissons"),
        TestProduct("3168930010883", "Activia Nature", "Danone", "Frais"),
        TestProduct("3124480186386", "Oasis Tropical", "Oasis", "Boissons"),
        TestProduct("87157126", "Ketchup Tomato", "Heinz", "Condiments"),
        TestProduct("7622210139276", "Prince Chocolat", "LU", "Biscuits"),
        TestProduct("3274080011208", "Perrier", "Perrier", "Boissons"),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestBarcodeScreen(
    onBarcodeSelected: (String) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var selectedBarcode by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🧪 Simulateur de codes-barres",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(0.1f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "💡 Mode Test",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Text(
                    "Cliquez sur un produit pour simuler un scan instantané",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2).copy(0.7f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Selected barcode display
        selectedBarcode?.let { barcode ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(0.1f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📱 Code sélectionné:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )
                    Text(
                        barcode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF388E3C)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onBarcodeSelected(barcode) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("▶ Simuler le scan", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Products list
        Text(
            "Produits disponibles:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TestBarcodes.products) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedBarcode = product.barcode },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedBarcode == product.barcode) {
                            MaterialTheme.colorScheme.primary.copy(0.1f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedBarcode == product.barcode) 4.dp else 1.dp
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.name,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                                Text(
                                    product.brand,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    product.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Surface(
                                color = Color.Gray.copy(0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    product.barcode,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}