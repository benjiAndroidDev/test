package com.Upermarket.upermarket

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    authManager: AuthManager,
    cartViewModel: CartViewModel,
    favoritesViewModel: FavoritesViewModel,
    scanHistoryManager: ScanHistoryManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        composable(Destination.SEARCH.route) {
            SearchScreen(favoritesViewModel, cartViewModel)
        }
        composable(Destination.SCAN.route) {
            ScanScreen(cartViewModel, favoritesViewModel, scanHistoryManager,)
        }
        composable(Destination.VIP.route) {
            VipScreen(authManager)
        }
        composable(Destination.SETTINGS.route) {
        }
    }
}
