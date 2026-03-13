package com.Upermarket.upermarket

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.upermarket.R
import com.Upermarket.upermarket.ui.theme.UpermarketTheme
data class Category(
    val name: String, 
    val imageRes: Int, 
    val color: Color,
    val apiTag: String
)

data class Brand(val name: String, val logoRes: Int, val storeUrl: String)

@Composable
fun HomeScreen(
    favoritesViewModel: FavoritesViewModel,
    cartViewModel: CartViewModel,
    navController: NavController
) {
    val context = LocalContext.current

    val categories = listOf(
        Category("Fruits", R.drawable.fruits, Color(0xFFFFE0E0), "en:fruits"),
        Category("Légumes", R.drawable.legumes, Color(0xFFE0FFE0), "en:vegetables"),
        Category("Viandes", R.drawable.viande, Color(0xFFFFE0B2), "en:meats"),
        Category("Boissons", R.drawable.boissons, Color(0xFFE1F5FE), "en:beverages"),
        Category("Épicerie", R.drawable.epicerie, Color(0xFFF3E5F5), "en:groceries"),
        Category("Surgelés", R.drawable.surgele, Color(0xFFE0F7FA), "en:frozen-foods"),
        Category("Frais", R.drawable.produit_frais, Color(0xFFFFF9C4), "en:fresh-foods"),
        Category("Laiterie", R.drawable.produits_laitiers, Color(0xFFF5F5F5), "en:dairies"),
        Category("Charcut.", R.drawable.charcuterie, Color(0xFFFBE9E7), "en:charcuteries")
    )

    val brands = listOf(
        Brand("Lidl", R.drawable.lidl_logo_svg, "https://www.lidl.fr"),
        Brand("Carrefour", R.drawable.carrefour_logo_1982, "https://www.carrefour.fr"),
        Brand("E.Leclerc", R.drawable.e_leclerc_logo_svg, "https://www.e.leclerc"),
        Brand("Auchan", R.drawable.logo_auchan__2015__svg, "https://www.auchan.fr"),
        Brand("Intermarché", R.drawable.nouveau_logo_intermarche, "https://www.intermarche.com"),
        Brand("Casino", R.drawable.casino_supermarket_logo, "https://www.casinosupermarches.fr"),
        Brand("Netto", R.drawable.french_netto_logo_2019_svg, "https://www.netto.fr"),
        Brand("Monoprix", R.drawable.monoprix, "https://www.monoprix.fr"),
        Brand("Franprix", R.drawable.franprix, "https://www.franprix.fr")
    )

    val carouselImages = listOf(
        R.drawable.top_ub_2_800x1000_saint_valentin_2026_s06_0,
        R.drawable.top_ub_800x1000_fete_des_enfants_hm_2026_s06_0,
        R.drawable.top_ub_800x1000_ramadan_hm_2026_s06,
        R.drawable.top_ub_800x1000_fond_de_panier_hm_2026_s06,
        R.drawable.promos1
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            val pagerState = rememberPagerState(pageCount = { carouselImages.size })
            Column(modifier = Modifier.padding(top = 16.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    Card(
                        modifier = Modifier.fillMaxSize().shadow(16.dp, RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Image(
                            painter = painterResource(id = carouselImages[page]),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Nos Enseignes")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(brands) { brand ->
                    BrandCard(brand) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(brand.storeUrl))
                        context.startActivity(intent)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Rayons")
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                categories.chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { cat ->
                            ModernCategoryItem(
                                category = cat, 
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navController.navigate("category_products/${cat.name}/${cat.apiTag}")
                                }
                            )
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrandCard(brand: Brand, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(100.dp).padding(4.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize().clickable(onClick = onClick).padding(16.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = brand.logoRes), contentDescription = brand.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}

@Composable
fun ModernCategoryItem(category: Category, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.padding(4.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(75.dp).background(category.color, CircleShape).padding(15.dp), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = category.imageRes), contentDescription = category.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name, 
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), 
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title, 
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 0.5.sp), 
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onBackground
    )
}
