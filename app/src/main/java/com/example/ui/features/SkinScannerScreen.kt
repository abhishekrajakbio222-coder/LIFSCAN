package com.example.ui.features

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SkinScanEntity
import com.example.ui.components.VoiceSpeechInputButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinScannerScreen(viewModel: LifscanViewModel) {
    val skinScanLoading by viewModel.skinScanLoading.collectAsState()
    val latestResult by viewModel.latestSkinScanResult.collectAsState()
    val scanHistory by viewModel.patientSkinScans.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val doctors by viewModel.doctorsList.collectAsState()

    var selectedSampleType by remember { mutableStateOf("Eczema (Atopic Dermatitis)") }
    var affectedAreaInput by remember { mutableStateOf("Forearm / Inner Elbow") }
    var symptomsNotes by remember { mutableStateOf("Redness with intense itching and dry scaly skin that worsens at night.") }
    var selectedScanForModal by remember { mutableStateOf<SkinScanEntity?>(null) }
    var isLiveCameraActive by remember { mutableStateOf(false) }
    var capturedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val sampleSkinConditions = listOf(
        "Eczema (Atopic Dermatitis)" to "🔴 Pruritic scaly patches",
        "Plaque Psoriasis" to "⚪ Silvery scale plaques",
        "Acne Vulgaris" to "🟡 Follicular inflammatory papules",
        "Melanoma Screening" to "⚠️ Asymmetric pigmented nevus",
        "Fungal Ringworm (Tinea)" to "⭕ Annular red borders",
        "Contact Dermatitis" to "🌸 Acute erythema & rash"
    )

    val scrollState = rememberScrollState()

    // Full-screen Live CameraX Scanner Dialog
    if (isLiveCameraActive) {
        Dialog(
            onDismissRequest = { isLiveCameraActive = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
        ) {
            SkinScanner(
                modifier = Modifier.fillMaxSize(),
                title = "AI Skin Lesion Scanner",
                guideInstructions = "Center lesion inside brackets • Hold 10–15cm away",
                showGuideOverlay = true,
                showControls = true,
                onImageCaptured = { bitmap ->
                    capturedPhotoBitmap = bitmap
                    isLiveCameraActive = false
                    viewModel.showFeedback("Skin photo captured successfully! Ready for AI analysis.")
                },
                onError = { exc ->
                    viewModel.showFeedback("Camera capture error: ${exc.message}")
                },
                onClose = { isLiveCameraActive = false }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SkinScannerPink.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DocumentScanner,
                                    contentDescription = "Skin Scanner",
                                    tint = SkinScannerPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Dermatological AI Scanner",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Advanced Skin Disease Classification",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        modifier = Modifier.testTag("dark_mode_toggle_skin_scan")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Quick Switch to Multi-Category Medical & Injury Scanner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2563EB).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(ScreenNav.MedicalImageScan) }
                    .testTag("switch_to_med_scanner_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Healing, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Looking for Injury, Burns or Eye Scan?", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2563EB))
                        Text("Switch to AI Medical Image & Injury Scanner with live reticle HUD", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2563EB))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scanner Viewport Box with Live Camera Preview or Captured Photo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .testTag("camera_viewport_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFF1E293B)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val captured = capturedPhotoBitmap
                    if (captured != null) {
                        // Display captured high-resolution skin photo
                        Image(
                            bitmap = captured.asImageBitmap(),
                            contentDescription = "Captured Skin Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Retake / Clear overlay
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { isLiveCameraActive = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retake", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake Photo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Viewport placeholder with CTA to open Live CameraX
                        Column(
                            modifier = Modifier
                                .size(170.dp)
                                .border(2.dp, Brush.sweepGradient(listOf(SkinScannerPink, TealAccent, SkinScannerPink)), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera Target",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "DERMA SCAN LENS",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "Hold 10-15cm from lesion",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                        }

                        // Open Live Camera Action overlay
                        Button(
                            onClick = { isLiveCameraActive = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .testTag("open_live_camera_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SkinScannerPink),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Live CameraX Scanner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Encryption and AI Badge overlay
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = SuccessGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CameraX 4K HD", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    if (captured == null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            color = SkinScannerPink.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "Selected: $selectedSampleType",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sample lesion selector chips
            Text(
                "SELECT DERMATOLOGY CONDITION FOR AI SCAN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sampleSkinConditions) { (name, desc) ->
                    val isSelected = selectedSampleType == name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSampleType = name
                            when (name) {
                                "Eczema (Atopic Dermatitis)" -> {
                                    affectedAreaInput = "Forearm / Inner Elbow"
                                    symptomsNotes = "Redness, intense nocturnal itching, and dry scaly skin with micro-cracks."
                                }
                                "Plaque Psoriasis" -> {
                                    affectedAreaInput = "Knee / Elbow Extensor"
                                    symptomsNotes = "Thick, raised silvery-white scale plaques with red borders."
                                }
                                "Acne Vulgaris" -> {
                                    affectedAreaInput = "Face / Forehead / Cheeks"
                                    symptomsNotes = "Inflammatory tender papules, comedones, and excess sebum production."
                                }
                                "Melanoma Screening" -> {
                                    affectedAreaInput = "Upper Back / Shoulder"
                                    symptomsNotes = "Dark asymmetric mole with irregular borders and color variation."
                                }
                                "Fungal Ringworm (Tinea)" -> {
                                    affectedAreaInput = "Torso / Groin Area"
                                    symptomsNotes = "Circular red rash with raised borders and clearer skin in the center."
                                }
                                "Contact Dermatitis" -> {
                                    affectedAreaInput = "Hands / Wrists"
                                    symptomsNotes = "Sudden red blister rash after touching harsh detergent/metal jewelry."
                                }
                            }
                        },
                        label = { Text(name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Affected Area Input
            OutlinedTextField(
                value = affectedAreaInput,
                onValueChange = { affectedAreaInput = it },
                label = { Text("Affected Body Location / Area") },
                leadingIcon = { Icon(Icons.Outlined.PersonPinCircle, contentDescription = "Area") },
                modifier = Modifier.fillMaxWidth().testTag("affected_area_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Voice-to-Text Input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SYMPTOMS & NARRATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VoiceSpeechInputButton(
                    onSpokenText = { spoken ->
                        symptomsNotes = if (symptomsNotes.isBlank()) spoken else "$symptomsNotes $spoken"
                    },
                    promptTitle = "Narrate your skin symptoms or history..."
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Symptoms Description Input
            OutlinedTextField(
                value = symptomsNotes,
                onValueChange = { symptomsNotes = it },
                label = { Text("Symptoms & Lesion Description (Itch, Duration, Pain)") },
                leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = "Notes") },
                modifier = Modifier.fillMaxWidth().testTag("symptoms_notes_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scan Action Button
            Button(
                onClick = {
                    viewModel.runSkinScan(
                        bitmap = capturedPhotoBitmap,
                        skinTypeHint = if (capturedPhotoBitmap != null) "HD Camera Capture ($selectedSampleType)" else selectedSampleType,
                        affectedArea = affectedAreaInput,
                        notes = symptomsNotes
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("run_skin_scan_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkinScannerPink
                ),
                enabled = !skinScanLoading
            ) {
                if (skinScanLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Analyzing Dermatological Lesion with Gemini AI...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan & Analyze Skin Condition", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Analysis Result Card (if available)
            val result = latestResult
            if (result != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFFFF1F2)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(SkinScannerPink, TealAccent))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "AI DERMATOLOGY REPORT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SkinScannerPink
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (result.riskLevel) {
                                    "High", "Critical" -> EmergencyRed
                                    "Moderate" -> WarningAmber
                                    else -> SuccessGreen
                                }
                            ) {
                                Text(
                                    "${result.riskLevel} Risk Level",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            result.conditionName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // Confidence meter
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI Match Confidence: ${(result.confidenceScore * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { result.confidenceScore },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = SkinScannerPink,
                            trackColor = BorderLight
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Observation
                        Text("🔬 Clinical Findings:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(result.aiAnalysisSummary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(10.dp))
                        // Non-pharma care
                        Text("💧 First-Line Care Protocol:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(result.recommendedTreatment, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(10.dp))
                        // Medications advice
                        Text("💊 Medication & Topical Guidance:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(result.medicationAdvice, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(10.dp))
                        // Recommended Specialist
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = InfoBlueLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Recommended Specialist:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InfoBlue)
                                    Text(result.recommendedSpecialist, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E3A8A))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions: Book Dermatologist or Consult
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val doctor = doctors.firstOrNull { it.specialization.contains("Dermatolog", ignoreCase = true) } ?: doctors.firstOrNull()
                                    if (doctor != null) {
                                        viewModel.bookDoctorAppointment(
                                            doctor = doctor,
                                            date = "Today",
                                            timeSlot = "Immediate Slot",
                                            isVideo = true,
                                            notes = "Consultation for ${result.conditionName} (${result.affectedArea})",
                                            paymentMethod = "eSewa / Khalti / UPI"
                                        )
                                        viewModel.navigateTo(ScreenNav.PatientDashboard)
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("consult_derma_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DoctorPurple)
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Video Consult Dr.", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.showFeedback("PDF record generated for ${result.conditionName}!")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save PDF", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Historical Scans List
            Text(
                "SAVED SKIN SCAN HISTORY (${scanHistory.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (scanHistory.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        "No past skin scans found. Run your first analysis above.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                scanHistory.forEach { scan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedScanForModal = scan },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SkinScannerPink.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = SkinScannerPink, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(scan.conditionName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "Area: ${scan.affectedArea} • ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(scan.timestamp))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (scan.riskLevel) {
                                    "High", "Critical" -> EmergencyRedLight
                                    "Moderate" -> WarningAmberLight
                                    else -> SuccessGreenLight
                                }
                            ) {
                                Text(
                                    scan.riskLevel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (scan.riskLevel) {
                                        "High", "Critical" -> EmergencyRedDark
                                        "Moderate" -> Color(0xFFB45309)
                                        else -> Color(0xFF065F46)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Scan Detail Dialog
    val detail = selectedScanForModal
    if (detail != null) {
        AlertDialog(
            onDismissRequest = { selectedScanForModal = null },
            title = { Text(detail.conditionName, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("Affected Area: ${detail.affectedArea}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Risk Level: ${detail.riskLevel} (${(detail.confidenceScore * 100).toInt()}% Match)", fontSize = 12.sp, color = TealPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI Summary:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(detail.aiAnalysisSummary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Treatment & Care:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(detail.recommendedTreatment, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Medication Protocol:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(detail.medicationAdvice, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedScanForModal = null }) {
                    Text("Close")
                }
            }
        )
    }
}
