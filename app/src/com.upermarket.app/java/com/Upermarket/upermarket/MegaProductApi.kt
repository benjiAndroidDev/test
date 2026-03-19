package com.Upermarket.upermarket

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class MegaProductSearchManager(private val context: Context) {

    private val offApi = OpenFoodFactsApi.create()
    // Débit industriel pour saturer les rayons
    private val networkSemaphore = Semaphore(50)

    companion object {
        private val memoryCache = mutableMapOf<String, List<Product>>()
    }

    fun prefetchCategories(categories: List<Category>) {
        CoroutineScope(Dispatchers.IO).launch {
            categories.forEach { cat ->
                val cacheKey = "${cat.name}_${cat.apiTag}"
                if (!memoryCache.containsKey(cacheKey)) {
                    searchProductsFlow(cat.name, cat.apiTag).collect { }
                }
            }
        }
    }

    fun searchProductsFlow(query: String? = null, categoryTag: String? = null): Flow<List<Product>> = channelFlow {
        val cacheKey = "${query}_${categoryTag}"
        if (memoryCache.containsKey(cacheKey)) {
            send(memoryCache[cacheKey]!!)
        }

        val allProducts = mutableSetOf<Product>()
        val tag = categoryTag?.lowercase() ?: ""

        // INVENTAIRE GÉANT 2026 : Liste exhaustive par rayon
        val subTerms = when {
            tag.contains("fruits") -> listOf("pomme", "banane", "orange", "fraise", "raisin", "kiwi", "ananas", "mangue", "poire", "abricot", "citron", "framboise", "myrtille", "melon", "pastèque", "pêche", "cerise", "prune", "clémentine", "pamplemousse", "litchi", "avocat", "datte", "figue", "mirabelle", "nectarine", "mûre", "groseille", "grenade", "fruit de la passion", "kaki", "coing")
            tag.contains("vegetables") -> listOf("carotte", "tomate", "salade", "pomme de terre", "oignon", "courgette", "poivron", "aubergine", "haricot", "brocoli", "chou", "concombre", "ail", "épinard", "champignon", "asperge", "artichaut", "radis", "poireau", "petit pois", "maïs", "potiron", "endive", "céleri", "betterave", "navet", "panais", "topinambour", "fenouil", "épinards", "mâche", "laitue")
            tag.contains("meats") -> listOf("poulet", "escalope", "steak", "haché", "cuisse", "filet", "aiguillettes", "émincé", "cordon bleu", "dinde", "porc", "veau", "agneau", "canard", "brochette", "lardons", "saucisse", "merguez", "rôti", "magret", "pilon", "travers", "jambon", "saucisson", "chorizo", "pâté", "terrine", "boudin", "entrecôte", "steak haché", "bifteck", "faux filet")
            tag.contains("beverages") -> listOf("eau", "cristaline", "evian", "vittel", "volvic", "perrier", "badoit", "jus", "orange", "pomme", "multi", "coca", "pepsi", "fanta", "sprite", "orangina", "ice tea", "oasis", "sirop", "café", "thé", "bière", "vin", "champagne", "red bull", "monster", "schweppes", "limonade", "smoothie", "nectar", "grenadine", "menthe")
            tag.contains("dairies") -> listOf("yaourt", "danone", "yoplait", "activia", "fromage", "emmental", "comté", "camembert", "mozzarella", "feta", "brie", "roquefort", "cheddar", "lait", "lait entier", "lait demi", "beurre", "président", "crème", "skyr", "petit suisse", "saint moret", "kiri", "vache qui rit", "beurre doux", "beurre demi-sel", "ricotta", "mascarpone", "parmesan")
            tag.contains("groceries") -> listOf("pâtes", "riz", "farine", "sucre", "huile", "sel", "poivre", "sauce", "conserve", "biscuit", "gâteau", "chocolat", "café", "thé", "petit déjeuner", "céréales", "confiture", "miel", "pâte à tartiner", "épices")
            tag.contains("frozen") -> listOf("pizza", "plat cuisiné", "glace", "sorbet", "poisson pané", "frites", "légumes surgelés", "steak surgelé", "burger", "dessert glacé")
            tag.contains("charcuteries") -> listOf("jambon", "saucisson", "chorizo", "pâté", "terrine", "rillauds", "rillettes", "coppa", "pancetta", "mortadelle", "rosette", "bacon", "lardons")
            else -> listOf(query ?: "")
        }

        coroutineScope {
            // PHASE 1 : Surcharge massive du rayon (100 pages = 10 000 produits potentiels)
            launch {
                (1..100).forEach { page ->
                    launch { networkSemaphore.withPermit { fetchAndEmit(null, categoryTag, page, allProducts, cacheKey) } }
                }
            }

            // PHASE 2 : Bombardement chirurgical par aliment (30 pages par terme précis)
            subTerms.forEach { term ->
                launch {
                    (1..30).forEach { page ->
                        launch { networkSemaphore.withPermit { fetchAndEmit(term, categoryTag, page, allProducts, cacheKey) } }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun ProducerScope<List<Product>>.fetchAndEmit(
        term: String?, 
        tag: String?, 
        page: Int, 
        allProducts: MutableSet<Product>,
        cacheKey: String
    ) {
        try {
            val response = offApi.searchProducts(
                terms = term, 
                categoryTag = tag, 
                pageSize = 100, 
                page = page
            )
            response.products?.let { products ->
                val validOnes = products.filter { p -> 
                    val hasName = !p.name.isNullOrBlank() || !p.nameFr.isNullOrBlank()
                    val hasImage = p.imageUrl != null
                    hasName && hasImage
                }
                
                if (validOnes.isNotEmpty()) {
                    synchronized(allProducts) {
                        allProducts.addAll(validOnes)
                    }
                    val currentList = synchronized(allProducts) { 
                        allProducts.toList().distinctBy { it.code ?: it.displayName }.shuffled() 
                    }
                    memoryCache[cacheKey] = currentList
                    send(currentList)
                }
            }
        } catch (e: Exception) { }
    }
}
