package com.Upermarket.upermarket

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import androidx.credentials.exceptions.GetCredentialException
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
    val authMode = authManager.authMode

    // BLEU UPERMARKET
    val brandBlue = Color(0xFF1976D2)

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Halo bleu subtil en arrière-plan
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(brandBlue.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // LOGO UPERMARKET PREMIUM - PLUS PETIT AVEC OMBRE PROFONDE
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 30.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = Color.Black.copy(alpha = 0.5f),
                        ambientColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.upermarket__7_),
                    contentDescription = "Logo Upermarket",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (authMode == "OTP") "Vérification sécurisée" else "Vos courses, en toute simplicité.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // CARTE DE CONNEXION
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = authMode,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
                        }, label = ""
                    ) { mode ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (mode == "PHONE") "Numéro de téléphone" else "Code secret SMS",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = brandBlue,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            if (mode == "PHONE") {
                                OutlinedTextField(
                                    value = authManager.phoneInput,
                                    onValueChange = { authManager.phoneInput = it },
                                    placeholder = { Text("06 12 34 56 78") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = brandBlue) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = brandBlue,
                                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
                                    )
                                )
                            } else {
                                OutlinedTextField(
                                    value = authManager.otpInput,
                                    onValueChange = { if (it.length <= 6) authManager.otpInput = it },
                                    placeholder = { Text("Code à 6 chiffres") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Rounded.Numbers, null, tint = brandBlue) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = brandBlue,
                                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (authState is AuthState.Error) {
                        Text(
                            text = authState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }

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
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                        enabled = authState !is AuthState.Loading
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (authMode == "OTP") "Vérifier" else "Continuer", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    if (authMode == "OTP") {
                        TextButton(onClick = { authManager.authMode = "PHONE" }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Modifier le numéro", color = brandBlue)
                        }
                    }
                }
            }

            if (authMode == "PHONE") {
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.3f))
                    Text("OU", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.3f))
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                val activity = context.findActivity() ?: return@launch
                                val credentialManager = CredentialManager.create(context)
                                val serverClientId = "609518440764-10tpfadosn1am0fu170rvrttlvaiaghn.apps.googleusercontent.com"
                                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
                                val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                                val result = credentialManager.getCredential(activity, request)
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                authManager.signInWithGoogle(googleIdTokenCredential.idToken)
                            } catch (e: Exception) {
                                if (e is GetCredentialException && e.message?.contains("cancelled") == false) {
                                    authManager.setError("Erreur Google: ${e.message}")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, Color.LightGray.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    enabled = authState !is AuthState.Loading
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google__g__logo_svg),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continuer avec Google", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "Upermarket v1.8 • © 2026",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
