package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbulanceTrackerScreen(viewModel: LifscanViewModel) {
    val activeSOS by viewModel.activeEmergencyAlert.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var zoomLevel by remember { mutableStateOf(1f) }
    var progress by remember { mutableStateOf(0.42f) }
    var speedKmH by remember { mutableStateOf(56) }
    var distanceKm by remember { mutableStateOf(1.2) }
    var etaMinutes by remember { mutableStateOf(4) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "strobe_full")
    val strobeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_alpha_full"
    )

    val radarRadius = remember { Animatable(10f) }
    LaunchedEffect(Unit) {
        radarRadius.animateTo(
            targetValue = 50f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    // Dynamic telemetry progress loop
    LaunchedEffect(activeSOS) {
        while (true) {
            delay(1200)
            if (progress < 0.95f) {
                progress += 0.015f
                distanceKm = maxOf(0.1, 1.8 * (1f - progress))
                etaMinutes = maxOf(1, (distanceKm * 2.5).toInt())
                speedKmH = (50..65).random()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ErrorRed.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Emergency,
                                    contentDescription = "Ambulance Tracker",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Ambulance Live Tracker",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = ErrorRed.copy(alpha = strobeAlpha),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                            Text(
                                "ALS Unit BA 1 PA 4921 • En Route",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("tracker_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCancelDialog = true }) {
                        Icon(Icons.Default.Cancel, contentDescription = "Dismiss SOS", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
        ) {
            // MAP VIEWPORT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Roads Background
                    val roadColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                    val highwayColor = if (isDarkMode) Color(0xFF334155) else Color(0xFF94A3B8)

                    // Secondary grid
                    drawLine(roadColor, Offset(0f, h * 0.2f), Offset(w, h * 0.2f), strokeWidth = 10f)
                    drawLine(roadColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 10f)
                    drawLine(roadColor, Offset(0f, h * 0.8f), Offset(w, h * 0.8f), strokeWidth = 10f)
                    drawLine(roadColor, Offset(w * 0.2f, 0f), Offset(w * 0.2f, h), strokeWidth = 10f)
                    drawLine(roadColor, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 10f)
                    drawLine(roadColor, Offset(w * 0.8f, 0f), Offset(w * 0.8f, h), strokeWidth = 10f)

                    // Main Emergency Route
                    val startPt = Offset(w * 0.15f, h * 0.85f) // Hospital Base
                    val turnPt1 = Offset(w * 0.5f, h * 0.85f)
                    val turnPt2 = Offset(w * 0.5f, h * 0.25f)
                    val endPt = Offset(w * 0.85f, h * 0.25f) // Patient Beacon

                    val roadPath = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        lineTo(turnPt1.x, turnPt1.y)
                        lineTo(turnPt2.x, turnPt2.y)
                        lineTo(endPt.x, endPt.y)
                    }

                    drawPath(roadPath, color = highwayColor, style = Stroke(width = 30f))

                    // Route Trajectory Line
                    drawPath(
                        roadPath,
                        color = Color(0xFF0284C7),
                        style = Stroke(
                            width = 8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), 0f)
                        )
                    )

                    // 1. HOSPITAL BASE PIN
                    drawCircle(Color(0xFF0284C7), radius = 14f, center = startPt)
                    drawCircle(Color.White, radius = 7f, center = startPt)

                    // 2. PATIENT LOCATION PIN & RADAR
                    drawCircle(
                        color = ErrorRed.copy(alpha = 0.35f),
                        radius = radarRadius.value * 1.5f,
                        center = endPt
                    )
                    drawCircle(ErrorRed, radius = 16f, center = endPt)
                    drawCircle(Color.White, radius = 8f, center = endPt)

                    // 3. MOVING AMBULANCE POSITION
                    val ambPos = calculatePositionOnRoute(startPt, turnPt1, turnPt2, endPt, progress)
                    drawCircle(Color.White, radius = 20f, center = ambPos)
                    drawCircle(ErrorRed, radius = 17f, center = ambPos)
                    drawCircle(Color.White, radius = 10f, center = ambPos)
                }

                // Top Left Overlay: Priority Route Status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (strobeAlpha > 0.5f) ErrorRed else Color(0xFF38BDF8),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "SIRENS & STROBES ACTIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Map Zoom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(2f) },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                    }
                    FloatingActionButton(
                        onClick = { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.8f) },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // BOTTOM TELEMETRY & CONTROLS SHEET
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorRed.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ESTIMATED ETA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                Text("$etaMinutes MIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                Text("High Priority", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("REMAINING DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(String.format("%.1f KM", distanceKm), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                Text("Direct GPS Line", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SPEED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$speedKmH", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                Text("km/h", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Paramedic & Transport Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = TealPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Ramesh Thapa (Lead Paramedic)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("BA 1 PA 4921 • Bir Hospital Trauma Unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { viewModel.showFeedback("Calling Paramedic Ramesh Thapa (+977-9801234567)...") },
                                    modifier = Modifier.testTag("full_call_paramedic_btn")
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = SuccessGreen)
                                }
                                IconButton(
                                    onClick = { viewModel.showFeedback("Direct radio patch connected to Central EMS Dispatch.") }
                                ) {
                                    Icon(Icons.Default.Radio, contentDescription = "Radio", tint = TealPrimary)
                                }
                            }
                        }
                    }

                    // Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cancel_sos_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Text("Resolve / Cancel", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.navigateTo(ScreenNav.FacilitiesMap)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_hospital_route_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hospital Route", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Resolve Emergency Alert?") },
            text = { Text("Are you sure you want to mark this emergency alert as resolved or safely cancelled?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissSOS()
                        showCancelDialog = false
                        viewModel.navigateTo(ScreenNav.PatientDashboard)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Yes, Resolve Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Active")
                }
            }
        )
    }
}
