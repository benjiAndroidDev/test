package com.example.upermarket

import android.content.Context
import android.util.Log
import com.Upermarket.upermarket.FastProductManager
import com.Upermarket.upermarket.LocalProductDatabase
import com.Upermarket.upermarket.OpenFoodFactsApi
import com.Upermarket.upermarket.ProductResponse

sealed interface ConnectionState {
    data object Online : ConnectionState
    data object Offline : ConnectionState
    data class OfflineWithCache(val cachedProducts: Int) : ConnectionState
}

data class CacheStats(
    val isOnline: Boolean,
    val totalProducts: Int
)

class OfflineProductManager(private val context: Context, private val api: OpenFoodFactsApi? = null) {

    // Nouvelle API rapide
    private val fastProductManager = FastProductManager(context)

    companion object {
        private const val TAG = "OfflineProductManager"
    }

    suspend fun getProductByBarcode(barcode: String): ProductResponse? {
        return try {
            val startTime = System.currentTimeMillis()

            // Utiliser la nouvelle API rapide avec base locale + APIs multiples
            val result = fastProductManager.getProductByBarcode(barcode)

            val responseTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Product search completed in ${responseTime}ms for barcode: $barcode")

            result

        } catch (e: Exception) {
            Log.e(TAG, "Error getting product for barcode $barcode: ${e.message}")
            null
        }
    }

    fun getCacheStats(): CacheStats {
        val localProductsCount = LocalProductDatabase.getAllBarcodes().size
        return CacheStats(
            isOnline = true,
            totalProducts = localProductsCount
        )
    }
}