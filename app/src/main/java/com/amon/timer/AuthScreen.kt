package com.amon.timer

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

// 🔒 पासवर्ड को सुरक्षित SHA-256 हैश में बदलने का फ़ंक्शन
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Composable
fun AuthScreen(
    onAuthComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }
    val scrollState = rememberScrollState()

    // 🎨 प्रीमियम डार्क ऑब्सीडियन + वार्म गोल्ड (पर्पल टिंट पूरी तरह साफ़)
    val screenBg = Color(0xFF0A0A0E)
    val cardBg = Color(0xFF131318)
    val inputBg = Color(0xFF1A1A22)
    val goldAccent = Color(0xFFF5A524)       // वार्म गोल्ड एक्सेंट
    val textMain = Color(0xFFF8FAFC)
    val textMuted = Color(0xFF94A3B8)
    val cardBorder = Color(0x22FFFFFF)       // क्लीन सब्टल बॉर्डर
    val inputBorder = Color(0xFF282834)
    val slimBorderStroke = 0.8.dp

    val credentialManager = remember { CredentialManager.create(context) }
    val webClientId = stringResource(id = R.string.default_web_client_id)

    var isLoginMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val isEmailValid = remember(email) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onAuthComplete()
    }

    val proceedToApp = {
        userManager.setFreshAuthDone(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAuthComplete()
        }
    }

    val handleAuthSubmit = {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPass = password.trim()

        if (!isLoginMode && cleanName.isBlank()) {
            errorMessage = "कृपया अपना नाम दर्ज करें!"
        } else if (cleanEmail.isBlank()) {
            errorMessage = "कृपया अपना ईमेल दर्ज करें!"
        } else if (!isEmailValid) {
            errorMessage = "कृपया सही ईमेल पता दर्ज करें!"
        } else if (cleanPass.length < 6) {
            errorMessage = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए!"
        } else {
            isLoading = true
            errorMessage = ""
            val hashedPass = hashPassword(cleanPass)

            if (isLoginMode) {
                val savedHashedPass = userManager.getPassword()
                val savedName = userManager.getUserName()

                if (savedHashedPass.isNotBlank() && savedHashedPass != hashedPass) {
                    isLoading = false
                    errorMessage = "गलत पासवर्ड! कृपया दोबारा प्रयास करें।"
                } else {
                    userManager.setGuestUser(false)
                    userManager.setUserEmail(cleanEmail)
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val finalDisplayName = if (savedName.isNotBlank() && !savedName.equals("Guest", true)) {
                                savedName
                            } else {
                                cleanEmail.substringBefore("@")
                            }
                            
                            CloudSyncManager.registerUserOnCloud(
                                context = context,
                                userId = userManager.getUserId(),
                                nickname = finalDisplayName,
                                email = cleanEmail
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                proceedToApp()
                            }
                        }
                    }
                }
            } else {
                userManager.setUserName(cleanName)
                userManager.setUserEmail(cleanEmail)
                userManager.setPassword(hashedPass)
                userManager.setGuestUser(false)

                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val userId = userManager.getUserId()
                        CloudSyncManager.registerUserOnCloud(
                            context = context,
                            userId = userId,
                            nickname = cleanName,
                            email = cleanEmail
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            proceedToApp()
                        }
                    }
                }
            }
        }
    }

    val handleGoogleSignIn = {
        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    isLoading = true
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val displayName = googleIdTokenCredential.displayName 
                        ?: googleIdTokenCredential.id.substringBefore("@")
                    val userEmail = googleIdTokenCredential.id

                    userManager.setUserName(displayName)
                    userManager.setUserEmail(userEmail)
                    userManager.setGuestUser(false)

                    withContext(Dispatchers.IO) {
                        try {
                            val userId = userManager.getUserId()
                            CloudSyncManager.registerUserOnCloud(
                                context = context,
                                userId = userId,
                                nickname = displayName,
                                email = userEmail
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    isLoading = false
                    Toast.makeText(context, "Welcome $displayName!", Toast.LENGTH_SHORT).show()
                    proceedToApp()
                }
            } catch (e: GetCredentialCancellationException) {
                // Cancelled
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Google Sign-In विफल: ${e.localizedMessage}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .imePadding() // 🚀 कीबोर्ड खुलते ही स्क्रीन को सुरक्षित ऊपर रखेगा
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(slimBorderStroke, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp), // कॉम्पैक्ट इनर पैडिंग
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 🌟 हेडर (कॉम्पैक्ट और ऊपर की तरफ)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AMON",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = goldAccent,
                        letterSpacing = 2.5.sp
                    )
                    Text(
                        text = "Focus Timer App",
                        fontSize = 11.sp,
                        color = textMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isLoginMode) "Welcome Back" else "Create Account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                }

                // 👤 Name फ़ील्ड
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Name",
                            fontWeight = FontWeight.SemiBold,
                            color = textMain,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it 
                                if (errorMessage.isNotEmpty()) errorMessage = ""
                            },
                            placeholder = { Text("Enter your Name", color = textMuted.copy(alpha = 0.5f), fontSize = 12.5.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Name",
                                    tint = goldAccent,
                                    modifier = Modifier.size(17.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = goldAccent,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                cursorColor = goldAccent
                            ),
                            singleLine = true
                        )
                    }
                }

                // ✉️ Email फ़ील्ड
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Email",
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it 
                            if (errorMessage.isNotEmpty()) errorMessage = ""
                        },
                        placeholder = { Text("Enter your Email", color = textMuted.copy(alpha = 0.5f), fontSize = 12.5.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = goldAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = isEmailValid,
                                enter = fadeIn(tween(150)),
                                exit = fadeOut(tween(150))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = if (isEmailValid) Color(0xFF22C55E) else goldAccent,
                            unfocusedBorderColor = if (isEmailValid) Color(0xFF22C55E).copy(alpha = 0.5f) else inputBorder,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            cursorColor = goldAccent
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        singleLine = true
                    )
                }

                // 🔒 Password फ़ील्ड
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Password",
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            if (errorMessage.isNotEmpty()) errorMessage = ""
                        },
                        placeholder = { Text("Enter your Password", color = textMuted.copy(alpha = 0.5f), fontSize = 12.5.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = goldAccent,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = goldAccent,
                            unfocusedBorderColor = inputBorder,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            cursorColor = goldAccent
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { handleAuthSubmit() }),
                        singleLine = true
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 🎯 मुख्य बटन (Sign Up / Sign In)
                Button(
                    onClick = { handleAuthSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goldAccent,
                        contentColor = Color.Black
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isLoginMode) "Sign In" else "Sign Up",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 🔄 स्विच टेक्स्ट
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                    Text(
                        text = if (isLoginMode) "Sign Up" else "Sign In",
                        fontSize = 12.sp,
                        color = goldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            isLoginMode = !isLoginMode
                            errorMessage = ""
                        }
                    )
                }

                // 🌐 Continue with Google बटन
                OutlinedButton(
                    onClick = { handleGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(slimBorderStroke, cardBorder),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue with Google",
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
