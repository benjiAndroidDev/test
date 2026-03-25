package com.Upermarket.upermarket

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItem(val id: Int, val name: String, val isChecked: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListSheet(
    viewModel: ShoppingListViewModel,
    onDismiss: () -> Unit
) {
    var newItemName by remember { mutableStateOf("") }
    val items = viewModel.items

    Column(
        modifier = Modifier
            .fillMaxHeight(0.85f)
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Ma Liste", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("${items.count { !it.isChecked }} articles restants", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (items.any { it.isChecked }) {
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text("Tout effacer", color = Color.Red)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))

        // Input Area Premium
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8F9FA)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    placeholder = { Text("Ex: Baguette, Pommes...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                IconButton(
                    onClick = { 
                        if (newItemName.isNotBlank()) {
                            viewModel.addItem(newItemName)
                            newItemName = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black, CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Color.White)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Rounded.ListAlt, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("Votre liste est vide", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ShoppingRow(
                        item = item,
                        onCheckedChange = { viewModel.toggleItem(item.id) },
                        onDelete = { viewModel.removeItem(item.id) }
                    )
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("FERMER", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShoppingRow(
    item: ShoppingItem, 
    onCheckedChange: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (item.isChecked) Color(0xFFF1F8E9) else Color(0xFFF8F9FA),
        label = "bg"
    )

    Surface(
        onClick = onCheckedChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (item.isChecked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null,
                tint = if (item.isChecked) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = item.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.SemiBold
                ),
                color = if (item.isChecked) Color.Gray else Color.Black
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}
