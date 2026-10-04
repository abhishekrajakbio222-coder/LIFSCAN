package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthReportEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.SkinScanEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import com.example.util.PdfHealthReportExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboardScreen(viewModel: LifscanViewModel) {
    val context = LocalContext.current
    val isVaultAuthenticated by viewModel.isVaultAuthenticated.collectAsState()
    val reports by viewModel.patientHealthReports.collectAsState()
    val skinScans by viewModel.patientSkinScans.collectAsState()
    val medications by viewModel.patientMedications.collectAsState()
    val appointments by viewModel.patientAppointments.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var selectedTimeframe by remember { mutableStateOf("7D") } // "7D", "30D", "ALL"
    var showExportSuccessDialog by remember { mutableStateOf<PdfHealthReportExporter.ExportResult?>(null) }
    var showBiometricPromptDialog by remember { mutableStateOf(false) }

    // Aggregate statistics
    val totalScans = skinScans.size
    val highRiskScans = skinScans.count { it.riskLevel.equals("High", true) || it.riskLevel.equals("Critical", true) }
    val moderateRiskScans = skinScans.count { it.riskLevel.equals("Moderate", true) }
    val lowRiskScans = totalScans - highRiskScans - moderateRiskScans

    val totalMeds = medications.size
    val takenMeds = medications.count { it.isTakenToday }
    val adherencePercent = if (totalMeds > 0) ((takenMeds.toFloat() / totalMeds) * 100).toInt() else 100

    // Prompt Biometric Gate if dashboard is locked
    if (!isVaultAuthenticated || showBiometricPromptDialog) {
        BiometricAuthGateDialog(
            vaultTitle = "Health Records & Diagnostic Analytics",
            onAuthenticated = {
                viewModel.setVaultAuthenticated(true)
                showBiometricPromptDialog = false
            },
            onDismiss = {
                showBiometricPromptDialog = false
                if (!isVaultAuthenticated) {
                    viewModel.navigateTo(ScreenNav.PatientDashboard)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Health Analytics",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isVaultAuthenticated) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Security Status",
                                    tint = if (isVaultAuthenticated) SuccessGreen else AmberWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                if (isVaultAuthenticated) "Biometrically Verified • Encrypted" else "Biometric Verification Required",
                                fontSize = 11.sp,
                                color = if (isVaultAuthenticated) SuccessGreen else AmberWarning
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("analytics_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("health_dashboard_theme_palette_btn")
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Toggle Health Green / Medical Blue",
                            tint = if (themeMode == HealthThemeMode.HEALTH_GREEN) HealthGreenPrimary else MedicalBluePrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isVaultAuthenticated) {
                                viewModel.setVaultAuthenticated(false)
                                viewModel.showFeedback("Health Records locked. Fingerprint or Face verification required.")
                            } else {
                                showBiometricPromptDialog = true
                            }
                        },
                        modifier = Modifier.testTag("analytics_lock_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isVaultAuthenticated) Icons.Default.Lock else Icons.Default.Fingerprint,
                            contentDescription = "Lock/Unlock",
                            tint = if (isVaultAuthenticated) TealPrimary else AmberWarning
                        )
                    }

                    IconButton(
                        onClick = {
                            val result = viewModel.generateSecureHealthProfilePdf(context)
                            showExportSuccessDialog = result
                        },
                        modifier = Modifier.testTag("analytics_export_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = TealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (!isVaultAuthenticated) {
            // Locked Vault State Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Lock",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Protected Health Records",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            "Biometric authentication (fingerprint or face verification) is required to decrypt and view clinical biomarkers, diagnostic graphs, and medical summaries.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                        )

                        Button(
                            onClick = { showBiometricPromptDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_dashboard_biometric_btn")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Biometrics to View")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            // Timeframe Selector & Overall Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceDarkElevated else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("OVERVIEW TIMEFRAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("7D", "30D", "ALL").forEach { tf ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selectedTimeframe == tf) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable { selectedTimeframe = tf }
                                    ) {
                                        Text(
                                            tf,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTimeframe == tf) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 Key Metrics Cards in a Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricSummaryBox(
                                title = "Avg BP",
                                value = "118/78",
                                unit = "mmHg",
                                status = "Optimal",
                                statusColor = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MetricSummaryBox(
                                title = "Heart Rate",
                                value = "72",
                                unit = "BPM",
                                status = "Normal",
                                statusColor = InfoBlue,
                                modifier = Modifier.weight(1f)
                            )
                            MetricSummaryBox(
                                title = "Adherence",
                                value = "$adherencePercent%",
                                unit = "Daily",
                                status = "On Track",
                                statusColor = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

// Interactive Line Graph: Blood Pressure & Heart Rate Trends (Recharts-Style)
            item {
                var selectedPointIndex by remember { mutableIntStateOf(6) } // Defaults to most recent day
                val days7 = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val systolic7 = listOf(122, 120, 118, 125, 119, 117, 118)
                val diastolic7 = listOf(82, 80, 78, 84, 79, 76, 78)
                val heartRate7 = listOf(74, 72, 70, 76, 71, 69, 70)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceDarkElevated else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Blood Pressure & Pulse Trends", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Recharts-styled interactive multi-axis trends", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(TealPrimary, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.size(8.dp).background(EmergencyRed, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("HR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Selected Point Tooltip Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TealPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Day: ${days7[selectedPointIndex]}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "BP: ${systolic7[selectedPointIndex]}/${diastolic7[selectedPointIndex]} mmHg",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TealPrimary
                                )
                                Text(
                                    "Pulse: ${heartRate7[selectedPointIndex]} BPM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmergencyRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Canvas Chart with Tap to Inspect
                        BloodPressureLineChart(
                            isDarkMode = isDarkMode,
                            selectedIndex = selectedPointIndex,
                            onPointSelected = { selectedPointIndex = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }

            // Symptom & Disease Severity Trends Over Time
            item {
                var selectedSymptomDayIndex by remember { mutableIntStateOf(6) }
                val symptomDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val itchLevels = listOf(3, 4, 3, 2, 2, 1, 1) // 0-5 scale
                val fatigueLevels = listOf(2, 3, 2, 2, 1, 2, 1) // 0-5 scale
                val painLevels = listOf(1, 2, 1, 0, 0, 0, 0) // 0-5 scale

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceDarkElevated else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Symptom & Flare Trends", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Clinical severity tracking (0 to 5 index)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(SkinScannerPink, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Itch/Rash", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SkinScannerPink)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.size(8.dp).background(WarningAmber, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fatigue", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Selected Symptom Tooltip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SkinScannerPink.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Day: ${symptomDays[selectedSymptomDayIndex]}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Itch: ${itchLevels[selectedSymptomDayIndex]}/5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkinScannerPink)
                                Text("Fatigue: ${fatigueLevels[selectedSymptomDayIndex]}/5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                                Text("Pain: ${painLevels[selectedSymptomDayIndex]}/5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InfoBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Symptom Multi-Area Trend Chart
                        SymptomTrendsAreaChart(
                            isDarkMode = isDarkMode,
                            selectedIndex = selectedSymptomDayIndex,
                            onPointSelected = { selectedSymptomDayIndex = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }

            // Dual Bar Chart: Blood Sugar & SpO2
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceDarkElevated else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Blood Sugar & SpO2 Levels", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Fasting Glucose (mg/dL) & Oxygen Saturation (%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        VitalsBarChart(
                            isDarkMode = isDarkMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }

            // AI Symptom & Dermatology Risk Distribution Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceDarkElevated else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Diagnostic Severity Distribution", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Aggregated risk ratings from symptom & skin scans", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Chart Canvas
                            RiskDistributionDonutChart(
                                lowCount = lowRiskScans.coerceAtLeast(2),
                                moderateCount = moderateRiskScans.coerceAtLeast(1),
                                highCount = highRiskScans.coerceAtLeast(0),
                                modifier = Modifier.size(110.dp)
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            // Legend and counts
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RiskLegendItem(label = "Low Risk / Benign", count = lowRiskScans.coerceAtLeast(2), color = SuccessGreen)
                                RiskLegendItem(label = "Moderate (Eczema/Allergy)", count = moderateRiskScans.coerceAtLeast(1), color = WarningAmber)
                                RiskLegendItem(label = "High / Urgent Care", count = highRiskScans, color = EmergencyRed)
                            }
                        }
                    }
                }
            }

            // Export PDF CTA Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TealPrimary.copy(alpha = 0.1f)
                    ),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TealPrimary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Medical Summary PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Generate encrypted clinical summary with all vitals, scans & prescriptions.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val result = viewModel.generateSecureHealthProfilePdf(context)
                                showExportSuccessDialog = result
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Export", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        }
    }

    // Export Result Dialog
    showExportSuccessDialog?.let { result ->
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = null },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp)) },
            title = { Text("Secure PDF Export Ready", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your health profile has been compiled and encrypted into a clinical PDF document.")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("File: Lifscan_Health_Summary.pdf", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Size: ${result.fileSizeFormatted} • Pages: ${result.pageCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Security: 256-Bit SQLCipher Decrypted Hash", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.shareExportedPdf(context, result)
                        showExportSuccessDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share / Save PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportSuccessDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun MetricSummaryBox(
    title: String,
    value: String,
    unit: String,
    status: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Text(unit, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor)
        }
    }
}

@Composable
fun BloodPressureLineChart(
    isDarkMode: Boolean,
    selectedIndex: Int = 6,
    onPointSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset: Offset ->
                    val width = size.width
                    val daysCount = 7
                    val stepX = width / (daysCount - 1)
                    val tappedIndex = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, daysCount - 1)
                    onPointSelected(tappedIndex)
                }
            )
        }
    ) {
        val width = size.width
        val height = size.height
        val paddingBottom = 24f
        val paddingTop = 12f
        val chartHeight = height - paddingBottom - paddingTop

        // Days labels
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val systolic = listOf(122f, 120f, 118f, 125f, 119f, 117f, 118f)
        val diastolic = listOf(82f, 80f, 78f, 84f, 79f, 76f, 78f)
        val heartRate = listOf(74f, 72f, 70f, 76f, 71f, 69f, 70f)

        val minVal = 60f
        val maxVal = 140f
        val stepX = width / (days.size - 1)

        // Draw horizontal grid lines (Recharts dashed style)
        val gridLines = listOf(80f, 100f, 120f)
        val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        gridLines.forEach { gVal ->
            val gy = paddingTop + chartHeight - ((gVal - minVal) / (maxVal - minVal)) * chartHeight
            drawLine(
                color = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                start = Offset(0f, gy),
                end = Offset(width, gy),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // Draw Systolic Area Fill Gradient (Recharts style)
        val systolicAreaPath = Path()
        val systolicPath = Path()
        val diastolicPath = Path()
        val heartRatePath = Path()

        systolic.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - ((v - minVal) / (maxVal - minVal)) * chartHeight
            if (i == 0) {
                systolicPath.moveTo(x, y)
                systolicAreaPath.moveTo(x, paddingTop + chartHeight)
                systolicAreaPath.lineTo(x, y)
            } else {
                systolicPath.lineTo(x, y)
                systolicAreaPath.lineTo(x, y)
            }
        }
        systolicAreaPath.lineTo((systolic.size - 1) * stepX, paddingTop + chartHeight)
        systolicAreaPath.close()

        drawPath(
            path = systolicAreaPath,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0D9488).copy(alpha = 0.25f), Color(0xFF0D9488).copy(alpha = 0.01f))
            )
        )

        diastolic.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - ((v - minVal) / (maxVal - minVal)) * chartHeight
            if (i == 0) diastolicPath.moveTo(x, y) else diastolicPath.lineTo(x, y)
        }

        heartRate.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - ((v - minVal) / (maxVal - minVal)) * chartHeight
            if (i == 0) heartRatePath.moveTo(x, y) else heartRatePath.lineTo(x, y)
        }

        // Draw Lines
        drawPath(systolicPath, color = Color(0xFF0D9488), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        drawPath(diastolicPath, color = Color(0xFF14B8A6), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawPath(heartRatePath, color = Color(0xFFD90429), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Draw Selected Indicator Line
        val selectedX = selectedIndex * stepX
        drawLine(
            color = Color(0xFF0D9488).copy(alpha = 0.5f),
            start = Offset(selectedX, paddingTop),
            end = Offset(selectedX, paddingTop + chartHeight),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = dashEffect
        )

        // Draw Data Points on Systolic & Heart Rate
        systolic.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - ((v - minVal) / (maxVal - minVal)) * chartHeight
            val isSel = i == selectedIndex
            drawCircle(color = Color.White, radius = if (isSel) 7.dp.toPx() else 4.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color(0xFF0D9488), radius = if (isSel) 5.dp.toPx() else 2.5.dp.toPx(), center = Offset(x, y))
        }

        heartRate.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - ((v - minVal) / (maxVal - minVal)) * chartHeight
            val isSel = i == selectedIndex
            drawCircle(color = Color.White, radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color(0xFFD90429), radius = if (isSel) 4.5.dp.toPx() else 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun SymptomTrendsAreaChart(
    isDarkMode: Boolean,
    selectedIndex: Int = 6,
    onPointSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset: Offset ->
                    val width = size.width
                    val daysCount = 7
                    val stepX = width / (daysCount - 1)
                    val tappedIndex = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, daysCount - 1)
                    onPointSelected(tappedIndex)
                }
            )
        }
    ) {
        val width = size.width
        val height = size.height
        val paddingBottom = 20f
        val paddingTop = 10f
        val chartHeight = height - paddingBottom - paddingTop

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val itchLevels = listOf(3f, 4f, 3f, 2f, 2f, 1f, 1f)
        val fatigueLevels = listOf(2f, 3f, 2f, 2f, 1f, 2f, 1f)
        val maxScale = 5f
        val stepX = width / (days.size - 1)

        val dashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        // Draw horizontal levels
        listOf(1f, 2f, 3f, 4f).forEach { lvl ->
            val y = paddingTop + chartHeight - (lvl / maxScale) * chartHeight
            drawLine(
                color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )
        }

        // Itch Area Path (SkinScannerPink)
        val itchAreaPath = Path()
        val itchPath = Path()
        itchLevels.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - (v / maxScale) * chartHeight
            if (i == 0) {
                itchPath.moveTo(x, y)
                itchAreaPath.moveTo(x, paddingTop + chartHeight)
                itchAreaPath.lineTo(x, y)
            } else {
                itchPath.lineTo(x, y)
                itchAreaPath.lineTo(x, y)
            }
        }
        itchAreaPath.lineTo((itchLevels.size - 1) * stepX, paddingTop + chartHeight)
        itchAreaPath.close()

        drawPath(
            path = itchAreaPath,
            brush = Brush.verticalGradient(
                listOf(Color(0xFFEC4899).copy(alpha = 0.28f), Color(0xFFEC4899).copy(alpha = 0.02f))
            )
        )
        drawPath(itchPath, color = Color(0xFFEC4899), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

        // Fatigue Path (WarningAmber)
        val fatiguePath = Path()
        fatigueLevels.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - (v / maxScale) * chartHeight
            if (i == 0) fatiguePath.moveTo(x, y) else fatiguePath.lineTo(x, y)
        }
        drawPath(fatiguePath, color = Color(0xFFF59E0B), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Points
        itchLevels.forEachIndexed { i, v ->
            val x = i * stepX
            val y = paddingTop + chartHeight - (v / maxScale) * chartHeight
            val isSel = i == selectedIndex
            drawCircle(color = Color.White, radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(), center = Offset(x, y))
            drawCircle(color = Color(0xFFEC4899), radius = if (isSel) 4.5.dp.toPx() else 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun VitalsBarChart(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingBottom = 20f
        val chartHeight = height - paddingBottom

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sugarValues = listOf(95f, 92f, 98f, 94f, 90f, 96f, 92f) // Fasting blood sugar (mg/dL)
        val spo2Values = listOf(98f, 99f, 97f, 98f, 99f, 98f, 99f) // SpO2 (%)

        val groupWidth = width / days.size
        val barWidth = 10f

        days.forEachIndexed { i, _ ->
            val groupCenter = i * groupWidth + (groupWidth / 2f)

            // Sugar Bar (Teal Primary)
            val sugarVal = sugarValues[i]
            val sugarH = (sugarVal / 150f) * chartHeight
            val sugarY = chartHeight - sugarH
            drawRoundRect(
                color = Color(0xFF0D9488),
                topLeft = Offset(groupCenter - barWidth - 2f, sugarY),
                size = Size(barWidth, sugarH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // SpO2 Bar (Blue)
            val spo2Val = spo2Values[i]
            val spo2H = (spo2Val / 100f) * chartHeight * 0.9f
            val spo2Y = chartHeight - spo2H
            drawRoundRect(
                color = Color(0xFF2563EB),
                topLeft = Offset(groupCenter + 2f, spo2Y),
                size = Size(barWidth, spo2H),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
fun RiskDistributionDonutChart(
    lowCount: Int,
    moderateCount: Int,
    highCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (lowCount + moderateCount + highCount).toFloat().coerceAtLeast(1f)
    val lowSweep = (lowCount / total) * 360f
    val modSweep = (moderateCount / total) * 360f
    val highSweep = (highCount / total) * 360f

    Canvas(modifier = modifier) {
        val strokeWidth = 24f
        val chartSize = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = Size(chartSize, chartSize)

        var startAngle = -90f

        // Low Risk (Green)
        drawArc(
            color = Color(0xFF0F9D58),
            startAngle = startAngle,
            sweepAngle = lowSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )
        startAngle += lowSweep

        // Moderate (Amber)
        drawArc(
            color = Color(0xFFF59E0B),
            startAngle = startAngle,
            sweepAngle = modSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )
        startAngle += modSweep

        // High (Red)
        if (highSweep > 0f) {
            drawArc(
                color = Color(0xFFD90429),
                startAngle = startAngle,
                sweepAngle = highSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

@Composable
fun RiskLegendItem(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp)
        }
        Text("$count scans", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
