package com.Upermarket.upermarket

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MegaProductSearchManager(private val context: Context) {

    private val offApi = OpenFoodFactsApi.create()

    companion object {
        private val memoryCache = mutableMapOf<String, List<Product>>()
    }

    // Préchargement massif en arrière-plan
    fun prefetchCategories(categories: List<Category>) {
        CoroutineScope(Dispatchers.IO).launch {
            categories.forEach { cat ->
                val cacheKey = "${cat.name}_${cat.apiTag}"
                if (!memoryCache.containsKey(cacheKey)) {
                    Log.d("MegaSearch", "Préchargement de ${cat.name}...")
                    searchProductsFlow(cat.name, cat.apiTag).collect { }
                }
            }
        }
    }

    fun searchProductsFlow(query: String? = null, categoryTag: String? = null): Flow<List<Product>> = channelFlow {
        val cacheKey = "${query}_${categoryTag}"
        
        // 1. Envoi immédiat du cache si disponible
        if (memoryCache.containsKey(cacheKey)) {
            send(memoryCache[cacheKey]!!)
        }

        val allProducts = mutableSetOf<Product>()
        val tag = categoryTag?.lowercase() ?: ""

        // Liste exhaustive pour saturer les rayons (500+ produits)
        val searchTerms = when {
            tag.contains("fruits") -> listOf("fruits", "pomme", "banane", "orange", "fraise", "raisin", "kiwi", "ananas", "mangue", "poire", "abricot", "citron", "framboise", "myrtille", "melon", "pastèque", "pêche", "cerise", "prune", "mûre", "figue", "grenade", "litchi")
            tag.contains("vegetables") -> listOf("légumes", "carotte", "tomate", "salade", "pomme de terre", "oignon", "courgette", "poivron", "aubergine", "haricot", "brocoli", "chou", "concombre", "ail", "épinard", "champignon", "avocat", "asperge", "artichaut", "radis", "poireau", "potiron", "navet")
            tag.contains("meats") -> listOf("viandes", "poulet", "boeuf", "steak", "escalope", "cuisse", "dinde", "porc", "veau", "agneau", "canard", "haché", "brochette", "lardons", "saucisse", "merguez", "rôti", "filet", "magret", "pilon", "travers", "jambon")
            tag.contains("beverages") -> listOf("boissons", "eau", "jus", "soda", "cola", "limonade", "thé glacé", "sirop", "café", "thé", "bière", "vin", "champagne", "énergisante", "lait de coco", "smoothie", "nectar", "grenadine", "menthe", "vittel", "evian", "volvic")
            tag.contains("dairies") -> listOf("laiterie", "yaourt", "fromage", "lait", "beurre", "crème", "emmental", "camembert", "petit suisse", "comté", "mozzarella", "skyr", "chèvre", "roquefort", "brie", "mimolette", "gorgonzola", "feta", "ricotta", "mascarpone", "parmesan")
            else -> listOf(query ?: "", tag)
        }

        suspend fun fetchAndEmit(term: String?, page: Int) {
            try {
                val response = offApi.searchProducts(
                    terms = if (term == tag) null else term, 
                    categoryTag = categoryTag, 
                    pageSize = 100, 
                    page = page
                )
                response.products?.let { products ->
                    val validOnes = products.filter { !it.name.isNullOrBlank() || !it.nameFr.isNullOrBlank() }
                    if (validOnes.isNotEmpty()) {
                        synchronized(allProducts) {
                            allProducts.addAll(validOnes)
                        }
                        val currentList = synchronized(allProducts) { 
                            allProducts.toList().distinctBy { it.code ?: it.name ?: it.nameFr }.shuffled() 
                        }
                        memoryCache[cacheKey] = currentList
                        send(currentList)
                    }
                }
            } catch (e: Exception) {
                Log.e("MegaSearch", "Erreur page $page pour $term", e)
            }
        }

        coroutineScope {
            // STRATÉGIE DE SATURATION
            // 1. On lance 10 pages génériques pour remplir le rayon globalement
            launch {
                (1..10).forEach { page ->
                    launch { fetchAndEmit(null, page) }
                }
            }

            // 2. On lance des recherches ciblées pour chaque aliment (5 pages chacun)
            searchTerms.forEach { term ->
                launch {
                    (1..5).forEach { page ->
                        launch { fetchAndEmit(term, page) }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
