package com.Upermarket.upermarket

import android.os.*
import android.content.*
import android.util.Log
import com.google.gson.Gson
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.Upermarket.upermarket.ui.theme.UpermarketTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// ==================== AUTH SYSTEM ====================

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object NotAuthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

// User class is already in AuthManager.kt but using this version for _Updated.kt
data class User(val uid: String = "", val name: String = "", val email: String = "", val isVip: Boolean = false)

class AuthManager(private val context: Context) {
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    var authState by mutableStateOf<AuthState>(AuthState.Idle); private set

    private var verificationId: String? = null

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                try {
                    val firebaseAppCheck = FirebaseAppCheck.getInstance()
                    firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                } catch (e: Exception) {
                    Log.e("AuthManager", "AppCheck init skip: ${e.message}")
                }
            }
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            
            auth?.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    fetchUserData(currentUser.uid)
                } else {
                    authState = AuthState.NotAuthenticated
                }
            } ?: run {
                authState = AuthState.NotAuthenticated
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Firebase init error", e)
            authState = AuthState.NotAuthenticated
        }
    }

    private fun fetchUserData(uid: String) {
        // Only set Loading if we are not already in a process
        if (authState !is AuthState.Loading) {
            authState = AuthState.Loading
        }
        db?.collection("users")?.document(uid)?.get()?.addOnSuccessListener {
            authState = AuthState.Authenticated(it.toObject(User::class.java) ?: User(uid=uid))
        }?.addOnFailureListener {
            Log.e("AuthManager", "Fetch user data failed", it)
            authState = AuthState.Authenticated(User(uid=uid))
        } ?: run {
            authState = AuthState.Authenticated(User(uid=uid))
        }
    }

    fun signIn(email: String, pass: String) {
        authState = AuthState.Loading
        auth?.signInWithEmailAndPassword(email, pass)?.addOnFailureListener {
            authState = AuthState.Error(it.localizedMessage ?: "Erreur de connexion")
        }
    }

    fun signInWithGoogle(idToken: String) {
        authState = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth?.signInWithCredential(credential)?.addOnSuccessListener { res ->
            val user = res.user
            if (user != null) {
                val u = User(user.uid, user.displayName ?: "", user.email ?: "")
                db?.collection("users")?.document(u.uid)?.set(u)?.addOnCompleteListener {
                    fetchUserData(u.uid)
                }
            }
        }?.addOnFailureListener {
            authState = AuthState.Error(it.localizedMessage ?: "Erreur Google")
        }
    }

    fun sendOtp(phone: String, activity: android.app.Activity) {
        // We don't set global Loading state here to keep AuthScreen visible
        val options = PhoneAuthOptions.newBuilder(auth!!)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("AuthManager", "SMS verification failed", e)
                    authState = AuthState.Error(e.localizedMessage ?: "Échec de vérification")
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    // Keep NotAuthenticated to stay on AuthScreen and see OTP field
                    authState = AuthState.NotAuthenticated 
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        val id = verificationId ?: return
        val credential = PhoneAuthProvider.getCredential(id, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        authState = AuthState.Loading
        auth?.signInWithCredential(credential)?.addOnSuccessListener { res ->
            val user = res.user
            if (user != null) {
                val u = User(user.uid, user.displayName ?: "Utilisateur", user.phoneNumber ?: "")
                db?.collection("users")?.document(u.uid)?.set(u)?.addOnCompleteListener {
                    fetchUserData(u.uid)
                }
            }
        }?.addOnFailureListener {
            authState = AuthState.Error(it.localizedMessage ?: "Erreur SMS")
        }
    }

    fun signUp(email: String, pass: String, name: String) {
        authState = AuthState.Loading
        auth?.createUserWithEmailAndPassword(email, pass)?.addOnSuccessListener { res ->
            val u = User(res.user!!.uid, name, email)
            db?.collection("users")?.document(u.uid)?.set(u)?.addOnCompleteListener {
                fetchUserData(u.uid)
            }
        }?.addOnFailureListener {
            authState = AuthState.Error(it.localizedMessage ?: "Erreur d'inscription")
        }
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

@Composable
fun MainApp() {
    val context = LocalContext.current.applicationContext
    val authManager = remember { AuthManager(context) }
    var isDarkMode by remember { mutableStateOf(false) }

    UpermarketTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val state = authManager.authState) {
                is AuthState.Idle, is AuthState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00C853)) }
                is AuthState.NotAuthenticated -> AuthScreen(authManager)
                is AuthState.Authenticated -> {
                    val cartViewModel = remember(state.user.uid) { CartViewModel(context, state.user.uid) }
                    MainAppContent(authManager, cartViewModel, isDarkMode, onDarkModeChange = { isDarkMode = it })
                }
                is AuthState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(state.message, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { authManager.signOut() }) { Text("Retour") }
                    }
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
        f.removeAll { (it.code ?: "${it.name}_${p.brands}") == id }
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

