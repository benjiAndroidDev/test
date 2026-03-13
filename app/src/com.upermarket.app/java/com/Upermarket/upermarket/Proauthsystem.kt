package com.Upermarket.upermarket

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.util.UUID
import java.util.regex.Pattern

/**
 * SYSTÈME D'AUTHENTIFICATION PROFESSIONNEL
 * Style Firebase / Supabase
 */

// ==================== MODELS ====================

data class ProUser(
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val displayName: String = "",
    val passwordHash: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val emailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),

    // Préférences
    val isVip: Boolean = false,
    val vipExpiryDate: Long? = null,
    val favoritesCount: Int = 0,
    val scansCount: Int = 0,

    // Metadata
    val metadata: UserMetadata = UserMetadata()
) {
    // Compatibility with legacy code
    val name: String get() = displayName
    val phone: String? get() = phoneNumber
}

data class UserMetadata(
    val signUpMethod: String = "email",
    val lastPasswordChange: Long = System.currentTimeMillis(),
    val sessionCount: Int = 0
)

// ==================== AUTH STATES ====================

sealed class ProAuthState {
    object Idle : ProAuthState()
    object Loading : ProAuthState()
    data class Authenticated(val user: ProUser) : ProAuthState()
    object NotAuthenticated : ProAuthState()
    data class Error(val error: AuthError) : ProAuthState() {
        val message: String get() = error.message
    }
}

data class AuthError(
    val code: ErrorCode,
    val message: String
)

enum class ErrorCode {
    INVALID_EMAIL,
    EMAIL_ALREADY_EXISTS,
    WEAK_PASSWORD,
    WRONG_PASSWORD,
    USER_NOT_FOUND,
    USER_DISABLED,
    NETWORK_ERROR,
    TIMEOUT,
    UNKNOWN_ERROR,
    OPERATION_NOT_ALLOWED
}

// ==================== AUTH RESULT ====================

sealed class ProAuthResult {
    data class Success(val user: ProUser) : ProAuthResult()
    data class Failure(val error: AuthError) : ProAuthResult()
}

// ==================== VALIDATORS ====================

object AuthValidator {
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    fun validateEmail(email: String): AuthError? {
        return when {
            email.isBlank() -> AuthError(ErrorCode.INVALID_EMAIL, "L'email est requis")
            !EMAIL_PATTERN.matcher(email).matches() -> AuthError(ErrorCode.INVALID_EMAIL, "Format d'email invalide")
            email.length > 254 -> AuthError(ErrorCode.INVALID_EMAIL, "Email trop long")
            else -> null
        }
    }

    fun validatePassword(password: String): AuthError? {
        return when {
            password.length < 8 -> AuthError(ErrorCode.WEAK_PASSWORD, "Le mot de passe doit contenir au moins 8 caractères")
            !password.any { it.isDigit() } -> AuthError(ErrorCode.WEAK_PASSWORD, "Le mot de passe doit contenir au moins un chiffre")
            !password.any { it.isLetter() } -> AuthError(ErrorCode.WEAK_PASSWORD, "Le mot de passe doit contenir au moins une lettre")
            password.length > 128 -> AuthError(ErrorCode.WEAK_PASSWORD, "Mot de passe trop long")
            else -> null
        }
    }

    fun validateDisplayName(name: String): AuthError? {
        return when {
            name.isBlank() -> AuthError(ErrorCode.INVALID_EMAIL, "Le nom est requis")
            name.length < 2 -> AuthError(ErrorCode.INVALID_EMAIL, "Le nom doit contenir au moins 2 caractères")
            name.length > 50 -> AuthError(ErrorCode.INVALID_EMAIL, "Nom trop long (max 50 caractères)")
            !name.matches(Regex("^[a-zA-ZÀ-ÿ\\s'-]+$")) -> AuthError(ErrorCode.INVALID_EMAIL, "Le nom contient des caractères invalides")
            else -> null
        }
    }
}

// ==================== AUTH SERVICE ====================

