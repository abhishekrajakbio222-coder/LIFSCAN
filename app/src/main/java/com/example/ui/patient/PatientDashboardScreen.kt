package com.example.ui.patient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.LanguageHelper
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.features.AmbulanceTrackerWidget
import com.example.ui.features.EmergencySOSWidget
import com.example.ui.features.ExportHealthDataDialog
import com.example.ui.features.OfflineSecurityStatusDialog
import com.example.ui.features.PaymentCheckoutDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboardScreen(viewModel: LifscanViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeSOS by viewModel.activeEmergencyAlert.collectAsState()
    val skinScans by viewModel.patientSkinScans.collectAsState()
    val appointments by viewModel.patientAppointments.collectAsState()
    val doctors by viewModel.doctorsList.collectAsState()
    val facilities by viewModel.allFacilities.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isOfflineEncryptionActive by viewModel.isOfflineEncryptionActive.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var selectedDoctorForBooking by remember { mutableStateOf<UserEntity?>(null) }
    var showBookingModal by remember { mutableStateOf(false) }
    var showPaymentModal by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf("10:30 AM (Today)") }
    var bookingNotes by remember { mutableStateOf("Consultation for skin lesion symptoms.") }

    val user = currentUser ?: UserEntity(
        id = "demo_patient",
        phone = "+977-9841234567",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Aayush Shrestha",
        role = UserRole.PATIENT
    )

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(ScreenNav.ProfileAndSettings) }
                            .testTag("patient_header_profile_click")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${user.countryName} (${user.phone}) • ${user.bloodGroup}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("patient_dashboard_theme_palette_btn")
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Toggle Health Green / Medical Blue",
                            tint = if (themeMode == HealthThemeMode.HEALTH_GREEN) HealthGreenPrimary else MedicalBluePrimary
                        )
                    }
                    IconButton(onClick = { viewModel.lockApp() }, modifier = Modifier.testTag("lock_app_btn")) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock with Biometrics", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showSecurityDialog = true }, modifier = Modifier.testTag("security_status_btn")) {
                        Icon(Icons.Default.Shield, contentDescription = "Offline Security & SQLCipher", tint = SuccessGreen)
                    }
                    IconButton(onClick = { showExportDialog = true }, modifier = Modifier.testTag("export_dialog_btn")) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Records")
                    }
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme")
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // 🔒 SQLCipher 256-bit AES Offline Encryption Status Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SuccessGreen.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSecurityDialog = true }
                    .padding(bottom = 12.dp)
                    .testTag("offline_encryption_status_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SQLCipher 256-bit AES Encrypted at Rest",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SuccessGreen
                    ) {
                        Text(
                            "SECURE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // 🚨 REAL-TIME AMBULANCE DISPATCH TRACKER (when active or previewable)
            if (activeSOS != null) {
                AmbulanceTrackerWidget(
                    viewModel = viewModel,
                    onExpandFullScreen = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) },
                    modifier = Modifier.testTag("patient_dashboard_ambulance_tracker")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 🚨 DEDICATED EMERGENCY SOS WIDGET (Instant location-based ambulance alert)
            EmergencySOSWidget(
                viewModel = viewModel,
                onNavigateToFacilities = { viewModel.navigateTo(ScreenNav.FacilitiesMap) },
                modifier = Modifier.testTag("patient_dashboard_sos_widget")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🛡️ SECURE MEDICAL VAULT CARD (Biometric Protected & SQLCipher 256-bit AES)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_vault_dashboard_card")
                    .clickable { viewModel.navigateTo(ScreenNav.MedicalVault) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF0FDF4)
                ),
                border = BorderStroke(1.5.dp, SuccessGreen.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SuccessGreen,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.EnhancedEncryption, contentDescription = "Medical Vault", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Encrypted Medical Vault",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SuccessGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen.copy(alpha = 0.2f)
                            ) {
                                Text("BIOMETRIC", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text(
                            "Access SQLCipher encrypted health records, diagnostic lab reports, skin scans, vitals history & Rx prescriptions.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 📅 TELEHEALTH CONSULTATION SCHEDULER CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("consultation_scheduler_card")
                    .clickable { viewModel.navigateTo(ScreenNav.ConsultationScheduler) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SurfaceVariantDark else TealLight
                ),
                border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TealPrimary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Scheduler", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Consultation Scheduler",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TealPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TealPrimary.copy(alpha = 0.2f)
                            ) {
                                Text("TELEHEALTH", color = TealPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text(
                            "Pick time slots, choose verified doctors across Nepal/Asia, and book HD video consultations with encrypted notes.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TealPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 📷 AI MEDICAL IMAGE & INJURY SCANNER CARD (Flagship Feature with Live Camera Overlay)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_image_scanner_card")
                    .clickable { viewModel.navigateTo(ScreenNav.MedicalImageScan) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFEFF6FF)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4)))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Medical Scanner", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI Medical Image Scanner",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.2f)
                            ) {
                                Text("CAMERA OVERLAY", color = Color(0xFF2563EB), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text(
                            "Real-time instructions, wound/burn/lesion scanning, live alignment reticle & instant Gemini AI triage.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 📄 AI MEDICAL DOCUMENT & PRESCRIPTION SCANNER CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_document_scanner_card")
                    .clickable { viewModel.navigateTo(ScreenNav.MedicalDocumentScan) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF0F9FF)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8)))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0EA5E9),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "Document Scanner", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Document & Rx AI Scanner",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF0EA5E9)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0EA5E9).copy(alpha = 0.2f)
                            ) {
                                Text("OCR & RX IMPORT", color = Color(0xFF0EA5E9), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text(
                            "Scan doctor prescriptions, lab test reports & discharge notes. Auto-extracts dosages and schedules med reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF0EA5E9))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🩺 SEPARATED AI SKIN DISEASE SCANNER CARD (Dedicated feature highlight)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("skin_scanner_card")
                    .clickable { viewModel.navigateTo(ScreenNav.SkinScanner) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFFFF1F2)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(SkinScannerPink, Color(0xFFF43F5E)))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SkinScannerPink,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "Skin Scan", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI Symptom & Skin Scanner",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SkinScannerPink
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SkinScannerPink.copy(alpha = 0.2f)
                            ) {
                                Text("GEMINI AI", color = SkinScannerPink, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                        Text(
                            "Scan rashes, eczema, psoriasis, acne & skin lesions with integrated CameraX & Gemini API diagnostic assessment.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SkinScannerPink)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Feature Grid: Medication Schedule & Health Analytics Dashboard
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Medication Schedule Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(ScreenNav.MedicationSchedule) }
                        .testTag("medication_schedule_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Surface(shape = CircleShape, color = Color(0xFF3B82F6), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Medication, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Med Schedule", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Push alerts & Rx", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // AI Health Chat Bot Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(ScreenNav.AIChat) }
                        .testTag("ai_chat_bot_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFFAF5FF)),
                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Surface(shape = CircleShape, color = Color(0xFFA855F7), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SmartToy, contentDescription = "AI Chat Bot", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("AI Chat Bot", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("24/7 Clinical Assistant", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Feature Grid: Symptom Scan, AI Chat, Facilities Map
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Symptom Scan Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(ScreenNav.GeneralHealthScan) }
                        .testTag("symptom_scan_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else TealLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Surface(shape = CircleShape, color = TealPrimary, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Symptom Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Check vitals & triage", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // AI Health Chat Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(ScreenNav.AIChat) }
                        .testTag("ai_chat_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else InfoBlueLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Surface(shape = CircleShape, color = InfoBlue, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("AI Assistant", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("24/7 health queries", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Map & Facilities Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(ScreenNav.FacilitiesMap) }
                        .testTag("map_facilities_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else SuccessGreenLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Surface(shape = CircleShape, color = SuccessGreen, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Nearby GPS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Hospitals & Meds", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Security & Emergency Access Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Emergency Contacts Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(ScreenNav.EmergencyContacts) }
                        .testTag("emergency_contacts_quick_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else EmergencyRedLight),
                    border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = EmergencyRed, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("SOS Contacts", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = EmergencyRed)
                            Text("Manage responders", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Security Audit Logs Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.navigateTo(ScreenNav.AuditLogViewer) }
                        .testTag("audit_logs_quick_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, TealDark.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = TealDark, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Audit Logs", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TealDark)
                            Text("Vault access audit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Admin Control Facilities Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(ScreenNav.AdminFacilitiesControl) }
                    .testTag("admin_facilities_quick_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color(0xFF0284C7), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Control", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Admin Facility Command", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0284C7).copy(alpha = 0.3f)) {
                                Text("CRUD & LIVE ICU", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text("Manage 24/7 status, ICU bed capacity, ventilators & add hospitals", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF38BDF8))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // UPCOMING TELEHEALTH APPOINTMENTS SECTION
            Text(
                "YOUR DOCTOR APPOINTMENTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (appointments.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No active appointments", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Book an online video consultation with a verified specialist below.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                appointments.forEach { apt ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else InfoBlueLight)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(apt.doctorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (apt.status == "COMPLETED") SuccessGreen else TealPrimary
                                ) {
                                    Text(
                                        apt.status,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("${apt.doctorSpecialty} • ${apt.clinicName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("📅 ${apt.appointmentDate} at ${apt.timeSlot}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                            if (apt.prescriptionNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreenLight, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("📝 Doctor's Digital Prescription:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SuccessGreenDark)
                                        Text(apt.prescriptionNotes, fontSize = 11.sp, color = Color(0xFF065F46))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.navigateTo(ScreenNav.VideoCall(apt)) },
                                    modifier = Modifier.weight(1f).testTag("join_video_call_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPurple)
                                ) {
                                    Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Join Video Call", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.navigateTo(ScreenNav.ChatDetail(apt.id, apt.doctorName, apt.doctorId, UserRole.DOCTOR))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Direct Chat", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BOOK SPECIALIST DOCTORS SECTION
            Text(
                "VERIFIED DOCTORS & SPECIALISTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            doctors.forEach { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = DoctorPurple.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = DoctorPurple, modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(doc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = InfoBlue, modifier = Modifier.size(14.dp))
                            }
                            Text(doc.specialization, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("⭐ ${doc.rating} • Fee: ${doc.preferredCurrency} ${doc.consultationFee.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TealPrimary)
                        }

                        Button(
                            onClick = {
                                selectedDoctorForBooking = doc
                                showBookingModal = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text("Book", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // NEARBY HOSPITALS & 24/7 PHARMACIES MINI PREVIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NEARBY FACILITIES (KATHMANDU / ASIA)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) }) {
                    Text("View Map", fontSize = 11.sp)
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(facilities.take(3)) { fac ->
                    Card(
                        modifier = Modifier.width(220.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(fac.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(fac.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("${fac.distanceKm} km away", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                Text("⭐ ${fac.rating}", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Book Doctor Appointment
    val docToBook = selectedDoctorForBooking
    if (showBookingModal && docToBook != null) {
        AlertDialog(
            onDismissRequest = { showBookingModal = false },
            title = { Text("Book Appointment with ${docToBook.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("Specialty: ${docToBook.specialization}", fontSize = 12.sp)
                    Text("Hospital: ${docToBook.clinicAffiliation}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("SELECT TIME SLOT:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("10:30 AM (Today)", "02:00 PM (Today)", "05:00 PM (Tomorrow)").forEach { slot ->
                        FilterChip(
                            selected = selectedSlot == slot,
                            onClick = { selectedSlot = slot },
                            label = { Text(slot) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bookingNotes,
                        onValueChange = { bookingNotes = it },
                        label = { Text("Consultation Reason / Symptoms") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = TealLight, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Consultation Fee:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealDark)
                            Text("${docToBook.preferredCurrency} ${docToBook.consultationFee.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealDark)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBookingModal = false
                        showPaymentModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Proceed to Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookingModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Payment Checkout Dialog
    if (showPaymentModal && docToBook != null) {
        PaymentCheckoutDialog(
            title = "Video Consultation with ${docToBook.name}",
            amount = docToBook.consultationFee,
            currency = docToBook.preferredCurrency,
            onDismiss = { showPaymentModal = false },
            onPaymentSuccess = { paymentMethod ->
                showPaymentModal = false
                viewModel.bookDoctorAppointment(
                    doctor = docToBook,
                    date = "Today",
                    timeSlot = selectedSlot,
                    isVideo = true,
                    notes = bookingNotes,
                    paymentMethod = paymentMethod
                )
            }
        )
    }

    // Modal: Export Health Data Dialog
    if (showExportDialog) {
        ExportHealthDataDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }

    // Modal: Offline Security Status Dialog
    if (showSecurityDialog) {
        OfflineSecurityStatusDialog(
            viewModel = viewModel,
            onDismiss = { showSecurityDialog = false }
        )
    }
}
