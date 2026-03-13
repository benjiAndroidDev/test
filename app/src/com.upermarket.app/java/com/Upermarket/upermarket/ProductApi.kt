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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@Serializable
data class SearchResponse(
    val products: List<Product>? = emptyList(),
    val count: Int? = null,
    val page: Int? = null,
    @SerialName("page_size") val pageSize: Int? = null
)

@Serializable
data class ProductResponse(
    val code: String? = null,
    val product: Product? = null,
    val status: Int? = null,
    @SerialName("status_verbose") val statusVerbose: String? = null
)

@Serializable
data class Product(
    val code: String? = null,
    @SerialName("product_name") val name: String? = "Produit inconnu",
    @SerialName("image_front_url") val imageUrl: String? = null,
    val brands: String? = null,
    @SerialName("nutriscore_grade") val nutriscore: String? = null,
    @SerialName("ecoscore_grade") val ecoscore: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    val quantity: String? = null,
    val categories: String? = null,
    @SerialName("ingredients_text") val ingredients: String? = null,
    @SerialName("additives_n") val additivesCount: Int? = 0,
    @SerialName("nutrient_levels") val levels: NutrientLevels? = null
)

@Serializable
data class NutrientLevels(
    val fat: String? = null,
    val salt: String? = null,
    @SerialName("saturated-fat") val saturatedFat: String? = null,
    val sugars: String? = null
)

interface OpenFoodFactsApi {
    @GET("api/v2/search")
    suspend fun searchProducts(
        @Query("search_terms") terms: String? = null,
        @Query("categories_tags") categoryTag: String? = null,
        @Query("lc") lang: String = "fr",
        @Query("cc") country: String = "fr",
        @Query("fields") fields: String = "code,product_name,image_front_url,brands,nutriscore_grade,ecoscore_grade,nova_group,quantity,categories,ingredients_text,additives_n,nutrient_levels",
        @Query("page_size") pageSize: Int = 50,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("api/v2/product/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "code,product_name,image_front_url,brands,nutriscore_grade,ecoscore_grade,nova_group,quantity,categories,ingredients_text,additives_n,nutrient_levels"
    ): ProductResponse

    companion object {
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        fun create(): OpenFoodFactsApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OpenFoodFactsApi::class.java)
        }
    }
}
