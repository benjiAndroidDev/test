package com.Upermarket.upermarket

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val progress = (currentTotal / maxBudget).coerceIn(0f, 1f)
    val remaining = (maxBudget - currentTotal).coerceAtLeast(0f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color(0xFF00C853), modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text("Gestion du Budget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- DASHBOARD VISUEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dépenses actuelles", color = Color.Gray)
                    Text("${String.format("%.2f", currentTotal)} €", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                
                // Barre de progression pro
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = if (progress > 0.9f) Color.Red else Color(0xFF00C853),
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                )
                
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Reste à dépenser", color = Color.Gray)
                    Text("${String.format("%.2f", remaining)} €", fontWeight = FontWeight.ExtraBold, color = if (remaining < 10) Color.Red else Color(0xFF00C853))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- MODIFICATION DU BUDGET ---
        Text("Définir votre budget max", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = budgetInput,
            onValueChange = { 
                budgetInput = it
                it.toFloatOrNull()?.let { newBudget -> cartViewModel.updateBudget(newBudget) }
            },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Color.Gray) },
            suffix = { Text("€", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00C853),
                focusedLabelColor = Color(0xFF00C853)
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("ENREGISTRER", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
