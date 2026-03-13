package com.Upermarket.upermarket

import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * SYSTÈME D'API AMÉLIORÉ - MULTI-SOURCES
 *
 * Sources :
 * 1. Open Food Facts (base de données mondiale)
 * 2. Base locale pour les produits fréquents
 * 3. Cache intelligent
 */

// ==================== MODELS ====================

@Serializable
data class ImprovedProduct(
    @SerializedName("code") @SerialName("code") val code: String? = null,
    @SerializedName("product_name") @SerialName("product_name") val name: String? = null,
    @SerializedName("brands") @SerialName("brands") val brands: String? = null,
    @SerializedName("quantity") @SerialName("quantity") val quantity: String? = null,
    @SerializedName("image_url") @SerialName("image_url") val imageUrl: String? = null,
    @SerializedName("nutriscore_grade") @SerialName("nutriscore_grade") val nutriscore: String? = null,
    @SerializedName("categories") @SerialName("categories") val categories: String? = null,
    @SerializedName("stores") @SerialName("stores") val stores: String? = null,
    @SerializedName("countries") @SerialName("countries") val countries: String? = null,

    // Champs locaux
    @kotlinx.serialization.Transient var isFavorite: Boolean = false,
    @kotlinx.serialization.Transient var localPrice: Double? = null,
    @kotlinx.serialization.Transient var lastScanned: Long? = null
)

@Serializable
data class ImprovedProductResponse(
    @SerializedName("status") @SerialName("status") val status: Int,
    @SerializedName("product") @SerialName("product") val product: ImprovedProduct?
)

@Serializable
data class ImprovedSearchResponse(
    @SerializedName("count") @SerialName("count") val count: Int,
    @SerializedName("page") @SerialName("page") val page: Int,
    @SerializedName("page_size") @SerialName("page_size") val pageSize: Int,
    @SerializedName("products") @SerialName("products") val products: List<ImprovedProduct>
)

// ==================== API INTERFACES ====================

interface OpenFoodFactsImprovedApi {
    @GET("api/v2/product/{barcode}")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): ImprovedProductResponse

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") searchTerms: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("json") json: Boolean = true,
        @Query("fields") fields: String = "code,product_name,brands,quantity,image_url,nutriscore_grade,categories,stores"
    ): ImprovedSearchResponse

    @GET("cgi/search.pl")
    suspend fun searchByCategory(
        @Query("tagtype_0") tagtype: String = "categories",
        @Query("tag_contains_0") contains: String = "contains",
        @Query("tag_0") category: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("json") json: Boolean = true,
        @Query("fields") fields: String = "code,product_name,brands,quantity,image_url,nutriscore_grade,categories,stores"
    ): ImprovedSearchResponse

    @GET("cgi/search.pl")
    suspend fun getPopularProducts(
        @Query("countries") country: String = "France",
        @Query("sort_by") sortBy: String = "unique_scans_n",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("json") json: Boolean = true,
        @Query("fields") fields: String = "code,product_name,brands,quantity,image_url,nutriscore_grade,categories,stores"
    ): ImprovedSearchResponse

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        fun create(): OpenFoodFactsImprovedApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
                .create(OpenFoodFactsImprovedApi::class.java)
        }
    }
}

// ==================== GESTIONNAIRE DE FAVORIS ====================

class ImprovedFavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("improved_favorites", Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()

    companion object {
        private const val KEY_FAVORITES = "favorite_products"
    }

    fun getFavorites(): List<ImprovedProduct> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<ImprovedProduct>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addFavorite(product: ImprovedProduct): Boolean {
        val favorites = getFavorites().toMutableList()

        // Vérifier si déjà dans les favoris
        if (favorites.any { it.code == product.code }) {
            return false
        }

        favorites.add(product.copy(isFavorite = true))
        saveFavorites(favorites)
        return true
    }

    fun removeFavorite(productCode: String?): Boolean {
        if (productCode == null) return false

        val favorites = getFavorites().toMutableList()
        val removed = favorites.removeAll { it.code == productCode }

        if (removed) {
            saveFavorites(favorites)
        }

        return removed
    }

    fun isFavorite(productCode: String?): Boolean {
        if (productCode == null) return false
        return getFavorites().any { it.code == productCode }
    }

    fun toggleFavorite(product: ImprovedProduct): Boolean {
        return if (isFavorite(product.code)) {
            removeFavorite(product.code)
            false
        } else {
            addFavorite(product)
            true
        }
    }

    private fun saveFavorites(favorites: List<ImprovedProduct>) {
        val json = gson.toJson(favorites)
        prefs.edit().putString(KEY_FAVORITES, json).apply()
    }

    fun getFavoriteCount(): Int = getFavorites().size

    fun clearFavorites() {
        prefs.edit().remove(KEY_FAVORITES).apply()
    }
}

// ==================== GESTIONNAIRE DE CACHE ====================

