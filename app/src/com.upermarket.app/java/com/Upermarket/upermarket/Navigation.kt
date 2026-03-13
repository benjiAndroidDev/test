package com.Upermarket.upermarket

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

// Icônes modernes 2026 - Vraies icônes Material 3
enum class Destination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val contentDescription: String
) {
    HOME(
        "home",
        Icons.Rounded.Home,
        Icons.Outlined.Home,
        "Accueil",
        "Accueil"
    ),
    SEARCH(
        "search",
        Icons.Rounded.TravelExplore,
        Icons.Outlined.TravelExplore,
        "Explorer",
        "Explorer"
    ),
    SCAN(
        "scan",
        Icons.Rounded.QrCodeScanner,
        Icons.Outlined.QrCodeScanner,
        "Scanner",
        "Scanner"
    ),
    VIP(
        "vip",
        Icons.Rounded.WorkspacePremium,
        Icons.Outlined.WorkspacePremium,
        "Membre",
        "Devenir membre"
    ),
    SETTINGS(
        "settings",
        Icons.Rounded.Settings,
        Icons.Outlined.Settings,
        "Paramètres",
        "Paramètres"
    )
}
