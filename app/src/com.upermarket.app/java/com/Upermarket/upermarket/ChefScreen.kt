package com.Upermarket.upermarket

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upermarket.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Recipe(
    val title: String,
    val description: String,
    val time: String,
    val difficulty: String,
    val imageRes: Int,
    val rating: String = "4.9",
    val ingredients: List<String> = emptyList()
)

data class ChefMessage(
    val text: String,
    val isUser: Boolean,
    val recipes: List<Recipe> = emptyList(),
    val isTyping: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefScreen() {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var messages by remember { 
        mutableStateOf(listOf(
            ChefMessage(
                "Bonjour gourmet ! Je suis UperChef. Dites-moi ce que vous avez (hot dog, steak, poulet, patates...) et je vous prépare un festin !",
                false
            )
        )) 
    }
    var inputText by remember { mutableStateOf("") }
    var selectedRecipeForIngredients by remember { mutableStateOf<Recipe?>(null) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- HEADER ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            painter = painterResource(id = R.drawable.uperchef),
                            contentDescription = "UperChef",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFF00C853), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF00C853), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("UperChef AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("Maître cuisinier • En ligne", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00C853))
                    }
                }
            }

            // --- MESSAGES ---
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    ChefChatBubble(message, onRecipeClick = { selectedRecipeForIngredients = it })
                }
            }

            // --- INPUT AREA (FIXÉ POUR COLLER AU CLAVIER) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding() 
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Un hot dog ? Un steak ?", color = Color.Gray, fontSize = 15.sp) },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F3F4),
                            unfocusedContainerColor = Color(0xFFF1F3F4),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        maxLines = 1,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userMsg = inputText
                                messages = messages + ChefMessage(userMsg, true)
                                inputText = ""
                                scope.launch {
                                    val typingMsg = ChefMessage("", false, isTyping = true)
                                    messages = messages + typingMsg
                                    delay(1000)
                                    messages = messages.filter { !it.isTyping }
                                    messages = messages + simulateChefResponse(userMsg)
                                }
                            }
                        },
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // --- SHEET INGRÉDIENTS ---
        if (selectedRecipeForIngredients != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedRecipeForIngredients = null },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                IngredientsDetailSheet(recipe = selectedRecipeForIngredients!!)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun IngredientsDetailSheet(recipe: Recipe) {
    var isAdding by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8F9FA)
            ) {
                Image(
                    painter = painterResource(id = recipe.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(recipe.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("${recipe.time} • ${recipe.difficulty}", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text("Ingrédients nécessaires", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        
        LazyColumn(
            modifier = Modifier.weight(weight = 1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recipe.ingredients) { ingredient ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF1F3F4),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(ingredient, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // --- BOUTON AJOUTER AU PANIER ULTRA MODERNE ---
        Button(
            onClick = { 
                if (!isAdding && !isDone) {
                    scope.launch {
                        isAdding = true
                        delay(1200) // Simulation chargement
                        isAdding = false
                        isDone = true
                        delay(2000) // Affichage du succès
                        isDone = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isDone -> Color(0xFF00C853)
                    else -> Color.Black
                },
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            AnimatedContent(
                targetState = when {
                    isAdding -> "adding"
                    isDone -> "done"
                    else -> "idle"
                },
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                }
            ) { state ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when(state) {
                        "adding" -> {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Ajout en cours...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        "done" -> {
                            Icon(Icons.Rounded.Done, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("C'est dans le panier !", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Icon(Icons.Rounded.ShoppingBag, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Ajouter tout au panier", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ChefChatBubble(message: ChefMessage, onRecipeClick: (Recipe) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (message.isTyping) {
            Box(modifier = Modifier.padding(start = 8.dp).background(Color(0xFFF1F3F4), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("UperChef réfléchit...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else if (message.text.isNotEmpty()) {
            Surface(
                color = if (message.isUser) Color.Black else Color.White,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.isUser) 18.dp else 2.dp,
                    bottomEnd = if (message.isUser) 2.dp else 18.dp
                ),
                modifier = Modifier.widthIn(max = 300.dp).shadow(2.dp, RoundedCornerShape(18.dp))
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (message.isUser) Color.White else Color.Black,
                    fontSize = 15.sp
                )
            }
        }

        if (message.recipes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(message.recipes) { recipe ->
                    RecipeMiniCard(recipe, onCuisinerClick = { onRecipeClick(recipe) })
                }
            }
        }
    }
}

@Composable
fun RecipeMiniCard(recipe: Recipe, onCuisinerClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.height(160.dp).fillMaxWidth()) {
                Image(
                    painter = painterResource(id = recipe.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopEnd),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Text(" ${recipe.rating}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(recipe.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Text(" ${recipe.time} • ", color = Color.Gray, fontSize = 13.sp)
                    Text(recipe.difficulty, color = Color(0xFF00C853), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCuisinerClick,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Cuisiner", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun simulateChefResponse(input: String): ChefMessage {
    val low = input.lowercase()
    return when {
        low.contains("hot dog") || low.contains("hotdog") || low.contains("dog") -> ChefMessage(
            "Le Hot Dog, un classique Cali ! Voici mes versions préférées :",
            false,
            listOf(
                Recipe("Hot Dog New York", "Saucisse pur bœuf et oignons.", "10 min", "Facile", R.drawable.i30919_hot_dog_new_yorkais, ingredients = listOf("Pains Hot Dog", "Saucisses Francfort", "Oignons frits", "Moutarde douce", "Ketchup")),
                Recipe("Hot Dog Cali Style", "Version gourmande et colorée.", "12 min", "Moyen", R.drawable.screenshot_from_2026_03_16_18_47_23, ingredients = listOf("Pains artisanaux", "Saucisses", "Avocat", "Piments", "Coriandre", "Oignons rouges")),
                Recipe("Hot Dog Bacon", "Enroulé de bacon croustillant.", "15 min", "Gourmet", R.drawable.tk_photo_2025_05_2025_2025_05_la_bacon_wrapped_hot_dogs_la_bacon_wrapped_hot_dogs_455, ingredients = listOf("Pains briochés", "Saucisses", "Bacon fumé", "Cheddar fondu", "Cornichons"))
            )
        )
        low.contains("poulet") -> ChefMessage(
            "Le poulet Cali, tendre et savoureux ! Voici mes meilleures idées :",
            false,
            listOf(
                Recipe("Poulet Curry Coco", "Crémeux et exotique.", "20 min", "Facile", R.drawable.pouletcurrycoco_4, ingredients = listOf("Blancs de poulet", "Lait de coco", "Poudre de Curry", "Riz Basmati", "Oignons")),
                Recipe("Suprême au Basilic", "Tomates braisées et herbes.", "35 min", "Moyen", R.drawable.chiken_tomate_basil, ingredients = listOf("Filets de poulet", "Tomates cerises", "Basilic frais", "Ail", "Huile d'olive")),
                Recipe("Wok de Poulet", "Légumes croquants sauté minute.", "15 min", "Express", R.drawable.wokdepoulet, ingredients = listOf("Aiguillettes de poulet", "Poivrons", "Sauce soja", "Gingembre", "Carottes"))
            )
        )
        low.contains("steak") || low.contains("haché") || low.contains("viande") -> ChefMessage(
            "Pour les carnivores gourmets, voici des recettes premium :",
            false,
            listOf(
                Recipe("Burger Gourmet", "Steak haché boucher et cheddar.", "15 min", "Facile", R.drawable._772327608_recette_du_gros_pepere_burger_maison_faites_le_vous_meme, ingredients = listOf("Pains Burger", "Steak haché", "Cheddar", "Salade", "Sauce Maison", "Oignons rouges")),
                Recipe("Pavé Rossini", "Bœuf noble et sa sauce madère.", "20 min", "Chef", R.drawable.recette_tournedos_rossini_1, ingredients = listOf("Filet de bœuf", "Foie gras", "Pain de mie", "Sauce Madère", "Beurre")),
                Recipe("Tartare Minute", "Préparé au couteau, frais et Cali.", "10 min", "Moyen", R.drawable._150, ingredients = listOf("Bœuf extra frais", "Câpres", "Échalotes", "Jaune d'oeuf", "Cornichons", "Moutarde"))
            )
        )
        low.contains("pomme de terre") || low.contains("patate") -> ChefMessage(
            "La pomme de terre dans tous ses états ! Fondant et rustique :",
            false,
            listOf(
                Recipe("Gratin Dauphinois", "Le classique à la crème entière.", "50 min", "Facile", R.drawable.gratin_dauphinois_cremeux_traditionnel, ingredients = listOf("Pommes de terre", "Crème fraîche", "Ail", "Beurre", "Muscade")),
                Recipe("Grenailles Rôties", "À l'ail rose et romarin.", "30 min", "Moyen", R.drawable.pommes_de_terre_grenailles_saut_es___la_sauge, ingredients = listOf("Pommes de terre grenailles", "Romarin frais", "Fleur de sel", "Huile d'olive", "Ail")),
                Recipe("Écrasé de Patates", "À l'huile d'olive vierge.", "25 min", "Facile", R.drawable.ecrase_de_pomme_de_terre_a_lhuile_dolive, ingredients = listOf("Pommes de terre", "Huile d'olive", "Persil plat", "Poivre noir", "Sel"))
            )
        )
        low.contains("légume") -> ChefMessage(
            "Vitamines et fraîcheur ! Les légumes du marché sublimés :",
            false,
            listOf(
                Recipe("Ratatouille Cali", "Mijotée doucement avec amour.", "45 min", "Moyen", R.drawable._221997_w1776h1332c1cx888cy542cxt0cyt0cxb1775cyb1084, ingredients = listOf("Aubergines", "Courgettes", "Poivrons", "Tomates", "Oignons", "Huile d'olive")),
                Recipe("Légumes au Four", "Rôtis au miel et sésame.", "30 min", "Facile", R.drawable._1874_3_2_1920_1280, ingredients = listOf("Carottes", "Patates douces", "Miel", "Graines de sésame", "Huile d'olive")),
                Recipe("Wok de Saison", "Sauté de légumes ultra croquants.", "15 min", "Express", R.drawable.wokdepoulet, ingredients = listOf("Légumes verts", "Champignons", "Sauce soja", "Ail", "Gingembre"))
            )
        )
        low.contains("fruit") || low.contains("salade") -> ChefMessage(
            "La touche sucrée et légère pour finir en beauté :",
            false,
            listOf(
                Recipe("Salade de Fruits", "Mélange frais menthe et agrumes.", "10 min", "Facile", R.drawable.saladedefrutis, ingredients = listOf("Fraises", "Kiwi", "Orange", "Menthe fraîche", "Sirop de canne")),
                Recipe("Carpaccio d'Ananas", "Fines tranches et sirop basilic.", "15 min", "Chef", R.drawable.i20641_carpaccio_d_ananas, ingredients = listOf("Ananas frais", "Sirop de sucre", "Basilic", "Baies roses", "Citron vert")),
                Recipe("Smoothie Bowl", "Fruits rouges et éclats de granola.", "5 min", "Express", R.drawable.smoothie_bowl_16df176, ingredients = listOf("Banane", "Framboises surgelées", "Lait d'amande", "Granola", "Graines de chia"))
            )
        )
        else -> ChefMessage("Je connais des milliers de recettes pour les hot dogs, le steak, le poulet, les légumes ou les fruits ! Dites-moi ce que vous préférez.", false)
    }
}
