package com.amon.timer

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }
    val scrollState = rememberScrollState()

    val isDark = ThemeManager.isDarkTheme.value
    val bgColor = ThemeManager.getBackgroundColor()
    val cardBg = ThemeManager.getCardColor()
    val textMain = ThemeManager.getTextColor()
    val textMuted = ThemeManager.getTextMutedColor()
    val goldColor = ThemeManager.getAccentColor()
    val cardBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)

    val credentialManager = remember { CredentialManager.create(context) }
    val webClientId = stringResource(id = R.string.default_web_client_id)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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

    // 🛡️ Sign Up / Login Action (Supabase Sync Integrated)
    val handleSignUp = {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPass = password.trim()

        if (cleanName.isBlank()) {
            errorMessage = "कृपया अपना नाम दर्ज करें!"
        } else if (cleanPass.length < 6) {
            errorMessage = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए!"
        } else {
            isLoading = true
            userManager.setUserName(cleanName)
            userManager.setPassword(cleanPass)
            userManager.setGuestUser(false)

            // 🚀 सुप्राबेस पर प्रोफ़ाइल सिंक
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
                    coroutineScope.launch(Dispatchers.Main) {
                        isLoading = false
                        proceedToApp()
                    }
                }
            }
        }
    }

    // 🌐 Google Sign-In Action (Supabase Sync Integrated)
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
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val displayName = googleIdTokenCredential.displayName 
                        ?: googleIdTokenCredential.id.substringBefore("@")
                    val userEmail = googleIdTokenCredential.id

                    userManager.setUserName(displayName)
                    userManager.setGuestUser(false)

                    // 🚀 सुप्राबेस पर गूगल यूज़र सिंक
                    coroutineScope.launch(Dispatchers.IO) {
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

                    Toast.makeText(context, "Welcome $displayName!", Toast.LENGTH_SHORT).show()
                    proceedToApp()
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed sheet
            } catch (e: Exception) {
                errorMessage = "Google Sign-In विफल: ${e.localizedMessage}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AMON",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textMain,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Focus Timer & Study Circle",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Name",
                    fontWeight = FontWeight.SemiBold,
                    color = textMain,
                    fontSize = 13.5.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = { Text("Enter your Name", color = textMuted.copy(alpha = 0.7f), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Name",
                            tint = goldColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = goldColor,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        cursorColor = goldColor
                    ),
                    singleLine = true
                )

                Text(
                    text = "Email",
                    fontWeight = FontWeight.SemiBold,
                    color = textMain,
                    fontSize = 13.5.sp
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it 
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = { Text("Enter your Email", color = textMuted.copy(alpha = 0.7f), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = goldColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = goldColor,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        cursorColor = goldColor
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Text(
                    text = "Password",
                    fontWeight = FontWeight.SemiBold,
                    color = textMain,
                    fontSize = 13.5.sp
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it 
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = { Text("Enter your Password", color = textMuted.copy(alpha = 0.7f), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = goldColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = goldColor,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        cursorColor = goldColor
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { handleSignUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goldColor,
                        contentColor = Color.Black
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Sign Up",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        fontSize = 13.5.sp,
                        color = textMuted
                    )
                    Text(
                        text = "login",
                        fontSize = 13.5.sp,
                        color = goldColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { handleSignUp() }
                    )
                }

                OutlinedButton(
                    onClick = { handleGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDark) Color(0x18FFFFFF) else Color(0xFFF8FAFC)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
