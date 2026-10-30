package com.amon.timer

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userManager = remember { UserManager(context) }
    val scrollState = rememberScrollState()

    // 🔑 Google Credential Manager & Client ID
    val credentialManager = remember { CredentialManager.create(context) }
    val webClientId = stringResource(id = R.string.default_web_client_id)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // 🔔 Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onAuthComplete()
    }

    val proceedToApp = {
        userManager.setFreshAuthDone(true) // 🔒 हमेशा के लिए सेव: दोबारा कभी लॉगिन स्क्रीन नहीं आएगी
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAuthComplete()
        }
    }

    // 🛡️ Sign Up / Login Action
    val handleSignUp = {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanPass = password.trim()

        if (cleanName.isBlank()) {
            errorMessage = "कृपया अपना नाम दर्ज करें!"
        } else if (cleanPass.length < 6) {
            errorMessage = "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए!"
        } else {
            userManager.setUserName(cleanName)
            userManager.setPassword(cleanPass)
            userManager.setGuestUser(false)
            proceedToApp()
        }
    }

    // 🌐 Google Sign-In Action
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

                    userManager.setUserName(displayName)
                    userManager.setGuestUser(false)

                    Toast.makeText(context, "Welcome $displayName!", Toast.LENGTH_SHORT).show()
                    proceedToApp()
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the sheet
            } catch (e: Exception) {
                errorMessage = "Google Sign-In विफल: ${e.localizedMessage}"
            }
        }
    }

    // ----------------- UI SCREEN (Uiverse Clean White Design) -----------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(420.dp)
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name Field
                Text(
                    text = "Name",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF151717),
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = { Text("Enter your Name", color = Color(0xFF9E9E9E)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Name",
                            tint = Color(0xFF757575)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFECEDEC),
                        focusedBorderColor = Color(0xFF2D79F3)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Email Field
                Text(
                    text = "Email",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF151717),
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it 
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = { Text("Enter your Email", color = Color(0xFF9E9E9E)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = Color(0xFF757575)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFECEDEC),
                        focusedBorderColor = Color(0xFF2D79F3)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Password Field
                Text(
                    text = "Password",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF151717),
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it 
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    placeholder = { Text("Enter your Password", color = Color(0xFF9E9E9E)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = Color(0xFF757575)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFECEDEC),
                        focusedBorderColor = Color(0xFF2D79F3)
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                // Error Text
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Sign Up Button
                Button(
                    onClick = { handleSignUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF151717),
                        contentColor = Color.White
                    )
                ) {
                    Text("Sign Up", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                // Switch text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Already have an account? ", fontSize = 14.sp, color = Color.Black)
                    Text(
                        text = "login",
                        fontSize = 14.sp,
                        color = Color(0xFF2D79F3),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Google Sign-In Button
                OutlinedButton(
                    onClick = { handleGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFEDEDEF)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Google",
                            color = Color(0xFF151717),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
