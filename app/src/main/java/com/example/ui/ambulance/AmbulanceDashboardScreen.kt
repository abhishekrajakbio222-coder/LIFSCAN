package com.example.ui.ambulance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SOSAlertEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbulanceDashboardScreen(viewModel: LifscanViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activeAlerts by viewModel.activeSOSAlerts.collectAsState()
    val allAlerts by viewModel.allSOSAlerts.collectAsState()

    var isOnDuty by remember { mutableStateOf(true) }
    var showWithdrawModal by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("3000") }

    val driver = currentUser ?: UserEntity(
        id = "drv_ramesh",
        phone = "+977-9801234567",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Ramesh Thapa (ALS Specialist Driver)",
        role = UserRole.AMBULANCE_DRIVER,
        vehicleNumber = "BA 1 PA 4921",
        vehicleType = "ICU Cardiac Advanced Life Support Unit",
        isLicenseVerified = true,
        preferredCurrency = "NPR",
        walletBalance = 8400.0,
        rating = 4.92f,
        totalConsultationsOrTrips = 95
    )

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmergencyRed,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Emergency, contentDescription = "Ambulance", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(driver.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                "${driver.vehicleNumber} • ${driver.vehicleType}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenNav.ProfileAndSettings) }, modifier = Modifier.testTag("ambulance_profile_btn")) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Ambulance Driver Profile", tint = EmergencyRed)
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
            // Driver Status & Duty Toggle Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnDuty) EmergencyRedLight else if (isDarkMode) SurfaceVariantDark else Color(0xFFF1F5F9)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isOnDuty) SuccessGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                if (isOnDuty) "AMBULANCE ON ACTIVE DUTY" else "OFFLINE / STANDBY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isOnDuty) EmergencyRedDark else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isOnDuty) "Receiving GPS 1-Tap SOS Emergency Dispatches" else "Toggle switch to receive emergency requests",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isOnDuty,
                        onCheckedChange = { isOnDuty = it },
                        modifier = Modifier.testTag("ambulance_duty_toggle"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmergencyRed
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wallet & Trip Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFFEF2F2))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DISPATCH TRIPS WALLET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
                            Text(
                                "${driver.preferredCurrency} ${String.format("%.2f", driver.walletBalance)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmergencyRedDark
                            )
                        }
                        Button(
                            onClick = { showWithdrawModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
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
                            Text("Total Completed Trips", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${driver.totalConsultationsOrTrips} trips", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text("Unit Oxygen / ALS Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("100% Ready (ALS)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreenDark)
                        }
                        Column {
                            Text("Driver Rating", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("⭐ ${driver.rating} / 5.0", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // LIVE INCOMING EMERGENCY SOS DISPATCHES
            Text(
                "INCOMING LIVE SOS DISPATCHES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (activeAlerts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Emergency Radar Scanning Active...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("No pending SOS emergency alerts in your immediate sector.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                activeAlerts.forEach { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmergencyRedLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmergencyRed, Color(0xFFEF4444))))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, tint = EmergencyRed)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(alert.emergencyType, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmergencyRedDark)
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = EmergencyRed) {
                                    Text(alert.status, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Patient: ${alert.patientName} (${alert.patientPhone})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("📍 Pickup Address: ${alert.locationAddress}", fontSize = 12.sp, color = Color(0xFF1E293B))
                            Text("⏱️ Estimated Arrival Time: ${alert.estimatedArrivalMinutes} minutes (1.8 km)", fontSize = 11.sp, color = EmergencyRedDark, fontWeight = FontWeight.SemiBold)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.acceptSOSByDriver(alert) },
                                    modifier = Modifier.weight(1f).testTag("accept_sos_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Accept & Route", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.showFeedback("Calling patient ${alert.patientName} at ${alert.patientPhone}") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Call Patient", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PAST EMERGENCY LOG HISTORY
            Text(
                "AMBULANCE TRIP LOGS & PROTOCOLS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            allAlerts.forEach { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = SuccessGreenLight, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(alert.emergencyType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Patient: ${alert.patientName} • ${alert.locationAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreenDark)
                    }
                }
            }
        }
    }

    // Modal: Driver Withdrawal Dialog
    if (showWithdrawModal) {
        AlertDialog(
            onDismissRequest = { showWithdrawModal = false },
            title = { Text("Withdraw Driver Earnings", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Available Wallet Balance: ${driver.preferredCurrency} ${String.format("%.2f", driver.walletBalance)}", fontWeight = FontWeight.Bold, color = EmergencyRed)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Amount to Withdraw (${driver.preferredCurrency})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = withdrawAmount.toDoubleOrNull() ?: 0.0
                        viewModel.requestWalletWithdrawal(amount, "eSewa / Bank Mobile Transfer")
                        showWithdrawModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Submit Payout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
