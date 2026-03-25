package com.Upermarket.upermarket

import android.os.*
import android.content.*
import android.util.Log
import com.google.gson.Gson
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.navigation.compose.*
import com.Upermarket.upermarket.ui.theme.UpermarketTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.upermarket.R

// ==================== AUTH SYSTEM ====================

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class User(val uid: String = "", val name: String = "", val email: String = "", val isVip: Boolean = false)

class AuthManager(private val context: Context) {
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    var authState by mutableStateOf<AuthState>(AuthState.Idle); private set

    var authMode by mutableStateOf("PHONE") 
    var phoneInput by mutableStateOf("")
    var otpInput by mutableStateOf("")

    private var verificationId: String? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()

            auth?.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    if (authState !is AuthState.Authenticated) {
                        fetchUserData(currentUser.uid)
                    }
                } else {
                    authState = AuthState.NotAuthenticated
                }
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Init error", e)
            authState = AuthState.NotAuthenticated
        }
    }

    private fun fetchUserData(uid: String) {
        val currentUser = auth?.currentUser
        val fallbackUser = User(uid = uid, name = currentUser?.displayName ?: "Utilisateur", email = currentUser?.email ?: "")
        authState = AuthState.Authenticated(fallbackUser)
        db?.collection("users")?.document(uid)?.get()?.addOnSuccessListener { doc ->
            doc.toObject(User::class.java)?.let { authState = AuthState.Authenticated(it) }
        }
    }

    fun setError(message: String) {
        authState = AuthState.Error(message)
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            setError("Erreur: Token Google vide")
            return
        }
        Log.d("AuthManager", "signInWithGoogle: Tentative Firebase")
        authState = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthManager", "Firebase: Connexion réussie")
                    task.result?.user?.let { fetchUserData(it.uid) }
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Erreur inconnue"
                    Log.e("AuthManager", "Firebase: Échec de la connexion - $errorMsg")
                    setError("Détail Firebase: $errorMsg")
                }
            }
    }

    fun sendOtp(phone: String, activity: android.app.Activity) {
        authState = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(auth!!)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) { signInWithPhoneCredential(credential) }
                override fun onVerificationFailed(e: FirebaseException) { authState = AuthState.Error("SMS: ${e.localizedMessage}") }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    authMode = "OTP"
                    authState = AuthState.NotAuthenticated
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        verificationId?.let { signInWithPhoneCredential(PhoneAuthProvider.getCredential(it, code)) }
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        authState = AuthState.Loading
        auth?.signInWithCredential(credential)?.addOnSuccessListener { res ->
            res.user?.let { fetchUserData(it.uid) }
        }?.addOnFailureListener { authState = AuthState.Error("Code invalide: ${it.localizedMessage}") }
    }

    fun signOut() {
        auth?.signOut()
        authState = AuthState.NotAuthenticated
    }

    fun getCurrentUser(): User? = (authState as? AuthState.Authenticated)?.user
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MainApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    authManager: AuthManager,
    cartViewModel: CartViewModel,
    shoppingListViewModel: ShoppingListViewModel,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val user = authManager.getCurrentUser()
    val favoritesViewModel = remember(user?.uid) { FavoritesViewModel(FavoritesManager(context, user?.uid ?: "default")) }
    val scanHistoryManager = remember(user?.uid) { ScanHistoryManager(context, user?.uid ?: "default") }
    val navController = rememberNavController()
    val megaSearchManager = remember { MegaProductSearchManager(context) }

    // PRECHARGEMENT MASQUÉ AU DÉMARRAGE
    LaunchedEffect(Unit) {
        val categories = listOf(
            Category("Fruits", R.drawable.fruits, 0xFFFFE0E0L, "en:fruits"),
            Category("Légumes", R.drawable.legumes, 0xFFE0FFE0L, "en:vegetables"),
            Category("Viandes", R.drawable.viandes, 0xFFFFE0B2L, "en:meats"),
            Category("Boissons", R.drawable.boissons, 0xFFE1F5FEL, "en:beverages"),
            Category("Laiterie", R.drawable.produits_laitiers, 0xFFF5F5F5L, "en:dairies")
        )
        megaSearchManager.prefetchCategories(categories)
    }

    var showHistorySheet by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showShoppingListSheet by remember { mutableStateOf(false) }
    var showBudgetManagerSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableIntStateOf(0) }

    val destinations = Destination.entries.toList()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showBudgetManagerSheet = true }) { Icon(Icons.Rounded.Tune, null, tint = Color(0xFF00C853)) }
                        IconButton(onClick = { showShoppingListSheet = true }) {
                            BadgedBox(badge = { if (shoppingListViewModel.items.any { !it.isChecked }) Badge() }) {
                                Icon(Icons.Rounded.ChecklistRtl, null, tint = Color(0xFF1976D2))
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showHistorySheet = true }) { Icon(Icons.Rounded.History, null) }
                    IconButton(onClick = { showFavoritesSheet = true }) {
                        BadgedBox(badge = { if (favoritesViewModel.favoriteCount > 0) Badge { Text(favoritesViewModel.favoriteCount.toString()) } }) {
                            val isFav = favoritesViewModel.favoriteCount > 0
                            Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    IconButton(onClick = { showCartSheet = true }) {
                        BadgedBox(badge = { if (cartViewModel.itemCount > 0) Badge { Text(cartViewModel.itemCount.toString()) } }) { Icon(Icons.Rounded.ShoppingCart, null) }
                    }
                    IconButton(onClick = { showProfileSheet = true }) { Icon(Icons.Rounded.Person, null, modifier = Modifier.size(32.dp)) }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                destinations.forEachIndexed { index, dest ->
                    val isSelected = selectedItem == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (selectedItem != index) {
                                selectedItem = index
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { 
                            if (dest == Destination.CHEF) {
                                Image(painter = painterResource(id = R.drawable.uperchef), contentDescription = dest.label, modifier = Modifier.size(56.dp).clip(CircleShape))
                            } else {
                                Icon(imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label, tint = Color.Black)
                            }
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black, unselectedIconColor = Color.Black, indicatorColor = Color.Transparent)
                    )
                }
            }
        }
    ) { padding ->
        AppNavHost(navController, Destination.HOME.route, authManager, cartViewModel, favoritesViewModel, scanHistoryManager, Modifier.padding(padding))

        if (showCartSheet) ModalBottomSheet(onDismissRequest = { showCartSheet = false }) { CartSheet(cartViewModel, favoritesViewModel) }
        if (showFavoritesSheet) ModalBottomSheet(onDismissRequest = { showFavoritesSheet = false }) { FavoritesSheet(favoritesViewModel, cartViewModel) }
        if (showProfileSheet) ModalBottomSheet(onDismissRequest = { showProfileSheet = false }) {
            ProfileScreen(authManager, 
                onNavigateToFavorites = { showProfileSheet = false; showFavoritesSheet = true },
                onNavigateToSettings = { showProfileSheet = false; selectedItem = destinations.indexOf(Destination.SETTINGS); navController.navigate(Destination.SETTINGS.route) },
                onDismiss = { showProfileSheet = false })
        }
        if (showShoppingListSheet) ModalBottomSheet(onDismissRequest = { showShoppingListSheet = false }) { ShoppingListSheet(shoppingListViewModel) { showShoppingListSheet = false } }
        if (showBudgetManagerSheet) ModalBottomSheet(onDismissRequest = { showBudgetManagerSheet = false }) { BudgetManagerSheet({ showBudgetManagerSheet = false }, cartViewModel, context) }
        if (showHistorySheet) ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) { ScanHistorySheet(scanHistoryManager, favoritesViewModel, cartViewModel) }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current.applicationContext
    val authManager = remember { AuthManager(context) }
    var isDarkMode by remember { mutableStateOf(false) }
    UpermarketTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val state = authManager.authState) {
                is AuthState.Idle, is AuthState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00C853)) }
                is AuthState.NotAuthenticated, is AuthState.Error -> AuthScreen(authManager)
                is AuthState.Authenticated -> {
                    val cartViewModel = remember(state.user.uid) { CartViewModel(context, state.user.uid) }
                    val shoppingListViewModel = remember(state.user.uid) { ShoppingListViewModel(ShoppingListManager(context, state.user.uid)) }
                    MainAppContent(authManager, cartViewModel, shoppingListViewModel, isDarkMode, onDarkModeChange = { isDarkMode = it })
                }
            }
        }
    }
}

