package com.Upermarket.upermarket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.http.Path
import retrofit2.http.Header
import android.content.Context
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import android.util.Log

// ==================== FAST MODERN API 2026 ====================

@Serializable
data class FastProductResponse(
    val product: com.Upermarket.upermarket.Product? = null,
    val found: Boolean = false,
    val source: String = "",
    val responseTime: Long = 0L
)

// API Walmart/Amazon-like (simulation d'une API moderne)
interface FastRetailApi {
    @GET("v3/products/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Header("X-API-Key") apiKey: String = "demo-key",
        @Query("locale") locale: String = "fr-FR",
        @Query("fields") fields: String = "basic,nutrition,images"
    ): FastProductResponse
}

// API Nutritionix (plus rapide que OpenFoodFacts)
interface NutritionixApi {
    @GET("v1_1/item")
    suspend fun getProductByUPC(
        @Query("upc") barcode: String,
        @Header("X-APP-ID") appId: String = "demo-id",
        @Header("X-APP-KEY") appKey: String = "demo-key"
    ): NutritionixResponse
}

@Serializable
data class NutritionixResponse(
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("brand_name") val brandName: String? = null,
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("upc") val upc: String? = null,
    @SerialName("nf_ingredient_statement") val ingredients: String? = null
)

// Base de données locale des produits courants français
object LocalProductDatabase {
    private val commonProducts = mapOf(
        // Coca Cola
        "5000112548136" to Product(
            code = "5000112548136",
            name = "Coca-Cola Original",
            imageUrl = "https://images.openfoodfacts.org/images/products/500/011/254/8136/front_fr.png",
            brands = "Coca-Cola",
            nutriscore = "e",
            ecoscore = "d",
            novaGroup = 4,
            quantity = "33 cl",
            categories = "Boissons gazeuses",
            ingredients = "Eau gazéifiée, sucre, colorant (E150d), acidifiant (E338), arômes naturels dont caféine"
        ),

        // Danette Vanille
        "3033710065967" to Product(
            code = "3033710065967",
            name = "Danette Vanille",
            imageUrl = "https://images.openfoodfacts.org/images/products/303/371/006/5967/front_fr.png",
            brands = "Danone",
            nutriscore = "d",
            ecoscore = "c",
            novaGroup = 4,
            quantity = "4 x 125 g",
            categories = "Desserts lactés",
            ingredients = "Lait écrémé, sucre, crème, amidon modifié, arôme vanille"
        ),

        // Pain de mie Harrys
        "3228857000906" to Product(
            code = "3228857000906",
            name = "Pain de mie American sandwich",
            imageUrl = "https://images.openfoodfacts.org/images/products/322/885/700/0906/front_fr.png",
            brands = "Harry's",
            nutriscore = "c",
            ecoscore = "c",
            novaGroup = 4,
            quantity = "550 g",
            categories = "Pains de mie",
            ingredients = "Farine de blé, eau, sucre, huile de colza, levure, sel, gluten de blé, conservateur"
        ),

        // Nutella
        "3017624010701" to Product(
            code = "3017624010701",
            name = "Nutella",
            imageUrl = "https://images.openfoodfacts.org/images/products/301/762/401/0701/front_fr.png",
            brands = "Ferrero",
            nutriscore = "e",
            ecoscore = "e",
            novaGroup = 4,
            quantity = "400 g",
            categories = "Pâtes à tartiner aux noisettes et au cacao",
            ingredients = "Sucre, huile de palme, noisettes, cacao maigre, lait écrémé en poudre, lactosérum en poudre, émulsifiants"
        ),

        // Evian 1.5L
        "3068320055503" to Product(
            code = "3068320055503",
            name = "Evian Eau minérale naturelle",
            imageUrl = "https://images.openfoodfacts.org/images/products/306/832/005/5503/front_fr.png",
            brands = "Evian",
            nutriscore = "a",
            ecoscore = "b",
            novaGroup = 1,
            quantity = "1,5 L",
            categories = "Eaux minérales naturelles",
            ingredients = "Eau minérale naturelle"
        ),

        // Kinder Bueno
        "80176756" to Product(
            code = "80176756",
            name = "Kinder Bueno",
            imageUrl = "https://images.openfoodfacts.org/images/products/80176756/front_fr.png",
            brands = "Ferrero",
            nutriscore = "e",
            ecoscore = "e",
            novaGroup = 4,
            quantity = "43 g",
            categories = "Barres chocolatées fourrées",
            ingredients = "Chocolat au lait, sucre, huile de palme, noisettes, cacao, lait écrémé en poudre"
        ),

        // Beurre Président
        "3228021170008" to Product(
            code = "3228021170008",
            name = "Beurre doux",
            imageUrl = "https://images.openfoodfacts.org/images/products/322/802/117/0008/front_fr.png",
            brands = "Président",
            nutriscore = "e",
            ecoscore = "d",
            novaGroup = 1,
            quantity = "250 g",
            categories = "Beurres",
            ingredients = "Crème pasteurisée, ferments lactiques"
        ),

        // Activia Nature Danone
        "3168930010883" to Product(
            code = "3168930010883",
            name = "Activia Nature",
            imageUrl = "https://images.openfoodfacts.org/images/products/316/893/001/0883/front_fr.png",
            brands = "Danone",
            nutriscore = "a",
            ecoscore = "c",
            novaGroup = 3,
            quantity = "4 x 125 g",
            categories = "Yaourts nature",
            ingredients = "Lait écrémé, crème, ferments lactiques dont Bifidus ActiRegularis"
        ),

        // Badoit
        "3029330003533" to Product(
            code = "3029330003533",
            name = "Badoit Eau minérale gazeuse naturelle",
            imageUrl = "https://images.openfoodfacts.org/images/products/302/933/000/3533/front_fr.png",
            brands = "Badoit",
            nutriscore = "a",
            ecoscore = "b",
            novaGroup = 1,
            quantity = "1 L",
            categories = "Eaux gazeuses",
            ingredients = "Eau minérale naturelle gazeuse"
        ),

        // Emmental Président
        "3274080005003" to Product(
            code = "3274080005003",
            name = "Emmental français",
            imageUrl = "https://images.openfoodfacts.org/images/products/327/408/000/5003/front_fr.png",
            brands = "Président",
            nutriscore = "d",
            ecoscore = "d",
            novaGroup = 1,
            quantity = "200 g",
            categories = "Fromages",
            ingredients = "Lait, sel, ferments lactiques, présure"
        ),

        // Oasis Tropical
        "3124480186386" to Product(
            code = "3124480186386",
            name = "Oasis Tropical",
            imageUrl = "https://images.openfoodfacts.org/images/products/312/448/018/6386/front_fr.png",
            brands = "Oasis",
            nutriscore = "d",
            ecoscore = "d",
            novaGroup = 4,
            quantity = "2 L",
            categories = "Boissons aux fruits",
            ingredients = "Eau, jus de fruits, sucre, arômes naturels, acidifiants"
        ),

        // Ricard
        "3035542400013" to Product(
            code = "3035542400013",
            name = "Ricard",
            imageUrl = "https://images.openfoodfacts.org/images/products/303/554/240/0013/front_fr.png",
            brands = "Ricard",
            nutriscore = "e",
            ecoscore = "e",
            novaGroup = 4,
            quantity = "70 cl",
            categories = "Boissons alcoolisées",
            ingredients = "Alcool, extrait d'anis étoilé, arômes naturels, sucre"
        ),

        // Ketchup Heinz
        "87157126" to Product(
            code = "87157126",
            name = "Ketchup Tomato",
            imageUrl = "https://images.openfoodfacts.org/images/products/87157126/front_fr.png",
            brands = "Heinz",
            nutriscore = "c",
            ecoscore = "c",
            novaGroup = 4,
            quantity = "570 g",
            categories = "Condiments",
            ingredients = "Concentré de tomates, vinaigre, sucre, sel, arôme naturel d'épices"
        ),

        // Prince LU
        "7622210139276" to Product(
            code = "7622210139276",
            name = "Prince - Biscuit au chocolat",
            imageUrl = "https://images.openfoodfacts.org/images/products/762/221/013/9276/front_fr.png",
            brands = "LU",
            nutriscore = "d",
            ecoscore = "d",
            novaGroup = 4,
            quantity = "300 g",
            categories = "Biscuits au chocolat",
            ingredients = "Chocolat au lait, farine de blé, sucre, huile de palme"
        ),

        // Perrier
        "3274080011208" to Product(
            code = "3274080011208",
            name = "Perrier Eau minérale gazeuse naturelle",
            imageUrl = "https://images.openfoodfacts.org/images/products/327/408/001/1208/front_fr.png",
            brands = "Perrier",
            nutriscore = "a",
            ecoscore = "b",
            novaGroup = 1,
            quantity = "1 L",
            categories = "Eaux gazeuses",
            ingredients = "Eau minérale naturelle gazeuse"
        )
    )

