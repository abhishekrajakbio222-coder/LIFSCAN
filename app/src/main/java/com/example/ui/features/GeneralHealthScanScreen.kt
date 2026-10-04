package com.example.ui.features

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VoiceSpeechInputButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralHealthScanScreen(viewModel: LifscanViewModel) {
    val symptomLoading by viewModel.symptomScanLoading.collectAsState()
    val analysisResult by viewModel.latestSymptomAnalysis.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val doctors by viewModel.doctorsList.collectAsState()

    var symptomsText by remember { mutableStateOf("Fever 101F, persistent dry cough, sore throat, and body ache for 2 days.") }
    var bpInput by remember { mutableStateOf("120/80") }
    var hrInput by remember { mutableStateOf("76") }
    var spo2Input by remember { mutableStateOf("98") }
    var tempInput by remember { mutableStateOf("101.2") }

    val quickSymptomTags = listOf(
        "High Fever & Chills",
        "Chest Pain / Breathlessness",
        "Stomach Cramps & Diarrhea",
        "Severe Headache & Dizziness",
        "Sore Throat & Cough"
    )

    val scrollState = rememberScrollState()

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
                                    Icons.Default.MonitorHeart,
                                    contentDescription = "Symptom Checker",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Symptom & Health Scan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Real-time Clinical Triage Assistant", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle_symptom_scan")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
            // Patient Vitals Section
            Text(
                "PATIENT VITALS & BIOMETRICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bpInput,
                    onValueChange = { bpInput = it },
                    label = { Text("BP (mmHg)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hrInput,
                    onValueChange = { hrInput = it },
                    label = { Text("Heart Rate") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = spo2Input,
                    onValueChange = { spo2Input = it },
                    label = { Text("SpO2 %") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tempInput,
                    onValueChange = { tempInput = it },
                    label = { Text("Temp (°F)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Symptom Suggestions & Voice-to-Text Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SYMPTOM NARRATION & DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VoiceSpeechInputButton(
                    onSpokenText = { spoken ->
                        symptomsText = if (symptomsText.isBlank()) spoken else "$symptomsText $spoken"
                    },
                    promptTitle = "Narrate your symptoms or medical concern..."
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quickSymptomTags.take(3).forEach { tag ->
                    AssistChip(
                        onClick = { symptomsText = tag },
                        label = { Text(tag, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Symptom Description Input
            OutlinedTextField(
                value = symptomsText,
                onValueChange = { symptomsText = it },
                label = { Text("Describe Symptoms, Duration & Pain Level") },
                leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = "Symptoms") },
                modifier = Modifier.fillMaxWidth().testTag("symptoms_input_field"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Run Symptom Scan Button
            Button(
                onClick = { viewModel.runSymptomScan(symptomsText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("run_symptom_scan_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                enabled = !symptomLoading
            ) {
                if (symptomLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Triage Analysis in Progress...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Symptoms with AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Analysis Result Display
            val analysis = analysisResult
            if (analysis != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceVariantDark else TealLight
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CLINICAL AI ASSESSMENT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (analysis.severity) {
                                    "Urgent", "Critical" -> EmergencyRed
                                    "Moderate" -> WarningAmber
                                    else -> SuccessGreen
                                }
                            ) {
                                Text(
                                    "${analysis.severity} Severity",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            analysis.primaryDiagnosis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("🔍 Key Clinical Findings:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        analysis.keyFindings.forEach { finding ->
                            Text("• $finding", fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("⚡ Immediate Action Steps:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        analysis.immediateSteps.forEach { step ->
                            Text("✓ $step", fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = InfoBlueLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Recommended Action: ${analysis.recommendedAction}",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E3A8A)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Explicit Medical Disclaimer (Mandatory)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AmberWarningLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Disclaimer",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "MEDICAL DISCLAIMER: This symptom analysis is generated by AI for informational and triage purposes only. It does not constitute professional medical diagnosis, prescription, or emergency treatment. If you are experiencing severe symptoms, call emergency services immediately.",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E),
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (analysis.severity == "Urgent" || analysis.severity == "Critical") {
                                Button(
                                    onClick = { viewModel.triggerSOS("Severe Acute Emergency Symptoms") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                                ) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dispatch Ambulance", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    val doctor = doctors.firstOrNull()
                                    if (doctor != null) {
                                        viewModel.bookDoctorAppointment(
                                            doctor = doctor,
                                            date = "Today",
                                            timeSlot = "Telehealth Video Slot",
                                            isVideo = true,
                                            notes = "Symptoms: ${analysis.primaryDiagnosis}",
                                            paymentMethod = "eSewa / Khalti / UPI"
                                        )
                                        viewModel.navigateTo(ScreenNav.PatientDashboard)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DoctorPurple)
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Book Video Consult", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
