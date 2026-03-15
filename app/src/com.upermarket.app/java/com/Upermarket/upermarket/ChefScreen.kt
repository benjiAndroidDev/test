package com.Upermarket.upermarket

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upermarket.R

data class Message(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefScreen() {
    var messages by remember { mutableStateOf(listOf(Message("Bonjour ! Je suis UperChef. De quoi as-tu envie aujourd'hui ? Je peux te proposer des recettes avec ce que tu as dans ton frigo !", false))) }
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.uperchef),
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("UperChef", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Votre assistant cuisine IA", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        // Chat Area
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        // Input Area
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(12.dp).navigationBarsPadding().imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Demandez une recette...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00C853),
                        unfocusedBorderColor = Color.LightGray
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userMsg = inputText
                            messages = messages + Message(userMsg, true)
                            inputText = ""
                            // Mock AI Response
                            generateAiResponse(userMsg) { aiMsg ->
                                messages = messages + Message(aiMsg, false)
                            }
                        }
                    },
                    modifier = Modifier.background(Color(0xFF00C853), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (message.isUser) Color(0xFF00C853) else Color(0xFFF1F1F1),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) Color.White else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

fun generateAiResponse(input: String, onResponse: (String) -> Unit) {
    // Simple logic for mock recipes
    val response = when {
        input.contains("pâtes", ignoreCase = true) -> "Pour des pâtes express, je te conseille des pâtes à l'ail et au piment ! Il te faut : pâtes, ail, huile d'olive et un peu de parmesan. Fais revenir l'ail dans l'huile, ajoute les pâtes cuites et hop !"
        input.contains("poulet", ignoreCase = true) -> "Un poulet curry coco ? Émince le poulet, fais-le dorer, ajoute du lait de coco et du curry. Sers ça avec du riz, c'est un régal !"
        input.contains("salade", ignoreCase = true) -> "Une salade grecque : Tomates, concombres, feta, olives et oignons rouges. Un filet d'huile d'olive et c'est prêt !"
        else -> "C'est une super idée ! Pour cette recette, commence par préparer tes ingrédients frais. Veux-tu que je cherche les prix de ces articles chez Upermarket ?"
    }
    onResponse(response)
}