    fun getProduct(barcode: String): Product? = commonProducts[barcode]

    fun getAllBarcodes(): Set<String> = commonProducts.keys
}

class FastProductManager(private val context: Context) {

    companion object {
        private const val TIMEOUT_MS = 2000L // 2 secondes max par API
        private const val TAG = "FastProductManager"
    }

    // Client optimisé pour la rapidité
    private val fastClient = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(1, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Upermarket-Fast/2.0")
                .header("Accept", "application/json")
                .header("Cache-Control", "max-age=300") // Cache 5 minutes
                .build()
            chain.proceed(request)
        }
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Multiple APIs pour maximiser les chances de succès
    private val fastRetailApi = Retrofit.Builder()
        .baseUrl("https://api.retaildata.com/") // API fictive mais réaliste
        .client(fastClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FastRetailApi::class.java)

    private val nutritionixApi = Retrofit.Builder()
        .baseUrl("https://api.nutritionix.com/")
        .client(fastClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NutritionixApi::class.java)

    private val openFoodFactsApi = OpenFoodFactsApi.create()

    suspend fun getProductByBarcode(barcode: String): ProductResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            // 1. Priorité absolue : base locale (instantané)
            LocalProductDatabase.getProduct(barcode)?.let { product ->
                Log.d(TAG, "✓ Found in local DB: ${product.name} (${System.currentTimeMillis() - startTime}ms)")
                return@withContext ProductResponse(
                    code = barcode,
                    product = product,
                    status = 1,
                    statusVerbose = "found_local"
                )
            }

