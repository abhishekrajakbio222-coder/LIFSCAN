package com.example.ui.features

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ai.AIMedicalScanAnalysis
import com.example.data.model.SkinScanEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalScanResultDialog(
    analysis: AIMedicalScanAnalysis,
    capturedBitmap: Bitmap?,
    scanEntity: SkinScanEntity?,
    viewModel: LifscanViewModel,
    onDismiss: () -> Unit,
    onRetake: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // Interactive First-Aid Checklist Checked States
    val checkedFirstAidSteps = remember { mutableStateMapOf<Int, Boolean>() }

    val isCritical = analysis.severity.contains("Critical", ignoreCase = true)
    val isUrgent = analysis.severity.contains("Urgent", ignoreCase = true)

    val severityColor = when {
        isCritical -> ErrorRed
        isUrgent -> Color(0xFFF97316)
        analysis.severity.contains("Moderate", ignoreCase = true) -> WarningAmber
        else -> SuccessGreen
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "AI Diagnostic Assessment",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                analysis.category,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_assessment_dialog_btn")) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.showFeedback("Medical scan report saved & encrypted into local Room Vault.")
                                viewModel.navigateTo(ScreenNav.MedicalVault)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("save_vault_report_btn")
                        ) {
                            Icon(Icons.Default.EnhancedEncryption, contentDescription = "Vault", tint = SuccessGreen)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Triage Severity Header Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = severityColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, severityColor),
                        modifier = Modifier.fillMaxWidth().testTag("assessment_severity_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = severityColor,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isCritical || isUrgent) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "TRIAGE SEVERITY: ${analysis.severity.uppercase()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = severityColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    analysis.conditionName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Confidence: ${(analysis.confidence * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LinearProgressIndicator(
                                        progress = { analysis.confidence },
                                        modifier = Modifier.width(80.dp).height(6.dp).clip(CircleShape),
                                        color = severityColor,
                                        trackColor = severityColor.copy(alpha = 0.2f),
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. High-Definition Captured Image & AI Region Analysis
                if (capturedBitmap != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("captured_image_card"),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF8FAFC))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Captured Clinical Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = TealPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "AI Calibrated",
                                            color = TealPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                ) {
                                    Image(
                                        bitmap = capturedBitmap.asImageBitmap(),
                                        contentDescription = "Captured Scan",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Bounding Box Overlay Marker
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(120.dp)
                                            .border(2.dp, severityColor, RoundedCornerShape(8.dp))
                                            .background(severityColor.copy(alpha = 0.15f))
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = severityColor,
                                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                        ) {
                                            Text(
                                                "Target Zone",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Clinical Observation Summary
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clinical Assessment Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                analysis.summary,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // 4. Detected Health Markers Grid
                item {
                    Text(
                        "Detected Health Markers & Tissue Indicators",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        analysis.detectedMarkers.forEach { marker ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkMode) SurfaceDark else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = severityColor, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(marker, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // 5. Interactive Step-by-Step First-Aid & Pre-Hospital Protocol
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFEFF6FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Immediate First-Aid Protocol", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1D4ED8))
                                }
                                Text("Check off steps", fontSize = 10.sp, color = Color(0xFF3B82F6))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            analysis.firstAidProtocol.forEachIndexed { index, step ->
                                val isChecked = checkedFirstAidSteps[index] == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { checkedFirstAidSteps[index] = !isChecked }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checkedFirstAidSteps[index] = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3B82F6))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        step,
                                        fontSize = 12.sp,
                                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Emergency Red Flags Warning Banner
                if (analysis.redFlags.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ErrorRed.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Emergency Red Flags (Seek Immediate ER)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ErrorRed)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                analysis.redFlags.forEach { flag ->
                                    Text("• $flag", fontSize = 11.sp, color = ErrorRed, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }

                // 7. Clinical Specialist & Treatment Guidance
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Recommended Care & Specialist", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessGreen)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Specialist: ${analysis.recommendedSpecialist}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Medication Advice: ${analysis.medicationAdvice}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Care Protocol: ${analysis.treatment}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 8. Mandatory AI Medical Disclaimer
                item {
                    Text(
                        "Lifscan AI preliminary analysis is for emergency decision support and does not replace in-person examination by a licensed medical practitioner. In life-threatening scenarios, immediately call local emergency services (102/112).",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }

                // 9. Quick Action CTA Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        // If critical or urgent -> High Priority 1-Tap SOS Dispatch
                        if (isCritical || isUrgent || analysis.emergencySOSRecommended) {
                            Button(
                                onClick = {
                                    val user = viewModel.currentUser.value
                                    viewModel.triggerSOS(
                                        emergencyType = "Trauma / Medical Injury (${analysis.conditionName})"
                                    )
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("dialog_sos_ambulance_btn")
                            ) {
                                Icon(Icons.Default.EmergencyShare, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("🚨 1-Tap SOS: Dispatch Emergency Ambulance", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Video Consult Specialist Doctor
                        Button(
                            onClick = {
                                viewModel.navigateTo(ScreenNav.ConsultationScheduler)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("dialog_video_consult_btn")
                        ) {
                            Icon(Icons.Default.VideoCall, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Book Video Doctor Consultation", fontWeight = FontWeight.Bold)
                        }

                        // Secondary actions: Save Vault & Retake
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.navigateTo(ScreenNav.MedicalVault)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("dialog_view_vault_btn")
                            ) {
                                Icon(Icons.Default.FolderShared, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Medical Vault", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onRetake()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("dialog_retake_scan_btn")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Another", fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
