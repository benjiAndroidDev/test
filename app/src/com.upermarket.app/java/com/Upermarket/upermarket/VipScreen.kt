package com.Upermarket.upermarket

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VipScreen(authManager: AuthManager) {
    val user = authManager.getCurrentUser()
    val isVip = user?.isVip ?: false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER PREMIUM ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1A1A1A), Color(0xFF2D2D2D))
                    )
                )
                .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Text(
                    text = "ESPACE MEMBRE",
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bonjour, ${user?.name ?: "Membre"}",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- CARTE DE FIDÉLITÉ ---
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        colors = listOf(Color.White, Color(0xFFFDFCF0))
                    )
                ))
                
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFFFFD700))
                        Text("UPERMARKET GOLD", fontWeight = FontWeight.Black, color = Color.LightGray)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.size(80.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("ID: ${user?.uid?.take(8)?.uppercase() ?: "INVITÉ"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(if(isVip) "MEMBRE VIP" else "MEMBRE STANDARD", fontWeight = FontWeight.Bold, color = if(isVip) Color(0xFF00C853) else Color.Black)
                        }
                    }
                }
            }
        }

        // --- SECTION : GARDER MA PLACE (PREMIER PLAN) ---
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFFDE7), Color.White)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFFB8860B), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "EXCLUSIF",
                            color = Color(0xFFB8860B),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Garder ma place au premier plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        "Soyez mis en avant dans la communauté pour seulement 1$.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { /* Action 1$ */ },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Text("RÉSERVER POUR 1$", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Vos Avantages",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            BenefitItem(Icons.Rounded.Percent, "Réductions exclusives", "Jusqu'à -15% sur vos rayons favoris.")
            BenefitItem(Icons.Rounded.Bolt, "Scan & Go Ultra-Rapide", "Évitez l'attente en caisse grâce au scan pro.")
            BenefitItem(Icons.Rounded.LocalActivity, "Événements VIP", "Accès prioritaire aux nouvelles collections.")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOUTON D'ACTION PRINCIPAL ---
        Box(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            if (!isVip) {
                Button(
                    onClick = { /* Action rejoindre */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1A1A1A), Color(0xFF424242))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD700))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "REJOINDRE L'ESPACE MEMBRE",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { /* Gérer abonnement */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, Color.Black)
                ) {
                    Text(
                        "GÉRER MON ESPACE MEMBRE",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun BenefitItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF00C853), modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
