package com.example.ui.components

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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.EmergencyContactEntity
import com.example.data.model.SOSAlertEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import com.example.util.GeolocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * High-Priority Floating Emergency SOS Action Button & Simulated Emergency Sequence.
 *
 * Features:
 * 1. Movable/Draggable Floating Action Button with radar wave halo and heartbeat pulse.
 * 2. Active emergency state indicator & flashing badge.
 * 3. 5-Second Safety Abort Countdown window with audio beeps and instant cancel.
 * 4. Real-time GPS Location & Telemetry Lock (Coordinates, Address, Google Maps Live Pin).
 * 5. Notifying Emergency Services (Simulated Central EMS 102/112 API uplink with ALS Ambulance dispatch).
 * 6. Live Location & SOS Broadcast to Pre-Set Contacts (SMS delivery status, 1-tap dial, maps link).
 * 7. Active Emergency Console HUD with first-aid guidance, audio alarm beacon, and live driver tracking.
 */
@Composable
fun FloatingEmergencySOSButton(
    viewModel: LifscanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeEmergency by viewModel.activeEmergencyAlert.collectAsState()
    val emergencyContacts by viewModel.patientEmergencyContacts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showEmergencyModal by remember { mutableStateOf(false) }

    // Draggable position state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Pulsing heartbeat & radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "sos_float_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (activeEmergency != null) 1.18f else 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (activeEmergency != null) 600 else 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sos_pulse"
    )

    val radarRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (activeEmergency != null) 1200 else 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sos_radar"
    )

    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (activeEmergency != null) 1200 else 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sos_radar_alpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .padding(12.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("floating_emergency_sos_container")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            // Direct Call 102 Quick Pill Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF22C55E)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:102"))
                            context.startActivity(intent)
                            Toast.makeText(context, "Direct Call: Dialing 102 (Ambulance)...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Dialer error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .testTag("floating_direct_call_102_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhoneInTalk,
                        contentDescription = "Direct Call 102",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Text(
                            text = "CALL 102",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Direct Line",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E)
                        )
                    }
                }
            }

            // Main High-Priority Floating SOS Button with radar wave
            Box(contentAlignment = Alignment.Center) {
                // Multi-Layer Radar Ripple Background
                Canvas(
                    modifier = Modifier.size(76.dp)
                ) {
                    val centerOffset = Offset(size.width / 2, size.height / 2)
                    val baseRadius = (size.minDimension / 2) * 0.75f
                    drawCircle(
                        color = (if (activeEmergency != null) Color(0xFFFF1744) else EmergencyRed).copy(alpha = radarAlpha),
                        radius = baseRadius * radarRadius,
                        center = centerOffset
                    )
                    drawCircle(
                        color = (if (activeEmergency != null) Color(0xFFFFD600) else Color(0xFFFF5252)).copy(alpha = radarAlpha * 0.7f),
                        radius = baseRadius * ((radarRadius + 0.4f) % 1.4f),
                        center = centerOffset
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (activeEmergency != null) Color(0xFFB71C1C) else EmergencyRed,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(pulseScale)
                        .border(
                            width = 2.5.dp,
                            color = if (activeEmergency != null) Color(0xFFFFEB3B) else Color.White,
                            shape = CircleShape
                        )
                        .clickable {
                            showEmergencyModal = true
                        }
                        .testTag("floating_sos_fab")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (activeEmergency != null) Color(0xFFFF1744) else Color(0xFFEF4444),
                                        if (activeEmergency != null) Color(0xFF880E4F) else Color(0xFF991B1B)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (activeEmergency != null) Icons.Default.Campaign else Icons.Default.Emergency,
                                contentDescription = "Trigger Emergency SOS",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                if (activeEmergency != null) "ACTIVE" else "SOS",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = if (activeEmergency != null) 9.sp else 11.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }

        // Active Emergency Indicator Badge
        if (activeEmergency != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFD600),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB71C1C))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "DISPATCHED",
                        color = Color(0xFF7F0000),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }

    // Comprehensive Emergency Sequence & Control Modal
    if (showEmergencyModal) {
        SimulatedEmergencySequenceModal(
            viewModel = viewModel,
            activeSOS = activeEmergency,
            emergencyContacts = emergencyContacts,
            currentUserName = currentUser?.name ?: "Aayush Shrestha",
            currentUserBloodGroup = currentUser?.bloodGroup ?: "O+",
            onDismiss = { showEmergencyModal = false }
        )
    }
}

