package com.example.ui.features

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.EmergencyContactEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.*

enum class ProfileSubScreen {
    MAIN,
    HEALTH_DETAILS,
    DOCTOR_CREDENTIALS,
    DRIVER_VEHICLE_INFO,
    MEDICAL_ID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndSettingsScreen(viewModel: LifscanViewModel) {
    var currentSubScreen by remember { mutableStateOf(ProfileSubScreen.MAIN) }
    val currentUser by viewModel.currentUser.collectAsState()
    val userRole = currentUser?.role ?: UserRole.PATIENT

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != ProfileSubScreen.MAIN) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
            } else {
                slideInHorizontally { width -> -width / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "profile_subscreen_transition"
    ) { subScreen ->
        when (subScreen) {
            ProfileSubScreen.MAIN -> ProfileMainView(
                viewModel = viewModel,
                onNavigateToHealthDetails = { currentSubScreen = ProfileSubScreen.HEALTH_DETAILS },
                onNavigateToDoctorsCredentials = { currentSubScreen = ProfileSubScreen.DOCTOR_CREDENTIALS },
                onNavigateToDriverVehicleInfo = { currentSubScreen = ProfileSubScreen.DRIVER_VEHICLE_INFO },
                onNavigateToMedicalID = { currentSubScreen = ProfileSubScreen.MEDICAL_ID },
                onDone = {
                    when (userRole) {
                        UserRole.PATIENT -> viewModel.navigateTo(ScreenNav.PatientDashboard)
                        UserRole.DOCTOR -> viewModel.navigateTo(ScreenNav.DoctorDashboard)
                        UserRole.AMBULANCE_DRIVER -> viewModel.navigateTo(ScreenNav.AmbulanceDashboard)
                        UserRole.ADMIN -> viewModel.navigateTo(ScreenNav.AdminFacilitiesControl)
                    }
                }
            )
            ProfileSubScreen.HEALTH_DETAILS -> HealthDetailsView(
                viewModel = viewModel,
                onBack = { currentSubScreen = ProfileSubScreen.MAIN }
            )
            ProfileSubScreen.DOCTOR_CREDENTIALS -> DoctorCredentialsView(
                viewModel = viewModel,
                onBack = { currentSubScreen = ProfileSubScreen.MAIN }
            )
            ProfileSubScreen.DRIVER_VEHICLE_INFO -> DriverVehicleInfoView(
                viewModel = viewModel,
                onBack = { currentSubScreen = ProfileSubScreen.MAIN }
            )
            ProfileSubScreen.MEDICAL_ID -> MedicalIDView(
                viewModel = viewModel,
                onBack = { currentSubScreen = ProfileSubScreen.MAIN }
            )
        }
    }
}

