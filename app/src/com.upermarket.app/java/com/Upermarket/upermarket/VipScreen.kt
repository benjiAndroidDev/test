package com.Upermarket.upermarket

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
    
    val infiniteTransition = rememberInfiniteTransition(label = "hologram")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Fond noir profond pro
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER FUTURISTE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, bottom = 20.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "✧ UPERMARKET ELITE 2026",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "L'expérience\nsans limites.",
                    color = Color.White,
                    lineHeight = 40.sp,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // --- CARTE HOLOGRAPHIQUE DYNAMIQUE ---
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .height(220.dp)
                .rotate(rotation) // Effet de flottement
                .shadow(30.dp, RoundedCornerShape(28.dp), ambientColor = Color(0xFFFFD700), spotColor = Color(0xFFFFD700))
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1E1E1E),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFFFD700))))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Effet de texture grainée / brillante
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD700).copy(alpha = 0.15f), Color.Transparent),
                            radius = 600f
                        )
                    ))
                    
                    Column(
                        modifier = Modifier.padding(28.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AllInclusive, null, tint = Color(0xFFFFD700), modifier = Modifier.size(32.dp))
                            Text("GOLD PASS", fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f), letterSpacing = 2.sp)
                        }
                        
                        Column {
                            Text(
                                text = user?.name?.uppercase() ?: "INVITÉ SPÉCIAL",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "MEMBRE DEPUIS 2025",
                                color = Color(0xFFFFD700).copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "•••• •••• •••• ${user?.uid?.take(4) ?: "0000"}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 16.sp
                            )
                            Icon(Icons.Rounded.Nfc, null, tint = Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // --- SECTION : OFFRE EXCLUSIVE 1$ ---
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF151515),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(Color(0xFFFFD700), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PriorityHigh, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("BOOST DE VISIBILITÉ", color = Color(0xFFFFD700), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("Garder ma place au premier plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Devenez prioritaire pour 1$", color = Color.Gray, fontSize = 13.sp)
                }
                Surface(
                    modifier = Modifier.clickable { },
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ACTIVER", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- GRID D'AVANTAGES PRO ---
        Text(
            "Privilèges Elite",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ModernBenefitItem(Icons.Rounded.Bolt, "Priorité Absolue", "Zéro attente en caisse et livraison éclair.")
            ModernBenefitItem(Icons.Rounded.AutoGraph, "Cashback 5%", "Cumulez de l'argent sur chaque achat.")
            ModernBenefitItem(Icons.Rounded.SupportAgent, "Conciergerie 24/7", "Un assistant personnel pour vos courses.")
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- BOUTON D'ACTION MAGNÉTIQUE ---
        Box(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
            Button(
                onClick = { /* Action */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Color.Black)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if(isVip) "ACCÉDER AU DASHBOARD" else "DEVENIR MEMBRE ELITE",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun ModernBenefitItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFFFFD700), modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