// ==================== VIEWMODELS & MANAGERS ====================

data class CartItem(val product: Product, val quantity: Int, val price: Float)

class CartViewModel(context: Context, private val uid: String) : ViewModel() {
    private val prefs = context.getSharedPreferences("cart_$uid", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _cartItems = mutableStateOf<List<CartItem>>(loadCart())
    val cartItems: List<CartItem> get() = _cartItems.value

    val itemCount by derivedStateOf { _cartItems.value.sumOf { it.quantity } }
    val totalPrice by derivedStateOf { _cartItems.value.sumOf { (it.quantity * it.price).toDouble() }.toFloat() }

    var userMaxBudget by mutableStateOf(prefs.getFloat("max_budget", 100f))
    var selectedDrive by mutableStateOf(prefs.getString("selected_drive", "Drive") ?: "Drive")
    var selectedDriveLogo by mutableIntStateOf(prefs.getInt("selected_drive_logo", 0))

    private fun saveCart() {
        prefs.edit().putString("items", gson.toJson(_cartItems.value))
            .putFloat("max_budget", userMaxBudget)
            .putString("selected_drive", selectedDrive)
            .putInt("selected_drive_logo", selectedDriveLogo)
            .apply()
    }

    private fun loadCart(): List<CartItem> {
        val json = prefs.getString("items", null) ?: return emptyList()
        return try { gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<CartItem>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    fun addToCart(product: Product, initialPrice: Float = 0f) {
        val id = product.code ?: "${product.name}_${product.brands}"
        val existing = _cartItems.value.find { (it.product.code ?: "${it.product.name}_${it.product.brands}") == id }
        if (existing != null) {
            _cartItems.value = _cartItems.value.map { if ((it.product.code ?: "${it.product.name}_${it.product.brands}") == id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            _cartItems.value = _cartItems.value + CartItem(product, 1, initialPrice)
        }
        saveCart()
    }

    fun updatePrice(product: Product, newPrice: Float) {
        val id = product.code ?: "${product.name}_${product.brands}"
        _cartItems.value = _cartItems.value.map { if ((it.product.code ?: "${it.product.name}_${it.product.brands}") == id) it.copy(price = newPrice) else it }
        saveCart()
    }

    fun removeFromCart(product: Product) {
        val id = product.code ?: "${product.name}_${product.brands}"
        _cartItems.value = _cartItems.value.filter { (it.product.code ?: "${it.product.name}_${it.product.brands}") != id }
        saveCart()
    }

    fun updateQuantity(product: Product, quantity: Int) {
        if (quantity <= 0) removeFromCart(product)
        else {
            val id = product.code ?: "${product.name}_${product.brands}"
            _cartItems.value = _cartItems.value.map { if ((it.product.code ?: "${it.product.name}_${it.product.brands}") == id) it.copy(quantity = quantity) else it }
        }
        saveCart()
    }
    fun clearCart() { _cartItems.value = emptyList(); saveCart() }
    fun updateBudget(b: Float) { userMaxBudget = b; saveCart() }
}

class ShoppingListManager(context: Context, private val uid: String) {
    private val prefs = context.getSharedPreferences("shopping_list_$uid", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getItems(): List<ShoppingItem> {
        val json = prefs.getString("items", null) ?: return emptyList()
        return try { gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<ShoppingItem>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    fun saveItems(items: List<ShoppingItem>) {
        prefs.edit().putString("items", gson.toJson(items)).apply()
    }
}

class ShoppingListViewModel(private val mgr: ShoppingListManager) : ViewModel() {
    private var _items = mutableStateOf(mgr.getItems())
    val items: List<ShoppingItem> get() = _items.value

    fun addItem(name: String) {
        val id = if (_items.value.isEmpty()) 1 else _items.value.maxOf { it.id } + 1
        _items.value = _items.value + ShoppingItem(id, name)
        mgr.saveItems(_items.value)
    }

    fun toggleItem(id: Int) {
        _items.value = _items.value.map { if (it.id == id) it.copy(isChecked = !it.isChecked) else it }
        mgr.saveItems(_items.value)
    }

    fun removeItem(id: Int) {
        _items.value = _items.value.filter { it.id != id }
        mgr.saveItems(_items.value)
    }

    fun clearAll() {
        _items.value = emptyList()
        mgr.saveItems(_items.value)
    }
}

class FavoritesViewModel(private val mgr: FavoritesManager) : ViewModel() {
    private val _favorites = mutableStateOf<List<Product>>(emptyList())
    val favorites: List<Product> get() = _favorites.value
    init { loadFavorites() }
    fun loadFavorites() { _favorites.value = mgr.getFavorites() }
    fun toggleFavorite(p: Product): Boolean { val res = mgr.toggleFavorite(p); loadFavorites(); return res }
    fun isFavorite(p: Product): Boolean = mgr.isFavorite(p)
    fun removeFavorite(p: Product) { mgr.removeFavorite(p); loadFavorites() }
    val favoriteCount: Int get() = _favorites.value.size
}

class FavoritesManager(context: Context, private val uid: String) {
    private val prefs = context.getSharedPreferences("favorites_$uid", Context.MODE_PRIVATE); private val gson = Gson()
    fun getFavorites(): List<Product> { val json = prefs.getString("favorite_products", null) ?: return emptyList(); return try { gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<Product>>() {}.type) } catch (e: Exception) { emptyList() } }
    fun toggleFavorite(p: Product): Boolean {
        val f = getFavorites().toMutableList(); val id = p.code ?: "${p.name}_${p.brands}"
        val exists = f.any { (it.code ?: "${it.name}_${it.brands}") == id }
        if (exists) f.removeAll { (it.code ?: "${it.name}_${it.brands}") == id } else f.add(p)
        prefs.edit().putString("favorite_products", gson.toJson(f)).apply(); return !exists
    }
    fun isFavorite(p: Product): Boolean { val id = p.code ?: "${p.name}_${p.brands}"; return getFavorites().any { (it.code ?: "${it.name}_${it.brands}") == id } }
    fun removeFavorite(p: Product) {
        val f = getFavorites().toMutableList()
        val id = p.code ?: "${p.name}_${p.brands}"
        f.removeAll { (it.code ?: "${it.name}_${it.brands}") == id }
        prefs.edit().putString("favorite_products", gson.toJson(f)).apply()
    }
}

class ScanHistoryManager(context: Context, private val uid: String) {
    private val prefs = context.getSharedPreferences("history_$uid", Context.MODE_PRIVATE); private val gson = Gson()
    fun getHistory(): List<Product> { val json = prefs.getString("history", null) ?: return emptyList(); return try { gson.fromJson(json, object : com.google.gson.reflect.TypeToken<List<Product>>() {}.type) } catch (e: Exception) { emptyList() } }
    fun addToHistory(p: Product) {
        val list = getHistory().toMutableList(); val id = p.code ?: "${p.name}_${p.brands}"
        list.removeAll { (it.code ?: "${it.name}_${it.brands}") == id }
        list.add(0, p)
        if (list.size > 50) list.removeAt(list.size - 1)
        prefs.edit().putString("history", gson.toJson(list)).apply()
    }
    fun clearHistory() { prefs.edit().remove("history").apply() }
}
