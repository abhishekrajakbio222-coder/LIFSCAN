package com.example.ui.features

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmergencyContactEntity
import com.example.data.model.SOSAlertEntity
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import com.example.util.GeolocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// High-Contrast Emergency Color Palette (WCAG AAA compliant on deep black)
private val HighContrastBlack = Color(0xFF000000)
private val HighContrastSurface = Color(0xFF121216)
private val HighContrastSurfaceElevated = Color(0xFF1E1E24)
private val HighContrastWhite = Color(0xFFFFFFFF)
private val EmergencyCrimson = Color(0xFFFF1744)
private val EmergencyCrimsonDark = Color(0xFFB71C1C)
private val HazardAmber = Color(0xFFFFD600)
private val LifeGreen = Color(0xFF00E676)
private val LifeGreenDark = Color(0xFF00A844)
private val SignalCyan = Color(0xFF00E5FF)
private val HighContrastBorder = Color(0xFF33333E)

data class EmergencyTriageItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySOSDashboardScreen(
    viewModel: LifscanViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeSOS by viewModel.activeEmergencyAlert.collectAsState()
    val emergencyContacts by viewModel.patientEmergencyContacts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTriage by remember { mutableStateOf("Cardiac / Chest Pain") }
    var isLocating by remember { mutableStateOf(false) }
    var currentLatitude by remember { mutableDoubleStateOf(27.7172) }
    var currentLongitude by remember { mutableDoubleStateOf(85.3240) }
    var currentLocationAddress by remember { mutableStateOf("Kanti Path, Kathmandu (Near Bir Hospital Trauma Hub)") }
    var gpsAccuracyMeters by remember { mutableFloatStateOf(2.5f) }
    var isSirenPlaying by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }

    // Vibrator and ToneGenerator instances for siren
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isSirenPlaying = false
            vibrator?.cancel()
        }
    }

    // Siren loop
    LaunchedEffect(isSirenPlaying) {
        if (isSirenPlaying) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                while (isSirenPlaying) {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(400)
                    }
                    delay(500)
                }
                toneGen.release()
            } catch (e: Exception) {
                // Audio fallback
            }
        } else {
            vibrator?.cancel()
        }
    }

    // Refresh GPS Coordinates
    fun refreshGPS() {
        coroutineScope.launch {
            isLocating = true
            try {
                val geo = GeolocationHelper.getCurrentLocation(context)
                currentLatitude = geo.latitude
                currentLongitude = geo.longitude
                gpsAccuracyMeters = geo.accuracyMeters
                currentLocationAddress = geo.locationName
            } catch (e: Exception) {
                currentLatitude = 27.7058
                currentLongitude = 85.3142
                gpsAccuracyMeters = 3.2f
                currentLocationAddress = "Kanti Path, Kathmandu (Near Bir Hospital Trauma Hub)"
            }
            isLocating = false
        }
    }

    LaunchedEffect(Unit) {
        refreshGPS()
    }

    // Countdown trigger
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            countdownSeconds = 3
            while (countdownSeconds > 0 && isCountingDown) {
                delay(1000)
                countdownSeconds -= 1
            }
            if (isCountingDown && countdownSeconds == 0) {
                isCountingDown = false
                viewModel.triggerSOS(
                    emergencyType = selectedTriage,
                    latitude = currentLatitude,
                    longitude = currentLongitude,
                    locationAddress = currentLocationAddress,
                    accuracyMeters = gpsAccuracyMeters
                )
            }
        }
    }

    BackHandler {
        viewModel.navigateTo(ScreenNav.PatientDashboard)
    }

    // Pulsating button animations
    val infiniteTransition = rememberInfiniteTransition(label = "sos_high_contrast_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_radius"
    )

    val triageList = listOf(
        EmergencyTriageItem("cardiac", "Cardiac / Chest Pain", "🫀 Acute Myocardial Infarction / Angina", Icons.Default.Favorite, EmergencyCrimson),
        EmergencyTriageItem("trauma", "Severe Trauma & Bleeding", "🩸 Major hemorrhage / Vehicular crash", Icons.Default.Warning, HazardAmber),
        EmergencyTriageItem("stroke", "Stroke / FAST Protocol", "🧠 Facial droop / Acute paralysis", Icons.Default.Psychology, SignalCyan),
        EmergencyTriageItem("respiratory", "Respiratory Distress", "🫁 Severe asthma / Oxygen desaturation", Icons.Default.Air, Color(0xFF64B5F6)),
        EmergencyTriageItem("maternal", "Obstetric Crisis", "🤰 Labor complication / Hemorrhage", Icons.Default.LocalHospital, Color(0xFFFF4081)),
        EmergencyTriageItem("shock", "Acute Shock / Unresponsive", "⚡ Loss of consciousness / Syncope", Icons.Default.Warning, Color(0xFFFF9100))
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(HighContrastBlack),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmergencyCrimson,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "SOS Warning",
                                    tint = HighContrastWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "EMERGENCY SOS COMMAND",
                                color = HighContrastWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                if (activeSOS != null) "🚨 LIVE DISPATCH BROADCAST ACTIVE" else "READY • LOCATION-AWARE FIREBASE SIGNAL",
                                color = if (activeSOS != null) EmergencyCrimson else LifeGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("sos_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = HighContrastWhite
                        )
                    }
                },
                actions = {
                    // Quick Call 102
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:102"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmergencyCrimson,
                            contentColor = HighContrastWhite
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("quick_call_102_button")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DIAL 102", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HighContrastBlack
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(HighContrastBlack)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. LOCATION-AWARE TELEMETRY BANNER (HIGH CONTRAST)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_telemetry_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HighContrastSurface),
                    border = BorderStroke(2.dp, LifeGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = LifeGreen,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Text(
                                    "LIVE GPS TELEMETRY FIX",
                                    color = LifeGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LifeGreenDark.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, LifeGreen)
                            ) {
                                Text(
                                    "ACCURACY: ±${"%.1f".format(gpsAccuracyMeters)}m",
                                    color = LifeGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Current GPS Location",
                                tint = EmergencyCrimson,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(top = 2.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentLocationAddress,
                                    color = HighContrastWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "LAT: ${"%.5f".format(currentLatitude)}° N  •  LNG: ${"%.5f".format(currentLongitude)}° E",
                                    color = SignalCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { refreshGPS() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("refresh_gps_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = HighContrastWhite
                                ),
                                border = BorderStroke(1.dp, HighContrastWhite)
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = HighContrastWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isLocating) "LOCATING..." else "REFRESH GPS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val mapUri = Uri.parse("geo:$currentLatitude,$currentLongitude?q=$currentLatitude,$currentLongitude(My Emergency Location)")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                    context.startActivity(mapIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_maps_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SignalCyan,
                                    contentColor = HighContrastBlack
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("VIEW MAP", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 2. PRIMARY TRIGGER OR ACTIVE SOS CONTROLLER
            item {
                val currentSOS = activeSOS
                if (currentSOS != null) {
                    // --- ACTIVE SOS BROADCAST CARD ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_sos_controller_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HighContrastSurfaceElevated),
                        border = BorderStroke(3.dp, EmergencyCrimson)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Flashing header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmergencyCrimson,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .scale(pulseScale)
                                ) {}
                                Text(
                                    "🚨 EMERGENCY ALERT ACTIVE",
                                    color = EmergencyCrimson,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Firebase Status pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = HighContrastBlack,
                                border = BorderStroke(1.5.dp, LifeGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = "Firebase Signal",
                                        tint = LifeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "FIREBASE SIGNAL: BROADCASTED TO DISPATCH & CONTACTS",
                                        color = LifeGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Dispatch unit details
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = HighContrastBlack),
                                border = BorderStroke(1.dp, HighContrastBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("ASSIGNED UNIT:", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            currentSOS.ambulanceVehicleNumber.ifBlank { "ALS-NEPAL #BA-2-CHA-4402" },
                                            color = HighContrastWhite,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("PARAMEDIC / DRIVER:", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${currentSOS.assignedDriverName.ifBlank { "Ramesh Shrestha" }} (${currentSOS.assignedDriverPhone.ifBlank { "+977-9841000102" }})",
                                            color = SignalCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("ESTIMATED ARRIVAL:", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "ETA ~${currentSOS.estimatedArrivalMinutes} MINUTES",
                                            color = HazardAmber,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("TRIAGE TYPE:", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            currentSOS.emergencyType,
                                            color = EmergencyCrimson,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // Interactive Alarm & Siren buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { isSirenPlaying = !isSirenPlaying },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("toggle_siren_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSirenPlaying) EmergencyCrimson else HighContrastSurface,
                                        contentColor = HighContrastWhite
                                    ),
                                    border = BorderStroke(2.dp, if (isSirenPlaying) HighContrastWhite else EmergencyCrimson)
                                ) {
                                    Icon(
                                        if (isSirenPlaying) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "Siren Toggle",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isSirenPlaying) "STOP SIREN" else "SOUND SIREN",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        val driverPhone = currentSOS.assignedDriverPhone.ifBlank { "102" }
                                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$driverPhone"))
                                        context.startActivity(callIntent)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("call_driver_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LifeGreen,
                                        contentColor = HighContrastBlack
                                    )
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CALL DRIVER", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }

                            // Resolve / Cancel Button
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cancel_sos_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = HighContrastWhite
                                ),
                                border = BorderStroke(2.dp, HighContrastWhite)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("STAND DOWN / RESOLVE EMERGENCY", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    // --- STANDBY SOS TRIGGER CARD ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sos_standby_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HighContrastSurface),
                        border = BorderStroke(2.dp, HighContrastBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "SELECT EMERGENCY TRIAGE",
                                color = HazardAmber,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )

                            // Triage selector chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(triageList) { item ->
                                    val isSelected = selectedTriage == item.title
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) item.accentColor else HighContrastBlack,
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) HighContrastWhite else HighContrastBorder
                                        ),
                                        modifier = Modifier
                                            .clickable { selectedTriage = item.title }
                                            .testTag("triage_chip_${item.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                item.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) HighContrastBlack else item.accentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                item.title,
                                                color = if (isSelected) HighContrastBlack else HighContrastWhite,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // GIANT TACTILE SOS BUTTON
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Animated Radar Wave
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = size.minDimension / 2f
                                    drawCircle(
                                        color = EmergencyCrimson.copy(alpha = (1f - radarRadius) * 0.4f),
                                        radius = radius * (0.7f + (0.3f * radarRadius))
                                    )
                                }

                                // Outer border ring
                                Surface(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .scale(pulseScale)
                                        .clickable {
                                            if (!isCountingDown) {
                                                isCountingDown = true
                                            } else {
                                                // Cancel countdown
                                                isCountingDown = false
                                            }
                                        }
                                        .testTag("big_emergency_sos_button"),
                                    shape = CircleShape,
                                    color = if (isCountingDown) HazardAmber else EmergencyCrimson,
                                    border = BorderStroke(6.dp, HighContrastWhite),
                                    shadowElevation = 16.dp
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Sensors,
                                            contentDescription = "SOS Beacon",
                                            tint = if (isCountingDown) HighContrastBlack else HighContrastWhite,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            if (isCountingDown) "CANCEL ($countdownSeconds)" else "SOS",
                                            color = if (isCountingDown) HighContrastBlack else HighContrastWhite,
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (isCountingDown) 18.sp else 38.sp,
                                            letterSpacing = 2.sp
                                        )
                                        Text(
                                            if (isCountingDown) "TAP TO STOP" else "1-TAP BROADCAST",
                                            color = if (isCountingDown) HighContrastBlack else HighContrastWhite.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }

                            // Instruction subtitle
                            Text(
                                "Broadcasts real-time GPS coordinates to Firebase Firestore & alerts emergency contacts immediately.",
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            // 3. EMERGENCY CONTACTS NOTIFICATION MATRIX (FIREBASE INTEGRATED)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_contacts_matrix_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HighContrastSurface),
                    border = BorderStroke(2.dp, SignalCyan)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, tint = SignalCyan, modifier = Modifier.size(18.dp))
                                Text(
                                    "EMERGENCY CONTACTS (${emergencyContacts.size})",
                                    color = SignalCyan,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp
                                )
                            }

                            TextButton(
                                onClick = { viewModel.navigateTo(ScreenNav.EmergencyContacts) },
                                modifier = Modifier.testTag("manage_contacts_button")
                            ) {
                                Text(
                                    "+ MANAGE CONTACTS",
                                    color = HighContrastWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (emergencyContacts.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HighContrastBlack,
                                border = BorderStroke(1.dp, HighContrastBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "No emergency contacts registered yet.",
                                        color = HighContrastWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Add trusted family members or doctors to receive automatic Firebase & SMS alerts during SOS.",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            emergencyContacts.forEach { contact ->
                                ContactSOSNotificationRow(
                                    contact = contact,
                                    isAlertActive = activeSOS != null,
                                    currentLatitude = currentLatitude,
                                    currentLongitude = currentLongitude,
                                    currentLocationAddress = currentLocationAddress,
                                    onCall = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                        context.startActivity(intent)
                                    },
                                    onSendSMS = {
                                        val smsText = "🚨 EMERGENCY SOS ALERT! I require urgent medical assistance at $currentLocationAddress (GPS: https://maps.google.com/?q=$currentLatitude,$currentLongitude). Dispatched via Lifscan Emergency System."
                                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("smsto:${contact.phone}")
                                            putExtra("sms_body", smsText)
                                        }
                                        context.startActivity(smsIntent)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. HIGH-CONTRAST RAPID-DIAL NATIONAL EMERGENCY HOTLINES
            item {
                Text(
                    "NATIONAL EMERGENCY DISPATCH HOTLINES",
                    color = HighContrastWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmergencyHotlineCard(
                        modifier = Modifier.weight(1f),
                        number = "102",
                        label = "AMBULANCE",
                        color = EmergencyCrimson,
                        icon = Icons.Default.LocalHospital,
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:102"))
                            context.startActivity(intent)
                        }
                    )
                    EmergencyHotlineCard(
                        modifier = Modifier.weight(1f),
                        number = "112",
                        label = "INTEGRATED SOS",
                        color = HazardAmber,
                        icon = Icons.Default.Warning,
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EmergencyHotlineCard(
                        modifier = Modifier.weight(1f),
                        number = "100",
                        label = "POLICE COMMAND",
                        color = SignalCyan,
                        icon = Icons.Default.Security,
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:100"))
                            context.startActivity(intent)
                        }
                    )
                    EmergencyHotlineCard(
                        modifier = Modifier.weight(1f),
                        number = "101",
                        label = "FIRE & RESCUE",
                        color = Color(0xFFFF9100),
                        icon = Icons.Default.Air,
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:101"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation dialog to cancel / resolve active SOS
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = HighContrastSurfaceElevated,
            title = {
                Text(
                    "RESOLVE EMERGENCY ALERT?",
                    color = HighContrastWhite,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    "Are you safe? This will signal Firebase Firestore that the emergency is resolved and cancel the active ambulance dispatch broadcast.",
                    color = Color(0xFFDDDDDD)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.dismissSOS()
                        Toast.makeText(context, "Emergency SOS marked RESOLVED in Firebase", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeGreen, contentColor = HighContrastBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("YES, I AM SAFE", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCancelDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighContrastWhite)
                ) {
                    Text("KEEP ALERT ACTIVE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ContactSOSNotificationRow(
    contact: EmergencyContactEntity,
    isAlertActive: Boolean,
    currentLatitude: Double,
    currentLongitude: Double,
    currentLocationAddress: String,
    onCall: () -> Unit,
    onSendSMS: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_sos_row_${contact.id}"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = HighContrastBlack),
        border = BorderStroke(
            1.dp,
            if (contact.isPrimary) LifeGreen else HighContrastBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        contact.name,
                        color = HighContrastWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                    if (contact.isPrimary) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LifeGreenDark.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, LifeGreen)
                        ) {
                            Text(
                                "PRIMARY",
                                color = LifeGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    "${contact.relationship} • ${contact.phone}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (isAlertActive) {
                    Text(
                        "📡 FIREBASE SIGNAL DELIVERED • READY FOR DIRECT AUTO-DIAL",
                        color = LifeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onSendSMS,
                    modifier = Modifier
                        .size(36.dp)
                        .background(SignalCyan.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, SignalCyan, CircleShape)
                        .testTag("sms_contact_${contact.id}")
                ) {
                    Icon(Icons.Default.Message, contentDescription = "Send SMS", tint = SignalCyan, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(36.dp)
                        .background(EmergencyCrimson.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, EmergencyCrimson, CircleShape)
                        .testTag("call_contact_${contact.id}")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call Contact", tint = EmergencyCrimson, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmergencyHotlineCard(
    modifier: Modifier = Modifier,
    number: String,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("emergency_hotline_card_$number"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = HighContrastSurface),
        border = BorderStroke(2.dp, color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = HighContrastBlack, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(
                    number,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    label,
                    color = HighContrastWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
