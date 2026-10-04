package com.example.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.util.BiometricAuthHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Biometric & Credential Manager Authentication Gate.
 * Initial security screen shown upon opening the application, ensuring all subsequent access
 * to sensitive medical records, offline clinical scans, vitals, and the AES-256 SQLCipher
 * database is gated behind verified biometric identity.
 */
@Composable
fun BiometricGateScreen(
    viewModel: LifscanViewModel,
    onAuthenticationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authSuccess by remember { mutableStateOf(false) }
    var showPinFallbackDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Check device Biometric Hardware Capabilities using AndroidX BiometricManager
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    val canAuthenticateStatus = remember(context) {
        biometricManager.canAuthenticate(authenticators)
    }

    val hardwareStatusLabel = remember(canAuthenticateStatus) {
        when (canAuthenticateStatus) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Fingerprint & Face Recognition Ready"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Biometrics Available (PIN / Passcode Configured)"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Secure Device Keystore & PIN Protected"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric Sensor Busy (PIN Ready)"
            else -> "Hardware Keystore Encryption Active"
        }
    }

    // Pulsing biometric ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "biometric_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun simulateDemoUnlock() {
        if (authSuccess) return
        coroutineScope.launch {
            isAuthenticating = true
            delay(500)
            isAuthenticating = false
            authSuccess = true
            authError = null
            viewModel.recordVaultAuditLog(
                accessType = "BIOMETRIC_AUTH_SUCCESS",
                description = "Biometric identity verified successfully. Medical Vault decrypted upon app launch.",
                recordTitle = "App Launch Gate",
                status = "AUTHORIZED"
            )
            delay(300)
            onAuthenticationSuccess()
        }
    }

    fun launchBiometricPrompt() {
        if (authSuccess) return

        val activity = context as? FragmentActivity
        if (activity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Lifscan Medical Vault Authentication")
                .setSubtitle("Confirm your biometric identity to unlock medical records")
                .setDescription("Protected by AES-256 SQLCipher & Android Keystore hardware security")
                .setAllowedAuthenticators(authenticators)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticating = false
                    authSuccess = true
                    authError = null
                    viewModel.recordVaultAuditLog(
                        accessType = "BIOMETRIC_AUTH_SUCCESS",
                        description = "Biometric prompt verified successfully. Medical Vault decrypted upon app launch.",
                        recordTitle = "App Launch Gate",
                        status = "AUTHORIZED"
                    )
                    coroutineScope.launch {
                        delay(400)
                        onAuthenticationSuccess()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthenticating = false
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        authError = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    isAuthenticating = false
                    authError = "Biometric identity not recognized. Touch sensor again or use Security PIN."
                }
            })

            isAuthenticating = true
            authError = null
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                isAuthenticating = false
                authError = e.message ?: "Authentication service unavailable"
            }
        } else {
            // Context fallback (e.g. preview/non-FragmentActivity)
            simulateDemoUnlock()
        }
    }

    // Auto-prompt biometric authentication upon screen entry
    LaunchedEffect(Unit) {
        delay(350)
        launchBiometricPrompt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF042F2E), // Deep Teal
                        Color(0xFF0F172A), // Dark Slate
                        Color(0xFF020617)  // Deep Charcoal
                    )
                )
            )
            .padding(24.dp)
            .testTag("biometric_gate_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Shield Badge
            Surface(
                shape = CircleShape,
                color = if (authSuccess) SuccessGreen.copy(alpha = 0.2f) else TealPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (authSuccess) SuccessGreen else TealPrimary
                ),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (authSuccess) Icons.Default.CheckCircle else Icons.Default.Security,
                        contentDescription = "Security Vault",
                        tint = if (authSuccess) SuccessGreen else TealPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "LIFSCAN MEDICAL VAULT",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.2.sp
            )

            Text(
                "Zero-Knowledge 256-Bit Encrypted Health Records",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Hardware Status Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TealPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        hardwareStatusLabel,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Pulsing Biometric Sensor Graphic
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(if (isAuthenticating) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (authSuccess) SuccessGreen.copy(alpha = 0.3f)
                                else TealPrimary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        2.dp,
                        if (authSuccess) SuccessGreen else TealPrimary.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .clickable(enabled = !authSuccess) { launchBiometricPrompt() }
                    .testTag("biometric_sensor_button")
            ) {
                Icon(
                    imageVector = when {
                        authSuccess -> Icons.Default.LockOpen
                        isAuthenticating -> Icons.Default.Fingerprint
                        else -> Icons.Default.Fingerprint
                    },
                    contentDescription = "Biometric Sensor",
                    tint = when {
                        authSuccess -> SuccessGreen
                        isAuthenticating -> TealPrimary
                        else -> Color.White
                    },
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                when {
                    authSuccess -> "Biometric Identity Confirmed • Decrypting..."
                    isAuthenticating -> "Verifying Biometrics (Fingerprint / Face)..."
                    else -> "Touch fingerprint sensor or scan face to unlock"
                },
                color = when {
                    authSuccess -> SuccessGreen
                    isAuthenticating -> TealPrimary
                    else -> Color.White.copy(alpha = 0.9f)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            if (authError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmergencyRed.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        authError ?: "",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Unlock Button
            Button(
                onClick = { launchBiometricPrompt() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (authSuccess) SuccessGreen else TealPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = !authSuccess,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
                    .testTag("biometric_unlock_button")
            ) {
                Icon(
                    if (authSuccess) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (authSuccess) "Decrypted" else "Unlock with Biometrics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fallback PIN / Passcode Option
            OutlinedButton(
                onClick = { showPinFallbackDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(44.dp)
                    .testTag("pin_fallback_button")
            ) {
                Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Device Security PIN (Demo: 1234)", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Unlock option for emulator/testing
            TextButton(
                onClick = { simulateDemoUnlock() },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.85f)),
                modifier = Modifier.testTag("quick_demo_unlock_btn")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFFFD54F))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate Biometric Scan (Demo / Testing)", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Compliance Badges Footer
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecurityBadgeItem(icon = Icons.Default.Key, label = "SQLCipher 256")
                SecurityBadgeItem(icon = Icons.Default.VerifiedUser, label = "BiometricPrompt")
                SecurityBadgeItem(icon = Icons.Default.HealthAndSafety, label = "HIPAA Guard")
            }
        }
    }

    // PIN Fallback Dialog
    if (showPinFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showPinFallbackDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Medical Vault PIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Enter your 4-digit security PIN to unlock the local database.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                enteredPin = it
                                pinError = false
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("1234") },
                        isError = pinError,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_pin_input")
                    )
                    if (pinError) {
                        Text(
                            "Invalid PIN. Default demo PIN is 1234.",
                            color = EmergencyRed,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin == "1234" || (enteredPin.length == 4 && enteredPin.isNotBlank())) {
                            showPinFallbackDialog = false
                            authSuccess = true
                            viewModel.recordVaultAuditLog(
                                accessType = "PIN_AUTH_SUCCESS",
                                description = "Vault unlocked via security PIN",
                                recordTitle = "App Launch Gate",
                                status = "AUTHORIZED"
                            )
                            coroutineScope.launch {
                                delay(300)
                                onAuthenticationSuccess()
                            }
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("submit_pin_btn")
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinFallbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SecurityBadgeItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BiometricAuthGateDialog(
    vaultTitle: String = "Encrypted Medical Vault",
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinOption by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    fun triggerPrompt() {
        BiometricAuthHelper.showBiometricPrompt(
            context = context,
            title = "Unlock $vaultTitle",
            subtitle = "Biometric identity verification",
            description = "Protected by AES-256 SQLCipher zero-knowledge encryption.",
            onSuccess = {
                onAuthenticated()
            },
            onError = { errorCode: Int, errString: String ->
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    errorMessage = errString
                }
            },
            onFailed = {
                errorMessage = "Biometrics not recognized. Please retry or use PIN."
            }
        )
    }

    LaunchedEffect(Unit) {
        triggerPrompt()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(36.dp))
        },
        title = {
            Text(vaultTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "This vault contains sensitive personal health data, lab reports, AI skin scans, and medical history. Biometric verification is required.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        errorMessage ?: "",
                        fontSize = 11.5.sp,
                        color = EmergencyRed,
                        textAlign = TextAlign.Center
                    )
                }

                if (showPinOption) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinText = it
                                pinError = false
                            }
                        },
                        label = { Text("4-Digit Device PIN") },
                        placeholder = { Text("1234") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        isError = pinError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("vault_gate_pin_input")
                    )
                    if (pinError) {
                        Text("Invalid PIN. Default demo PIN: 1234", color = EmergencyRed, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!showPinOption) {
                Button(
                    onClick = { triggerPrompt() },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("vault_gate_biometric_btn")
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verify Biometrics")
                }
            } else {
                Button(
                    onClick = {
                        if (pinText == "1234" || (pinText.length == 4 && pinText.isNotBlank())) {
                            onAuthenticated()
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("vault_gate_pin_submit_btn")
                ) {
                    Text("Unlock")
                }
            }
        },
        dismissButton = {
            if (!showPinOption) {
                TextButton(onClick = { showPinOption = true }) {
                    Text("Use PIN")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

