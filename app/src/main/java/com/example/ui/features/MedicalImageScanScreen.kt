package com.example.ui.features

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AIMedicalScanAnalysis
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
fun MedicalImageScanScreen(viewModel: LifscanViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val medicalScanLoading by viewModel.medicalScanLoading.collectAsState()
    val latestAnalysis by viewModel.latestMedicalScanAnalysis.collectAsState()
    val latestScanEntity by viewModel.latestMedicalScanEntity.collectAsState()
    val capturedBitmap by viewModel.capturedMedicalBitmap.collectAsState()
    val pastScans by viewModel.patientSkinScans.collectAsState()

    var selectedCategory by remember { mutableStateOf("Physical Injury & Trauma") }
    var selectedLocation by remember { mutableStateOf("Forearm") }
    var userNotes by remember { mutableStateOf("Acute injury with localized redness and pain upon contact.") }
    var isLiveCameraActive by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    val currentCatInfo = remember(selectedCategory) {
        MedicalScanCategories.find { it.id == selectedCategory } ?: MedicalScanCategories[0]
    }

    // Photo Picker contract
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                viewModel.runMedicalImageScan(
                    bitmap = bitmap,
                    category = selectedCategory,
                    anatomicalLocation = selectedLocation,
                    notes = userNotes
                )
                showResultDialog = true
            } catch (e: Exception) {
                viewModel.showFeedback("Error loading gallery image: ${e.message}")
            }
        }
    }

    // Auto-open result dialog when analysis completes
    LaunchedEffect(latestAnalysis) {
        if (latestAnalysis != null) {
            showResultDialog = true
        }
    }

    // 1. Full-screen CameraX Live Scanner Overlay
    if (isLiveCameraActive) {
        Dialog(
            onDismissRequest = { isLiveCameraActive = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
        ) {
            MedicalScanCameraOverlay(
                modifier = Modifier.fillMaxSize(),
                initialCategory = selectedCategory,
                initialLocation = selectedLocation,
                onCategoryChanged = { cat -> selectedCategory = cat },
                onLocationChanged = { loc -> selectedLocation = loc },
                onImageCaptured = { bitmap, cat, loc ->
                    isLiveCameraActive = false
                    selectedCategory = cat
                    selectedLocation = loc
                    viewModel.runMedicalImageScan(
                        bitmap = bitmap,
                        category = cat,
                        anatomicalLocation = loc,
                        notes = userNotes
                    )
                    showResultDialog = true
                },
                onSelectSampleCase = { cat, loc, notes ->
                    isLiveCameraActive = false
                    selectedCategory = cat
                    selectedLocation = loc
                    userNotes = notes
                    // Create simulated sample image
                    val sampleBmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                    viewModel.runMedicalImageScan(
                        bitmap = sampleBmp,
                        category = cat,
                        anatomicalLocation = loc,
                        notes = notes
                    )
                    showResultDialog = true
                },
                onClose = { isLiveCameraActive = false }
            )
        }
    }

    // 2. Comprehensive AI Result Assessment Dialog
    if (showResultDialog && latestAnalysis != null) {
        MedicalScanResultDialog(
            analysis = latestAnalysis!!,
            capturedBitmap = capturedBitmap,
            scanEntity = latestScanEntity,
            viewModel = viewModel,
            onDismiss = { showResultDialog = false },
            onRetake = {
                showResultDialog = false
                isLiveCameraActive = true
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
                            color = currentCatInfo.primaryColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Medical Scanner",
                                    tint = currentCatInfo.primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "AI Medical Image & Injury Scanner",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Real-Time Camera Guidance • Gemini API Triage",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("med_scan_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("med_scan_dark_toggle")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Camera Action Card (High-Impact CTA)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isLiveCameraActive = true }
                    .testTag("launch_camera_overlay_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFF0F172A)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = currentCatInfo.primaryColor,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, currentCatInfo.primaryColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SuccessGreen,
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LIVE CAMERA READY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Launch AI Camera Scanner",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Positions high-precision alignment reticles, real-time lighting & distance telemetry, and connects to Gemini API for instant trauma / marker classification.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { isLiveCameraActive = true },
                            colors = ButtonDefaults.buttonColors(containerColor = currentCatInfo.primaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_live_camera_overlay_btn")
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Live Guidance Overlay", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Category Selector Section
            Column {
                Text(
                    "Select Medical Category:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MedicalScanCategories) { cat ->
                        val isSelected = cat.id == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) cat.primaryColor else if (isDarkMode) SurfaceVariantDark else Color(0xFFF1F5F9),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clickable { selectedCategory = cat.id }
                                .testTag("cat_selector_${cat.id.replace(" ", "_")}")
                        ) {
                            Text(
                                cat.shortName,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Real-Time Guidance Info Box for Selected Category
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = currentCatInfo.primaryColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentCatInfo.primaryColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = currentCatInfo.primaryColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Live Instruction Protocol: ${currentCatInfo.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentCatInfo.primaryColor
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "• Guidance: ${currentCatInfo.realTimeInstruction}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                    Text(
                        "• Optimal Distance: ${currentCatInfo.distanceHint} • Target: ${currentCatInfo.targetDescription}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Anatomical Location Chips
            Column {
                Text(
                    "Anatomical Location:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AnatomicalLocations) { loc ->
                        val isSelected = loc == selectedLocation
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) currentCatInfo.primaryColor.copy(alpha = 0.2f) else if (isDarkMode) SurfaceVariantDark else Color(0xFFF1F5F9),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, currentCatInfo.primaryColor) else null,
                            modifier = Modifier
                                .clickable { selectedLocation = loc }
                                .testTag("loc_chip_$loc")
                        ) {
                            Text(
                                loc,
                                color = if (isSelected) currentCatInfo.primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Patient Symptom / Injury Notes with Voice Dictation
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Observed Symptoms / Injury Notes:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    VoiceSpeechInputButton(
                        onSpokenText = { spoken ->
                            userNotes = if (userNotes.isBlank()) spoken else "$userNotes $spoken"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = userNotes,
                    onValueChange = { userNotes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medical_scan_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    placeholder = { Text("Describe pain, duration, bleeding, or mechanism of injury...") }
                )
            }

            // Secondary Upload / Sample Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("pick_gallery_image_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick from Gallery", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        // Quick run with test sample
                        val sampleBmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
                        viewModel.runMedicalImageScan(
                            bitmap = sampleBmp,
                            category = selectedCategory,
                            anatomicalLocation = selectedLocation,
                            notes = userNotes
                        )
                        showResultDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("run_test_analysis_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analyze Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // AI Processing Loading Banner
            if (medicalScanLoading) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = currentCatInfo.primaryColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentCatInfo.primaryColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = currentCatInfo.primaryColor,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Gemini Medical AI Analyzing...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = currentCatInfo.primaryColor
                            )
                            Text(
                                "Evaluating tissue margins, erythema, edema & generating triage protocol...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recent Scans History Section
            if (pastScans.isNotEmpty()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Past Diagnostic Scans (${pastScans.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        TextButton(onClick = { viewModel.navigateTo(ScreenNav.MedicalVault) }) {
                            Text("View All in Vault", fontSize = 12.sp, color = SuccessGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    pastScans.take(4).forEach { scan ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable {
                                    // Construct analysis object from scan record
                                    val pastAnalysis = AIMedicalScanAnalysis(
                                        conditionName = scan.conditionName,
                                        category = scan.sampleSkinType.ifBlank { "Dermatological Scan" },
                                        severity = when (scan.riskLevel) {
                                            "Critical" -> "Critical (Call SOS)"
                                            "High" -> "Urgent (Clinic Today)"
                                            "Moderate" -> "Moderate (Doctor Review)"
                                            else -> "Mild (Home Care)"
                                        },
                                        confidence = scan.confidenceScore,
                                        summary = scan.aiAnalysisSummary,
                                        detectedMarkers = listOf("Tissue margins examined", "Erythema index recorded", "Localized presentation on ${scan.affectedArea}"),
                                        firstAidProtocol = listOf(
                                            "Keep area clean and dry with protective dressing.",
                                            "Avoid scratching or aggressive friction.",
                                            "Follow prescribed topical treatment."
                                        ),
                                        redFlags = listOf("Rapid spread of redness", "Ascending red streaks", "Systemic fever"),
                                        treatment = scan.recommendedTreatment,
                                        medicationAdvice = scan.medicationAdvice,
                                        recommendedSpecialist = scan.recommendedSpecialist,
                                        emergencySOSRecommended = scan.riskLevel == "Critical"
                                    )
                                    viewModel.runMedicalImageScan(
                                        bitmap = null,
                                        category = scan.sampleSkinType,
                                        anatomicalLocation = scan.affectedArea,
                                        notes = scan.symptomsDescription
                                    )
                                    showResultDialog = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF8FAFC)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (scan.riskLevel) {
                                        "Critical" -> ErrorRed
                                        "High" -> Color(0xFFF97316)
                                        "Moderate" -> WarningAmber
                                        else -> SuccessGreen
                                    },
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(scan.conditionName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "${scan.affectedArea} • ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(scan.timestamp))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "${(scan.confidenceScore * 100).toInt()}% Match",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
