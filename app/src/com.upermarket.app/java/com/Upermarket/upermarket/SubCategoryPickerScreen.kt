package com.Upermarket.upermarket

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.upermarket.R

data class SubCategory(
    val name: String,
    val tag: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryPickerScreen(
    parentName: String,
    navController: NavController
) {
    val subCategories = when (parentName) {
        "Petit déjeuner" -> listOf(
            SubCategory("Céréales enfants", "en:children-s-breakfast-cereals", R.drawable.istockphoto_1290306471_612x612),
            SubCategory("Céréales adultes", "en:breakfast-cereals", R.drawable.istockphoto_118964827_612x612),
            SubCategory("Mueslis et Avoines", "en:mueslis,en:oat-flakes", R.drawable._e123b47cd4e68a5ac790446e67f73d0),
            SubCategory("Biscottes, Pains grillés et Galettes", "en:rusks,en:toasts,en:galettes", R.drawable.aliment_biscotte),
            SubCategory("Barres céréales", "en:cereal-bars", R.drawable.picture4_png),
            SubCategory("Pâtes à tartiner et Miels", "en:spreads,en:honeys", R.drawable.pate_a_tartiner_40_de_noisette_400g),
            SubCategory("Confitures", "en:jams", R.drawable.confiture_4_fruits),
            SubCategory("Viennoiseries et Brioches", "en:viennoiseries,en:brioches", R.drawable.viennoiseries),
            SubCategory("Pains de mie", "en:sliced-breads", R.drawable._3228857000913_h1n1_s12)
        )
        "Fruits" -> listOf(
            SubCategory("Pommes", "en:apples", R.drawable.zoom_368),
            SubCategory("Bananes", "en:bananas", R.drawable._329_banane),
            SubCategory("Oranges", "en:oranges", R.drawable.aliment_orange),
            SubCategory("Fraises", "en:strawberries", R.drawable.fraise),
            SubCategory("Kiwis", "en:kiwis", R.drawable.kiwi),
            SubCategory("Mangues", "en:mangoes", R.drawable.mangue),
            SubCategory("Ananas", "en:pineapples", R.drawable.ananas)
        )
        "Légumes" -> listOf(
            SubCategory("Salades", "en:salads", R.drawable._04007_salade_verte_1),
            SubCategory("Tomates", "en:tomatoes", R.drawable.tomate),
            SubCategory("Pommes de terre", "en:potatoes", R.drawable.pomme_de_terre),
            SubCategory("Carottes", "en:carrots", R.drawable.carottes),
            SubCategory("Oignons", "en:onions", R.drawable.oignon),
            SubCategory("Poivrons", "en:peppers", R.drawable.poivron),
            SubCategory("Courgettes", "en:zucchinis", R.drawable.zucchetti_010919)
        )
        "Viandes" -> listOf(
            SubCategory("Poulet", "en:poultry", R.drawable.poulet),
            SubCategory("Boeuf", "en:beef", R.drawable.cote_de_boeuf),
            SubCategory("Steaks hachés", "en:ground-meat-steaks", R.drawable.db250987592ea6cfd1694b34ea91),
            SubCategory("Porc", "en:pork", R.drawable.cotes_de_porc_filet_par_6_en_barquette_s_atm_ls),
            SubCategory("Saucisses", "en:sausages", R.drawable.s)
        )
        "Charcut.", "Charcuterie" -> listOf(
            SubCategory("Jambons", "en:hams", R.drawable.jambon),
            SubCategory("Lardons et Bacon", "en:lardons,en:bacons", R.drawable._55470),
            SubCategory("Saucissons", "en:saucissons", R.drawable.saucisse_seche),
            SubCategory("Charcuteries de volaille", "en:poultry-charcuteries", R.drawable.blanc_de_poulet_naturals_300x300)
        )
        "Frais" -> listOf(
            SubCategory("Traiteur", "en:meals", R.drawable.traiteur),
            SubCategory("Plats préparés", "en:prepared-meals", R.drawable._4486_1640103633),
            SubCategory("Sandwichs", "en:sandwiches", R.drawable.sandwich_350628896),
            SubCategory("Pâtes fraîches", "en:fresh-pastas", R.drawable.pates)
        )
        "Surgelés" -> listOf(
            SubCategory("Pizzas surgelées", "en:frozen-pizzas", R.drawable.regina),
            SubCategory("Glaces et Sorbets", "en:ice-creams,en:sorbets", R.drawable.vanille_600x600_1),
            SubCategory("Légumes surgelés", "en:frozen-vegetables", R.drawable.front_fr_42_200),
            SubCategory("Plats cuisinés surgelés", "en:frozen-ready-meals", R.drawable._29125_coquillettes_jambon_emmental)
        )
        "Boissons" -> listOf(
            SubCategory("Eaux", "en:waters", R.drawable.volvic),
            SubCategory("Sodas", "en:sodas", R.drawable.coca),
            SubCategory("Jus de fruits", "en:fruit-juices", R.drawable._3045320104127_c1c11_1),
            SubCategory("Café", "en:coffees", R.drawable.caf_),
            SubCategory("Bières", "en:beers", R.drawable.bierre),
            SubCategory("Vins", "en:wines", R.drawable.vin)
        )
        "Laiterie" -> listOf(
            SubCategory("Yaourts", "en:yogurts", R.drawable._25151),
            SubCategory("Fromages", "en:cheeses", R.drawable.fromage),
            SubCategory("Lait", "en:milks", R.drawable.lait),
            SubCategory("Oeufs", "en:eggs", R.drawable.oeufs_val_fleuri_x6),
            SubCategory("Beurre et Crème", "en:butters,en:creams", R.drawable._cb34dac53d1ea6b82bf100655cf53cd)
        )
        "Épicerie" -> listOf(
            SubCategory("Pâtes", "en:pastas", R.drawable.pates),
            SubCategory("Riz", "en:rices", R.drawable.depositphotos_60627667_stock_photo_bowl_full_of_rice_on),
            SubCategory("Sauces", "en:sauces", R.drawable.sauce),
            SubCategory("Huiles", "en:oils", R.drawable.huile_de_tournesol_bouteille_1l),
            SubCategory("Biscuits", "en:biscuits", R.drawable.ai_generated_biscuit_on_white_background_photorealistic_best_quality_detailed_skin_hdr_sharp_focus_png),
            SubCategory("Sel et Sucre", "en:salts,en:sugars", R.drawable.la_baleine_sel_fin_250g)
        )
        else -> listOf(
            SubCategory("Tous les produits", parentName, R.drawable.cake)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = parentName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF1D1B20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color(0xFF0061FF)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Recherche */ }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Rechercher",
                            tint = Color(0xFF0061FF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            items(subCategories) { subCat ->
                SubCategoryItem(subCat) {
                    // FIX DU CRASH : Encodage des paramètres avec Uri.encode()
                    val encodedName = Uri.encode(subCat.name)
                    val encodedTag = Uri.encode(subCat.tag)
                    navController.navigate("category_products/$encodedName/$encodedTag")
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SubCategoryItem(
    subCategory: SubCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image de la sous-catégorie EN CERCLE
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = Color(0xFFF5F5F7),
            shadowElevation = 2.dp
        ) {
            Image(
                painter = painterResource(id = subCategory.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }

        Spacer(Modifier.width(20.dp))

        Text(
            text = subCategory.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color(0xFF1D1B20)
            ),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
    }
}