class AuthService(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_v2", Context.MODE_PRIVATE)
    private val gson = Gson()

    var authState by mutableStateOf<ProAuthState>(ProAuthState.Idle)
        private set

    companion object {
        private const val KEY_USERS = "users_database"
        private const val KEY_CURRENT_USER = "current_user_id"
        private const val KEY_SESSION_TOKEN = "session_token"
    }

    init {
        loadCurrentUser()
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        phoneNumber: String? = null
    ): ProAuthResult {
        authState = ProAuthState.Loading
        delay(800)

        AuthValidator.validateEmail(email)?.let {
            authState = ProAuthState.Error(it)
            return ProAuthResult.Failure(it)
        }

        AuthValidator.validatePassword(password)?.let {
            authState = ProAuthState.Error(it)
            return ProAuthResult.Failure(it)
        }

        AuthValidator.validateDisplayName(displayName)?.let {
            authState = ProAuthState.Error(it)
            return ProAuthResult.Failure(it)
        }

        if (emailExists(email)) {
            val error = AuthError(ErrorCode.EMAIL_ALREADY_EXISTS, "Cet email est déjà utilisé")
            authState = ProAuthState.Error(error)
            return ProAuthResult.Failure(error)
        }

        val user = ProUser(
            email = email.lowercase().trim(),
            displayName = displayName.trim(),
            passwordHash = hashPassword(password),
            phoneNumber = phoneNumber?.trim(),
            metadata = UserMetadata(
                signUpMethod = "email",
                sessionCount = 1
            )
        )

        saveUser(user)
        setCurrentUser(user.id)
        generateSessionToken(user.id)

        authState = ProAuthState.Authenticated(user)
        return ProAuthResult.Success(user)
    }

    suspend fun signIn(email: String, password: String): ProAuthResult {
        authState = ProAuthState.Loading
        delay(800)

        if (email.isBlank() || password.isBlank()) {
            val error = AuthError(ErrorCode.INVALID_EMAIL, "Email et mot de passe requis")
            authState = ProAuthState.Error(error)
            return ProAuthResult.Failure(error)
        }

        val user = findUserByEmail(email.lowercase().trim())

        if (user == null) {
            val error = AuthError(ErrorCode.USER_NOT_FOUND, "Email ou mot de passe incorrect")
            authState = ProAuthState.Error(error)
            return ProAuthResult.Failure(error)
        }

        if (user.passwordHash != hashPassword(password)) {
            val error = AuthError(ErrorCode.WRONG_PASSWORD, "Email ou mot de passe incorrect")
            authState = ProAuthState.Error(error)
            return ProAuthResult.Failure(error)
        }

        val updatedUser = user.copy(
            lastLogin = System.currentTimeMillis(),
            metadata = user.metadata.copy(sessionCount = user.metadata.sessionCount + 1)
        )

        saveUser(updatedUser)
        setCurrentUser(updatedUser.id)
        generateSessionToken(updatedUser.id)

        authState = ProAuthState.Authenticated(updatedUser)
        return ProAuthResult.Success(updatedUser)
    }

    fun signOut() {
        clearCurrentUser()
        clearSessionToken()
        authState = ProAuthState.NotAuthenticated
    }

    fun getCurrentUser(): ProUser? {
        return when (val state = authState) {
            is ProAuthState.Authenticated -> state.user
            else -> null
        }
    }

    fun isAuthenticated(): Boolean {
        return authState is ProAuthState.Authenticated
    }

    suspend fun updateProfile(
        displayName: String? = null,
        photoUrl: String? = null,
        phoneNumber: String? = null
    ): ProAuthResult {
        val currentUser = getCurrentUser() ?: return ProAuthResult.Failure(
            AuthError(ErrorCode.USER_NOT_FOUND, "Utilisateur non connecté")
        )

        authState = ProAuthState.Loading
        delay(500)

        if (displayName != null) {
            AuthValidator.validateDisplayName(displayName)?.let {
                authState = ProAuthState.Authenticated(currentUser)
                return ProAuthResult.Failure(it)
            }
        }

        val updatedUser = currentUser.copy(
            displayName = displayName?.trim() ?: currentUser.displayName,
            photoUrl = photoUrl ?: currentUser.photoUrl,
            phoneNumber = phoneNumber?.trim() ?: currentUser.phoneNumber
        )

        saveUser(updatedUser)
        authState = ProAuthState.Authenticated(updatedUser)

        return ProAuthResult.Success(updatedUser)
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): ProAuthResult {
        val currentUser = getCurrentUser() ?: return ProAuthResult.Failure(
            AuthError(ErrorCode.USER_NOT_FOUND, "Utilisateur non connecté")
        )

        authState = ProAuthState.Loading
        delay(500)

        if (currentUser.passwordHash != hashPassword(currentPassword)) {
            val error = AuthError(ErrorCode.WRONG_PASSWORD, "Mot de passe actuel incorrect")
            authState = ProAuthState.Authenticated(currentUser)
            return ProAuthResult.Failure(error)
        }

        AuthValidator.validatePassword(newPassword)?.let {
            authState = ProAuthState.Authenticated(currentUser)
            return ProAuthResult.Failure(it)
        }

        val updatedUser = currentUser.copy(
            passwordHash = hashPassword(newPassword),
            metadata = currentUser.metadata.copy(
                lastPasswordChange = System.currentTimeMillis()
            )
        )

        saveUser(updatedUser)
        authState = ProAuthState.Authenticated(updatedUser)

        return ProAuthResult.Success(updatedUser)
    }

    suspend fun deleteAccount(password: String): ProAuthResult {
        val currentUser = getCurrentUser() ?: return ProAuthResult.Failure(
            AuthError(ErrorCode.USER_NOT_FOUND, "Utilisateur non connecté")
        )

        authState = ProAuthState.Loading
        delay(500)

        if (currentUser.passwordHash != hashPassword(password)) {
            val error = AuthError(ErrorCode.WRONG_PASSWORD, "Mot de passe incorrect")
            authState = ProAuthState.Authenticated(currentUser)
            return ProAuthResult.Failure(error)
        }

        deleteUser(currentUser.id)
        signOut()

        return ProAuthResult.Success(currentUser)
    }

    fun updateVipStatus(isVip: Boolean, expiryDate: Long? = null): ProAuthResult {
        val currentUser = getCurrentUser() ?: return ProAuthResult.Failure(
            AuthError(ErrorCode.USER_NOT_FOUND, "Utilisateur non connecté")
        )

        val updatedUser = currentUser.copy(
            isVip = isVip,
            vipExpiryDate = expiryDate
        )

        saveUser(updatedUser)
        authState = ProAuthState.Authenticated(updatedUser)

        return ProAuthResult.Success(updatedUser)
    }

    private fun loadCurrentUser() {
        val userId = prefs.getString(KEY_CURRENT_USER, null)
        val sessionToken = prefs.getString(KEY_SESSION_TOKEN, null)

        if (userId != null && sessionToken != null && isValidSession(sessionToken)) {
            val user = getUserById(userId)
            authState = if (user != null) {
                ProAuthState.Authenticated(user)
            } else {
                ProAuthState.NotAuthenticated
            }
        } else {
            authState = ProAuthState.NotAuthenticated
        }
    }

    private fun getAllUsers(): Map<String, ProUser> {
        val json = prefs.getString(KEY_USERS, null) ?: return emptyMap()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, ProUser>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveAllUsers(users: Map<String, ProUser>) {
        val json = gson.toJson(users)
        prefs.edit().putString(KEY_USERS, json).apply()
    }

    private fun saveUser(user: ProUser) {
        val users = getAllUsers().toMutableMap()
        users[user.id] = user
        saveAllUsers(users)
    }

    private fun getUserById(id: String): ProUser? {
        return getAllUsers()[id]
    }

    private fun findUserByEmail(email: String): ProUser? {
        return getAllUsers().values.find { it.email == email }
    }

    private fun emailExists(email: String): Boolean {
        return findUserByEmail(email.lowercase().trim()) != null
    }

    private fun deleteUser(userId: String) {
        val users = getAllUsers().toMutableMap()
        users.remove(userId)
        saveAllUsers(users)
    }

    private fun setCurrentUser(userId: String) {
        prefs.edit().putString(KEY_CURRENT_USER, userId).apply()
    }

    private fun clearCurrentUser() {
        prefs.edit().remove(KEY_CURRENT_USER).apply()
    }

    private fun generateSessionToken(userId: String): String {
        val token = "${userId}_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val hashedToken = hashPassword(token)
        prefs.edit().putString(KEY_SESSION_TOKEN, hashedToken).apply()
        return hashedToken
    }

    private fun clearSessionToken() {
        prefs.edit().remove(KEY_SESSION_TOKEN).apply()
    }

    private fun isValidSession(token: String): Boolean {
        return token.isNotBlank()
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
