package com.Upermarket.upermarket

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

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
        composable(Destination.HOME.route) {
            HomeScreen(favoritesViewModel, cartViewModel, navController)
        }
        composable(Destination.SEARCH.route) {
            SearchScreen(favoritesViewModel, cartViewModel)
        }
        composable(Destination.SCAN.route) {
            ScanScreen(cartViewModel, favoritesViewModel, scanHistoryManager)
        }
        composable(Destination.CHEF.route) {
            ChefScreen()
        }
        composable(Destination.SETTINGS.route) {
            SettingsScreen(authManager, false, {})
        }
        
        // Route pour les produits par catégorie
        composable(
            route = "category_products/{categoryName}/{categoryTag}",
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType },
                navArgument("categoryTag") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            val categoryTag = backStackEntry.arguments?.getString("categoryTag") ?: ""
            CategoryProductsScreen(
                categoryName = categoryName,
                categoryTag = categoryTag,
                favoritesViewModel = favoritesViewModel,
                cartViewModel = cartViewModel,
                navController = navController
            )
        }
    }
}