class ProductCache(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("product_cache", Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()

    fun getProduct(barcode: String): ImprovedProduct? {
        val json = prefs.getString(barcode, null)
        return if (json != null) {
            gson.fromJson(json, ImprovedProduct::class.java)
        } else {
            null
        }
    }

    fun saveProduct(barcode: String, product: ImprovedProduct) {
        val json = gson.toJson(product)
        prefs.edit().putString(barcode, json).apply()
    }
}

// ==================== GESTIONNAIRE D'API AMÉLIORÉ ====================

class ImprovedApiManager(
    private val context: Context,
    private val api: OpenFoodFactsImprovedApi = OpenFoodFactsImprovedApi.create()
) {
    private val cache = ProductCache(context)
    private val favorites = ImprovedFavoritesManager(context)

    /**
     * Recherche améliorée avec plusieurs sources
     */
    suspend fun searchProducts(query: String, page: Int = 1): ImprovedSearchResponse = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            // Si recherche vide, retourner produits populaires
            return@withContext getPopularProducts(page)
        }

        try {
            // Recherche dans l'API
            val response = api.searchProducts(
                searchTerms = query,
                page = page,
                pageSize = 50
            )

            // Marquer les favoris
            val productsWithFavorites = response.products.map { product ->
                product.copy(isFavorite = favorites.isFavorite(product.code))
            }

            response.copy(products = productsWithFavorites)
        } catch (e: Exception) {
            // En cas d'erreur, rechercher dans les favoris localement
            val localResults = favorites.getFavorites().filter {
                it.name?.contains(query, ignoreCase = true) == true ||
                        it.brands?.contains(query, ignoreCase = true) == true
            }

            ImprovedSearchResponse(
                count = localResults.size,
                page = 1,
                pageSize = localResults.size,
                products = localResults
            )
        }
    }

    /**
     * Récupérer produits populaires
     */
    suspend fun getPopularProducts(page: Int = 1): ImprovedSearchResponse = withContext(Dispatchers.IO) {
        try {
            val response = api.getPopularProducts(page = page, pageSize = 50)

            val productsWithFavorites = response.products.map { product ->
                product.copy(isFavorite = favorites.isFavorite(product.code))
            }

            response.copy(products = productsWithFavorites)
        } catch (e: Exception) {
            ImprovedSearchResponse(count = 0, page = 1, pageSize = 0, products = emptyList())
        }
    }

    /**
     * Recherche par catégorie
     */
    suspend fun searchByCategory(category: String, page: Int = 1): ImprovedSearchResponse = withContext(Dispatchers.IO) {
        try {
            val response = api.searchByCategory(
                category = category,
                page = page,
                pageSize = 50
            )

            val productsWithFavorites = response.products.map { product ->
                product.copy(isFavorite = favorites.isFavorite(product.code))
            }

            response.copy(products = productsWithFavorites)
        } catch (e: Exception) {
            ImprovedSearchResponse(count = 0, page = 1, pageSize = 0, products = emptyList())
        }
    }

    /**
     * Récupérer un produit par code-barres
     */
    suspend fun getProductByBarcode(barcode: String): ImprovedProductResponse? = withContext(Dispatchers.IO) {
        try {
            val response = api.getProductByBarcode(barcode)

            if (response.status == 1 && response.product != null) {
                // Ajouter info favorite
                val product = response.product.copy(
                    isFavorite = favorites.isFavorite(response.product.code)
                )

                // Sauvegarder dans le cache
                cache.saveProduct(barcode, product)

                return@withContext response.copy(product = product)
            }

            response
        } catch (e: Exception) {
            // Fallback sur le cache
            val cached = cache.getProduct(barcode)
            if (cached != null) {
                ImprovedProductResponse(status = 1, product = cached.copy(isFavorite = favorites.isFavorite(cached.code)))
            } else {
                null
            }
        }
    }

    /**
     * Suggestions de recherche
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.length < 2) return@withContext emptyList()

        try {
            val response = api.searchProducts(searchTerms = query, pageSize = 10)
            response.products.mapNotNull { it.name }.take(5)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// ==================== CATÉGORIES PRÉDÉFINIES ====================

object ProductCategories {
    val categories = listOf(
        "Fruits et légumes" to "fruits-and-vegetables",
        "Viandes" to "meats",
        "Poissons" to "seafood",
        "Produits laitiers" to "dairies",
        "Pains et pâtisseries" to "breads-and-pastries",
        "Boissons" to "beverages",
        "Snacks" to "snacks",
        "Produits surgelés" to "frozen-foods",
        "Bio" to "organic",
        "Végétarien" to "vegetarian",
        "Sans gluten" to "gluten-free",
        "Petit déjeuner" to "breakfast",
        "Épicerie salée" to "groceries",
        "Épicerie sucrée" to "sweet-groceries",
        "Conserves" to "canned-foods"
    )

    fun getCategory(name: String): String? {
        return categories.find { it.first == name }?.second
    }
}
