package com.Upermarket.upermarket

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.example.upermarket.R

// Helper to find Activity from Context
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuthScreen(authManager: AuthManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState = authManager.authState
    
    // On utilise les états centralisés de l'authManager pour ne pas les perdre
    val authMode = authManager.authMode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (authMode == "OTP") "Vérification" else "Bienvenue",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (authMode == "OTP") "Entrez le code reçu par SMS" else "Connectez-vous pour commencer vos achats",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (authState is AuthState.Error) {
            Text(
                text = authState.message,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        when (authMode) {
            "PHONE" -> {
                OutlinedTextField(
                    value = authManager.phoneInput,
                    onValueChange = { authManager.phoneInput = it },
                    label = { Text("Numéro de téléphone (+33...)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
            "OTP" -> {
                OutlinedTextField(
                    value = authManager.otpInput,
                    onValueChange = { if (it.length <= 6) authManager.otpInput = it },
                    label = { Text("Code de validation") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Numbers, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                when (authMode) {
                    "PHONE" -> {
                        val activity = context.findActivity()
                        if (activity != null && authManager.phoneInput.isNotBlank()) {
                            authManager.sendOtp(authManager.phoneInput, activity)
                        }
                    }
                    "OTP" -> {
                        if (authManager.otpInput.length == 6) {
                            authManager.verifyOtp(authManager.otpInput)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                val btnText = if (authMode == "OTP") "Vérifier le code" else "Envoyer le code"
                Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (authMode == "OTP") {
            TextButton(onClick = { authManager.authMode = "PHONE" }) {
                Text("Modifier le numéro")
            }
        }

        if (authMode == "PHONE") {
            Spacer(modifier = Modifier.height(24.dp))
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // GOOGLE LOGIN
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val activity = context.findActivity() ?: return@launch
                            val credentialManager = CredentialManager.create(context)
                            
                            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder("609518440764-10tpfadosn1am0fu170rvrttlvaiaghn.apps.googleusercontent.com")
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(signInWithGoogleOption)
                                .build()

                            val result = credentialManager.getCredential(context = activity, request = request)
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                            authManager.signInWithGoogle(googleIdTokenCredential.idToken)
                        } catch (e: Exception) {
                            Log.e("AuthScreen", "Google Sign In failed: ${e.message}", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                enabled = authState !is AuthState.Loading
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google__g__logo_svg),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continuer avec Google", color = Color.Black, fontWeight = FontWeight.Medium)
            }
        }
    }
}
