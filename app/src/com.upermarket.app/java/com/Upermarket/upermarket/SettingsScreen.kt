package com.Upermarket.upermarket

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val translations = mapOf(
    "Français (FR)" to mapOf(
        "settings" to "Paramètres",
        "account" to "Compte",
        "personal_info" to "Informations personnelles",
        "security" to "Sécurité",
        "notifications" to "Notifications",
        "prefs" to "Préférences",
        "dark_mode" to "Mode sombre",
        "language" to "Langue de l'app",
        "assistance" to "Assistance",
        "help_center" to "Centre d'aide",
        "legal" to "Mentions légales",
        "logout" to "Se déconnecter"
    ),
    "English (US)" to mapOf(
        "settings" to "Settings",
        "account" to "Account",
        "personal_info" to "Personal Information",
        "security" to "Security",
        "notifications" to "Notifications",
        "prefs" to "Preferences",
        "dark_mode" to "Dark Mode",
        "language" to "App Language",
        "assistance" to "Support",
        "help_center" to "Help Center",
        "legal" to "Legal Mentions",
        "logout" to "Logout"
    )
)

@Composable
fun SettingsScreen(
    authManager: AuthManager,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val user = authManager.getCurrentUser()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Français (FR)") }
    var showLegalDialog by remember { mutableStateOf(false) }

    val t = translations[selectedLanguage] ?: translations["Français (FR)"]!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Text(
            text = t["settings"] ?: "Paramètres",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // --- SECTION : COMPTE ---
        SettingsSection(title = t["account"] ?: "Compte") {
            SettingsItem(
                icon = Icons.Rounded.Person,
                title = t["personal_info"] ?: "Infos",
                subtitle = user?.name ?: user?.email,
                onClick = { Toast.makeText(context, user?.email, Toast.LENGTH_SHORT).show() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION : PRÉFÉRENCES ---
        SettingsSection(title = t["prefs"] ?: "Préférences") {
            SettingsToggleItem(
                icon = Icons.Rounded.DarkMode,
                title = t["dark_mode"] ?: "Mode sombre",
                subtitle = "Optimiser l'affichage",
                value = isDarkMode,
                onValueChange = onDarkModeChange
            )
            SettingsItem(
                icon = Icons.Rounded.Language,
                title = t["language"] ?: "Langue",
                subtitle = selectedLanguage,
                onClick = { showLanguageDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION : ASSISTANCE ---
        SettingsSection(title = t["assistance"] ?: "Assistance") {
            SettingsItem(
                icon = Icons.AutoMirrored.Rounded.Help,
                title = t["help_center"] ?: "Aide",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:upermarket@upermarketapp.com")
                    }
                    context.startActivity(intent)
                }
            )
            SettingsItem(icon = Icons.Rounded.Gavel, title = t["legal"] ?: "Légal", onClick = { showLegalDialog = true })
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOUTON DÉCONNEXION ---
        Button(
            onClick = { authManager.signOut() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(t["logout"] ?: "Déconnexion", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Langue", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    translations.keys.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedLanguage = lang
                                    showLanguageDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = lang == selectedLanguage, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(lang, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Annuler", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            title = { Text("Mentions Légales", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Éditeur : Upermarket App\nHébergement : Firebase\n\nPropriété de Upermarket.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) {
                    Text("Fermer", color = Color(0xFF00C853))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title, 
            style = MaterialTheme.typography.labelLarge, 
            color = MaterialTheme.colorScheme.primary, 
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) { 
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp), 
            shape = CircleShape, 
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) { 
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) 
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun SettingsToggleItem(icon: ImageVector, title: String, subtitle: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp), 
            shape = CircleShape, 
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) { 
                Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) 
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = value, 
            onCheckedChange = onValueChange, 
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00C853))
        )
    }
}