// ==================== MAIN UI CONTENT ====================

@OptIn(ExperimentalMaterial3Api::class)@Composable
fun MainAppContent(
    authManager: AuthManager, 
    cartViewModel: CartViewModel,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val user = authManager.getCurrentUser()
    val favoritesManager = remember(user?.uid) { FavoritesManager(context, user?.uid ?: "default") }
    val favoritesViewModel = remember(user?.uid) { FavoritesViewModel(favoritesManager) }
    val scanHistoryManager = remember(user?.uid) { ScanHistoryManager(context, user?.uid ?: "default") }
    val navController = rememberNavController()

    var showHistorySheet by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showShoppingListSheet by remember { mutableStateOf(false) }
    var showBudgetManagerSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    Row {
                        IconButton(onClick = { showBudgetManagerSheet = true }) {
                            Icon(Icons.Rounded.Tune, null, tint = Color(0xFF00C853))
                        }
                        IconButton(onClick = { showShoppingListSheet = true }) {
                            Icon(Icons.Rounded.ChecklistRtl, null, tint = Color(0xFF1976D2))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(Icons.Rounded.History, null)
                    }
                    IconButton(onClick = { showFavoritesSheet = true }) {
                        BadgedBox(badge = {
                            if (favoritesViewModel.favoriteCount > 0) Badge { Text(favoritesViewModel.favoriteCount.toString()) }
                        }) {
                            val isFav = favoritesViewModel.favoriteCount > 0
                            Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    IconButton(onClick = { showCartSheet = true }) {
                        BadgedBox(badge = {
                            if (cartViewModel.itemCount > 0) Badge { Text(cartViewModel.itemCount.toString()) }
                        }) {
                            Icon(Icons.Rounded.ShoppingCart, null)
                        }
                    }
                    IconButton(onClick = { showProfileSheet = true }) {
                        Icon(Icons.Rounded.Person, null, modifier = Modifier.size(32.dp))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                Destination.entries.forEachIndexed { index, dest ->
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
                        icon = { Icon(if (isSelected) dest.selectedIcon else dest.unselectedIcon, null) },
                        label = null
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, Destination.HOME.route, Modifier.padding(padding)) {
            composable(Destination.HOME.route) { HomeScreen(favoritesViewModel, cartViewModel, navController) }

            composable(
                route = "category_products/{name}/{tag}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType },
                    navArgument("tag") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val tag = backStackEntry.arguments?.getString("tag") ?: ""
                CategoryProductsScreen(name, tag, favoritesViewModel, cartViewModel, navController)
            }

            composable(Destination.SEARCH.route) { SearchScreen(favoritesViewModel, cartViewModel) }
            composable(Destination.SCAN.route) { ScanScreen(cartViewModel, favoritesViewModel, scanHistoryManager) }
            composable(Destination.VIP.route) { VipScreen(authManager) }
            composable(Destination.SETTINGS.route) { SettingsScreen(authManager, isDarkMode, onDarkModeChange) }
        }

        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) { CartSheet(cartViewModel, favoritesViewModel) }
        }

        if (showFavoritesSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFavoritesSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) { FavoritesSheet(favoritesViewModel, cartViewModel) }
        }

        if (showProfileSheet) {
            ModalBottomSheet(onDismissRequest = { showProfileSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
                // ProfileScreen is expected to be in AuthManager.kt
                ProfileScreen(
                    authService = authManager,
                    onNavigateToVip = { showProfileSheet = false; selectedItem = 3; navController.navigate(Destination.VIP.route) },
                    onNavigateToFavorites = { showProfileSheet = false; showFavoritesSheet = true },
                    onNavigateToSettings = { showProfileSheet = false; selectedItem = 4; navController.navigate(Destination.SETTINGS.route) },
                    onDismiss = { showProfileSheet = false }
                )
            }
        }

        if (showShoppingListSheet) {
            ModalBottomSheet(onDismissRequest = { showShoppingListSheet = false }, containerColor = MaterialTheme.colorScheme.surface) { ShoppingListSheet { showShoppingListSheet = false } }
        }

        if (showBudgetManagerSheet) {
            ModalBottomSheet(onDismissRequest = { showBudgetManagerSheet = false }, containerColor = MaterialTheme.colorScheme.surface) { BudgetManagerSheet({ showBudgetManagerSheet = false }, cartViewModel, context) }
        }

        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) { ScanHistorySheet(scanHistoryManager, favoritesViewModel, cartViewModel) }
        }
    }
}
