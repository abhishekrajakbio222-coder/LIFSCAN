package com.example.ui.features

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SOSAlertEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import com.example.util.GeolocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated Emergency SOS Widget Component for the Home Dashboard.
 *
 * Provides:
 * - Real-time location & GPS coordinate tracking
 * - 1-Tap immediate location-based dispatch to nearest ALS/BLS ambulance units
 * - Emergency triage category selector (Cardiac, Trauma, Stroke, Respiratory, etc.)
 * - Live ambulance tracking telemetry with animated ETA, vehicle details & driver contact
 * - Animated radar wave ripple and emergency siren beacons
 * - Quick-dial national emergency hotlines (102, 112, 100)
 */
@Composable
fun EmergencySOSWidget(
    viewModel: LifscanViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFacilities: (() -> Unit)? = null
) {
    val activeSOS by viewModel.activeEmergencyAlert.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTriageCategory by remember { mutableStateOf("Cardiac / Chest Pain") }
    var isLocating by remember { mutableStateOf(false) }
    var currentLatitude by remember { mutableDoubleStateOf(27.7172) }
    var currentLongitude by remember { mutableDoubleStateOf(85.3240) }
    var currentLocationAddress by remember { mutableStateOf("Kanti Path, Kathmandu (Near Bir Hospital Trauma Center)") }
    var gpsAccuracyMeters by remember { mutableIntStateOf(3) }
    var showTriageSelector by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    // Pulsating animation for SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_radius"
    )

    val emergencyCategories = listOf(
        TriageOption("Cardiac / Chest Pain", Icons.Default.Favorite, "🫀 Urgent cardiac triage"),
        TriageOption("Severe Trauma / Accident", Icons.Default.Warning, "🩸 Major bleeding or accident"),
        TriageOption("Stroke / Unresponsive", Icons.Default.Psychology, "🧠 Sudden numbness or slurred speech"),
        TriageOption("Respiratory Distress", Icons.Default.Air, "🫁 Severe shortness of breath"),
        TriageOption("Obstetric / Pregnancy", Icons.Default.ChildCare, "🤰 Labor or acute complication"),
        TriageOption("General Acute Emergency", Icons.Default.LocalHospital, "⚠️ Immediate medical attention")
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("emergency_sos_widget_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (activeSOS != null) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF880816), EmergencyRed, Color(0xFF5C000B))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(EmergencyRed, Color(0xFFBA182B), Color(0xFF780A15))
                        )
                    }
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Title, Live GPS Badge & Beacon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Emergency,
                                    contentDescription = "SOS",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "EMERGENCY SOS",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Live pulsating beacon indicator
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (activeSOS != null) Color(0xFFFFD54F) else Color.White)
                                )
                            }
                            Text(
                                if (activeSOS != null) "🚨 AMBULANCE DISPATCHED • EN ROUTE" else "Instant Location-Based Ambulance Alert",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Emergency 102 Quick Hotline Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:102"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PhoneInTalk,
                                contentDescription = "Dial 102",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Dial 102",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // GPS Location Telemetry & Predefined Emergency Services API Status Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.28f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "GPS Location",
                                    tint = Color(0xFFFFCC00),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        currentLocationAddress,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "GPS: ${String.format("%.4f", currentLatitude)}° N, ${String.format("%.4f", currentLongitude)}° E • ±${gpsAccuracyMeters}m precision",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 9.5.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isLocating = true
                                        val loc = GeolocationHelper.getCurrentLocation(context)
                                        currentLatitude = loc.latitude
                                        currentLongitude = loc.longitude
                                        currentLocationAddress = loc.locationName
                                        gpsAccuracyMeters = loc.accuracyMeters.toInt().coerceAtLeast(2)
                                        delay(300)
                                        isLocating = false
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = "Refresh GPS",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = Color(0xFF80DEEA),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                "Firebase & Emergency Gateway: Live Signal Broadcast Ready",
                                fontSize = 9.sp,
                                color = Color(0xFFE0F7FA),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // High-Contrast Emergency SOS Command Launcher
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = BorderStroke(1.5.dp, Color(0xFFFFD600)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(ScreenNav.EmergencySOSDashboard)
                                }
                                .testTag("launch_high_contrast_sos_dashboard_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Sensors,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD600),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "OPEN HIGH-CONTRAST SOS DASHBOARD",
                                    color = Color(0xFFFFD600),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active SOS State vs Idle SOS Trigger State
                if (activeSOS != null) {
                    ActiveSOSLiveTelemetryView(
                        sosAlert = activeSOS!!,
                        onCancelClick = { showCancelConfirmDialog = true },
                        onCallDriver = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        },
                        onNavigateToFacilities = onNavigateToFacilities
                    )
                } else {
                    IdleSOSTriggerView(
                        selectedCategory = selectedTriageCategory,
                        pulseScale = pulseScale,
                        radarRadius = radarRadius,
                        onSelectCategoryClick = { showTriageSelector = !showTriageSelector },
                        onTriggerSOS = {
                            coroutineScope.launch {
                                val coords = GeolocationHelper.getCurrentLocation(context)
                                val patientName = viewModel.currentUser.value?.name ?: "Aayush Shrestha"
                                val contactPhones = viewModel.patientEmergencyContacts.value.map { it.phone }
                                
                                viewModel.triggerSOS(
                                    emergencyType = selectedTriageCategory,
                                    locationAddress = coords.locationName,
                                    latitude = coords.latitude,
                                    longitude = coords.longitude,
                                    accuracyMeters = coords.accuracyMeters,
                                    contactsNotified = contactPhones
                                )
                                
                                GeolocationHelper.triggerEmergencyContactsAlertSequence(
                                    context = context,
                                    patientName = patientName,
                                    emergencyType = selectedTriageCategory,
                                    contacts = viewModel.patientEmergencyContacts.value,
                                    coordinates = coords
                                )
                            }
                        }
                    )
                }

                // Expandable Triage Category Selector
                AnimatedVisibility(
                    visible = showTriageSelector && activeSOS == null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            "Select Triage Category:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        emergencyCategories.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { option ->
                                    val isSelected = selectedTriageCategory == option.name
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                selectedTriageCategory = option.name
                                                showTriageSelector = false
                                            }
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                option.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) EmergencyRed else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                option.name,
                                                color = if (isSelected) EmergencyRed else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // National Emergency Hotlines Strip
                Spacer(modifier = Modifier.height(14.dp))
                EmergencyHotlinesFooter(context = context)
            }
        }
    }

    // Cancel SOS Confirmation Dialog
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Emergency Alert?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to cancel this emergency dispatch? The assigned ambulance driver and hospital trauma team will be notified of the cancellation.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissSOS()
                        showCancelConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Yes, Cancel Alert", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Keep Active")
                }
            }
        )
    }
}

