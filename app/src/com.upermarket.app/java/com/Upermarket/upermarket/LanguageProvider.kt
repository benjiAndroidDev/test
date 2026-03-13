package com.Upermarket.upermarket

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val displayName: String) {
    FRENCH("Français (FR)"),
    ENGLISH("English (US)"),
    SPANISH("Español (ES)")
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.FRENCH }

object Translations {
    private val data = mapOf(
        AppLanguage.FRENCH to mapOf(
            "home" to "Accueil",
            "search" to "Recherche",
            "scan" to "Scanner",
            "member" to "Membre",
            "settings" to "Paramètres",
            "cart" to "Mon Panier",
            "favorites" to "Mes Favoris",
            "budget" to "Budget",
            "history" to "Historique",
            "add_to_cart" to "AJOUTER AU PANIER",
            "price_placeholder" to "Prix du produit (€)",
            "welcome" to "Bonjour",
            "sections" to "Rayons",
            "brands" to "Nos Enseignes",
            "logout" to "Se déconnecter"
        ),
        AppLanguage.ENGLISH to mapOf(
            "home" to "Home",
            "search" to "Search",
            "scan" to "Scan",
            "member" to "Member",
            "settings" to "Settings",
            "cart" to "My Cart",
            "favorites" to "Favorites",
            "budget" to "Budget",
            "history" to "History",
            "add_to_cart" to "ADD TO CART",
            "price_placeholder" to "Product Price ($)",
            "welcome" to "Hello",
            "sections" to "Categories",
            "brands" to "Our Stores",
            "logout" to "Log out"
        ),
        AppLanguage.SPANISH to mapOf(
            "home" to "Inicio",
            "search" to "Buscar",
            "scan" to "Escanear",
            "member" to "Miembro",
            "settings" to "Ajustes",
            "cart" to "Mi Carrito",
            "favorites" to "Mis Favoritos",
            "budget" to "Presupuesto",
            "history" to "Historial",
            "add_to_cart" to "AÑADIR AL CARRITO",
            "price_placeholder" to "Precio del producto (€)",
            "welcome" to "Hola",
            "sections" to "Categorías",
            "brands" to "Nuestras Tiendas",
            "logout" to "Cerrar sesión"
        )
    )

    fun get(key: String, language: AppLanguage): String {
        return data[language]?.get(key) ?: key
    }
}