            // 2. APIs parallèles avec timeout court
            val deferredResults = listOf(
                // API moderne (simulation)
                async {
                    try {
                        withTimeout(TIMEOUT_MS) {
                            val response = fastRetailApi.getProduct(barcode)
                            if (response.found) {
                                Log.d(TAG, "✓ Found via FastRetail API")
                                response.product
                            } else null
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "FastRetail API failed: ${e.message}")
                        null
                    }
                },

                // OpenFoodFacts en parallèle (mais avec timeout court)
                async {
                    try {
                        withTimeout(TIMEOUT_MS) {
                            val response = openFoodFactsApi.getProductByBarcode(barcode)
                            Log.d(TAG, "✓ Found via OpenFoodFacts")
                            response.product
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "OpenFoodFacts failed: ${e.message}")
                        null
                    }
                }
            )

            // 3. Prendre le premier résultat disponible
            for (deferred in deferredResults) {
                try {
                    val result = deferred.await()
                    if (result != null) {
                        val responseTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "✓ Product found in ${responseTime}ms")
                        return@withContext ProductResponse(
                            code = barcode,
                            product = result,
                            status = 1,
                            statusVerbose = "found_api"
                        )
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "API call failed: ${e.message}")
                }
            }

            // 4. Aucun résultat trouvé
            val responseTime = System.currentTimeMillis() - startTime
            Log.w(TAG, "✗ No product found for barcode: $barcode (${responseTime}ms)")

            return@withContext ProductResponse(
                code = barcode,
                product = null,
                status = 0,
                statusVerbose = "product_not_found"
            )

        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "✗ Error getting product $barcode (${responseTime}ms): ${e.message}")

            return@withContext ProductResponse(
                code = barcode,
                product = null,
                status = 0,
                statusVerbose = "error: ${e.message}"
            )
        }
    }
}