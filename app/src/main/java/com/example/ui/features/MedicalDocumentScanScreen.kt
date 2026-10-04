package com.example.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AIDocumentScanAnalysis
import com.example.ai.ExtractedLabResult
import com.example.ai.ExtractedMedication
import com.example.data.model.HealthReportEntity
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DocumentScanCategories = listOf(
    "Prescription (Rx)",
    "Lab Test Report",
    "Discharge Summary",
    "Radiology / Imaging",
    "Doctor Clinical Note",
    "Pharmacy Bill / Slip"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalDocumentScanScreen(
    viewModel: LifscanViewModel
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val isLoading by viewModel.documentScanLoading.collectAsState()
    val processingStep by viewModel.documentScanProcessingStep.collectAsState()
    val latestAnalysis by viewModel.latestDocumentScanAnalysis.collectAsState()
    val capturedBitmap by viewModel.capturedDocumentBitmap.collectAsState()
    val selectedCategory by viewModel.selectedDocumentCategory.collectAsState()
    val savedStatusMessage by viewModel.documentSavedStatusMessage.collectAsState()
    val allReports by viewModel.patientHealthReports.collectAsState()

    var showCameraOverlay by remember { mutableStateOf(false) }
    var userNotesInput by remember { mutableStateOf("") }
    var selectedPastReportForDialog by remember { mutableStateOf<HealthReportEntity?>(null) }
    var showInstructionsDialog by remember { mutableStateOf(false) }

    // Photo picker for uploading existing document / prescription photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let { bmp ->
                    viewModel.runMedicalDocumentScan(
                        bitmap = bmp,
                        category = selectedCategory,
                        notes = userNotesInput
                    )
                }
            } catch (e: Exception) {
                // Fallback scan with preset
                viewModel.runMedicalDocumentScan(
                    bitmap = null,
                    category = selectedCategory,
                    notes = userNotesInput
                )
            }
        }
    }

    // Voice recognition helper via standard RecognizerIntent
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                userNotesInput = if (userNotesInput.isBlank()) spokenText else "$userNotesInput $spokenText"
            }
        }
    }

    if (showCameraOverlay) {
        MedicalScanCameraOverlay(
            initialCategory = "Medical Documents & Prescriptions",
            initialLocation = selectedCategory,
            onCategoryChanged = { cat ->
                viewModel.setSelectedDocumentCategory(cat)
            },
            onLocationChanged = { loc ->
                viewModel.setSelectedDocumentCategory(loc)
            },
            onImageCaptured = { bitmap, category, location ->
                showCameraOverlay = false
                viewModel.runMedicalDocumentScan(
                    bitmap = bitmap,
                    category = if (location.isNotBlank()) location else category,
                    notes = userNotesInput
                )
            },
            onSelectSampleCase = { cat, loc, notes ->
                showCameraOverlay = false
                userNotesInput = notes
                viewModel.setSelectedDocumentCategory(loc)
                viewModel.runMedicalDocumentScan(
                    bitmap = null,
                    category = loc,
                    notes = notes
                )
            },
            onClose = { showCameraOverlay = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DocumentScanner,
                                    contentDescription = "Document Scanner",
                                    tint = Color(0xFF0EA5E9),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Document & Rx Scanner",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "AI Optical Prescription & Report Parser",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("doc_scan_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInstructionsDialog = true },
                        modifier = Modifier.testTag("doc_scan_info_btn")
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Instructions", tint = Color(0xFF0EA5E9))
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.MedicalVault) },
                        modifier = Modifier.testTag("doc_scan_vault_btn")
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = "Vault", tint = SuccessGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("medical_document_scan_screen_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Saved Status Banner (e.g., Meds scheduled or Vault saved)
            if (savedStatusMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("doc_saved_status_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                savedStatusMessage ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Document Category Selector Chips
            item {
                Column {
                    Text(
                        "TARGET DOCUMENT TYPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DocumentScanCategories) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedDocumentCategory(cat) },
                                label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(
                                        when (cat) {
                                            "Prescription (Rx)" -> Icons.Default.Medication
                                            "Lab Test Report" -> Icons.Default.Science
                                            "Discharge Summary" -> Icons.Default.LocalHospital
                                            "Radiology / Imaging" -> Icons.Default.Visibility
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color(0xFF0EA5E9) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFF0EA5E9)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) Color(0xFF0EA5E9) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.testTag("doc_category_chip_${cat.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 3. Primary Scanner & Upload Action Card (When no result or ready to scan)
            if (latestAnalysis == null && !isLoading) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scan_trigger_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Hero Camera Trigger Button
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF0EA5E9).copy(alpha = 0.25f),
                                                Color(0xFF0EA5E9).copy(alpha = 0.05f)
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .border(2.dp, Color(0xFF0EA5E9), CircleShape)
                                    .clickable { showCameraOverlay = true }
                                    .testTag("launch_camera_scanner_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Capture Document",
                                    tint = Color(0xFF0EA5E9),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                "Scan Prescription or Medical Document",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                "Point device camera at any handwritten or printed doctor slip, discharge note, or blood report for instant AI reading.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showCameraOverlay = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("start_camera_btn")
                                ) {
                                    Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF0EA5E9)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("upload_gallery_btn")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Upload Image", fontSize = 13.sp, color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 4. Optional Context Notes & Voice Dictation
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Doctor / Clinical Context (Optional)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak doctor notes or medical context...")
                                            }
                                            speechLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            // Handle case where speech recognition is unavailable
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("doc_voice_dictation_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = Color(0xFF0EA5E9),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = userNotesInput,
                                onValueChange = { userNotesInput = it },
                                placeholder = {
                                    Text(
                                        "e.g. Dr. Sandeep Adhikari OPD consultation notes or patient symptoms...",
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("doc_context_notes_input"),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                maxLines = 3
                            )
                        }
                    }
                }

                // 5. Preset Sample Documents for Instant 1-Tap Testing
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "QUICK TEST PRESETS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                "1-Tap Demo",
                                fontSize = 11.sp,
                                color = Color(0xFF0EA5E9),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        SampleDocCard(
                            icon = Icons.Default.Medication,
                            title = "Cardiology Prescription (Rx)",
                            doctor = "Dr. Sandeep Adhikari, MD (Bir Hospital)",
                            tags = listOf("Telmisartan 40mg", "Levocet-M", "Paracetamol"),
                            tagColor = Color(0xFF0EA5E9),
                            onClick = {
                                viewModel.setSelectedDocumentCategory("Prescription (Rx)")
                                viewModel.runMedicalDocumentScan(
                                    bitmap = null,
                                    category = "Prescription (Rx)",
                                    notes = "Hypertension and seasonal respiratory allergy follow-up prescription."
                                )
                            },
                            testTag = "preset_cardiology_rx"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SampleDocCard(
                            icon = Icons.Default.Science,
                            title = "Complete Blood Count & Metabolic Panel",
                            doctor = "National Public Health Laboratory",
                            tags = listOf("Hb: 12.8", "FBS: 112 mg/dL", "Vit D: 18.4"),
                            tagColor = WarningAmber,
                            onClick = {
                                viewModel.setSelectedDocumentCategory("Lab Test Report")
                                viewModel.runMedicalDocumentScan(
                                    bitmap = null,
                                    category = "Lab Test Report",
                                    notes = "Routine metabolic and fasting blood test report with prediabetes screening."
                                )
                            },
                            testTag = "preset_cbc_lab"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SampleDocCard(
                            icon = Icons.Default.LocalHospital,
                            title = "Surgical Trauma Discharge Summary",
                            doctor = "Bir Hospital Trauma Care Center",
                            tags = listOf("Cefuroxime 500mg", "Aceclofenac", "Pantoprazole"),
                            tagColor = SuccessGreen,
                            onClick = {
                                viewModel.setSelectedDocumentCategory("Discharge Summary")
                                viewModel.runMedicalDocumentScan(
                                    bitmap = null,
                                    category = "Discharge Summary",
                                    notes = "Post-operative soft tissue trauma discharge summary."
                                )
                            },
                            testTag = "preset_discharge_summary"
                        )
                    }
                }
            }

            // 6. Loading State (Multi-stage progress animation)
            if (isLoading) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("doc_scan_loading_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(54.dp),
                                color = Color(0xFF0EA5E9),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                "AI Optical Medical Document Parsing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                processingStep.ifBlank { "Extracting medical text, dosages, and diagnostic biomarkers..." },
                                fontSize = 12.sp,
                                color = Color(0xFF0EA5E9),
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF0EA5E9),
                                trackColor = Color(0xFF0EA5E9).copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // 7. Analysis Result Assessment View
            if (latestAnalysis != null && !isLoading) {
                val analysis = latestAnalysis!!

                // Document Thumbnail (if captured) & Re-scan action header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "AI CLINICAL EXTRACTION RESULT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        TextButton(
                            onClick = { viewModel.clearDocumentScanResult() },
                            modifier = Modifier.testTag("scan_another_doc_top_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Another", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Document Identity & Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("doc_result_header_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0EA5E9).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        analysis.documentType.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0EA5E9),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Confidence: ${(analysis.confidence * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SuccessGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                analysis.documentTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Patient: ${analysis.patientName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.width(16.dp))

                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(analysis.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(analysis.doctorOrClinicName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0EA5E9))
                            }
                        }
                    }
                }

                // Chief Diagnosis / Indication Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("CLINICAL DIAGNOSIS / INDICATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    analysis.diagnosisOrIndication,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 8. Extracted Prescriptions (Meds) Section
                if (analysis.extractedMedications.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("extracted_medications_section"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Medication, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Prescribed Medications", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0EA5E9).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "${analysis.extractedMedications.size} Meds",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0EA5E9),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // WorkManager System Banner
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0EA5E9).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth().testTag("workmanager_med_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Local WorkManager Notification System automatically queues exact background dose reminders with 'Mark Taken' and 'Snooze' actions.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                analysis.extractedMedications.forEachIndexed { index, med ->
                                    ExtractedMedicationItemCard(
                                        med = med,
                                        index = index + 1,
                                        onTestClick = {
                                            viewModel.sendParsedMedicationNotification(
                                                context = context,
                                                medName = med.name,
                                                dosage = med.dosage,
                                                timing = med.timing,
                                                instructions = med.instructions,
                                                doctorName = analysis.doctorOrClinicName
                                            )
                                        }
                                    )
                                    if (index < analysis.extractedMedications.size - 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action 1: Add all meds to Room and schedule with WorkManager
                                Button(
                                    onClick = {
                                        viewModel.saveExtractedMedicationsToSchedule(
                                            medications = analysis.extractedMedications,
                                            doctorName = analysis.doctorOrClinicName
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("add_meds_to_schedule_btn")
                                ) {
                                    Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Schedule WorkManager Reminders (${analysis.extractedMedications.size} Meds)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Action 2: Trigger immediate notification test for the first medicine
                                    OutlinedButton(
                                        onClick = {
                                            val firstMed = analysis.extractedMedications.firstOrNull()
                                            if (firstMed != null) {
                                                viewModel.sendParsedMedicationNotification(
                                                    context = context,
                                                    medName = firstMed.name,
                                                    dosage = firstMed.dosage,
                                                    timing = firstMed.timing,
                                                    instructions = firstMed.instructions,
                                                    doctorName = analysis.doctorOrClinicName
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("test_parsed_med_btn")
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Test Push Alert", fontSize = 11.sp, color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold)
                                    }

                                    // Action 3: Open Medication Schedule Screen
                                    OutlinedButton(
                                        onClick = { viewModel.navigateTo(ScreenNav.MedicationSchedule) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("view_med_schedule_btn")
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Schedule", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 9. Extracted Lab Biomarkers & Test Results
                if (analysis.extractedLabResults.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("extracted_labs_section"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Science, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Diagnostic Lab Biomarkers", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = WarningAmber.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "${analysis.extractedLabResults.size} Tests",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarningAmber,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                analysis.extractedLabResults.forEach { lab ->
                                    ExtractedLabResultItemRow(lab = lab)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // 10. AI Patient-Friendly Clinical Translation & Explanation
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Patient-Friendly AI Translation", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                analysis.clinicalSummary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 11. Diet & Lifestyle Guidance
                if (analysis.dietAndLifestyleAdvice.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Diet & Recovery Guidance", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                analysis.dietAndLifestyleAdvice.forEach { tip ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // 12. Drug Interactions & Safety Warnings
                if (analysis.drugInteractionsAndWarnings.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WarningAmber.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Drug Interactions & Precautions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                analysis.drugInteractionsAndWarnings.forEach { warning ->
                                    Text(
                                        "• $warning",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 13. Follow-up & Actions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recommended Follow-Up", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                analysis.recommendedFollowUp,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom Primary Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveDocumentReportToVault(analysis = analysis)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("save_doc_to_vault_btn")
                                ) {
                                    Icon(Icons.Default.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save to Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(ScreenNav.ConsultationScheduler) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF0EA5E9)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("book_doctor_from_doc_btn")
                                ) {
                                    Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Consult Doctor", fontSize = 12.sp, color = Color(0xFF0EA5E9), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 14. Vault History of Scanned Documents
            if (allReports.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SAVED VAULT DOCUMENTS (${allReports.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )

                            TextButton(onClick = { viewModel.navigateTo(ScreenNav.MedicalVault) }) {
                                Text("View All in Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        allReports.take(3).forEach { report ->
                            PastDocumentVaultItem(
                                report = report,
                                onClick = { selectedPastReportForDialog = report }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Secure Biometric Document Viewer for past records stored in Room Database
    if (selectedPastReportForDialog != null) {
        val rep = selectedPastReportForDialog!!
        SecureDocumentViewerDialog(
            report = rep,
            viewModel = viewModel,
            onDismiss = { selectedPastReportForDialog = null },
            onExportPdf = {
                selectedPastReportForDialog = null
                viewModel.navigateTo(ScreenNav.MedicalVault)
            },
            onDeleteRecord = {
                viewModel.deleteHealthReport(rep.id, rep.title)
                selectedPastReportForDialog = null
            }
        )
    }

    // Instructions Dialog
    if (showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            icon = {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFF0EA5E9))
            },
            title = {
                Text("How to Scan Medical Documents", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. **Positioning**: Align your prescription or test report inside the camera framing guides at 25–35 cm distance.", fontSize = 12.sp)
                    Text("2. **Lighting**: Ensure even, bright light without shadow. Use the Torch icon if in a dim room.", fontSize = 12.sp)
                    Text("3. **Focus**: Tap on the text on screen to trigger laser autofocus before taking the photo.", fontSize = 12.sp)
                    Text("4. **Automated Reminders**: Tap 'Add All to Medication Schedule' to convert extracted prescriptions directly into daily reminder alerts.", fontSize = 12.sp)
                    Text("5. **Encrypted Vault**: All scans are stored locally with 256-bit AES encryption.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInstructionsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun SampleDocCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    doctor: String,
    tags: List<String>,
    tagColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tagColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tagColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(doctor, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tagColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                tag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tagColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Run Test", tint = tagColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ExtractedMedicationItemCard(
    med: ExtractedMedication,
    index: Int,
    onTestClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("med_item_$index")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0EA5E9),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$index", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        med.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0EA5E9).copy(alpha = 0.15f)
                    ) {
                        Text(
                            med.dosage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0EA5E9),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (onTestClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onTestClick,
                            modifier = Modifier.size(28.dp).testTag("test_med_dose_btn_$index")
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "Test Notification",
                                tint = Color(0xFF0EA5E9),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(med.frequency, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(med.timing, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(med.duration, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "Instructions: ${med.instructions} • Purpose: ${med.purpose}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ExtractedLabResultItemRow(lab: ExtractedLabResult) {
    val statusColor = when (lab.status.uppercase()) {
        "NORMAL" -> SuccessGreen
        "ELEVATED", "HIGH" -> WarningAmber
        "LOW" -> Color(0xFF3B82F6)
        "CRITICAL" -> Color(0xFFEF4444)
        else -> SuccessGreen
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth().testTag("lab_item_${lab.testName.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(lab.testName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Ref: ${lab.referenceRange}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(lab.value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = statusColor)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        lab.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PastDocumentVaultItem(
    report: HealthReportEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("vault_report_${report.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = SuccessGreen.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${report.doctorOrLabName} • ${report.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SuccessGreen
            ) {
                Text(
                    "ENCRYPTED",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
