package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(appointment: AppointmentEntity, viewModel: LifscanViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDoctor = currentUser?.role == UserRole.DOCTOR

    var isMuted by remember { mutableStateOf(false) }
    var isVideoOff by remember { mutableStateOf(false) }
    var isPrescriptionPadOpen by remember { mutableStateOf(false) }
    var prescriptionText by remember { mutableStateOf("1. Cetirizine 10mg - 1 tab at bedtime (7 days)\n2. Hydrocortisone 1% Cream - Apply twice daily\n3. Gentle ceramide moisturizer after bath.") }
    var callSeconds by remember { mutableStateOf(42) }
    var showEndCallDialog by remember { mutableStateOf(false) }
    var doctorRating by remember { mutableStateOf(5) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val minutes = callSeconds / 60
    val secs = callSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, secs)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Main Remote Video Feed Canvas (Doctor or Patient view)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = if (isDoctor) TealPrimary else DoctorPurple,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isDoctor) Icons.Default.Person else Icons.Default.MedicalServices,
                            contentDescription = "Remote User",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (isDoctor) appointment.patientName else appointment.doctorName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isDoctor) "Patient • Telehealth Room #8492" else "${appointment.doctorSpecialty} • Lifscan Clinic",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Encrypted HD Live • $timeFormatted", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Top Bar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HIPAA End-to-End Encrypted", color = Color.White, fontSize = 10.sp)
                }
            }

            // In-call Doctor Tools / Patient Vitals
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isDoctor) {
                    FilledTonalButton(
                        onClick = { isPrescriptionPadOpen = !isPrescriptionPadOpen },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DoctorPurple,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("e-Prescribe", fontSize = 11.sp)
                    }
                }
            }
        }

        // PiP Self Video View
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp)
                .size(width = 90.dp, height = 130.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            color = Color(0xFF1E293B)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isVideoOff) {
                    Text("Video Off", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Self", tint = TealAccent, modifier = Modifier.size(32.dp))
                        Text("You", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Patient Vitals HUD (for doctor & patient context)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 100.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("LIVE PATIENT HUD", color = TealAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("❤️ HR: 74 bpm • 🫁 SpO2: 99%", color = Color.White, fontSize = 11.sp)
                Text("🩸 BP: 120/80 mmHg", color = Color.White, fontSize = 11.sp)
            }
        }

        // e-Prescription Pad Overlay (if toggled by doctor)
        AnimatedVisibility(
            visible = isPrescriptionPadOpen,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Digital e-Prescription Pad", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        IconButton(onClick = { isPrescriptionPadOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    OutlinedTextField(
                        value = prescriptionText,
                        onValueChange = { prescriptionText = it },
                        label = { Text("Rx Medicines & Care Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.completeAppointmentAndPrescribe(appointment, prescriptionText)
                            isPrescriptionPadOpen = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign & Issue e-Prescription")
                    }
                }
            }
        }

        // Bottom Call Control Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.Black.copy(alpha = 0.8f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isMuted) EmergencyRed else Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White
                    )
                }

                // Video toggle
                IconButton(
                    onClick = { isVideoOff = !isVideoOff },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (isVideoOff) EmergencyRed else Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        if (isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        contentDescription = "Video",
                        tint = Color.White
                    )
                }

                // Chat
                IconButton(
                    onClick = {
                        val otherId = if (isDoctor) appointment.patientId else appointment.doctorId
                        val otherName = if (isDoctor) appointment.patientName else appointment.doctorName
                        val otherRole = if (isDoctor) UserRole.PATIENT else UserRole.DOCTOR
                        viewModel.navigateTo(ScreenNav.ChatDetail(appointment.id, otherName, otherId, otherRole))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White)
                }

                // End Call Button
                IconButton(
                    onClick = { showEndCallDialog = true },
                    modifier = Modifier
                        .size(54.dp)
                        .background(EmergencyRed, CircleShape)
                        .testTag("end_call_btn")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    // End Call Confirmation & Summary Dialog
    if (showEndCallDialog) {
        AlertDialog(
            onDismissRequest = { showEndCallDialog = false },
            title = { Text("End Telehealth Consultation?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Call duration: $timeFormatted. The encrypted consultation log and digital prescription will be saved to medical records.")
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isDoctor) {
                        Text("Rate Consultation Experience:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            (1..5).forEach { star ->
                                IconButton(onClick = { doctorRating = star }) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Star $star",
                                        tint = if (star <= doctorRating) WarningAmber else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndCallDialog = false
                        viewModel.showFeedback("Consultation ended. Digital prescription saved!")
                        if (isDoctor) {
                            viewModel.navigateTo(ScreenNav.DoctorDashboard)
                        } else {
                            viewModel.navigateTo(ScreenNav.PatientDashboard)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("End & Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndCallDialog = false }) {
                    Text("Resume Call")
                }
            }
        )
    }
}
