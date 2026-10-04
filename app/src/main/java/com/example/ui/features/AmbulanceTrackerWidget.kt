package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SOSAlertEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import kotlinx.coroutines.delay

/**
 * Real-time Ambulance Tracker Dashboard Component that visualizes incoming emergency transport
 * location data on a simplified, interactive map interface with live telemetry.
 */
@Composable
fun AmbulanceTrackerWidget(
    viewModel: LifscanViewModel,
    onExpandFullScreen: () -> Unit = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) },
    modifier: Modifier = Modifier
) {
    val activeSOS by viewModel.activeEmergencyAlert.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // Simulation of moving ambulance coordinates along path towards patient
    var progress by remember { mutableStateOf(0.35f) }
    var speedKmH by remember { mutableStateOf(52) }
    var distanceKm by remember { mutableStateOf(1.4) }
    var etaMinutes by remember { mutableStateOf(4) }

    val infiniteTransition = rememberInfiniteTransition(label = "strobe_transition")
    val strobeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_alpha"
    )

    val radarRadius = remember { Animatable(10f) }
    val radarAlpha = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        radarRadius.animateTo(
            targetValue = 40f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    // Dynamic telemetry progress loop
    LaunchedEffect(activeSOS) {
        while (true) {
            delay(1500)
            if (progress < 0.95f) {
                progress += 0.02f
                distanceKm = maxOf(0.2, 1.8 * (1f - progress))
                etaMinutes = maxOf(1, (distanceKm * 2.5).toInt())
                speedKmH = (48..62).random()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ambulance_tracker_widget"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFFEF2F2)
        ),
        border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ErrorRed.copy(alpha = strobeAlpha),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LIVE AMBULANCE DISPATCH TRACKER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ErrorRed,
                    modifier = Modifier.clickable { onExpandFullScreen() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("EXPAND MAP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SIMPLIFIED REAL-TIME MAP CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { onExpandFullScreen() }
                    .testTag("tracker_map_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw Map Grid / Road Network
                    val roadColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                    val mainAvenueColor = if (isDarkMode) Color(0xFF475569) else Color(0xFF94A3B8)

                    // Secondary roads
                    drawLine(roadColor, Offset(0f, h * 0.3f), Offset(w, h * 0.3f), strokeWidth = 8f)
                    drawLine(roadColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 8f)
                    drawLine(roadColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), strokeWidth = 8f)
                    drawLine(roadColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 8f)

                    // Main Emergency Corridor Road Path
                    val startPt = Offset(w * 0.12f, h * 0.8f) // Hospital Base
                    val turnPt1 = Offset(w * 0.45f, h * 0.8f)
                    val turnPt2 = Offset(w * 0.45f, h * 0.25f)
                    val endPt = Offset(w * 0.85f, h * 0.25f) // Patient Pin

                    val roadPath = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        lineTo(turnPt1.x, turnPt1.y)
                        lineTo(turnPt2.x, turnPt2.y)
                        lineTo(endPt.x, endPt.y)
                    }

                    // Draw Wide Highway
                    drawPath(roadPath, color = mainAvenueColor, style = Stroke(width = 24f))

                    // Draw Emergency Route Highlight (Flashing Cyan / Green Polyline)
                    drawPath(
                        roadPath,
                        color = Color(0xFF0284C7),
                        style = Stroke(
                            width = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                        )
                    )

                    // 1. HOSPITAL BASE PIN (Start)
                    drawCircle(Color(0xFF0284C7), radius = 10f, center = startPt)
                    drawCircle(Color.White, radius = 5f, center = startPt)

                    // 2. PATIENT LOCATION PIN & RADAR (End)
                    drawCircle(
                        color = ErrorRed.copy(alpha = 0.3f),
                        radius = radarRadius.value,
                        center = endPt
                    )
                    drawCircle(ErrorRed, radius = 12f, center = endPt)
                    drawCircle(Color.White, radius = 6f, center = endPt)

                    // 3. MOVING AMBULANCE POSITION
                    val ambPos = calculatePositionOnRoute(startPt, turnPt1, turnPt2, endPt, progress)
                    drawCircle(Color.White, radius = 16f, center = ambPos)
                    drawCircle(ErrorRed, radius = 14f, center = ambPos)
                    drawCircle(Color.White, radius = 8f, center = ambPos)
                }

                // Map Overlay Badges
                // Top Left: Live Speed & Siren
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$speedKmH km/h", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (strobeAlpha > 0.5f) ErrorRed else Color(0xFF38BDF8),
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }

                // Bottom Right: Patient Destination Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ErrorRed,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonPinCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Patient Beacon", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TELEMETRY HUD ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ETA Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ErrorRed.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ESTIMATED ARRIVAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                        Text("$etaMinutes MINS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                        Text("En Route Now", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Distance Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DISTANCE REMAINING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format("%.1f KM", distanceKm), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                        Text("GPS Live Track", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Unit Type Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AMBULANCE UNIT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("ALS ICU", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        Text("BA 1 PA 4921", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DRIVER & PARAMEDIC DETAILS CARD
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Ramesh Thapa (Lead Paramedic)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("+977-9801234567 • Bir Hospital EMS Unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = { viewModel.showFeedback("Calling Paramedic Ramesh Thapa at +977-9801234567...") },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("call_paramedic_btn")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call Paramedic", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Calculates current (x,y) offset along the simulated polyline route based on progress 0.0..1.0
 */
fun calculatePositionOnRoute(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    progress: Float
): Offset {
    val d1 = kotlin.math.hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
    val d2 = kotlin.math.hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat()
    val d3 = kotlin.math.hypot((p3.x - p2.x).toDouble(), (p3.y - p2.y).toDouble()).toFloat()
    val total = d1 + d2 + d3
    val targetDist = progress * total

    return when {
        targetDist <= d1 -> {
            val t = if (d1 == 0f) 0f else targetDist / d1
            Offset(p0.x + (p1.x - p0.x) * t, p0.y + (p1.y - p0.y) * t)
        }
        targetDist <= d1 + d2 -> {
            val t = if (d2 == 0f) 0f else (targetDist - d1) / d2
            Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)
        }
        else -> {
            val t = if (d3 == 0f) 0f else (targetDist - d1 - d2) / d3
            Offset(p2.x + (p3.x - p2.x) * t, p2.y + (p3.y - p2.y) * t)
        }
    }
}