/**
 * Full-featured Simulated Emergency Sequence Dialog & Active Response Console.
 */
@Composable
fun SimulatedEmergencySequenceModal(
    viewModel: LifscanViewModel,
    activeSOS: SOSAlertEntity?,
    emergencyContacts: List<EmergencyContactEntity>,
    currentUserName: String,
    currentUserBloodGroup: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Emergency steps: "CONFIG" -> "COUNTDOWN" -> "DISPATCHING" -> "ACTIVE_HUD"
    var modalState by remember {
        mutableStateOf(if (activeSOS != null) "ACTIVE_HUD" else "CONFIG")
    }

    var selectedEmergencyType by remember { mutableStateOf("Cardiac / Acute Chest Pain") }
    var detectedCoords by remember {
        mutableStateOf(GeolocationHelper.getCurrentLocation(context))
    }

    // Countdown state (5 seconds)
    var countdownRemaining by remember { mutableIntStateOf(5) }
    var isCountdownRunning by remember { mutableStateOf(false) }

    // Dispatch progress steps simulation
    var dispatchStep by remember { mutableIntStateOf(0) }
    val dispatchLogs = remember { mutableStateListOf<String>() }

    // Audio alarm tone and vibrator siren system
    var isAudioAlarmPlaying by remember { mutableStateOf(false) }

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Helper sound & haptic beep player
    fun playBeepTone(toneType: Int = ToneGenerator.TONE_PROP_BEEP, vibrateMs: Long = 120L) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 85)
            toneGen.startTone(toneType, 200)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(vibrateMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrateMs)
            }
        } catch (_: Exception) {}
    }

    // Continuous Emergency Siren Loop (Audio ToneGenerator + Vibrator Waveform)
    LaunchedEffect(isAudioAlarmPlaying) {
        if (isAudioAlarmPlaying) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 450, 150, 450, 150, 700, 250)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 450, 150, 450, 150, 700, 250), 0)
                }
            } catch (_: Exception) {}

            while (isAudioAlarmPlaying) {
                try {
                    val toneHigh = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneHigh.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 420)
                    delay(480)
                    val toneLow = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneLow.startTone(ToneGenerator.TONE_SUP_RINGTONE, 420)
                    delay(480)
                } catch (_: Exception) {
                    delay(800)
                }
            }
        } else {
            try {
                vibrator?.cancel()
            } catch (_: Exception) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                vibrator?.cancel()
            } catch (_: Exception) {}
        }
    }

    // Update GPS coordinates whenever dialog opens
    LaunchedEffect(Unit) {
        detectedCoords = GeolocationHelper.getCurrentLocation(context)
    }

    // Countdown timer effect
    LaunchedEffect(isCountdownRunning) {
        if (isCountdownRunning) {
            countdownRemaining = 5
            while (countdownRemaining > 0 && isCountdownRunning) {
                playBeepTone(ToneGenerator.TONE_PROP_BEEP, 140L)
                delay(1000)
                countdownRemaining--
            }
            if (isCountdownRunning && countdownRemaining == 0) {
                playBeepTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 300L)
                isCountdownRunning = false
                modalState = "DISPATCHING"
            }
        }
    }

    // Multi-step Emergency Dispatch Simulation effect
    LaunchedEffect(modalState) {
        if (modalState == "DISPATCHING") {
            dispatchLogs.clear()
            dispatchStep = 1
            dispatchLogs.add("🛰️ Acquiring high-precision GPS satellite telemetry lock...")
            delay(800)

            dispatchStep = 2
            val coords = detectedCoords
            dispatchLogs.add("📍 Location locked: ${coords.formattedCoordinates} (±${coords.accuracyMeters.toInt()}m precision)")
            dispatchLogs.add("📡 Transmitting encrypted SOS beacon payload to Nepal EMS (102 / 112 Dispatch Hub)...")
            delay(1000)

            dispatchStep = 3
            dispatchLogs.add("🔐 Patient Medical ID transmitted: $currentUserName (Blood Group: $currentUserBloodGroup)")
            dispatchLogs.add("🏥 Bir Hospital Central Trauma desk & Emergency Command acknowledged alert.")
            delay(1100)

            dispatchStep = 4
            dispatchLogs.add("🚑 Assigning nearest ALS Type-A Ambulance Unit #AMB-104 (Driver: Ramesh Thapa)...")
            dispatchLogs.add("📲 Broadcasting live GPS tracking URL & emergency SMS to ${emergencyContacts.size} pre-set responders...")
            
            // Trigger actual repository & Room persistence
            val contactPhones = emergencyContacts.map { it.phone }
            viewModel.triggerSOS(
                emergencyType = selectedEmergencyType,
                locationAddress = coords.locationName,
                latitude = coords.latitude,
                longitude = coords.longitude,
                accuracyMeters = coords.accuracyMeters,
                contactsNotified = contactPhones
            )

            // Trigger real SMS / phone intents if available
            GeolocationHelper.triggerEmergencyContactsAlertSequence(
                context = context,
                patientName = currentUserName,
                emergencyType = selectedEmergencyType,
                contacts = emergencyContacts,
                coordinates = coords
            )

            delay(1200)
            dispatchStep = 5
            dispatchLogs.add("🚨 EMERGENCY RESPONSE ACTIVE! Ambulance en route. ETA: 4 Minutes.")
            delay(800)
            isAudioAlarmPlaying = true
            modalState = "ACTIVE_HUD"
        }
    }

    Dialog(
        onDismissRequest = {
            if (modalState != "COUNTDOWN" && modalState != "DISPATCHING") {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = modalState != "COUNTDOWN" && modalState != "DISPATCHING",
            dismissOnClickOutside = modalState != "COUNTDOWN" && modalState != "DISPATCHING"
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(8.dp)
                .testTag("simulated_emergency_sequence_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmergencyRed,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Emergency,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "LIFSCAN EMERGENCY SOS",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = EmergencyRed,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                if (modalState == "ACTIVE_HUD") "Active Emergency Tracking Console" else "Instant Simulated Emergency Dispatch",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = modalState != "COUNTDOWN" && modalState != "DISPATCHING",
                        modifier = Modifier.testTag("close_sos_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Helper for 1-tap direct dialing
                fun triggerDirectDial(phone: String, label: String) {
                    try {
                        val cleanNumber = phone.replace(" ", "").replace("-", "")
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
                        context.startActivity(intent)
                        Toast.makeText(context, "Direct Call: Dialing $label ($phone)...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to dial: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Switch Content Based on Modal State
                when (modalState) {
                    "CONFIG" -> {
                        EmergencyConfigView(
                            selectedEmergencyType = selectedEmergencyType,
                            onSelectType = { selectedEmergencyType = it },
                            detectedCoords = detectedCoords,
                            onRefreshLocation = {
                                detectedCoords = GeolocationHelper.getCurrentLocation(context)
                                Toast.makeText(context, "GPS Location Updated", Toast.LENGTH_SHORT).show()
                            },
                            emergencyContacts = emergencyContacts,
                            onStartSequence = {
                                isCountdownRunning = true
                                modalState = "COUNTDOWN"
                            },
                            onInstantBypass = {
                                modalState = "DISPATCHING"
                            },
                            onDirectCall = ::triggerDirectDial
                        )
                    }

                    "COUNTDOWN" -> {
                        EmergencyCountdownView(
                            countdownRemaining = countdownRemaining,
                            selectedEmergencyType = selectedEmergencyType,
                            onCancel = {
                                isCountdownRunning = false
                                modalState = "CONFIG"
                                Toast.makeText(context, "SOS Dispatch Cancelled Safely", Toast.LENGTH_SHORT).show()
                            },
                            onInstantDispatch = {
                                isCountdownRunning = false
                                modalState = "DISPATCHING"
                            },
                            onDirectCall = ::triggerDirectDial
                        )
                    }

                    "DISPATCHING" -> {
                        EmergencyDispatchingProgressView(
                            dispatchStep = dispatchStep,
                            dispatchLogs = dispatchLogs,
                            selectedEmergencyType = selectedEmergencyType,
                            currentUserName = currentUserName,
                            contactsCount = emergencyContacts.size
                        )
                    }

                    "ACTIVE_HUD" -> {
                        ActiveEmergencyConsoleHUD(
                            viewModel = viewModel,
                            sosAlert = activeSOS,
                            emergencyContacts = emergencyContacts,
                            detectedCoords = detectedCoords,
                            isAudioAlarmPlaying = isAudioAlarmPlaying,
                            onToggleAudioAlarm = {
                                isAudioAlarmPlaying = !isAudioAlarmPlaying
                                if (isAudioAlarmPlaying) {
                                    playBeepTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK)
                                }
                            },
                            onCopyLocationPin = {
                                val link = detectedCoords.googleMapsUrl
                                clipboardManager.setText(AnnotatedString(link))
                                Toast.makeText(context, "📍 Live GPS Link Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            onCancelSOS = {
                                isAudioAlarmPlaying = false
                                try { vibrator?.cancel() } catch (_: Exception) {}
                                viewModel.dismissSOS()
                                Toast.makeText(context, "SOS Alert resolved safely.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            onCallHotline = { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Step 1: Pre-Trigger Configuration & Triage Selection View.
 */
@Composable
private fun EmergencyConfigView(
    selectedEmergencyType: String,
    onSelectType: (String) -> Unit,
    detectedCoords: GeolocationHelper.GeoCoordinates,
    onRefreshLocation: () -> Unit,
    emergencyContacts: List<EmergencyContactEntity>,
    onStartSequence: () -> Unit,
    onInstantBypass: () -> Unit,
    onDirectCall: (phone: String, label: String) -> Unit = { _, _ -> }
) {
    val emergencyTypes = listOf(
        "Cardiac / Acute Chest Pain" to Icons.Default.Favorite,
        "Severe Trauma / Bleeding" to Icons.Default.Warning,
        "Respiratory Distress / Asthma" to Icons.Default.Air,
        "Stroke / Neurological Collapse" to Icons.Default.Psychology,
        "Road Traffic Accident" to Icons.Default.LocalHospital,
        "General Medical Emergency" to Icons.Default.Emergency
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Live GPS Telemetry Status Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live GPS Telemetry Lock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        IconButton(onClick = onRefreshLocation, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TealPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        detectedCoords.formattedCoordinates,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        detectedCoords.locationName,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Satellite Accuracy: ±${detectedCoords.accuracyMeters.toInt()}m • Emergency Services API Ready",
                            fontSize = 10.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            // Triage Category Selector
            Text("SELECT EMERGENCY CONDITION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emergencyTypes.chunked(2).forEach { rowList ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowList.forEach { (type, icon) ->
                            val isSelected = selectedEmergencyType == type
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) EmergencyRed else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) EmergencyRed else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectType(type) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        type,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Pre-Set Emergency Responders Notification Preview
            Text(
                "PRE-SET CONTACTS TO BE NOTIFIED (${emergencyContacts.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (emergencyContacts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "⚠️ No saved contacts yet. Universal 102/112 Central EMS will still be immediately dispatched.",
                        fontSize = 11.sp,
                        color = WarningAmber,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emergencyContacts.take(3).forEach { contact ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (contact.isPrimary) EmergencyRedLight else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (contact.isPrimary) EmergencyRed.copy(alpha = 0.4f) else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(contact.name.split(" ").firstOrNull() ?: contact.name, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1)
                                Text(if (contact.isPrimary) "PRIMARY SOS" else contact.relationship, fontSize = 9.sp, color = if (contact.isPrimary) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            // Launch Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartSequence,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_sos_sequence_btn")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START SOS DISPATCH (5s Countdown)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                OutlinedButton(
                    onClick = onInstantBypass,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("instant_sos_bypass_btn")
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bypass Countdown & Dispatch Immediately", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Direct Call System at Bottom of SOS Dialog
                SOSBottomDirectCallSystem(
                    contacts = emergencyContacts,
                    onCall = onDirectCall
                )
            }
        }
    }
}

/**
 * Step 2: 5-Second Safety Abort Countdown View.
 */
@Composable
private fun EmergencyCountdownView(
    countdownRemaining: Int,
    selectedEmergencyType: String,
    onCancel: () -> Unit,
    onInstantDispatch: () -> Unit,
    onDirectCall: (phone: String, label: String) -> Unit = { _, _ -> }
) {
    val progress = (countdownRemaining / 5f).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "DISPATCHING EMERGENCY IN",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = EmergencyRed,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Giant Circular Countdown Gauge
        Box(
            modifier = Modifier.size(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                // Background Track
                drawCircle(
                    color = EmergencyRed.copy(alpha = 0.2f),
                    radius = (size.minDimension - strokeWidth) / 2,
                    style = Stroke(width = strokeWidth)
                )
                // Sweeping Progress Arc
                drawArc(
                    color = EmergencyRed,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$countdownRemaining",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    color = EmergencyRed
                )
                Text(
                    "SECONDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Condition: $selectedEmergencyType",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            "Live GPS coordinates and Medical ID payload will be broadcast to Central EMS (102) and your saved contacts.",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Large Cancel Button (Safety Abort)
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(52.dp)
                .testTag("abort_sos_countdown_btn")
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CANCEL / SAFE ABORT", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onInstantDispatch,
            modifier = Modifier.testTag("skip_countdown_btn")
        ) {
            Text("Skip Countdown & Dispatch Now", color = EmergencyRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Direct 1-Tap Emergency Calling while Countdown Runs
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("countdown_direct_call_system")
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "DIRECT CALL HOTLINES",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onDirectCall("102", "Ambulance EMS") },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("countdown_direct_call_102")
                    ) {
                        Text("102 EMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onDirectCall("100", "Nepal Police") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("countdown_direct_call_100")
                    ) {
                        Text("100 Police", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onDirectCall("112", "Disaster EMS") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("countdown_direct_call_112")
                    ) {
                        Text("112 Disaster", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Step 3: Multi-Stage Dispatch Terminal Simulation View.
 */
@Composable
private fun EmergencyDispatchingProgressView(
    dispatchStep: Int,
    dispatchLogs: List<String>,
    selectedEmergencyType: String,
    currentUserName: String,
    contactsCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = EmergencyRed,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    "TRANSMITTING EMERGENCY BEACON",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp,
                    color = EmergencyRed
                )
                Text(
                    "Establishing High-Priority Satellite & EMS Dispatch Uplink",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terminal Log Console
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dispatchLogs) { log ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(">", color = TealPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(log, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        val progress = (dispatchStep / 5f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = EmergencyRed,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Step 4: Active Emergency Control HUD & Live Pre-Set Contacts Sharing Console.
 */
@Composable
private fun ActiveEmergencyConsoleHUD(
    viewModel: LifscanViewModel,
    sosAlert: SOSAlertEntity?,
    emergencyContacts: List<EmergencyContactEntity>,
    detectedCoords: GeolocationHelper.GeoCoordinates,
    isAudioAlarmPlaying: Boolean,
    onToggleAudioAlarm: () -> Unit,
    onCopyLocationPin: () -> Unit,
    onCancelSOS: () -> Unit,
    onCallHotline: (String) -> Unit
) {
    val context = LocalContext.current
    var showCancelConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("active_emergency_hud_console"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Live Status Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFB71C1C),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD600))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("🚨 EMERGENCY DISPATCH ACTIVE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text("ALS Ambulance Unit #AMB-104 en route", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFD600)
                    ) {
                        Text("ETA ~4m", color = Color(0xFF7F0000), fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }

        item {
            // Live Location Sharing Card with 1-Tap Google Maps Link
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live GPS Location Broadcast", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = onCopyLocationPin,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(13.dp), tint = TealPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Maps Pin", fontSize = 10.5.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(detectedCoords.formattedCoordinates, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(detectedCoords.locationName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            // Pre-Set Emergency Contacts Broadcast Feed
            Text(
                "PRE-SET CONTACTS STATUS (${emergencyContacts.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                emergencyContacts.forEach { contact ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (contact.isPrimary) EmergencyRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (contact.isPrimary) EmergencyRed else TealPrimary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (contact.isPrimary) Icons.Default.Star else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        if (contact.isPrimary) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = EmergencyRedLight
                                            ) {
                                                Text("PRIMARY", color = EmergencyRed, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("SMS & Live Location Pin Transmitted", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // 1-Tap Call Contact
                            IconButton(
                                onClick = { onCallHotline(contact.phone) },
                                modifier = Modifier
                                    .background(SuccessGreen, CircleShape)
                                    .size(32.dp)
                                    .testTag("call_contact_${contact.id}")
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call ${contact.name}", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            // Paramedic & Emergency Hotlines Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("DIRECT EMERGENCY DIALERS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onCallHotline("102") },
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("102 EMS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onCallHotline("112") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LocalPolice, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("112 Univ.", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onCallHotline(sosAlert?.assignedDriverPhone ?: "+977-9801234567") },
                            colors = ButtonDefaults.buttonColors(containerColor = AmbulanceOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.AirportShuttle, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call Driver", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            // Interactive Emergency Tools: Siren Alarm & Live Route Map
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleAudioAlarm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isAudioAlarmPlaying) EmergencyRed else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isAudioAlarmPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isAudioAlarmPlaying) "Siren ON" else "Siren Sound", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.navigateTo(ScreenNav.AmbulanceTracker)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live Map", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Cancel Emergency Button
            Button(
                onClick = { showCancelConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("hud_cancel_sos_btn")
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("I Am Safe / Resolve Emergency", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resolve SOS Alert?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you safe? Resolving this alert will notify the ambulance driver, hospital desk, and pre-set emergency contacts that the emergency has ended.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        onCancelSOS()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Yes, I Am Safe", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirm = false }) {
                    Text("Keep Active")
                }
            }
        )
    }
}

/**
 * Direct Call System component placed at the bottom of the SOS dialog.
 * Offers instant 1-tap dialer connectivity to 102 (Ambulance), 100 (Police), 112 (Disaster),
 * and user's primary emergency contact.
 */
@Composable
fun SOSBottomDirectCallSystem(
    contacts: List<EmergencyContactEntity>,
    onCall: (phone: String, label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("sos_bottom_direct_call_system")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmergencyRed,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SOS DIRECT CALL SYSTEM",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp,
                            color = EmergencyRed,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Instant 1-Tap Operator & Emergency Line",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "READY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Speed-Dial Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 102 Ambulance
                Button(
                    onClick = { onCall("102", "Ambulance EMS") },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("sos_bottom_call_102")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AirportShuttle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("102", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("Ambulance", fontSize = 8.sp)
                    }
                }

                // 100 Police
                Button(
                    onClick = { onCall("100", "Nepal Police") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("sos_bottom_call_100")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalPolice, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("100", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("Police", fontSize = 8.sp)
                    }
                }

                // 112 Disaster
                Button(
                    onClick = { onCall("112", "National Disaster EMS") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("sos_bottom_call_112")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("112", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("Disaster", fontSize = 8.sp)
                    }
                }

                // Primary Contact
                val primaryContact = contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()
                val targetPhone = primaryContact?.phone ?: "+977-9841234567"
                val contactName = primaryContact?.name?.split(" ")?.firstOrNull() ?: "Contact"

                Button(
                    onClick = { onCall(targetPhone, contactName) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f).testTag("sos_bottom_call_contact")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(contactName, fontSize = 11.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("Primary", fontSize = 8.sp)
                    }
                }
            }
        }
    }
}