// =========================================================================
// SCREEN 1: MAIN PROFILE VIEW (Role-Aware for Patient, Doctor, Driver, Admin)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMainView(
    viewModel: LifscanViewModel,
    onNavigateToHealthDetails: () -> Unit,
    onNavigateToDoctorsCredentials: () -> Unit,
    onNavigateToDriverVehicleInfo: () -> Unit,
    onNavigateToMedicalID: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()

    val user = currentUser ?: UserEntity(
        id = "demo_patient",
        phone = "+977-9841234567",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Aayush Shrestha",
        role = UserRole.PATIENT,
        gender = "Male",
        age = 30,
        bloodGroup = "O+"
    )

    var showChecklistModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showAppsModal by remember { mutableStateOf(false) }
    var showResearchModal by remember { mutableStateOf(false) }
    var showDevicesModal by remember { mutableStateOf(false) }
    var showExportConfirmDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSwitchAccountSheet by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val bgCanvasColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val cardBgColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val textSecondary = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isDarkMode) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val appleBlue = Color(0xFF007AFF)

    val roleThemeColor = when (user.role) {
        UserRole.PATIENT -> TealPrimary
        UserRole.DOCTOR -> DoctorPurple
        UserRole.AMBULANCE_DRIVER -> EmergencyRed
        UserRole.ADMIN -> Color(0xFF0284C7)
    }

    val exportButtonTitle = when (user.role) {
        UserRole.DOCTOR -> "Export Clinical & Consultation Ledger"
        UserRole.AMBULANCE_DRIVER -> "Export Emergency Dispatch & Route Logs"
        UserRole.PATIENT, UserRole.ADMIN -> "Export All Health Data"
    }

    Scaffold(
        containerColor = bgCanvasColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${user.role.displayName} Profile",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                },
                navigationIcon = { },
                actions = {
                    TextButton(
                        onClick = onDone,
                        modifier = Modifier.testTag("profile_done_button")
                    ) {
                        Text(
                            "Done",
                            color = roleThemeColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgCanvasColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Photo & Name & Role Badge
            ProfileAvatar(
                size = 92.dp,
                isDarkMode = isDarkMode,
                testTag = "profile_main_avatar",
                role = user.role
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = user.name.ifBlank { "User" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("profile_user_display_name")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Role Badge Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = roleThemeColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, roleThemeColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val roleIcon = when (user.role) {
                        UserRole.PATIENT -> Icons.Default.Person
                        UserRole.DOCTOR -> Icons.Default.MedicalServices
                        UserRole.AMBULANCE_DRIVER -> Icons.Default.LocalHospital
                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                    }
                    Icon(
                        roleIcon,
                        contentDescription = null,
                        tint = roleThemeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        user.role.displayName.uppercase(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = roleThemeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Description based on role
            val subtitleText = when (user.role) {
                UserRole.DOCTOR -> "${user.specialization.ifBlank { "General Medicine" }} • ${user.clinicAffiliation.ifBlank { "Medical Center" }}"
                UserRole.AMBULANCE_DRIVER -> "${user.vehicleType.ifBlank { "Emergency Unit" }} • Plate: ${user.vehicleNumber.ifBlank { "N/A" }}"
                UserRole.PATIENT -> "Blood Type: ${user.bloodGroup.ifBlank { "O+" }} • ${user.countryName} (${user.countryCode})"
                UserRole.ADMIN -> "Lifscan Health Facilities & Dispatch Command Hub"
            }
            Text(
                text = subtitleText,
                fontSize = 12.5.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Group 1: Role-Specific Core Details
            SectionHeaderLabel(text = "Core Profile Details", color = textSecondary)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("profile_group_core_health")
            ) {
                Column {
                    when (user.role) {
                        UserRole.DOCTOR -> {
                            ProfileMenuRow(
                                title = "Doctor Practice & Credentials",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_doctor_credentials_row",
                                onClick = onNavigateToDoctorsCredentials
                            )
                            ProfileMenuRow(
                                title = "Digital Medical License ID",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = false,
                                testTag = "profile_medical_id_row",
                                onClick = onNavigateToMedicalID
                            )
                        }
                        UserRole.AMBULANCE_DRIVER -> {
                            ProfileMenuRow(
                                title = "Ambulance Vehicle & EMS Credentials",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_driver_vehicle_row",
                                onClick = onNavigateToDriverVehicleInfo
                            )
                            ProfileMenuRow(
                                title = "Emergency Responder ID",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = false,
                                testTag = "profile_medical_id_row",
                                onClick = onNavigateToMedicalID
                            )
                        }
                        UserRole.PATIENT, UserRole.ADMIN -> {
                            ProfileMenuRow(
                                title = "Health Details",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_health_details_row",
                                onClick = onNavigateToHealthDetails
                            )
                            ProfileMenuRow(
                                title = "Medical ID",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = false,
                                testTag = "profile_medical_id_row",
                                onClick = onNavigateToMedicalID
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Features & Portals
            SectionHeaderLabel(text = "Features & Operations", color = textSecondary)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("profile_group_features")
            ) {
                Column {
                    when (user.role) {
                        UserRole.DOCTOR -> {
                            ProfileMenuRow(
                                title = "Clinical Portal Dashboard",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_doctor_dash_row",
                                onClick = { viewModel.navigateTo(ScreenNav.DoctorDashboard) }
                            )
                            ProfileMenuRow(
                                title = "Dermatology & Skin Biopsy Scanner",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_skin_scanner_row",
                                onClick = { viewModel.navigateTo(ScreenNav.SkinScanner) }
                            )
                            ProfileMenuRow(
                                title = "Clinical AI Diagnostic Assistant",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_ai_chat_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AIChat) }
                            )
                            ProfileMenuRow(
                                title = "Hospital Facilities & Beds Map",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_facilities_map_row",
                                onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) }
                            )
                            ProfileMenuRow(
                                title = "Security & Audit Logs",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = false,
                                testTag = "profile_audit_logs_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AuditLogViewer) }
                            )
                        }
                        UserRole.AMBULANCE_DRIVER -> {
                            ProfileMenuRow(
                                title = "EMS Dispatch Center",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_ambulance_dash_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AmbulanceDashboard) }
                            )
                            ProfileMenuRow(
                                title = "Live GPS Telemetry & Route Tracker",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_ambulance_tracker_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) }
                            )
                            ProfileMenuRow(
                                title = "Trauma Centers & Hospitals",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_facilities_map_row",
                                onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) }
                            )
                            ProfileMenuRow(
                                title = "Security & Audit Logs",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = false,
                                testTag = "profile_audit_logs_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AuditLogViewer) }
                            )
                        }
                        UserRole.PATIENT -> {
                            ProfileMenuRow(
                                title = "Health Checklist",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_health_checklist_row",
                                onClick = { showChecklistModal = true }
                            )
                            ProfileMenuRow(
                                title = "Encrypted Health Records (Vault)",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_health_records_row",
                                onClick = { viewModel.navigateTo(ScreenNav.MedicalVault) }
                            )
                            ProfileMenuRow(
                                title = "Document & Rx AI Scanner",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_doc_scan_row",
                                onClick = { viewModel.navigateTo(ScreenNav.MedicalDocumentScan) }
                            )
                            ProfileMenuRow(
                                title = "Notifications",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = false,
                                testTag = "profile_notifications_row",
                                onClick = { showNotificationsModal = true }
                            )
                        }
                        UserRole.ADMIN -> {
                            ProfileMenuRow(
                                title = "Facility & Regional Control",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_admin_control_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AdminFacilitiesControl) }
                            )
                            ProfileMenuRow(
                                title = "Live EMS Dispatch Monitor",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = true,
                                testTag = "profile_ambulance_tracker_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) }
                            )
                            ProfileMenuRow(
                                title = "Security & Audit Logs",
                                textColor = textPrimary,
                                dividerColor = dividerColor,
                                showDivider = false,
                                testTag = "profile_audit_logs_row",
                                onClick = { viewModel.navigateTo(ScreenNav.AuditLogViewer) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Security, Privacy & Settings
            SectionHeaderLabel(text = "Security & Preferences", color = textSecondary)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("profile_group_privacy")
            ) {
                Column {
                    ProfileMenuRow(
                        title = "Language (${selectedCountry.name})",
                        textColor = textPrimary,
                        dividerColor = dividerColor,
                        showDivider = true,
                        testTag = "profile_language_row",
                        onClick = { showLanguagePicker = true }
                    )
                    ProfileMenuRow(
                        title = if (isDarkMode) "Theme: Dark Mode" else "Theme: Light Mode",
                        textColor = textPrimary,
                        dividerColor = dividerColor,
                        showDivider = true,
                        testTag = "profile_theme_toggle_row",
                        onClick = { viewModel.toggleDarkMode() }
                    )
                    ProfileMenuRow(
                        title = "Connected Medical Devices",
                        textColor = textPrimary,
                        dividerColor = dividerColor,
                        showDivider = true,
                        testTag = "profile_devices_row",
                        onClick = { showDevicesModal = true }
                    )
                    ProfileMenuRow(
                        title = "Apps & Integrations",
                        textColor = textPrimary,
                        dividerColor = dividerColor,
                        showDivider = true,
                        testTag = "profile_apps_row",
                        onClick = { showAppsModal = true }
                    )
                    ProfileMenuRow(
                        title = "Zero-Knowledge Encryption & Privacy",
                        textColor = textPrimary,
                        dividerColor = dividerColor,
                        showDivider = false,
                        testTag = "profile_privacy_row",
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Export Clinical / Dispatch / Health Data
            val exportButtonTitle = when (user.role) {
                UserRole.DOCTOR -> "Export Clinical & Consultation Ledger"
                UserRole.AMBULANCE_DRIVER -> "Export Emergency Dispatch & Route Logs"
                UserRole.PATIENT, UserRole.ADMIN -> "Export All Health Data"
            }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showExportConfirmDialog = true }
                    .testTag("profile_export_health_data_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 15.dp)
                ) {
                    Text(
                        text = exportButtonTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = roleThemeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // ACCOUNT ACTIONS: SIGN IN / SWITCH ACCOUNT & LOGOUT SYSTEM (USER REQUEST)
            // =========================================================================
            SectionHeaderLabel(text = "Account & Session Management", color = textSecondary)

            // Switch / Sign In Account Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSwitchAccountSheet = true }
                    .testTag("profile_switch_account_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SwitchAccount,
                        contentDescription = "Switch Account",
                        tint = roleThemeColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign In / Switch Role Account",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Quickly sign in as Doctor, Driver, Patient, or Admin",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Log Out Card (Destructive Red)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF2C1515) else Color(0xFFFFEEEE)
                ),
                border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
                    .testTag("profile_logout_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Logout,
                        contentDescription = "Log Out",
                        tint = EmergencyRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log Out of Lifscan",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmergencyRed
                        )
                        Text(
                            text = "End active session for ${user.name}",
                            fontSize = 11.5.sp,
                            color = EmergencyRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Footer
            Text(
                text = "Lifscan Secure Healthcare Suite • Session Encrypted\nYour clinical credentials and logs are stored safely with on-device 256-bit AES encryption.",
                fontSize = 11.5.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Modals & Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(Icons.Default.Logout, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(32.dp))
            },
            title = { Text("Log Out of Lifscan?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You will be signed out from your active ${user.role.displayName} session. Encrypted database vault records remain safely preserved on this device.",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    modifier = Modifier.testTag("confirm_logout_btn")
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = textPrimary)
                }
            }
        )
    }

    // Modal Bottom Sheet: Sign In & Switch Account
    if (showSwitchAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSwitchAccountSheet = false },
            containerColor = cardBgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Sign In & Switch Role",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            "Select an account profile or log in with credentials",
                            fontSize = 12.5.sp,
                            color = textSecondary
                        )
                    }
                    IconButton(onClick = { showSwitchAccountSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1-Tap Quick Role Sign In Cards
                val roleProfiles = listOf(
                    Triple(UserRole.DOCTOR, "Dr. Sandeep Adhikari, MD", "Consultant Dermatologist • Bir Hospital & Nepal Skin Centre"),
                    Triple(UserRole.AMBULANCE_DRIVER, "Ramesh Thapa (ALS Driver)", "ALS ICU Cardiac Unit • BA 1 PA 4921"),
                    Triple(UserRole.PATIENT, "Aayush Shrestha (Patient)", "Encrypted Personal Health Vault • O+ Blood"),
                    Triple(UserRole.ADMIN, "Chief Administrator", "Lifscan Health Facilities & Command Center")
                )

                roleProfiles.forEach { (role, name, subtitle) ->
                    val isCurrent = user.role == role
                    val roleColor = when (role) {
                        UserRole.PATIENT -> TealPrimary
                        UserRole.DOCTOR -> DoctorPurple
                        UserRole.AMBULANCE_DRIVER -> EmergencyRed
                        UserRole.ADMIN -> Color(0xFF0284C7)
                    }
                    val roleIcon = when (role) {
                        UserRole.PATIENT -> Icons.Default.Person
                        UserRole.DOCTOR -> Icons.Default.MedicalServices
                        UserRole.AMBULANCE_DRIVER -> Icons.Default.LocalHospital
                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                    }

                    OutlinedCard(
                        onClick = {
                            showSwitchAccountSheet = false
                            viewModel.quickSignInAs(role)
                            Toast.makeText(context, "Switched to ${role.displayName} ($name)", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            if (isCurrent) 2.dp else 1.dp,
                            if (isCurrent) roleColor else dividerColor
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isCurrent) roleColor.copy(alpha = 0.1f) else cardBgColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("switch_account_${role.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = roleColor.copy(alpha = 0.18f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(roleIcon, contentDescription = null, tint = roleColor, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = textPrimary
                                    )
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = roleColor
                                        ) {
                                            Text(
                                                "ACTIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    subtitle,
                                    fontSize = 11.5.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (isCurrent) roleColor else textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigate to Full Login Screen
                OutlinedButton(
                    onClick = {
                        showSwitchAccountSheet = false
                        viewModel.logout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("go_to_full_login_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = appleBlue)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In with Phone Number & OTP", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showExportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExportConfirmDialog = false },
            title = { Text("Export $exportButtonTitle?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Exporting complete encrypted records, activity history, and authentication timestamps as an official PDF report.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportConfirmDialog = false
                    viewModel.exportAndShareHealthProfileFromDatabase(context)
                    Toast.makeText(context, "Exporting encrypted report...", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Export", color = appleBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChecklistModal) {
        HealthChecklistModal(onDismiss = { showChecklistModal = false }, isDarkMode = isDarkMode)
    }

    if (showNotificationsModal) {
        HealthNotificationsModal(onDismiss = { showNotificationsModal = false }, isDarkMode = isDarkMode)
    }

    if (showAppsModal) {
        PrivacyAppsModal(onDismiss = { showAppsModal = false }, isDarkMode = isDarkMode)
    }

    if (showResearchModal) {
        ResearchStudiesModal(onDismiss = { showResearchModal = false }, isDarkMode = isDarkMode)
    }

    if (showDevicesModal) {
        ConnectedDevicesModal(onDismiss = { showDevicesModal = false }, isDarkMode = isDarkMode)
    }

    if (showPrivacyDialog) {
        HealthPrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

// =========================================================================
// SCREEN 2: HEALTH DETAILS VIEW (Apple Health Prototype Screen 2)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDetailsView(
    viewModel: LifscanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val user = currentUser ?: UserEntity(
        id = "demo_patient",
        phone = "+977-9841234567",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Malcolm Owen",
        role = UserRole.PATIENT,
        gender = "Male",
        age = 30,
        bloodGroup = "Not Set"
    )

    var isEditMode by remember { mutableStateOf(false) }

    // Split name into first and last
    val nameParts = user.name.split(" ", limit = 2)
    var firstName by remember(user.name) { mutableStateOf(nameParts.getOrNull(0) ?: "Malcolm") }
    var lastName by remember(user.name) { mutableStateOf(nameParts.getOrNull(1) ?: "Owen") }
    var dateOfBirth by remember { mutableStateOf("Aug 15, 1994 (30 yrs)") }
    var sex by remember(user.gender) { mutableStateOf(if (user.gender == "Not Specified") "Male" else user.gender) }
    var bloodType by remember(user.bloodGroup) { mutableStateOf(if (user.bloodGroup.isBlank()) "Not Set" else user.bloodGroup) }
    var fitzpatrickSkinType by remember { mutableStateOf("Not Set") }
    var wheelchair by remember { mutableStateOf("No") }
    var heartRateMedsCount by remember { mutableStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showSexPicker by remember { mutableStateOf(false) }
    var showBloodTypePicker by remember { mutableStateOf(false) }
    var showSkinTypePicker by remember { mutableStateOf(false) }
    var showWheelchairPicker by remember { mutableStateOf(false) }
    var showPregnancySetupModal by remember { mutableStateOf(false) }
    var showHeartMedsModal by remember { mutableStateOf(false) }

    val bgCanvasColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val cardBgColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val textSecondary = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isDarkMode) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val appleBlue = Color(0xFF007AFF)

    fun saveChanges() {
        val updatedFullName = "${firstName.trim()} ${lastName.trim()}".trim()
        val updatedUser = user.copy(
            name = updatedFullName.ifBlank { "Malcolm Owen" },
            gender = sex,
            bloodGroup = bloodType
        )
        viewModel.updateUserProfile(updatedUser)
        Toast.makeText(context, "Health Details Saved", Toast.LENGTH_SHORT).show()
        isEditMode = false
    }

    Scaffold(
        containerColor = bgCanvasColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Health Details",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.testTag("health_details_back_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Profile",
                                tint = appleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Profile",
                                color = appleBlue,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isEditMode) {
                                saveChanges()
                            } else {
                                isEditMode = true
                            }
                        },
                        modifier = Modifier.testTag("health_details_edit_done_btn")
                    ) {
                        Text(
                            if (isEditMode) "Done" else "Edit",
                            color = appleBlue,
                            fontSize = 17.sp,
                            fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCanvasColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered Profile Avatar
            ProfileAvatar(
                size = 80.dp,
                isDarkMode = isDarkMode,
                testTag = "health_details_avatar"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Group 1: Demographics Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("health_details_demographics_card")
            ) {
                Column {
                    if (isEditMode) {
                        EditableFieldRow(
                            label = "First Name",
                            value = firstName,
                            onValueChange = { firstName = it },
                            textColor = textPrimary,
                            dividerColor = dividerColor
                        )
                        EditableFieldRow(
                            label = "Last Name",
                            value = lastName,
                            onValueChange = { lastName = it },
                            textColor = textPrimary,
                            dividerColor = dividerColor
                        )
                    } else {
                        StaticDetailRow(
                            label = "First Name",
                            value = firstName,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Last Name",
                            value = lastName,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                    }

                    InteractiveDetailRow(
                        label = "Date of Birth",
                        value = dateOfBirth,
                        textColor = textPrimary,
                        secondaryColor = textSecondary,
                        dividerColor = dividerColor,
                        showDivider = true,
                        testTag = "detail_dob_row",
                        onClick = { showDatePicker = true }
                    )

                    InteractiveDetailRow(
                        label = "Sex",
                        value = sex,
                        textColor = textPrimary,
                        secondaryColor = textSecondary,
                        dividerColor = dividerColor,
                        showDivider = true,
                        testTag = "detail_sex_row",
                        onClick = { showSexPicker = true }
                    )

                    InteractiveDetailRow(
                        label = "Blood Type",
                        value = bloodType,
                        textColor = textPrimary,
                        secondaryColor = textSecondary,
                        dividerColor = dividerColor,
                        showDivider = true,
                        testTag = "detail_blood_type_row",
                        onClick = { showBloodTypePicker = true }
                    )

                    InteractiveDetailRow(
                        label = "Fitzpatrick Skin Type",
                        value = fitzpatrickSkinType,
                        textColor = textPrimary,
                        secondaryColor = textSecondary,
                        dividerColor = dividerColor,
                        showDivider = false,
                        testTag = "detail_skin_type_row",
                        onClick = { showSkinTypePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 2: Mobility & Cycle Tracking
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("health_details_mobility_card")
            ) {
                Column {
                    InteractiveDetailRow(
                        label = "Wheelchair",
                        value = wheelchair,
                        textColor = textPrimary,
                        secondaryColor = textSecondary,
                        dividerColor = dividerColor,
                        showDivider = false,
                        testTag = "detail_wheelchair_row",
                        onClick = { showWheelchairPicker = true }
                    )
                }
            }

            Text(
                text = "Track pushes instead of steps on Apple Watch in the Activity app, and record them to Health when this setting is on, your iPhone stops tracking steps.",
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Set Up Pregnancy in Cycle Tracking
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPregnancySetupModal = true }
                    .testTag("setup_pregnancy_cycle_tracking_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Set Up Pregnancy in Cycle Tracking",
                        fontSize = 16.sp,
                        color = appleBlue,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 3: Medications That Affect Heart Rate
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("health_details_heart_meds_card")
            ) {
                InteractiveDetailRow(
                    label = "Medications That Affect Heart Rate",
                    value = heartRateMedsCount.toString(),
                    textColor = textPrimary,
                    secondaryColor = textSecondary,
                    dividerColor = dividerColor,
                    showDivider = false,
                    testTag = "detail_heart_rate_meds_row",
                    onClick = { showHeartMedsModal = true }
                )
            }

            Text(
                text = "Beta-blockers or calcium channel blockers can limit your heart rate. Lifscan can take this into account when estimating your cardio fitness levels.",
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Pickers & Modals
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val age = Calendar.getInstance().get(Calendar.YEAR) - year
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
                dateOfBirth = "${sdf.format(chosenCal.time)} ($age yrs)"
                showDatePicker = false
            },
            1994, 7, 15
        ).show()
    }

    if (showSexPicker) {
        SingleSelectOptionDialog(
            title = "Select Biological Sex",
            options = listOf("Male", "Female", "Other", "Not Set"),
            selectedOption = sex,
            onSelect = {
                sex = it
                showSexPicker = false
            },
            onDismiss = { showSexPicker = false }
        )
    }

    if (showBloodTypePicker) {
        SingleSelectOptionDialog(
            title = "Select Blood Type",
            options = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-", "Not Set"),
            selectedOption = bloodType,
            onSelect = {
                bloodType = it
                showBloodTypePicker = false
            },
            onDismiss = { showBloodTypePicker = false }
        )
    }

    if (showSkinTypePicker) {
        SingleSelectOptionDialog(
            title = "Fitzpatrick Skin Type",
            options = listOf(
                "Type I (Always burns, never tans)",
                "Type II (Usually burns, tans minimally)",
                "Type III (Sometimes mild burn, tans uniformly)",
                "Type IV (Burns minimally, always tans well)",
                "Type V (Very rarely burns, tans easily)",
                "Type VI (Never burns, deeply pigmented)",
                "Not Set"
            ),
            selectedOption = fitzpatrickSkinType,
            onSelect = {
                fitzpatrickSkinType = it.substringBefore(" (")
                showSkinTypePicker = false
            },
            onDismiss = { showSkinTypePicker = false }
        )
    }

    if (showWheelchairPicker) {
        SingleSelectOptionDialog(
            title = "Wheelchair User",
            options = listOf("No", "Yes"),
            selectedOption = wheelchair,
            onSelect = {
                wheelchair = it
                showWheelchairPicker = false
            },
            onDismiss = { showWheelchairPicker = false }
        )
    }

    if (showPregnancySetupModal) {
        PregnancyCycleTrackingModal(onDismiss = { showPregnancySetupModal = false }, isDarkMode = isDarkMode)
    }

    if (showHeartMedsModal) {
        HeartRateMedicationsModal(
            currentCount = heartRateMedsCount,
            onUpdateCount = { heartRateMedsCount = it },
            onDismiss = { showHeartMedsModal = false },
            isDarkMode = isDarkMode
        )
    }
}

// =========================================================================
// SCREEN 3: MEDICAL ID VIEW (Apple Health Prototype Screen 3)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalIDView(
    viewModel: LifscanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val emergencyContacts by viewModel.patientEmergencyContacts.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val user = currentUser ?: UserEntity(
        id = "demo_patient",
        phone = "+977-9841234567",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Malcolm Owen",
        role = UserRole.PATIENT,
        gender = "Male",
        age = 30,
        bloodGroup = "O+"
    )

    var showWhenLocked by remember { mutableStateOf(true) }
    var shareDuringEmergencyCall by remember { mutableStateOf(true) }

    var medicalConditions by remember { mutableStateOf(mutableListOf<String>()) }
    var pregnancyInfo by remember { mutableStateOf("") }
    var activeMedications by remember { mutableStateOf(mutableListOf("Paracetamol 500mg (SOS)")) }
    var allergiesList by remember { mutableStateOf(mutableListOf("Penicillin (Mild Rash)")) }

    var showAddConditionDialog by remember { mutableStateOf(false) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }
    var showAddAllergyDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val bgCanvasColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val cardBgColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val textSecondary = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isDarkMode) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val appleBlue = Color(0xFF007AFF)
    val medicalRed = Color(0xFFFF3B30)

    Scaffold(
        containerColor = bgCanvasColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "✳ ",
                            color = medicalRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Medical ID",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = textPrimary
                        )
                    }
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.testTag("medical_id_back_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Profile",
                                tint = appleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Profile",
                                color = appleBlue,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("medical_id_edit_btn")
                    ) {
                        Text(
                            "Edit",
                            color = appleBlue,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCanvasColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // SECTION: EMERGENCY ACCESS
            SectionHeaderLabel(text = "EMERGENCY ACCESS", color = textSecondary)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("medical_id_emergency_access_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Show When Locked",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary
                        )
                        Switch(
                            checked = showWhenLocked,
                            onCheckedChange = { showWhenLocked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759)
                            ),
                            modifier = Modifier.testTag("toggle_show_when_locked")
                        )
                    }
                }
            }

            Text(
                text = "Your Medical ID can be viewed when device is locked by tapping Emergency, then Medical ID. On connected devices, press and hold the power button and drag the Medical ID slider to the right.",
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Share During Emergency Call",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = textPrimary
                        )
                        Switch(
                            checked = shareDuringEmergencyCall,
                            onCheckedChange = { shareDuringEmergencyCall = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759)
                            ),
                            modifier = Modifier.testTag("toggle_share_during_emergency")
                        )
                    }
                }
            }

            Text(
                text = "If you call Emergency Services, your device can share your Medical ID with emergency dispatchers to help responders treat you.",
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: PHOTO AND INFORMATION
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photo and Information",
                    fontSize = 13.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Edit",
                    fontSize = 14.sp,
                    color = appleBlue,
                    modifier = Modifier.clickable { showEditProfileDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("medical_id_photo_info_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name.ifBlank { "Malcolm Owen" },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "30 yrs • Aug 15, 1994",
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                        Text(
                            text = "Blood Group: ${user.bloodGroup.ifBlank { "O+" }}",
                            fontSize = 13.sp,
                            color = medicalRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    ProfileAvatar(
                        size = 64.dp,
                        isDarkMode = isDarkMode,
                        testTag = "medical_id_avatar"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: Pregnancy / Medical Conditions
            MedicalIdSectionCard(
                title = "Medical Conditions",
                titleColor = medicalRed,
                actionLabel = "Add",
                items = medicalConditions,
                emptyPlaceholder = "None Recorded",
                onActionClick = { showAddConditionDialog = true },
                cardBgColor = cardBgColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                appleBlue = appleBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: Medications
            MedicalIdSectionCard(
                title = "Medications",
                titleColor = medicalRed,
                actionLabel = "Add",
                items = activeMedications,
                emptyPlaceholder = "None Recorded",
                onActionClick = { showAddMedicationDialog = true },
                cardBgColor = cardBgColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                appleBlue = appleBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: Allergies
            MedicalIdSectionCard(
                title = "Allergies",
                titleColor = medicalRed,
                actionLabel = "Add",
                items = allergiesList,
                emptyPlaceholder = "No Known Allergies",
                onActionClick = { showAddAllergyDialog = true },
                cardBgColor = cardBgColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                appleBlue = appleBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION: Emergency Contacts
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emergency Contacts",
                    fontSize = 13.sp,
                    color = medicalRed,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Add",
                    fontSize = 14.sp,
                    color = appleBlue,
                    modifier = Modifier.clickable { showAddContactDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("medical_id_emergency_contacts_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (emergencyContacts.isEmpty()) {
                        Column {
                            Text(
                                text = "Dr. Rajesh Sharma",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "+977-9841112233 • Primary Care Physician",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = dividerColor)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Pooja Shrestha",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "+977-9801234567 • Spouse",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                    } else {
                        emergencyContacts.forEachIndexed { index, contact ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = dividerColor)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                text = contact.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "${contact.phone} • ${contact.relationship}",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "When you use Emergency SOS to call emergency services, it also sends a message with your current location to your emergency contacts.",
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Add Condition Dialog
    if (showAddConditionDialog) {
        AddSimpleItemDialog(
            title = "Add Medical Condition",
            placeholder = "e.g. Asthma, Type 2 Diabetes, Hypertension",
            onConfirm = {
                if (it.isNotBlank()) medicalConditions.add(it.trim())
                showAddConditionDialog = false
            },
            onDismiss = { showAddConditionDialog = false }
        )
    }

    // Add Medication Dialog
    if (showAddMedicationDialog) {
        AddSimpleItemDialog(
            title = "Add Medication",
            placeholder = "e.g. Metformin 500mg, Atorvastatin 20mg",
            onConfirm = {
                if (it.isNotBlank()) activeMedications.add(it.trim())
                showAddMedicationDialog = false
            },
            onDismiss = { showAddMedicationDialog = false }
        )
    }

    // Add Allergy Dialog
    if (showAddAllergyDialog) {
        AddSimpleItemDialog(
            title = "Add Allergy & Reaction",
            placeholder = "e.g. Peanuts (Anaphylaxis), Sulfa drugs (Hives)",
            onConfirm = {
                if (it.isNotBlank()) allergiesList.add(it.trim())
                showAddAllergyDialog = false
            },
            onDismiss = { showAddAllergyDialog = false }
        )
    }

    // Add Emergency Contact Dialog
    if (showAddContactDialog) {
        AddEmergencyContactDialog(
            onConfirm = { name, phone, rel ->
                viewModel.addEmergencyContact(
                    name = name,
                    phone = phone,
                    relationship = rel,
                    isPrimary = false
                )
                showAddContactDialog = false
            },
            onDismiss = { showAddContactDialog = false }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditNameAndBloodGroupDialog(
            currentName = user.name,
            currentBloodGroup = user.bloodGroup,
            onConfirm = { newName, newBlood ->
                viewModel.updateUserProfile(user.copy(name = newName, bloodGroup = newBlood))
                showEditProfileDialog = false
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }
}

// =========================================================================
// REUSABLE COMPONENTS & ROWS (Apple Health Style)
// =========================================================================

@Composable
fun SectionHeaderLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
fun ProfileMenuRow(
    title: String,
    textColor: Color,
    dividerColor: Color,
    showDivider: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(14.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = dividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun StaticDetailRow(
    label: String,
    value: String,
    textColor: Color,
    dividerColor: Color,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = dividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun EditableFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    textColor: Color,
    dividerColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor,
                modifier = Modifier.width(100.dp)
            )
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(
            color = dividerColor,
            thickness = 0.5.dp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun InteractiveDetailRow(
    label: String,
    value: String,
    textColor: Color,
    secondaryColor: Color,
    dividerColor: Color,
    showDivider: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    color = secondaryColor,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color(0xFFC7C7CC),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = dividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun MedicalIdSectionCard(
    title: String,
    titleColor: Color,
    actionLabel: String,
    items: List<String>,
    emptyPlaceholder: String,
    onActionClick: () -> Unit,
    cardBgColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    appleBlue: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = titleColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = actionLabel,
                fontSize = 14.sp,
                color = appleBlue,
                modifier = Modifier.clickable { onActionClick() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (items.isEmpty()) {
                    Text(
                        text = emptyPlaceholder,
                        fontSize = 15.sp,
                        color = textSecondary
                    )
                } else {
                    items.forEachIndexed { index, item ->
                        if (index > 0) Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item,
                            fontSize = 15.sp,
                            color = textPrimary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// SUB-SHEETS & MODAL DIALOGS
// =========================================================================

@Composable
fun SingleSelectOptionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            color = if (option.startsWith(selectedOption)) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (option.startsWith(selectedOption)) FontWeight.Bold else FontWeight.Normal
                        )
                        if (option.startsWith(selectedOption)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF007AFF))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSimpleItemDialog(
    title: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(placeholder, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textInput) }) {
                Text("Save", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEmergencyContactDialog(
    onConfirm: (name: String, phone: String, relationship: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("Family") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = relation,
                    onValueChange = { relation = it },
                    label = { Text("Relationship (e.g. Spouse, Parent, Doctor)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    onConfirm(name.trim(), phone.trim(), relation.trim())
                }
            }) {
                Text("Add", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditNameAndBloodGroupDialog(
    currentName: String,
    currentBloodGroup: String,
    onConfirm: (name: String, blood: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var blood by remember { mutableStateOf(currentBloodGroup) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Medical ID Information", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = blood,
                    onValueChange = { blood = it },
                    label = { Text("Blood Group (e.g. O+, A+, B+, AB+)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), blood.trim())
            }) {
                Text("Save", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthChecklistModal(onDismiss: () -> Unit, isDarkMode: Boolean) {
    val cardBgColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color.Black

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Health Checklist", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            ChecklistItemRow("Medical ID Setup", "Configured and visible in Emergency SOS mode", true)
            ChecklistItemRow("Emergency Contacts", "Primary and secondary contacts synced with GPS", true)
            ChecklistItemRow("Vault 256-Bit SQLCipher", "Encrypted local SQLite storage active", true)
            ChecklistItemRow("Wearable Sensor Sync", "Heart rate and cardio metrics connected", true)

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChecklistItemRow(title: String, desc: String, completed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (completed) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (completed) Color(0xFF34C759) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthNotificationsModal(onDismiss: () -> Unit, isDarkMode: Boolean) {
    var medReminders by remember { mutableStateOf(true) }
    var vitalsAlerts by remember { mutableStateOf(true) }
    var emergencyBroadcasts by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Health Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))

            NotificationToggleRow("Medication Dosing Reminders", "Exact minute push notification alarms", medReminders) { medReminders = it }
            NotificationToggleRow("High Heart Rate & Vitals Alert", "Critical alert notification if resting HR exceeds thresholds", vitalsAlerts) { vitalsAlerts = it }
            NotificationToggleRow("Disaster & Emergency Broadcasts", "Civil defense and regional hospital trauma alerts", emergencyBroadcasts) { emergencyBroadcasts = it }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) {
                Text("Save Preferences", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NotificationToggleRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyAppsModal(onDismiss: () -> Unit, isDarkMode: Boolean) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Connected Apps & Health Data", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text("These apps have requested access to read or write specific health data categories:", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(14.dp))

            AppPermissionRow("Lifscan AI Scanner", "Read/Write: Skin Scans, Lab Vitals, Digital Rx", true)
            AppPermissionRow("Hospital EHR Sync", "Read: Emergency Medical ID, Allergies", true)
            AppPermissionRow("Ambulance SOS Dispatch", "Read: Live GPS Coordinates, Blood Group", true)

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AppPermissionRow(appName: String, permissions: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.AppShortcut, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(appName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(permissions, fontSize = 12.sp, color = Color.Gray)
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF34C759).copy(alpha = 0.15f)
        ) {
            Text("ACTIVE", color = Color(0xFF34C759), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchStudiesModal(onDismiss: () -> Unit, isDarkMode: Boolean) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Research Studies", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Contribute de-identified health and skin scan metrics to medical research institutions and university clinical trials.", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(14.dp))

            ResearchStudyRow("Dermatological AI Skin Analysis Study", "Participating • Kathmandu Medical College", true)
            ResearchStudyRow("Cardiovascular Fitness & Vitals Study", "Available to Join • WHO South Asia", false)

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ResearchStudyRow(title: String, desc: String, enrolled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFFAF52DE), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedDevicesModal(onDismiss: () -> Unit, isDarkMode: Boolean) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Connected Devices", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Bluetooth and health hardware devices paired to Lifscan:", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(14.dp))

            DeviceRow("Apple Watch Series 9", "Connected • Real-time Heart Rate & ECG", true)
            DeviceRow("Smart Pulse Oximeter", "Connected • SpO2 & Perfusion Index", true)
            DeviceRow("Digital Blood Pressure Monitor", "Bluetooth Paired • Last synced 2 hrs ago", true)

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeviceRow(name: String, status: String, connected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Watch, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(status, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyCycleTrackingModal(onDismiss: () -> Unit, isDarkMode: Boolean) {
    var isPregnant by remember { mutableStateOf(false) }
    var gestationalWeeks by remember { mutableStateOf(16) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Pregnancy & Cycle Tracking", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Lifscan adjusts heart rate baselines, vitals thresholds, and medication alerts during active pregnancy.", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Currently Pregnant", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = isPregnant,
                    onCheckedChange = { isPregnant = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                )
            }

            if (isPregnant) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Gestational Age: $gestationalWeeks weeks (2nd Trimester)", fontSize = 14.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.Medium)
                Slider(
                    value = gestationalWeeks.toFloat(),
                    onValueChange = { gestationalWeeks = it.toInt() },
                    valueRange = 1f..42f,
                    steps = 41
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) {
                Text("Save Configuration", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateMedicationsModal(
    currentCount: Int,
    onUpdateCount: (Int) -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    var count by remember { mutableStateOf(currentCount) }
    var betaBlockerSelected by remember { mutableStateOf(false) }
    var calciumBlockerSelected by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Medications That Affect Heart Rate", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Select active medications that lower or restrict maximal cardiac rate response during exercise:", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Beta-Blockers (e.g. Atenolol, Metoprolol)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Limits maximum heart rate response", fontSize = 12.sp, color = Color.Gray)
                }
                Checkbox(
                    checked = betaBlockerSelected,
                    onCheckedChange = {
                        betaBlockerSelected = it
                        count = (if (betaBlockerSelected) 1 else 0) + (if (calciumBlockerSelected) 1 else 0)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Calcium Channel Blockers (e.g. Diltiazem)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Slows resting ventricular conduction", fontSize = 12.sp, color = Color.Gray)
                }
                Checkbox(
                    checked = calciumBlockerSelected,
                    onCheckedChange = {
                        calciumBlockerSelected = it
                        count = (if (betaBlockerSelected) 1 else 0) + (if (calciumBlockerSelected) 1 else 0)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onUpdateCount(count)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("Confirm ($count Selected)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HealthPrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Health & Privacy", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Lifscan is designed from the ground up to protect your privacy and give you control over your clinical data.\n\n" +
                        "• All vitals, medical records, and AI scans are encrypted using on-device SQLCipher 256-bit AES encryption.\n" +
                        "• Your Health data is never sold, tracked, or shared with third-party advertising networks.\n" +
                        "• When synced, data uses end-to-end encryption keys stored exclusively in Android Keystore."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got It", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ProfileAvatar(
    size: androidx.compose.ui.unit.Dp,
    isDarkMode: Boolean,
    testTag: String,
    role: UserRole = UserRole.PATIENT
) {
    val roleColor = when (role) {
        UserRole.PATIENT -> Color(0xFF007AFF)
        UserRole.DOCTOR -> DoctorPurple
        UserRole.AMBULANCE_DRIVER -> EmergencyRed
        UserRole.ADMIN -> Color(0xFF0284C7)
    }
    val roleIcon = when (role) {
        UserRole.PATIENT -> Icons.Default.Person
        UserRole.DOCTOR -> Icons.Default.MedicalServices
        UserRole.AMBULANCE_DRIVER -> Icons.Default.LocalHospital
        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
    }

    Surface(
        shape = CircleShape,
        color = if (isDarkMode) roleColor.copy(alpha = 0.2f) else roleColor.copy(alpha = 0.12f),
        border = BorderStroke(2.dp, roleColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .size(size)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                roleIcon,
                contentDescription = "Profile Avatar",
                tint = roleColor,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

// =========================================================================
// SCREEN 4: DOCTOR CREDENTIALS & PRACTICE VIEW
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorCredentialsView(
    viewModel: LifscanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val user = currentUser ?: UserEntity(
        id = "demo_doctor",
        phone = "+977-9851011223",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Dr. Sandeep Adhikari, MD",
        role = UserRole.DOCTOR,
        specialization = "Consultant Dermatologist & Venereologist",
        clinicAffiliation = "Bir Hospital & Nepal Skin Centre, Kathmandu",
        licenseDocumentUrl = "NMC-REG-#448921"
    )

    var isEditMode by remember { mutableStateOf(false) }

    var doctorName by remember(user.name) { mutableStateOf(user.name.ifBlank { "Dr. Sandeep Adhikari, MD" }) }
    var specialization by remember(user.specialization) { mutableStateOf(user.specialization.ifBlank { "Consultant Dermatologist" }) }
    var licenseNumber by remember(user.licenseDocumentUrl) { mutableStateOf(user.licenseDocumentUrl.ifBlank { "NMC-REG-#448921" }) }
    var clinicAffiliation by remember(user.clinicAffiliation) { mutableStateOf(user.clinicAffiliation.ifBlank { "Bir Hospital Trauma Center" }) }
    var consultationFee by remember { mutableStateOf("NPR 850 (INR 530)") }
    var experienceYears by remember { mutableStateOf("12 Years Clinical Experience") }
    var workingHours by remember { mutableStateOf("Mon - Sat • 09:00 AM - 05:00 PM") }
    var languagesSpoken by remember { mutableStateOf("Nepali, English, Hindi") }
    var bio by remember { mutableStateOf("Board-certified consultant specializing in clinical dermatology, AI-assisted melanoma screening, and emergency skin lesion triage.") }

    val bgCanvasColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val cardBgColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val textSecondary = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isDarkMode) Color(0xFF38383A) else Color(0xFFE5E5EA)

    fun saveChanges() {
        val updatedUser = user.copy(
            name = doctorName.trim().ifBlank { "Dr. Sandeep Adhikari, MD" },
            specialization = specialization.trim(),
            licenseDocumentUrl = licenseNumber.trim(),
            clinicAffiliation = clinicAffiliation.trim()
        )
        viewModel.updateUserProfile(updatedUser)
        Toast.makeText(context, "Doctor Practice & Credentials Saved", Toast.LENGTH_SHORT).show()
        isEditMode = false
    }

    Scaffold(
        containerColor = bgCanvasColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Doctor Credentials",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.testTag("doctor_details_back_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Profile",
                                tint = DoctorPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Profile",
                                color = DoctorPurple,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isEditMode) {
                                saveChanges()
                            } else {
                                isEditMode = true
                            }
                        },
                        modifier = Modifier.testTag("doctor_details_edit_done_btn")
                    ) {
                        Text(
                            if (isEditMode) "Done" else "Edit",
                            color = DoctorPurple,
                            fontSize = 17.sp,
                            fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCanvasColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered Doctor Avatar
            ProfileAvatar(
                size = 80.dp,
                isDarkMode = isDarkMode,
                testTag = "doctor_details_avatar",
                role = UserRole.DOCTOR
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SuccessGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NMC Medical Council Verified", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Group 1: Professional Information Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("doctor_credentials_card")
            ) {
                Column {
                    if (isEditMode) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = doctorName,
                                onValueChange = { doctorName = it },
                                label = { Text("Doctor Full Name & Degrees") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_doctor_name"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = specialization,
                                onValueChange = { specialization = it },
                                label = { Text("Clinical Specialization") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_doctor_spec"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = licenseNumber,
                                onValueChange = { licenseNumber = it },
                                label = { Text("NMC Medical Registration Number") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_doctor_license"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = clinicAffiliation,
                                onValueChange = { clinicAffiliation = it },
                                label = { Text("Hospital / Clinic Affiliation") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_doctor_clinic"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = consultationFee,
                                onValueChange = { consultationFee = it },
                                label = { Text("Standard Consultation Fee") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = workingHours,
                                onValueChange = { workingHours = it },
                                label = { Text("OPD & Teleconsultation Hours") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = languagesSpoken,
                                onValueChange = { languagesSpoken = it },
                                label = { Text("Languages Spoken") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Professional Summary & Bio") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    } else {
                        StaticDetailRow(
                            label = "Full Name",
                            value = doctorName,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Specialization",
                            value = specialization,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Medical License",
                            value = licenseNumber,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Affiliation",
                            value = clinicAffiliation,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Experience",
                            value = experienceYears,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Consultation Fee",
                            value = consultationFee,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "OPD Hours",
                            value = workingHours,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Languages",
                            value = languagesSpoken,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Clinical Summary Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Clinical Bio & Expertise", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(bio, fontSize = 13.sp, color = textSecondary, lineHeight = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// =========================================================================
// SCREEN 5: AMBULANCE DRIVER & EMS VEHICLE VIEW
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverVehicleInfoView(
    viewModel: LifscanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val user = currentUser ?: UserEntity(
        id = "demo_driver",
        phone = "+977-9801234567",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Ramesh Thapa",
        role = UserRole.AMBULANCE_DRIVER,
        vehicleNumber = "BA 1 PA 4921",
        vehicleType = "ICU Cardiac Advanced Life Support Unit",
        licenseDocumentUrl = "DL-NP-2023-8899"
    )

    var isEditMode by remember { mutableStateOf(false) }

    var driverName by remember(user.name) { mutableStateOf(user.name.ifBlank { "Ramesh Thapa" }) }
    var vehicleNumber by remember(user.vehicleNumber) { mutableStateOf(user.vehicleNumber.ifBlank { "BA 1 PA 4921" }) }
    var vehicleType by remember(user.vehicleType) { mutableStateOf(user.vehicleType.ifBlank { "ALS ICU Cardiac Unit" }) }
    var licenseNumber by remember(user.licenseDocumentUrl) { mutableStateOf(user.licenseDocumentUrl.ifBlank { "DL-NP-2023-8899" }) }
    var baseStation by remember { mutableStateOf("Bir Hospital Trauma Center Hub, Kathmandu") }
    var dutyStatus by remember { mutableStateOf("Active On-Duty (Standby Dispatch)") }
    var gpsTelemetryId by remember { mutableStateOf("GPS-OBD-8821-KTM") }

    val bgCanvasColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F2F7)
    val cardBgColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color.Black
    val textSecondary = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF8E8E93)
    val dividerColor = if (isDarkMode) Color(0xFF38383A) else Color(0xFFE5E5EA)

    fun saveChanges() {
        val updatedUser = user.copy(
            name = driverName.trim().ifBlank { "Ramesh Thapa" },
            vehicleNumber = vehicleNumber.trim(),
            vehicleType = vehicleType.trim(),
            licenseDocumentUrl = licenseNumber.trim()
        )
        viewModel.updateUserProfile(updatedUser)
        Toast.makeText(context, "Ambulance & EMS Credentials Saved", Toast.LENGTH_SHORT).show()
        isEditMode = false
    }

    Scaffold(
        containerColor = bgCanvasColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ambulance & EMS Vehicle",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = textPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.testTag("driver_details_back_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Profile",
                                tint = EmergencyRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Profile",
                                color = EmergencyRed,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isEditMode) {
                                saveChanges()
                            } else {
                                isEditMode = true
                            }
                        },
                        modifier = Modifier.testTag("driver_details_edit_done_btn")
                    ) {
                        Text(
                            if (isEditMode) "Done" else "Edit",
                            color = EmergencyRed,
                            fontSize = 17.sp,
                            fontWeight = if (isEditMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgCanvasColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered Driver Avatar
            ProfileAvatar(
                size = 80.dp,
                isDarkMode = isDarkMode,
                testTag = "driver_details_avatar",
                role = UserRole.AMBULANCE_DRIVER
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmergencyRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ALS Emergency Response Certified", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Group 1: Driver & Vehicle Info Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("driver_credentials_card")
            ) {
                Column {
                    if (isEditMode) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = driverName,
                                onValueChange = { driverName = it },
                                label = { Text("Driver / Paramedic Full Name") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_driver_name"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = vehicleNumber,
                                onValueChange = { vehicleNumber = it },
                                label = { Text("Vehicle Registration Plate #") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_driver_plate"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = vehicleType,
                                onValueChange = { vehicleType = it },
                                label = { Text("Ambulance Unit Category") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_driver_type"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = licenseNumber,
                                onValueChange = { licenseNumber = it },
                                label = { Text("Commercial Driving License #") },
                                modifier = Modifier.fillMaxWidth().testTag("edit_driver_license"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = baseStation,
                                onValueChange = { baseStation = it },
                                label = { Text("Base Hospital / Station Hub") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = dutyStatus,
                                onValueChange = { dutyStatus = it },
                                label = { Text("Duty Dispatch Status") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    } else {
                        StaticDetailRow(
                            label = "Driver Name",
                            value = driverName,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Vehicle Plate",
                            value = vehicleNumber,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Unit Type",
                            value = vehicleType,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Driving License",
                            value = licenseNumber,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Station Base",
                            value = baseStation,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Duty Status",
                            value = dutyStatus,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = true
                        )
                        StaticDetailRow(
                            label = "Telemetry GPS Beacon",
                            value = gpsTelemetryId,
                            textColor = textPrimary,
                            dividerColor = dividerColor,
                            showDivider = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Onboard Life Support Equipment Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Onboard Critical Care Equipment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    listOf(
                        "✅ 10L Compressed Medical Oxygen Tank & Mask Set",
                        "✅ Automated External Defibrillator (AED)",
                        "✅ Portable Emergency Ventilator (BIPAP/CPAP)",
                        "✅ Multi-parameter Cardiac Monitor (ECG, SpO2, NIBP)",
                        "✅ Full Spinal Immobilization Board & Stretcher",
                        "✅ ALS Resuscitation Drug Kit & Suction Pump"
                    ).forEach { eq ->
                        Text(eq, fontSize = 12.5.sp, color = textSecondary, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
