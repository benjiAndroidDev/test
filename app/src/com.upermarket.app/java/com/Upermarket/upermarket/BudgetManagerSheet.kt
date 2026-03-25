package com.Upermarket.upermarket

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BudgetManagerSheet(
    onDismiss: () -> Unit,
    cartViewModel: CartViewModel,
    context: Context
) {
    var budgetInput by remember { mutableStateOf(cartViewModel.userMaxBudget.toString()) }
    val currentTotal = cartViewModel.totalPrice
    val maxBudget = budgetInput.toFloatOrNull() ?: 100f
    val progress by animateFloatAsState(targetValue = (currentTotal / maxBudget).coerceIn(0f, 1.1f), label = "progress")
    val remaining = (maxBudget - currentTotal)
    
    val statusColor by animateColorAsState(
        targetValue = when {
            progress > 1f -> Color(0xFFD32F2F) // Red
            progress > 0.8f -> Color(0xFFFBC02D) // Yellow/Orange
            else -> Color(0xFF00C853) // Green
        }, label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AccountBalanceWallet, null, tint = statusColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Gestion du Budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Optimisez vos dépenses", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- DASHBOARD VISUEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text("Dépenses", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text("${String.format("%.2f", currentTotal)} €", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Budget", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text("${String.format("%.0f", maxBudget)} €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Barre de progression améliorée
                Box(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress.coerceAtMost(1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        color = statusColor,
                        trackColor = Color.LightGray.copy(alpha = 0.2f),
                    )
                }
                
                if (progress > 1f) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Budget dépassé de ${String.format("%.2f", currentTotal - maxBudget)} €", color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(20.dp))
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (remaining >= 0) "Reste disponible" else "Dépassement", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${String.format("%.2f", if (remaining >= 0) remaining else -remaining)} €", 
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold, 
                        color = if (remaining < 0) Color.Red else if (remaining < 10) Color(0xFFFBC02D) else Color(0xFF00C853)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- MODIFICATION DU BUDGET ---
        Text("Ajuster mon budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Presets
        val presets = listOf(30f, 50f, 100f, 150f, 200f)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { amount ->
                FilterChip(
                    selected = maxBudget == amount,
                    onClick = { 
                        budgetInput = amount.toInt().toString()
                        cartViewModel.updateBudget(amount)
                    },
                    label = { Text("${amount.toInt()}€") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Black,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = budgetInput,
            onValueChange = { 
                budgetInput = it
                it.toFloatOrNull()?.let { newBudget -> cartViewModel.updateBudget(newBudget) }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Budget personnalisé") },
            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Color.Gray) },
            suffix = { Text("€", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                focusedLabelColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("CONFIRMER", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