@Composable
private fun IdleSOSTriggerView(
    selectedCategory: String,
    pulseScale: Float,
    radarRadius: Float,
    onSelectCategoryClick: () -> Unit,
    onTriggerSOS: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Triage Category selector chip
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectCategoryClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Triage: $selectedCategory",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center Giant SOS Action Button with Radar Waves
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Radar expanding ripples
            Canvas(modifier = Modifier.size(120.dp)) {
                val maxRadius = size.minDimension / 2
                drawCircle(
                    color = Color.White.copy(alpha = (1f - radarRadius) * 0.4f),
                    radius = maxRadius * radarRadius,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = (1f - ((radarRadius + 0.5f) % 1f)) * 0.3f),
                    radius = maxRadius * ((radarRadius + 0.5f) % 1f),
                    center = center
                )
            }

            // Main Pulsating SOS Trigger Button
            Surface(
                modifier = Modifier
                    .size(92.dp)
                    .scale(pulseScale)
                    .testTag("main_sos_dispatch_button"),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 10.dp,
                onClick = onTriggerSOS
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Emergency,
                        contentDescription = "Trigger SOS",
                        tint = EmergencyRed,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        "SOS",
                        color = EmergencyRed,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "1-TAP ALERT",
                        color = EmergencyRedDark,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Tap SOS to immediately dispatch the nearest ALS ambulance unit & notify hospital trauma desk with your live GPS location.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActiveSOSLiveTelemetryView(
    sosAlert: SOSAlertEntity,
    onCancelClick: () -> Unit,
    onCallDriver: (String) -> Unit,
    onNavigateToFacilities: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_sos_telemetry_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Live Status Banner with ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EmergencyRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "DISPATCH ACTIVE",
                        color = EmergencyRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmergencyRedLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = EmergencyRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "ETA: ${sosAlert.estimatedArrivalMinutes} MINS",
                            color = EmergencyRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(10.dp))

            // Ambulance & Paramedic Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmbulanceOrange.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AirportShuttle,
                            contentDescription = "Ambulance",
                            tint = AmbulanceOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        sosAlert.assignedDriverName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimaryLight
                    )
                    Text(
                        "Ambulance Plate: ${sosAlert.ambulanceVehicleNumber} • Type A ALS",
                        fontSize = 11.sp,
                        color = TextSecondaryLight
                    )
                    Text(
                        "Destination: Bir Hospital Central Emergency",
                        fontSize = 10.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Call Driver CTA
                IconButton(
                    onClick = { onCallDriver(sosAlert.assignedDriverPhone) },
                    modifier = Modifier
                        .background(SuccessGreen, CircleShape)
                        .size(36.dp)
                        .testTag("call_ambulance_driver_btn")
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Call Driver",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Track on Map & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onNavigateToFacilities != null) {
                    OutlinedButton(
                        onClick = onNavigateToFacilities,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live Route Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f).testTag("cancel_sos_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRedLight)
                ) {
                    Text("Cancel SOS", color = EmergencyRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmergencyHotlinesFooter(context: android.content.Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Emergency Hotlines:",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HotlineChip(label = "🚑 102 (Ambulance)", phone = "102", context = context)
            HotlineChip(label = "👮 100 (Police)", phone = "100", context = context)
            HotlineChip(label = "🔴 112 (Universal)", phone = "112", context = context)
        }
    }
}

@Composable
private fun HotlineChip(
    label: String,
    phone: String,
    context: android.content.Context
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.18f),
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        }
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private data class TriageOption(
    val name: String,
    val icon: ImageVector,
    val subtitle: String
)
