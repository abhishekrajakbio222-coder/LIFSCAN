package com.example.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.features.OfflineSecurityStatusDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(viewModel: LifscanViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activeSOSList by viewModel.activeSOSAlerts.collectAsState()
    val skinScans by viewModel.patientSkinScans.collectAsState()
    val appointments by viewModel.patientAppointments.collectAsState()

    var showSecurityDialog by remember { mutableStateOf(false) }
    var showWithdrawModal by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("5000") }
    var withdrawMethod by remember { mutableStateOf("eSewa Digital Wallet (Nepal)") }
    var selectedAppointmentForPrescription by remember { mutableStateOf<AppointmentEntity?>(null) }
    var prescriptionInput by remember { mutableStateOf("") }

    val doctor = currentUser ?: UserEntity(
        id = "doc_sandeep",
        phone = "+977-9851011223",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Dr. Sandeep Adhikari, MD",
        role = UserRole.DOCTOR,
        specialization = "Consultant Dermatologist & Venereologist",
        clinicAffiliation = "Bir Hospital & Nepal Skin Centre, Kathmandu",
        isLicenseVerified = true,
        consultationFee = 850.0,
        preferredCurrency = "NPR",
        walletBalance = 14850.0,
        rating = 4.95f,
        totalConsultationsOrTrips = 184
    )

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = DoctorPurple,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MedicalServices, contentDescription = "Doctor", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(doctor.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = InfoBlue, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                "${doctor.specialization} • ${doctor.clinicAffiliation}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenNav.ProfileAndSettings) }, modifier = Modifier.testTag("doctor_profile_btn")) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Doctor Profile", tint = DoctorPurple)
                    }
                    IconButton(onClick = { viewModel.lockApp() }, modifier = Modifier.testTag("doctor_lock_app_btn")) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock with Biometrics", tint = DoctorPurple)
                    }
                    IconButton(onClick = { showSecurityDialog = true }, modifier = Modifier.testTag("doctor_security_status_btn")) {
                        Icon(Icons.Default.Shield, contentDescription = "Offline Security & SQLCipher", tint = SuccessGreen)
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
            // Doctor Stats & Wallet Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else DoctorPurpleLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PRACTICE EARNINGS WALLET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DoctorPurple)
                            Text(
                                "${doctor.preferredCurrency} ${String.format("%.2f", doctor.walletBalance)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = DoctorPurple
                            )
                        }
                        Button(
                            onClick = { showWithdrawModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DoctorPurple),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Withdraw", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Consultations", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${doctor.totalConsultationsOrTrips} completed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text("License Verification", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("NMC Approved ✓", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreenDark)
                        }
                        Column {
                            Text("Patient Rating", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("⭐ ${doctor.rating} / 5.0", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Live Emergency Alert Banner (if active)
            if (activeSOSList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                val alert = activeSOSList.first()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = EmergencyRedLight)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Emergency, contentDescription = null, tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CRITICAL EMERGENCY RADAR ALERT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmergencyRedDark)
                            Text("${alert.patientName}: ${alert.emergencyType} at ${alert.locationAddress}", fontSize = 11.sp, color = Color.Black)
                        }
                        Button(
                            onClick = { viewModel.showFeedback("Connecting doctor emergency telehealth line to ${alert.patientName}...") },
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Assist", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Appointments & Telehealth Queue
            Text(
                "PATIENT CONSULTATION QUEUE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (appointments.isEmpty()) {
                // Demo appointment for doctor interaction
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Aayush Shrestha", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(shape = RoundedCornerShape(6.dp), color = TealPrimary) {
                                Text("WAITING IN ROOM", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text("Reason: Atopic Dermatitis flare-up with itching on forearm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📅 Today • 10:30 AM (Telehealth Video)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val demoApt = AppointmentEntity(
                                        id = "apt_demo_doc",
                                        patientId = "demo_patient",
                                        patientName = "Aayush Shrestha",
                                        doctorId = doctor.id,
                                        doctorName = doctor.name,
                                        doctorSpecialty = doctor.specialization,
                                        clinicName = doctor.clinicAffiliation,
                                        appointmentDate = "Today",
                                        timeSlot = "10:30 AM",
                                        fee = doctor.consultationFee,
                                        currency = doctor.preferredCurrency,
                                        status = "IN_PROGRESS"
                                    )
                                    viewModel.navigateTo(ScreenNav.VideoCall(demoApt))
                                },
                                modifier = Modifier.weight(1f).testTag("doctor_start_video_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DoctorPurple)
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Video Call", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.navigateTo(ScreenNav.ChatDetail("conv_doc_patient", "Aayush Shrestha", "demo_patient", UserRole.PATIENT))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat & Files", fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                appointments.forEach { apt ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(apt.patientName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(shape = RoundedCornerShape(6.dp), color = TealPrimary) {
                                    Text(apt.status, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(apt.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("📅 ${apt.appointmentDate} at ${apt.timeSlot}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.navigateTo(ScreenNav.VideoCall(apt)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPurple)
                                ) {
                                    Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Video Call", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        selectedAppointmentForPrescription = apt
                                        prescriptionInput = apt.prescriptionNotes.ifBlank { "1. Tab Cetirizine 10mg OD (5 days)\n2. Hydrocortisone 1% Topical BD" }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Prescribe", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Patient AI Skin Scans for Doctor Review
            Text(
                "PATIENT DERMATOLOGY SCAN RECORDS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            skinScans.forEach { scan ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(scan.conditionName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(shape = RoundedCornerShape(6.dp), color = SkinScannerPink.copy(alpha = 0.15f)) {
                                Text(
                                    "${(scan.confidenceScore * 100).toInt()}% AI Match (${scan.riskLevel})",
                                    color = SkinScannerPink,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("Patient: ${scan.patientName} • Area: ${scan.affectedArea}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("AI Findings: ${scan.aiAnalysisSummary}", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Modal: Payout / Withdrawal Dialog
    if (showWithdrawModal) {
        AlertDialog(
            onDismissRequest = { showWithdrawModal = false },
            title = { Text("Withdraw Practice Earnings", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Available Wallet Balance: ${doctor.preferredCurrency} ${String.format("%.2f", doctor.walletBalance)}", fontWeight = FontWeight.Bold, color = DoctorPurple)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Amount to Withdraw (${doctor.preferredCurrency})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PAYOUT METHOD:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("eSewa Digital Wallet (Nepal)", "Khalti Wallet (Nepal)", "Bank Direct Deposit", "UPI Transfer (India)").forEach { method ->
                        FilterChip(
                            selected = withdrawMethod == method,
                            onClick = { withdrawMethod = method },
                            label = { Text(method) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = withdrawAmount.toDoubleOrNull() ?: 0.0
                        viewModel.requestWalletWithdrawal(amount, withdrawMethod)
                        showWithdrawModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DoctorPurple)
                ) {
                    Text("Submit Withdrawal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: e-Prescription Writer Dialog
    val aptToPrescribe = selectedAppointmentForPrescription
    if (aptToPrescribe != null) {
        AlertDialog(
            onDismissRequest = { selectedAppointmentForPrescription = null },
            title = { Text("Write e-Prescription for ${aptToPrescribe.patientName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = prescriptionInput,
                        onValueChange = { prescriptionInput = it },
                        label = { Text("Prescribed Medication, Dosage & Advice") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeAppointmentAndPrescribe(aptToPrescribe, prescriptionInput)
                        selectedAppointmentForPrescription = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Save & Issue")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppointmentForPrescription = null }) {
                    Text("Cancel")
                }
            }
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
