package com.example.ui.auth

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AsianCountriesProvider
import com.example.data.model.AsianCountry
import com.example.data.model.LanguageHelper
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.util.BiometricAuthHelper

/**
 * Entrance authentication states matching the prototype mockup:
 * 1. LOGIN (Bottom-left mockup card)
 * 2. SIGN_UP (Center mockup card)
 * 3. FORGOT_PASSWORD (Top-right mockup card)
 * 4. SET_NEW_PASSWORD (Bottom-right mockup card)
 */
enum class AuthScreenMode {
    LOGIN,
    LOGIN_VERIFICATION,
    SIGN_UP,
    FORGOT_PASSWORD,
    SET_NEW_PASSWORD
}

// Prototype Theme Colors
val PrototypeIndigoPrimary = Color(0xFF1E1B4B)
val PrototypeIndigoButton = Color(0xFF2E1065)
val PrototypeIndigoDeep = Color(0xFF1D1B64)
val PrototypeWarmBgLight = Color(0xFFF6F4F0)
val PrototypeCardBgLight = Color(0xFFFFFFFF)
val PrototypeBorderLight = Color(0xFFE2E8F0)
val PrototypeTextMuted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: LifscanViewModel) {
    var screenMode by remember { mutableStateOf(AuthScreenMode.LOGIN) }

    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val phoneInput by viewModel.phoneNumberInput.collectAsState()
    val nameInput by viewModel.userNameInput.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    val doctorLicense by viewModel.doctorLicenseInput.collectAsState()
    val doctorSpecialty by viewModel.doctorSpecialtyInput.collectAsState()
    val driverLicense by viewModel.driverLicenseInput.collectAsState()
    val ambulancePlate by viewModel.ambulancePlateInput.collectAsState()
    val otpCode by viewModel.otpCode.collectAsState()
    val isOtpSent by viewModel.isOtpSent.collectAsState()
    val generatedOtp by viewModel.generatedDemoOtp.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // 2FA Verification & Admin State
    val isLoginVerifying by viewModel.isLoginVerifying.collectAsState()
    val pendingSession by viewModel.pendingLoginSession.collectAsState()
    val generatedVerificationCode by viewModel.generatedLoginVerificationCode.collectAsState()
    val verificationCodeInput by viewModel.loginVerificationCodeInput.collectAsState()
    val adminPhone by viewModel.adminPhoneNumber.collectAsState()
    val adminPass by viewModel.adminPassword.collectAsState()

    var emailOrPhone by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Signup Full Information State
    var signUpPhone by remember { mutableStateOf("") }
    var signUpGender by remember { mutableStateOf("Male") }
    var signUpAge by remember { mutableStateOf("28") }
    var signUpBloodGroup by remember { mutableStateOf("O+") }
    var signUpMedicalHistory by remember { mutableStateOf("None / Healthy") }
    var signUpEmergencyName by remember { mutableStateOf("Suman Shrestha (Brother)") }
    var signUpEmergencyPhone by remember { mutableStateOf("+977-9841998877") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    // Hidden Admin Portal Triggers
    var showSecretAdminDialog by remember { mutableStateOf(false) }
    var secretAdminTapCount by remember { mutableIntStateOf(0) }
    var secretAdminInputPhone by remember { mutableStateOf("") }
    var secretAdminInputPass by remember { mutableStateOf("") }

    // Signup Legal Consent Checkboxes (From Center Card Mockup)
    var agreedToPolicy by remember { mutableStateOf(true) }
    var agreedToTelemedicine by remember { mutableStateOf(true) }

    var recoveryEmail by remember { mutableStateOf("user@lifscan.com") }

    var showCountryPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var legalDialogContent by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current

    fun triggerBiometricLogin() {
        BiometricAuthHelper.showBiometricPrompt(
            context = context,
            title = "Lifscan Biometric Login",
            subtitle = "Confirm fingerprint or face to sign in directly",
            description = "Protected by AES-256 SQLCipher & Android Keystore",
            onSuccess = {
                viewModel.unlockApp()
                viewModel.loginWithCredentials(
                    emailOrPhone = if (emailOrPhone.isNotBlank()) emailOrPhone else "9841234567",
                    password = if (passwordInput.isNotBlank()) passwordInput else "Demo@1234"
                )
            },
            onError = { errorCode, _ ->
                if (errorCode == -1 || errorCode == androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS || errorCode == androidx.biometric.BiometricPrompt.ERROR_HW_NOT_PRESENT) {
                    viewModel.unlockApp()
                    viewModel.loginWithCredentials(
                        emailOrPhone = if (emailOrPhone.isNotBlank()) emailOrPhone else "9841234567",
                        password = if (passwordInput.isNotBlank()) passwordInput else "Demo@1234"
                    )
                }
            },
            onFailed = {}
        )
    }

    val bgCanvas = if (isDarkMode) Color(0xFF0F0F12) else PrototypeWarmBgLight
    val cardBg = if (isDarkMode) Color(0xFF1A1A22) else PrototypeCardBgLight
    val primaryText = if (isDarkMode) Color.White else PrototypeIndigoPrimary
    val buttonBg = if (isDarkMode) Color(0xFF4F46E5) else PrototypeIndigoDeep

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = buttonBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.HealthAndSafety,
                                    contentDescription = "Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Lifscan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = primaryText
                        )
                    }
                },
                actions = {
                    // Quick Role Switcher
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = buttonBg.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedRole.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color(0xFFA5B4FC) else buttonBg
                            )
                        }
                    }

                    // Language Picker
                    FilledTonalButton(
                        onClick = { showLanguagePicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("lang_picker_btn")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Language", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(currentLang, fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.size(36.dp).testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCanvas)
            )
        },
        containerColor = bgCanvas
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bgCanvas),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Outer Card matching mockup shape
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E38) else PrototypeBorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .testTag("entrance_card_root")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val effectiveScreenMode = if (isLoginVerifying) AuthScreenMode.LOGIN_VERIFICATION else screenMode

                        AnimatedContent(
                            targetState = effectiveScreenMode,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "auth_screen_transition"
                        ) { mode ->
                            when (mode) {
                                AuthScreenMode.LOGIN -> LoginView(
                                    emailOrPhone = emailOrPhone,
                                    onEmailOrPhoneChange = { 
                                        emailOrPhone = it 
                                        if (it.trim() == adminPhone.trim() || it.trim().contains("9768752782")) {
                                            viewModel.setSelectedRole(UserRole.ADMIN)
                                        }
                                    },
                                    password = passwordInput,
                                    onPasswordChange = { passwordInput = it },
                                    isPasswordVisible = isPasswordVisible,
                                    onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                                    rememberMe = rememberMe,
                                    onToggleRememberMe = { rememberMe = !rememberMe },
                                    onForgotPasswordClick = { screenMode = AuthScreenMode.FORGOT_PASSWORD },
                                    onSignUpClick = { screenMode = AuthScreenMode.SIGN_UP },
                                    selectedRole = selectedRole,
                                    onRoleSelect = { viewModel.setSelectedRole(it) },
                                    selectedCountry = selectedCountry,
                                    onPickCountry = { showCountryPicker = true },
                                    adminPhone = adminPhone,
                                    onSecretAdminClick = { showSecretAdminDialog = true },
                                    onLoginSubmit = {
                                        if (phoneInput.isBlank()) {
                                            viewModel.setPhoneNumber(if (emailOrPhone.isNotBlank()) emailOrPhone else "9841234567")
                                        }
                                        viewModel.loginWithCredentials(
                                            emailOrPhone = if (emailOrPhone.isNotBlank()) emailOrPhone else "9841234567",
                                            password = if (passwordInput.isNotBlank()) passwordInput else "Demo@1234"
                                        )
                                    },
                                    onBiometricQuickLogin = { triggerBiometricLogin() },
                                    isDarkMode = isDarkMode,
                                    buttonBg = buttonBg
                                )

                                AuthScreenMode.LOGIN_VERIFICATION -> LoginVerificationView(
                                    emailOrPhone = pendingSession?.emailOrPhone ?: (if (emailOrPhone.isNotBlank()) emailOrPhone else adminPhone),
                                    role = pendingSession?.role ?: selectedRole,
                                    verificationCode = verificationCodeInput,
                                    onCodeChange = { viewModel.setLoginVerificationCode(it) },
                                    demoCode = generatedVerificationCode,
                                    onVerifySubmit = {
                                        viewModel.verifyAndCompleteLogin(verificationCodeInput)
                                    },
                                    onResendCode = {
                                        val target = pendingSession?.emailOrPhone ?: (if (emailOrPhone.isNotBlank()) emailOrPhone else "9841234567")
                                        viewModel.initiateLoginVerification(target, selectedRole, nameInput)
                                    },
                                    onBackToLogin = {
                                        viewModel.cancelLoginVerification()
                                        screenMode = AuthScreenMode.LOGIN
                                    },
                                    isDarkMode = isDarkMode,
                                    buttonBg = buttonBg
                                )

                                AuthScreenMode.SIGN_UP -> SignUpView(
                                    nameInput = nameInput,
                                    onNameChange = { viewModel.setUserName(it) },
                                    emailInput = emailOrPhone,
                                    onEmailChange = { emailOrPhone = it },
                                    phoneInput = signUpPhone,
                                    onPhoneChange = { signUpPhone = it },
                                    password = passwordInput,
                                    onPasswordChange = { passwordInput = it },
                                    confirmPassword = confirmPasswordInput,
                                    onConfirmPasswordChange = { confirmPasswordInput = it },
                                    isPasswordVisible = isPasswordVisible,
                                    onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                                    gender = signUpGender,
                                    onGenderChange = { signUpGender = it },
                                    age = signUpAge,
                                    onAgeChange = { signUpAge = it },
                                    bloodGroup = signUpBloodGroup,
                                    onBloodGroupChange = { signUpBloodGroup = it },
                                    medicalHistory = signUpMedicalHistory,
                                    onMedicalHistoryChange = { signUpMedicalHistory = it },
                                    emergencyName = signUpEmergencyName,
                                    onEmergencyNameChange = { signUpEmergencyName = it },
                                    emergencyPhone = signUpEmergencyPhone,
                                    onEmergencyPhoneChange = { signUpEmergencyPhone = it },
                                    selectedRole = selectedRole,
                                    onRoleSelect = { viewModel.setSelectedRole(it) },
                                    doctorLicense = doctorLicense,
                                    onDoctorLicenseChange = { viewModel.setDoctorLicense(it) },
                                    doctorSpecialty = doctorSpecialty,
                                    onDoctorSpecialtyChange = { viewModel.setDoctorSpecialty(it) },
                                    driverLicense = driverLicense,
                                    onDriverLicenseChange = { viewModel.setDriverLicense(it) },
                                    ambulancePlate = ambulancePlate,
                                    onAmbulancePlateChange = { viewModel.setAmbulancePlate(it) },
                                    agreedToPolicy = agreedToPolicy,
                                    onTogglePolicy = { agreedToPolicy = !agreedToPolicy },
                                    agreedToTelemedicine = agreedToTelemedicine,
                                    onToggleTelemedicine = { agreedToTelemedicine = !agreedToTelemedicine },
                                    onShowLegalPolicy = { legalDialogContent = "Lifscan Terms & Privacy Policy:\n\n1. All biometric, diagnostic scans, and medical vault records are protected under SQLCipher 256-bit AES client-side encryption.\n2. In an emergency, SOS broadcasts GPS and emergency contact notices to verified dispatch units.\n3. Telemedicine consultations are encrypted peer-to-peer." },
                                    onShowTelemedicineConsent = { legalDialogContent = "Lifscan Telemedicine Consent:\n\n1. I understand that video consultations and AI symptom triage are supplementary health diagnostics.\n2. In acute life-threatening situations, use the Red SOS 102/112 emergency hotline immediately." },
                                    onSignInClick = { screenMode = AuthScreenMode.LOGIN },
                                    onCreateAccount = {
                                        val finalName = if (nameInput.isNotBlank()) nameInput else "Aayush Shrestha"
                                        val finalEmail = if (emailOrPhone.isNotBlank()) emailOrPhone else "user@lifscan.com"
                                        val finalPhone = if (signUpPhone.isNotBlank()) signUpPhone else "9841234567"
                                        val finalAge = signUpAge.toIntOrNull() ?: 28
                                        viewModel.registerFullAccount(
                                            name = finalName,
                                            email = finalEmail,
                                            phone = finalPhone,
                                            role = selectedRole,
                                            gender = signUpGender,
                                            age = finalAge,
                                            bloodGroup = signUpBloodGroup,
                                            medicalHistory = signUpMedicalHistory,
                                            emergencyContactName = signUpEmergencyName,
                                            emergencyContactPhone = signUpEmergencyPhone,
                                            licenseDoc = if (selectedRole == UserRole.DOCTOR) doctorLicense else driverLicense,
                                            specialty = doctorSpecialty,
                                            plate = ambulancePlate
                                        )
                                    },
                                    isDarkMode = isDarkMode,
                                    buttonBg = buttonBg
                                )

                                AuthScreenMode.FORGOT_PASSWORD -> ForgotPasswordView(
                                    recoveryEmail = recoveryEmail,
                                    onRecoveryEmailChange = { recoveryEmail = it },
                                    onSendClick = {
                                        screenMode = AuthScreenMode.SET_NEW_PASSWORD
                                    },
                                    onBackToLogin = { screenMode = AuthScreenMode.LOGIN },
                                    isDarkMode = isDarkMode,
                                    buttonBg = buttonBg
                                )

                                AuthScreenMode.SET_NEW_PASSWORD -> SetNewPasswordView(
                                    email = recoveryEmail,
                                    newPassword = newPasswordInput,
                                    onNewPasswordChange = { newPasswordInput = it },
                                    isPasswordVisible = isNewPasswordVisible,
                                    onTogglePasswordVisibility = { isNewPasswordVisible = !isNewPasswordVisible },
                                    onConfirmClick = {
                                        screenMode = AuthScreenMode.LOGIN
                                    },
                                    onBackToLogin = { screenMode = AuthScreenMode.LOGIN },
                                    isDarkMode = isDarkMode,
                                    buttonBg = buttonBg
                                )
                            }
                        }

                        // Auth Error Alert
                        if (authError != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmergencyRed.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = authError ?: "",
                                    color = EmergencyRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Demo Auto-Login Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = "Encrypted",
                        tint = SuccessGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "AES-256 SQLCipher Encrypted • Lifscan Emergency Network",
                        fontSize = 11.sp,
                        color = PrototypeTextMuted
                    )
                }
            }
        }
    }

    // Modal: Asian Country Picker Dialog
    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = { Text("Select Region & Dial Code", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search country (e.g. Nepal, +977)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val filtered = AsianCountriesProvider.countries.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.dialCode.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        filtered.forEach { country ->
                            ListItem(
                                leadingContent = { Text(country.flagEmoji, fontSize = 22.sp) },
                                headlineContent = { Text(country.name, fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("Dial code: ${country.dialCode}", fontSize = 11.sp) },
                                trailingContent = {
                                    Text(country.dialCode, fontWeight = FontWeight.Bold, color = PrototypeIndigoDeep)
                                },
                                modifier = Modifier
                                    .clickable {
                                        viewModel.setCountry(country)
                                        showCountryPicker = false
                                    }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal: Multi-language Picker Dialog
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("Select App Language", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    LanguageHelper.supportedLanguages.forEach { (langKey, langDisplay) ->
                        val isSelected = currentLang == langKey
                        ListItem(
                            headlineContent = {
                                Text(
                                    langDisplay,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) buttonBg else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = buttonBg)
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.setLanguage(langKey)
                                showLanguagePicker = false
                            }
                        )
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Modal: Legal / Telemedicine Policy Dialog
    if (legalDialogContent != null) {
        AlertDialog(
            onDismissRequest = { legalDialogContent = null },
            title = { Text("Legal & Telemedicine Terms", fontWeight = FontWeight.Bold) },
            text = {
                Text(legalDialogContent ?: "", fontSize = 13.sp, lineHeight = 19.sp)
            },
            confirmButton = {
                Button(
                    onClick = { legalDialogContent = null },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBg)
                ) {
                    Text("I Understand & Agree")
                }
            }
        )
    }

    // Modal: Hidden Admin System Dialog
    if (showSecretAdminDialog) {
        AlertDialog(
            onDismissRequest = { showSecretAdminDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = buttonBg.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.AdminPanelSettings,
                            contentDescription = null,
                            tint = buttonBg,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text("Master Admin Portal", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Restricted Entrance for Lifscan Central Command & Facility Controls.",
                        fontSize = 12.sp,
                        color = PrototypeTextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Default Master Admin Access:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                            Text(
                                "Phone: 9768752782 | Pass: 9768752782",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color(0xFFA5B4FC) else buttonBg
                            )
                            Text(
                                "Note: Credentials can only be changed by Admin inside Master Controls.",
                                fontSize = 10.5.sp,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Admin Phone Number", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = secretAdminInputPhone,
                        onValueChange = { secretAdminInputPhone = it },
                        placeholder = { Text(adminPhone.ifBlank { "9768752782" }, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("secret_admin_phone_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Admin Master Password", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = secretAdminInputPass,
                        onValueChange = { secretAdminInputPass = it },
                        placeholder = { Text("Enter Master Password", fontSize = 13.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("secret_admin_pass_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ph = if (secretAdminInputPhone.isNotBlank()) secretAdminInputPhone.trim() else adminPhone
                        val pass = if (secretAdminInputPass.isNotBlank()) secretAdminInputPass.trim() else adminPass
                        showSecretAdminDialog = false
                        viewModel.setSelectedRole(UserRole.ADMIN)
                        viewModel.loginWithCredentials(ph, pass)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
                    modifier = Modifier.testTag("secret_admin_login_btn")
                ) {
                    Text("Access Admin Console")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecretAdminDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================================
// MOCKUP VIEW 1: LOGIN (Matching Bottom-Left Card in Prototype)
// =========================================================================
@Composable
fun LoginView(
    emailOrPhone: String,
    onEmailOrPhoneChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    rememberMe: Boolean,
    onToggleRememberMe: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    selectedRole: UserRole,
    onRoleSelect: (UserRole) -> Unit,
    selectedCountry: AsianCountry,
    onPickCountry: () -> Unit,
    adminPhone: String,
    onSecretAdminClick: () -> Unit,
    onLoginSubmit: () -> Unit,
    onBiometricQuickLogin: () -> Unit = {},
    isDarkMode: Boolean,
    buttonBg: Color
) {
    val isAdminMode = selectedRole == UserRole.ADMIN || emailOrPhone.trim() == adminPhone.trim() || emailOrPhone.contains("9768752782")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prototype Title: "Login" + Secret Hidden Admin Trigger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else PrototypeIndigoPrimary,
                textAlign = TextAlign.Center
            )
            // Hidden Admin Access Shield Button
            IconButton(
                onClick = onSecretAdminClick,
                modifier = Modifier
                    .size(28.dp)
                    .padding(start = 6.dp)
                    .testTag("secret_admin_trigger_btn")
            ) {
                Icon(
                    imageVector = if (isAdminMode) Icons.Default.AdminPanelSettings else Icons.Outlined.Shield,
                    contentDescription = "Admin Portal",
                    tint = if (isAdminMode) Color(0xFFF59E0B) else PrototypeTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Prototype Subtitle: "Welcome back, use your credentials to login"
        Text(
            text = if (isAdminMode) "Master Admin Access - Lifscan Command Hub" else "Welcome back, use your credentials to login",
            fontSize = 13.sp,
            color = if (isAdminMode) Color(0xFF2563EB) else PrototypeTextMuted,
            fontWeight = if (isAdminMode) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )

        // Admin Notification Badge
        if (isAdminMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Admin Credentials: Phone & Pass: $adminPhone (Editable only by Admin)",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Account Role in One Box System (Excludes Admin by default unless Admin mode)
        AccountRoleBoxSelector(
            selectedRole = selectedRole,
            onRoleSelect = onRoleSelect,
            isDarkMode = isDarkMode,
            buttonBg = buttonBg,
            showAdminOption = isAdminMode
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Field 1: Email / Phone
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAdminMode) "Admin Phone Number" else "Email or Phone Number",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = onEmailOrPhoneChange,
                placeholder = { 
                    Text(
                        if (isAdminMode) "Admin Master Phone (e.g. $adminPhone)" else "Email or Phone Number", 
                        fontSize = 13.5.sp, 
                        color = Color.Gray 
                    ) 
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF374151) else PrototypeBorderLight,
                    focusedBorderColor = buttonBg
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Field 2: Password
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isAdminMode) "Admin Password" else "Password",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { 
                    Text(
                        if (isAdminMode) "Admin Master Password" else "Your password", 
                        fontSize = 13.5.sp, 
                        color = Color.Gray 
                    ) 
                },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF374151) else PrototypeBorderLight,
                    focusedBorderColor = buttonBg
                )
            )

            // Small Biometric Logo on Password Down Side (Fingerprint / Face Recognition)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                    border = BorderStroke(1.dp, buttonBg.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clickable { onBiometricQuickLogin() }
                        .testTag("biometric_password_down_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Login Logo",
                            tint = buttonBg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Biometric Sign-In",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color(0xFFC7D2FE) else buttonBg
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Remember me (Left) & Forgot Password (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleRememberMe() }
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { onToggleRememberMe() },
                    colors = CheckboxDefaults.colors(checkedColor = buttonBg),
                    modifier = Modifier.size(24.dp).testTag("remember_me_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Remember me",
                    fontSize = 12.5.sp,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            TextButton(
                onClick = onForgotPasswordClick,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.testTag("forgot_password_btn")
            ) {
                Text(
                    text = "Forgot Password",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color(0xFFA5B4FC) else PrototypeIndigoDeep
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Small Biometric Logo on Password Down Side (Fingerprint / Face Recognition)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, buttonBg.copy(alpha = 0.22f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBiometricQuickLogin() }
                .testTag("biometric_quick_login_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = buttonBg.copy(alpha = 0.12f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Sign-In Logo",
                            tint = if (isDarkMode) Color(0xFFA5B4FC) else buttonBg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick Biometric Sign-In (Fingerprint / Face)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color(0xFFE2E8F0) else PrototypeIndigoPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Solid Indigo Primary Button: "Login"
        Button(
            onClick = onLoginSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("submit_auth_btn"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg)
        ) {
            Text(
                text = if (isAdminMode) "Login as Administrator" else "Login",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Footer: "Don't have an account? Sign up"
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                fontSize = 13.sp,
                color = PrototypeTextMuted
            )
            Text(
                text = "Sign up",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFFA5B4FC) else PrototypeIndigoDeep,
                modifier = Modifier
                    .clickable { onSignUpClick() }
                    .testTag("switch_to_signup_btn")
            )
        }
    }
}

// =========================================================================
// MOCKUP VIEW: 2FA VERIFICATION (When Login/Signup Requires Verification)
// =========================================================================
@Composable
fun LoginVerificationView(
    emailOrPhone: String,
    role: UserRole,
    verificationCode: String,
    onCodeChange: (String) -> Unit,
    demoCode: String,
    onVerifySubmit: () -> Unit,
    onResendCode: () -> Unit,
    onBackToLogin: () -> Unit,
    isDarkMode: Boolean,
    buttonBg: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = buttonBg.copy(alpha = 0.12f),
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Security,
                    contentDescription = "2FA Verification",
                    tint = if (isDarkMode) Color(0xFFA5B4FC) else buttonBg,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Two-Factor Verification",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkMode) Color.White else PrototypeIndigoPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Enter the 6-digit verification code sent to your registered phone or email to authorize login",
            fontSize = 12.5.sp,
            color = PrototypeTextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = buttonBg.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, buttonBg.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    tint = buttonBg,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = emailOrPhone.ifBlank { "9768752782" },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFFA5B4FC) else buttonBg
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Demo Autofill Pill for Instant Verification Testing
        if (demoCode.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCodeChange(demoCode) }
                    .testTag("autofill_demo_code_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = "Code",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Demo Security Code: $demoCode (Tap to autofill)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 6-digit Code Input
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "6-Digit Security Code",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { if (it.length <= 6) onCodeChange(it) },
                placeholder = { Text("e.g. $demoCode", fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verification_code_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF374151) else PrototypeBorderLight,
                    focusedBorderColor = buttonBg
                )
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Verify & Sign In Button
        Button(
            onClick = onVerifySubmit,
            enabled = verificationCode.length >= 4,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("verify_and_login_btn"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg)
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Verify & Sign In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Resend code option
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Didn't receive code? ",
                fontSize = 12.5.sp,
                color = PrototypeTextMuted
            )
            Text(
                text = "Resend Code",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFFA5B4FC) else PrototypeIndigoDeep,
                modifier = Modifier
                    .clickable { onResendCode() }
                    .testTag("resend_code_btn")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.testTag("cancel_verification_btn")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Back to Login",
                fontSize = 13.sp,
                color = PrototypeTextMuted
            )
        }
    }
}

// =========================================================================
// MOCKUP VIEW 2: SIGN UP WITH COMPREHENSIVE USER INFORMATION
// =========================================================================
@Composable
fun SignUpView(
    nameInput: String,
    onNameChange: (String) -> Unit,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    gender: String,
    onGenderChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    bloodGroup: String,
    onBloodGroupChange: (String) -> Unit,
    medicalHistory: String,
    onMedicalHistoryChange: (String) -> Unit,
    emergencyName: String,
    onEmergencyNameChange: (String) -> Unit,
    emergencyPhone: String,
    onEmergencyPhoneChange: (String) -> Unit,
    selectedRole: UserRole,
    onRoleSelect: (UserRole) -> Unit,
    doctorLicense: String,
    onDoctorLicenseChange: (String) -> Unit,
    doctorSpecialty: String,
    onDoctorSpecialtyChange: (String) -> Unit,
    driverLicense: String,
    onDriverLicenseChange: (String) -> Unit,
    ambulancePlate: String,
    onAmbulancePlateChange: (String) -> Unit,
    agreedToPolicy: Boolean,
    onTogglePolicy: () -> Unit,
    agreedToTelemedicine: Boolean,
    onToggleTelemedicine: () -> Unit,
    onShowLegalPolicy: () -> Unit,
    onShowTelemedicineConsent: () -> Unit,
    onSignInClick: () -> Unit,
    onCreateAccount: () -> Unit,
    isDarkMode: Boolean,
    buttonBg: Color
) {
    val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    val genders = listOf("Male", "Female", "Other")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prototype Title: "Sign up"
        Text(
            text = "Sign up",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkMode) Color.White else PrototypeIndigoPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Prototype Subtitle: "Create a new account with complete profile"
        Text(
            text = "Create your comprehensive health & emergency account",
            fontSize = 12.5.sp,
            color = PrototypeTextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Account Role in One Box System (Excludes Admin by default)
        AccountRoleBoxSelector(
            selectedRole = selectedRole,
            onRoleSelect = onRoleSelect,
            isDarkMode = isDarkMode,
            buttonBg = buttonBg,
            showAdminOption = false
        )

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 1: PERSONAL DETAILS
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkMode) Color(0xFF1E1E26) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = buttonBg, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1. Personal Information",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else PrototypeIndigoPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Full Name
                Text("Full Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = onNameChange,
                    placeholder = { Text("e.g. Aayush Shrestha", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("name_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email
                Text("Email Address", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = onEmailChange,
                    placeholder = { Text("e.g. user@lifscan.com", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Phone
                Text("Primary Phone Number", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = onPhoneChange,
                    placeholder = { Text("e.g. 9841234567", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("signup_phone_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Gender & Age in Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            genders.forEach { g ->
                                val isSelected = gender == g
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) buttonBg else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) buttonBg else Color.Gray.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clickable { onGenderChange(g) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = g,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.width(80.dp)) {
                        Text("Age", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = age,
                            onValueChange = { if (it.length <= 3) onAgeChange(it) },
                            placeholder = { Text("28", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: HEALTH & EMERGENCY PROFILE
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkMode) Color(0xFF1E1E26) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalHospital, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "2. Emergency & Health Vault",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else PrototypeIndigoPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Blood Group Pills
                Text("Blood Group", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bloodGroups.take(4).forEach { bg ->
                        val isSelected = bloodGroup == bg
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFFDC2626) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFDC2626) else Color.Gray.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).height(32.dp).clickable { onBloodGroupChange(bg) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = bg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bloodGroups.takeLast(4).forEach { bg ->
                        val isSelected = bloodGroup == bg
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFFDC2626) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFDC2626) else Color.Gray.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).height(32.dp).clickable { onBloodGroupChange(bg) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = bg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Medical History & Known Allergies
                Text("Medical History & Allergies", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = medicalHistory,
                    onValueChange = onMedicalHistoryChange,
                    placeholder = { Text("e.g. Penicillin allergy, mild asthma, none", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_medical_history_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Emergency Contact Name
                Text("Emergency Contact Person", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = emergencyName,
                    onValueChange = onEmergencyNameChange,
                    placeholder = { Text("e.g. Suman Shrestha (Brother/Spouse)", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("signup_emergency_name_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Emergency Contact Phone
                Text("Emergency Contact Phone", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = onEmergencyPhoneChange,
                    placeholder = { Text("e.g. +977-9841998877", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("signup_emergency_phone_input"),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Role-Specific License Inputs
        if (selectedRole == UserRole.DOCTOR) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkMode) Color(0xFF1E1E26) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Doctor Clinical Credentials", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = doctorLicense,
                        onValueChange = onDoctorLicenseChange,
                        placeholder = { Text("NMC Medical Registration No. (e.g. NMC-14529)", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("doctor_license_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = doctorSpecialty,
                        onValueChange = onDoctorSpecialtyChange,
                        placeholder = { Text("Medical Specialty (e.g. Emergency Medicine)", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        } else if (selectedRole == UserRole.AMBULANCE_DRIVER) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkMode) Color(0xFF1E1E26) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF2E2E38) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Ambulance Dispatch Credentials", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = driverLicense,
                        onValueChange = onDriverLicenseChange,
                        placeholder = { Text("Commercial Driving License No.", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_license_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ambulancePlate,
                        onValueChange = onAmbulancePlateChange,
                        placeholder = { Text("Ambulance Vehicle Plate (e.g. BA 1 JA 4521)", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ambulance_plate_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Field: Password with requirement helper
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Account Password",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { Text("Your secure password", fontSize = 13.5.sp, color = Color.Gray) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF374151) else PrototypeBorderLight,
                    focusedBorderColor = buttonBg
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Minimum 8 characters, including letters, numbers, and symbols",
                fontSize = 10.5.sp,
                color = PrototypeTextMuted,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Checkbox 1 (Terms and conditions, privacy policy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = agreedToPolicy,
                onCheckedChange = { onTogglePolicy() },
                colors = CheckboxDefaults.colors(checkedColor = buttonBg),
                modifier = Modifier.size(22.dp).testTag("policy_checkbox")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I have read and agreed to the Lifscan Terms of Service, Medical Privacy Policy, and Emergency SOS Protocol",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier.clickable { onShowLegalPolicy() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Checkbox 2 (Telemedicine consent form)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = agreedToTelemedicine,
                onCheckedChange = { onToggleTelemedicine() },
                colors = CheckboxDefaults.colors(checkedColor = buttonBg),
                modifier = Modifier.size(22.dp).testTag("telemed_checkbox")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I have read and agreed to the Telemedicine Consent and Emergency Dispatch Authorization",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier.clickable { onShowTelemedicineConsent() }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Solid Indigo Primary Button: "Create account"
        Button(
            onClick = onCreateAccount,
            enabled = agreedToPolicy && agreedToTelemedicine,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("create_account_btn"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg)
        ) {
            Text(
                text = "Create Account & Sign In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: "Already have an account? Sign in"
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 13.sp,
                color = PrototypeTextMuted
            )
            Text(
                text = "Sign in",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFFA5B4FC) else PrototypeIndigoDeep,
                modifier = Modifier
                    .clickable { onSignInClick() }
                    .testTag("switch_to_signin_btn")
            )
        }
    }
}

// =========================================================================
// MOCKUP VIEW 3: FORGOT PASSWORD (Matching Top-Right Card in Prototype)
// =========================================================================
@Composable
fun ForgotPasswordView(
    recoveryEmail: String,
    onRecoveryEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackToLogin: () -> Unit,
    isDarkMode: Boolean,
    buttonBg: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prototype Title: "Forgot password"
        Text(
            text = "Forgot password",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkMode) Color.White else PrototypeIndigoPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Prototype Subtitle: "Enter your email address to recover your password"
        Text(
            text = "Enter your email address to recover your password",
            fontSize = 13.sp,
            color = PrototypeTextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Field: Email
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Email",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = recoveryEmail,
                onValueChange = onRecoveryEmailChange,
                placeholder = { Text("Email", fontSize = 13.5.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forgot_email_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF374151) else PrototypeBorderLight,
                    focusedBorderColor = buttonBg
                )
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Primary Button: "Send"
        Button(
            onClick = onSendClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("send_recovery_btn"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg)
        ) {
            Text(
                text = "Send",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: "Back to login"
        Text(
            text = "Back to login",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDarkMode) Color(0xFFA5B4FC) else PrototypeIndigoDeep,
            modifier = Modifier
                .clickable { onBackToLogin() }
                .testTag("back_to_login_btn")
        )
    }
}

// =========================================================================
// MOCKUP VIEW 4: SET NEW PASSWORD (Matching Bottom-Right Card in Prototype)
// =========================================================================
@Composable
fun SetNewPasswordView(
    email: String,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onConfirmClick: () -> Unit,
    onBackToLogin: () -> Unit,
    isDarkMode: Boolean,
    buttonBg: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Prototype Title: "Set new password"
        Text(
            text = "Set new password",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkMode) Color.White else PrototypeIndigoPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Prototype Subtitle: "Create a new password for <email>"
        Text(
            text = "Create a new password for ${if (email.isNotBlank()) email else "user@lifscan.com"}",
            fontSize = 13.sp,
            color = PrototypeTextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Field: Password with requirement text
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Password",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                placeholder = { Text("Password", fontSize = 13.5.sp, color = Color.Gray) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("set_new_password_input"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF374151) else PrototypeBorderLight,
                    focusedBorderColor = buttonBg
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Minimum 8 characters, including uppercase, lowercase, numbers, and special characters",
                fontSize = 10.5.sp,
                color = PrototypeTextMuted,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Primary Button: "Confirm"
        Button(
            onClick = onConfirmClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("confirm_new_password_btn"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg)
        ) {
            Text(
                text = "Confirm",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Footer: "Back to login"
        Text(
            text = "Back to login",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDarkMode) Color(0xFFA5B4FC) else PrototypeIndigoDeep,
            modifier = Modifier
                .clickable { onBackToLogin() }
                .testTag("back_to_login_btn_2")
        )
    }
}

/**
 * Unified "One Box System" for selecting Account Role.
 * Formatted cleanly as a single form field matching the prototype aesthetic.
 */
@Composable
fun AccountRoleBoxSelector(
    selectedRole: UserRole,
    onRoleSelect: (UserRole) -> Unit,
    isDarkMode: Boolean,
    buttonBg: Color,
    showAdminOption: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val selectableRoles = remember(showAdminOption, selectedRole) {
        if (showAdminOption || selectedRole == UserRole.ADMIN) {
            UserRole.values().toList()
        } else {
            UserRole.values().filter { it != UserRole.ADMIN }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Account Role",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .testTag("account_role_box_selector"),
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkMode) Color(0xFF1E1E28) else Color(0xFFFAFAFC),
                border = BorderStroke(
                    1.dp,
                    if (expanded) buttonBg else if (isDarkMode) Color(0xFF374151) else PrototypeBorderLight
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = buttonBg.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (selectedRole) {
                                        UserRole.DOCTOR -> Icons.Outlined.MedicalServices
                                        UserRole.PATIENT -> Icons.Outlined.Person
                                        UserRole.AMBULANCE_DRIVER -> Icons.Outlined.Emergency
                                        UserRole.ADMIN -> Icons.Outlined.AdminPanelSettings
                                    },
                                    contentDescription = selectedRole.displayName,
                                    tint = if (isDarkMode) Color(0xFFA5B4FC) else buttonBg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = selectedRole.displayName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else PrototypeIndigoPrimary
                            )
                            Text(
                                text = when (selectedRole) {
                                    UserRole.DOCTOR -> "Specialist Consultation & Telemedicine"
                                    UserRole.PATIENT -> "Health Monitoring, Vault & SOS"
                                    UserRole.AMBULANCE_DRIVER -> "Emergency Dispatch & Route Nav"
                                    UserRole.ADMIN -> "CMMS Facilities & Diagnostics Ops"
                                },
                                fontSize = 10.5.sp,
                                color = PrototypeTextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Role",
                        tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(if (isDarkMode) Color(0xFF22222E) else Color.White)
            ) {
                selectableRoles.forEach { role ->
                    val isSelected = selectedRole == role
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) buttonBg.copy(alpha = 0.15f) else Color.Transparent,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (role) {
                                                UserRole.DOCTOR -> Icons.Outlined.MedicalServices
                                                UserRole.PATIENT -> Icons.Outlined.Person
                                                UserRole.AMBULANCE_DRIVER -> Icons.Outlined.Emergency
                                                UserRole.ADMIN -> Icons.Outlined.AdminPanelSettings
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) buttonBg else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = role.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) buttonBg else if (isDarkMode) Color.White else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = when (role) {
                                            UserRole.DOCTOR -> "Consultations & Clinical Rx"
                                            UserRole.PATIENT -> "Vault, Vitals & SOS Alert"
                                            UserRole.AMBULANCE_DRIVER -> "Emergency Dispatch & GPS"
                                            UserRole.ADMIN -> "CMMS Hospital Operations"
                                        },
                                        fontSize = 10.5.sp,
                                        color = PrototypeTextMuted
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = buttonBg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onRoleSelect(role)
                            expanded = false
                        },
                        modifier = Modifier.testTag("role_option_${role.name.lowercase()}")
                    )
                }
            }
        }
    }
}

