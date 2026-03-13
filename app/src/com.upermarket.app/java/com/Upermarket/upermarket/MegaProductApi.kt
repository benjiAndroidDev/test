package com.Upermarket.upermarket

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*

class MegaProductSearchManager(private val context: Context) {

    private val offApi = OpenFoodFactsApi.create()

    suspend fun searchProducts(query: String? = null, categoryTag: String? = null): List<Product> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Product>()
        val isMeatRayon = categoryTag?.contains("meats", ignoreCase = true) == true
        val isCharcuterieRayon = categoryTag?.contains("charcuteries", ignoreCase = true) == true

        try {
            coroutineScope {
                val searches = mutableListOf<Deferred<SearchResponse>>()

                // STRATÉGIE "MENU SUR MESURE" POUR LA BOUCHERIE / CHARCUTERIE
                when {
                    isMeatRayon -> {
                        Log.d("MegaSearch", "Stratégie 'Menu du Boucher' activée")
                        val meatTerms = listOf("poulet rôti", "steak haché", "escalope de dinde", "cuisse de poulet", "filet de poulet", "boeuf", "saucisse")
                        meatTerms.forEach { term ->
                            searches.add(async { try { offApi.searchProducts(terms = term, categoryTag = categoryTag, pageSize = 25) } catch(e:Exception) { SearchResponse() } })
                        }
                        // On ajoute une touche de jambon, mais de façon contrôlée
                        searches.add(async { try { offApi.searchProducts(terms = "jambon", categoryTag = categoryTag, pageSize = 10) } catch(e:Exception) { SearchResponse() } })
                    }

                    isCharcuterieRayon -> {
                        Log.d("MegaSearch", "Stratégie 'Charcuterie' activée")
                        val charcuterieTerms = listOf("saucisson", "jambon cru", "rosette", "chorizo", "pâté de campagne")
                        charcuterieTerms.forEach { term ->
                            searches.add(async { try { offApi.searchProducts(terms = term, categoryTag = categoryTag, pageSize = 20) } catch(e:Exception) { SearchResponse() } })
                        }
                        searches.add(async { try { offApi.searchProducts(terms = "jambon blanc", categoryTag = categoryTag, pageSize = 15) } catch(e:Exception) { SearchResponse() } })
                    }

                    else -> {
                        // Stratégie classique pour tous les autres rayons
                        if (!categoryTag.isNullOrBlank()) {
                            searches.add(async { try { offApi.searchProducts(categoryTag = categoryTag, pageSize = 50, page = 1) } catch(e:Exception) { SearchResponse() } })
                            searches.add(async { try { offApi.searchProducts(categoryTag = categoryTag, pageSize = 50, page = 2) } catch(e:Exception) { SearchResponse() } })
                        }
                        if (!query.isNullOrBlank()) {
                             searches.add(async { try { offApi.searchProducts(terms = query, pageSize = 50) } catch(e:Exception) { SearchResponse() } })
                        }
                    }
                }

                // On collecte tous les résultats
                searches.forEach { deferred ->
                    try {
                        val response = deferred.await()
                        response.products?.let { results.addAll(it) }
                    } catch (e: Exception) { Log.e("MegaSearch", "Une sous-recherche a échoué", e) }
                }
            }
        } catch (e: Exception) {
            Log.e("MegaSearch", "Erreur critique de recherche", e)
        }

        // Nettoyage final et mélange pour un affichage parfait
        results.filter { !it.name.isNullOrBlank() && it.name != "Produit inconnu" }
               .distinctBy { it.code ?: it.name }
               .shuffled()
    }
}
