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

    // 🎨 Phone 1 कलर पैलेट (डार्क ऑब्सिडियन + सॉफ्ट लैवेंडर + वार्म गोल्ड)
    val screenBg = Color(0xFF0F0E17)
    val cardBg = Color(0xFF16151F)
    val inputBg = Color(0xFF1A1826)
    val lavenderAccent = Color(0xFFC084FC) // हेडर और आइकन्स का सॉफ्ट लैवेंडर
    val goldButton = Color(0xFFF5A524)     // मुख्य बटन का वार्म गोल्ड
    val textMain = Color(0xFFFFFFFF)
    val textMuted = Color(0xFF94A3B8)
    
    // 🪶 अल्ट्रा-स्लिम हेयरलाइन बॉर्डर्स (0.8dp)
    val slimBorderStroke = 0.8.dp
    val cardBorder = Color(0x33C084FC)
    val inputBorder = Color(0xFF4A3E68)

    val credentialManager = remember { CredentialManager.create(context) }
    val webClientId = stringResource(id = R.string.default_web_client_id)

    // स्टेट्स
    var isLoginMode by remember { mutableStateOf(false) } // false = Sign Up, true = Sign In
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 🟢 रियल-टाइम ईमेल वेरिफिकेशन चेक (Professional Check Rule)
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

    // 🛡️ Sign Up / Sign In सबमिशन लॉजिक
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
                // 🔑 Login Mode: पासवर्ड हैश मैच और सुप्राबेस सिंक
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
                            CloudSyncManager.registerUserOnCloud(
                                context = context,
                                userId = userManager.getUserId(),
                                nickname = savedName.ifBlank { cleanEmail.substringBefore("@") },
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
            } else {
                // ✨ Sign Up Mode: लोकल मेमोरी में हैश स्टोर और सुप्राबेस रजिस्ट्रेशन
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
                        coroutineScope.launch(Dispatchers.Main) {
                            isLoading = false
                            proceedToApp()
                        }
                    }
                }
            }
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
                    val userEmail = googleIdTokenCredential.id

                    userManager.setUserName(displayName)
                    userManager.setUserEmail(userEmail)
                    userManager.setGuestUser(false)

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
                // User closed login
            } catch (e: Exception) {
                errorMessage = "Google Sign-In विफल: ${e.localizedMessage}"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(slimBorderStroke, cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 🌟 AMON हेडर
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AMON",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = lavenderAccent,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Focus Timer App",
                        fontSize = 12.5.sp,
                        color = textMuted,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = if (isLoginMode) "Welcome Back" else "Create Account",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textMain
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 👤 Name फ़ील्ड (सिर्फ Sign Up में दिखेगा, Sign In में छिप जाएगा)
                AnimatedVisibility(
                    visible = !isLoginMode,
                    enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Name",
                            fontWeight = FontWeight.SemiBold,
                            color = textMain,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                if (errorMessage.isNotEmpty()) errorMessage = ""
                            },
                            placeholder = { Text("Enter your Name", color = textMuted.copy(alpha = 0.6f), fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Name",
                                    tint = lavenderAccent,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedBorderColor = lavenderAccent,
                                unfocusedBorderColor = inputBorder,
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                cursorColor = lavenderAccent
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ✉️ Email फ़ील्ड (ग्रीन वेरिफिकेशन टिक के साथ)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Email",
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it 
                            if (errorMessage.isNotEmpty()) errorMessage = ""
                        },
                        placeholder = { Text("Enter your Email", color = textMuted.copy(alpha = 0.6f), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = lavenderAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        // 🟢 दाएँ कोने में वेरिफिकेशन ग्रीन टिक
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = isEmailValid,
                                enter = fadeIn(tween(200)),
                                exit = fadeOut(tween(150))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid Email Verified",
                                    tint = Color(0xFF22C55E), // वाइब्रेंट ग्रीन टिक
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            // ✅ ईमेल सत्यापित होने पर बॉर्डर का रंग भी हल्का हरा हो जाएगा
                            focusedBorderColor = if (isEmailValid) Color(0xFF22C55E) else lavenderAccent,
                            unfocusedBorderColor = if (isEmailValid) Color(0xFF22C55E).copy(alpha = 0.6f) else inputBorder,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            cursorColor = lavenderAccent
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
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            if (errorMessage.isNotEmpty()) errorMessage = ""
                        },
                        placeholder = { Text("Enter your Password", color = textMuted.copy(alpha = 0.6f), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = lavenderAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = lavenderAccent,
                            unfocusedBorderColor = inputBorder,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            cursorColor = lavenderAccent
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { handleAuthSubmit() }),
                        singleLine = true
                    )
                }

                // एरर टेक्स्ट (गलत/अधूरे इनपुट पर)
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 🎯 मुख्य बटन (Sign Up / Sign In) - सॉलिड गोल्ड पिल
                Button(
                    onClick = { handleAuthSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goldButton,
                        contentColor = Color.Black
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isLoginMode) "Sign In" else "Sign Up",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 🔄 स्विच टेक्स्ट (Sign Up <-> login)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLoginMode) "Don't have an account? " else "Already have an account? ",
                        fontSize = 13.sp,
                        color = textMuted
                    )
                    Text(
                        text = if (isLoginMode) "Sign Up" else "login",
                        fontSize = 13.sp,
                        color = goldButton,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { 
                            isLoginMode = !isLoginMode
                            errorMessage = ""
                        }
                    )
                }

                // 🌐 Continue with Google बटन (स्लिम आउटलाइन पिल)
                OutlinedButton(
                    onClick = { handleGoogleSignIn() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(slimBorderStroke, cardBorder),
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
                            fontSize = 18.sp,
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
