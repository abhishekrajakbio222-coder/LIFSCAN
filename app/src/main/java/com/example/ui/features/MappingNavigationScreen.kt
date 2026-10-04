package com.example.ui.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import com.example.util.GeolocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class NavigationManeuver(
    val instruction: String,
    val distanceRemainingText: String,
    val roadName: String,
    val maneuverType: String, // "STRAIGHT", "TURN_RIGHT", "TURN_LEFT", "MERGE", "ARRIVE"
    val subInstruction: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MappingNavigationScreen(
    viewModel: LifscanViewModel,
    destinationName: String,
    destinationAddress: String,
    destinationLat: Double,
    destinationLng: Double,
    facilityType: String = "GOVT_HOSPITAL",
    initialDistanceKm: Double = 1.8,
    isEmergency: Boolean = false,
    contactPhone: String = "+977-1-4221111"
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isVoiceGuidanceActive by remember { mutableStateOf(true) }
    var isNorthUpLocked by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var isNavigating by remember { mutableStateOf(true) }

    // Live Telemetry simulation
    var routeProgress by remember { mutableStateOf(0.15f) }
    var currentSpeedKmH by remember { mutableStateOf(46) }
    var remainingDistanceKm by remember { mutableStateOf(initialDistanceKm) }
    var etaMinutes by remember { mutableStateOf(5) }
    var activeStepIndex by remember { mutableStateOf(0) }

    val navigationSteps = remember(destinationName, destinationAddress) {
        listOf(
            NavigationManeuver(
                instruction = "In 150m, Turn Right onto Kanti Path",
                distanceRemainingText = "150 m",
                roadName = "Kanti Path Emergency Corridor",
                maneuverType = "TURN_RIGHT",
                subInstruction = "Follow signs towards City Medical Hub"
            ),
            NavigationManeuver(
                instruction = "Continue straight on Kanti Path for 600m",
                distanceRemainingText = "600 m",
                roadName = "Kanti Path",
                maneuverType = "STRAIGHT",
                subInstruction = "Passing Central Ambulance Station on right"
            ),
            NavigationManeuver(
                instruction = "Turn Left at Baneshwor Medical Crossing",
                distanceRemainingText = "350 m",
                roadName = "Medical Crossing Way",
                maneuverType = "TURN_LEFT",
                subInstruction = "Use the left 2 lanes"
            ),
            NavigationManeuver(
                instruction = "In 200m, Arrive at $destinationName",
                distanceRemainingText = "200 m",
                roadName = destinationAddress,
                maneuverType = "ARRIVE",
                subInstruction = "Destination will be on your left"
            )
        )
    }

    val currentStep = navigationSteps.getOrElse(activeStepIndex) { navigationSteps.last() }

    // Pulse transition for GPS beacons
    val infiniteTransition = rememberInfiniteTransition(label = "nav_radar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nav_pulse"
    )

    val beaconRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_rot"
    )

    // Dynamic Navigation Telemetry advancement loop
    LaunchedEffect(isNavigating) {
        while (isNavigating) {
            delay(1000)
            if (routeProgress < 0.96f) {
                routeProgress += 0.012f
                remainingDistanceKm = (initialDistanceKm * (1f - routeProgress)).coerceAtLeast(0.05)
                etaMinutes = (remainingDistanceKm * 2.8).toInt().coerceAtLeast(1)
                currentSpeedKmH = (42..54).random()

                // Step Progression
                activeStepIndex = when {
                    routeProgress < 0.30f -> 0
                    routeProgress < 0.60f -> 1
                    routeProgress < 0.85f -> 2
                    else -> 3
                }
            } else {
                currentSpeedKmH = 0
                remainingDistanceKm = 0.0
                etaMinutes = 0
            }
        }
    }

    fun launchExternalGoogleMaps() {
        try {
            val gmmIntentUri = Uri.parse("google.navigation:q=$destinationLat,$destinationLng&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback web / generic geo intent
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$destinationLat,$destinationLng?q=$destinationLat,$destinationLng($destinationName)"))
                context.startActivity(fallbackIntent)
            }
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$destinationLat,$destinationLng"))
            context.startActivity(browserIntent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isEmergency) EmergencyRed else TealPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = "Navigation",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp).rotate(beaconRotate * 0.1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Live Turn-by-Turn Navigation",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "To $destinationName",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) },
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isVoiceGuidanceActive = !isVoiceGuidanceActive
                            viewModel.showFeedback(
                                if (isVoiceGuidanceActive) "Voice navigation alerts activated"
                                else "Voice navigation muted"
                            )
                        },
                        modifier = Modifier.testTag("toggle_voice_guidance_btn")
                    ) {
                        Icon(
                            if (isVoiceGuidanceActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Voice Guidance",
                            tint = if (isVoiceGuidanceActive) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDarkMode) Color(0xFF0B1120) else Color(0xFFF1F5F9))
        ) {
            // ==========================================
            // 1. TOP HIGH-VISIBILITY HUD MANEUVER BANNER
            // ==========================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = if (isEmergency) Color(0xFFB71C1C) else Color(0xFF0F766E)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (currentStep.maneuverType) {
                                    "TURN_RIGHT" -> Icons.Default.TurnRight
                                    "TURN_LEFT" -> Icons.Default.TurnLeft
                                    "ARRIVE" -> Icons.Default.Place
                                    else -> Icons.Default.Straight
                                },
                                contentDescription = "Maneuver",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                currentStep.distanceRemainingText,
                                color = Color(0xFFFFD54F),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Black.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    currentStep.roadName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            currentStep.instruction,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            currentStep.subInstruction,
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ==========================================
            // 2. VECTOR MAPPING NAVIGATION CANVAS
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                    .border(
                        1.dp,
                        if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                // Interactive Turn-by-Turn Canvas Engine
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("interactive_navigation_map_canvas")
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 1. Draw Simulated Road Network Grid
                    drawRoadNetwork(canvasWidth, canvasHeight, isDarkMode)

                    // 2. Compute Dynamic Route Waypoints
                    val pStart = Offset(canvasWidth * 0.25f, canvasHeight * 0.85f)
                    val pTurn1 = Offset(canvasWidth * 0.25f, canvasHeight * 0.60f)
                    val pTurn2 = Offset(canvasWidth * 0.65f, canvasHeight * 0.60f)
                    val pTurn3 = Offset(canvasWidth * 0.65f, canvasHeight * 0.25f)
                    val pDestination = Offset(canvasWidth * 0.80f, canvasHeight * 0.25f)

                    val routePoints = listOf(pStart, pTurn1, pTurn2, pTurn3, pDestination)

                    // 3. Draw Route Polyline Background Glow
                    val fullRoutePath = Path().apply {
                        moveTo(pStart.x, pStart.y)
                        lineTo(pTurn1.x, pTurn1.y)
                        lineTo(pTurn2.x, pTurn2.y)
                        lineTo(pTurn3.x, pTurn3.y)
                        lineTo(pDestination.x, pDestination.y)
                    }

                    // Route Shadow / Glow
                    drawPath(
                        path = fullRoutePath,
                        color = (if (isEmergency) EmergencyRed else TealPrimary).copy(alpha = 0.25f),
                        style = Stroke(
                            width = 24f * zoomLevel,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Main Route Active Polyline
                    drawPath(
                        path = fullRoutePath,
                        brush = Brush.linearGradient(
                            colors = if (isEmergency) listOf(Color(0xFFFF1744), Color(0xFFFF5252), Color(0xFFFF8A80))
                            else listOf(TealDark, TealPrimary, Color(0xFF00E676))
                        ),
                        style = Stroke(
                            width = 12f * zoomLevel,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Waypoint Turn Nodes
                    routePoints.forEachIndexed { index, point ->
                        if (index in 1..3) {
                            drawCircle(
                                color = Color.White,
                                radius = 7f,
                                center = point
                            )
                            drawCircle(
                                color = if (isEmergency) EmergencyRed else TealPrimary,
                                radius = 4f,
                                center = point
                            )
                        }
                    }

                    // 4. Calculate Animated Vehicle GPS Position along route
                    val currentVehiclePosition = calculatePositionOnPath(routePoints, routeProgress)

                    // Draw Radar Wave Pulse from Vehicle
                    drawCircle(
                        color = (if (isEmergency) EmergencyRed else TealPrimary).copy(alpha = 0.45f * (1f - radarPulse)),
                        radius = 45f * radarPulse,
                        center = currentVehiclePosition
                    )

                    // Draw Directional Heading Cone
                    val headingAngle = calculateHeadingAngle(routePoints, routeProgress)
                    val coneLength = 32f
                    val conePoint1 = Offset(
                        currentVehiclePosition.x + coneLength * cos(Math.toRadians(headingAngle - 25.0)).toFloat(),
                        currentVehiclePosition.y + coneLength * sin(Math.toRadians(headingAngle - 25.0)).toFloat()
                    )
                    val conePoint2 = Offset(
                        currentVehiclePosition.x + coneLength * cos(Math.toRadians(headingAngle + 25.0)).toFloat(),
                        currentVehiclePosition.y + coneLength * sin(Math.toRadians(headingAngle + 25.0)).toFloat()
                    )
                    val headingConePath = Path().apply {
                        moveTo(currentVehiclePosition.x, currentVehiclePosition.y)
                        lineTo(conePoint1.x, conePoint1.y)
                        lineTo(conePoint2.x, conePoint2.y)
                        close()
                    }
                    drawPath(
                        path = headingConePath,
                        brush = Brush.radialGradient(
                            colors = listOf((if (isEmergency) Color(0xFFFF8A80) else Color(0xFF80CBC4)).copy(alpha = 0.7f), Color.Transparent),
                            center = currentVehiclePosition,
                            radius = 35f
                        )
                    )

                    // Draw Vehicle GPS Position Ring
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = currentVehiclePosition
                    )
                    drawCircle(
                        color = if (isEmergency) EmergencyRed else InfoBlue,
                        radius = 8f,
                        center = currentVehiclePosition
                    )

                    // 5. Draw Destination Pin Marker
                    drawCircle(
                        color = (if (isEmergency) Color(0xFFFF1744) else SuccessGreen).copy(alpha = 0.35f * (1f - radarPulse)),
                        radius = 35f * radarPulse,
                        center = pDestination
                    )
                    drawCircle(
                        color = if (isEmergency) Color(0xFFFF1744) else SuccessGreen,
                        radius = 16f,
                        center = pDestination
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = pDestination
                    )
                }

                // Overlay Controls: Zoom (+/-), Compass, and GPS Re-center
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { isNorthUpLocked = !isNorthUpLocked }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = "Compass",
                                tint = if (isNorthUpLocked) Color(0xFFFF5252) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(1.8f) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.6f) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                coroutineScope.launch {
                                    routeProgress = 0.15f
                                    viewModel.showFeedback("Re-centering on live GPS location")
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Re-center", tint = TealPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Destination Banner Pill inside Map
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.82f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                destinationName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "GPS: $destinationLat° N, $destinationLng° E",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. BOTTOM TELEMETRY & ROUTE CONTROLS
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) SurfaceDark else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Telemetry Row: Speed, Distance, ETA, Traffic
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Distance Remaining
                        Column {
                            Text("DISTANCE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text(
                                "${String.format("%.1f", remainingDistanceKm)} km",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealPrimary
                            )
                        }

                        // ETA
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EST. TIME", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text(
                                "$etaMinutes min",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Speed
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SPEED", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text(
                                "$currentSpeedKmH km/h",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Traffic Status
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreenLight
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreenDark)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Traffic", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreenDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Launch External Google Maps App
                        Button(
                            onClick = { launchExternalGoogleMaps() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEmergency) EmergencyRed else TealPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("open_external_google_maps_btn")
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = "Maps", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Call Facility
                        OutlinedButton(
                            onClick = {
                                try {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contactPhone"))
                                    context.startActivity(callIntent)
                                } catch (e: Exception) {
                                    viewModel.showFeedback("Dialing $destinationName ($contactPhone)")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("call_destination_facility_btn")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call", fontSize = 12.sp)
                        }

                        // Recalculate
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    routeProgress = 0.05f
                                    viewModel.showFeedback("Route recalculating... Optimal bypass loaded.")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("recalculate_navigation_route_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reroute", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reroute", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws background road network grid
 */
private fun DrawScope.drawRoadNetwork(width: Float, height: Float, isDarkMode: Boolean) {
    val roadColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFCBD5E1)
    val minorRoadColor = if (isDarkMode) Color(0xFF162032) else Color(0xFFE2E8F0)

    // Minor grid roads
    for (i in 1..6) {
        val y = height * (i / 7f)
        drawLine(
            color = minorRoadColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 6f
        )
    }
    for (i in 1..5) {
        val x = width * (i / 6f)
        drawLine(
            color = minorRoadColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 6f
        )
    }

    // Main thoroughfares
    drawLine(
        color = roadColor,
        start = Offset(width * 0.25f, 0f),
        end = Offset(width * 0.25f, height),
        strokeWidth = 20f
    )
    drawLine(
        color = roadColor,
        start = Offset(0f, height * 0.60f),
        end = Offset(width, height * 0.60f),
        strokeWidth = 20f
    )
    drawLine(
        color = roadColor,
        start = Offset(width * 0.65f, 0f),
        end = Offset(width * 0.65f, height),
        strokeWidth = 20f
    )
    drawLine(
        color = roadColor,
        start = Offset(0f, height * 0.25f),
        end = Offset(width, height * 0.25f),
        strokeWidth = 20f
    )
}

/**
 * Calculates current coordinate along polyline path based on fractional progress [0f..1f]
 */
private fun calculatePositionOnPath(points: List<Offset>, progress: Float): Offset {
    if (points.size < 2) return points.firstOrNull() ?: Offset.Zero
    val totalSegments = points.size - 1
    val segmentProgress = progress * totalSegments
    val currentSegmentIndex = segmentProgress.toInt().coerceIn(0, totalSegments - 1)
    val localT = (segmentProgress - currentSegmentIndex).coerceIn(0f, 1f)

    val pA = points[currentSegmentIndex]
    val pB = points[currentSegmentIndex + 1]

    return Offset(
        x = pA.x + (pB.x - pA.x) * localT,
        y = pA.y + (pB.y - pA.y) * localT
    )
}

/**
 * Calculates directional heading angle in degrees along current segment
 */
private fun calculateHeadingAngle(points: List<Offset>, progress: Float): Double {
    if (points.size < 2) return 0.0
    val totalSegments = points.size - 1
    val segmentProgress = progress * totalSegments
    val currentSegmentIndex = segmentProgress.toInt().coerceIn(0, totalSegments - 1)

    val pA = points[currentSegmentIndex]
    val pB = points[currentSegmentIndex + 1]

    val dx = (pB.x - pA.x).toDouble()
    val dy = (pB.y - pA.y).toDouble()

    return Math.toDegrees(Math.atan2(dy, dx))
}
