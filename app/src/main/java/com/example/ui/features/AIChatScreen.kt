package com.example.ui.features

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GeminiHealthService
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import com.example.util.AudioRecorderHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(viewModel: LifscanViewModel) {
    val context = LocalContext.current
    val chatMessages by viewModel.aiChatMessages.collectAsState()
    val isLoading by viewModel.aiChatLoading.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val selectedModel by viewModel.selectedChatModel.collectAsState()
    val selectedRole by viewModel.selectedChatRole.collectAsState()
    val isMapsGrounding by viewModel.isGoogleMapsGrounding.collectAsState()
    val isSearchGrounding by viewModel.isGoogleSearchGrounding.collectAsState()
    val isTranscribing by viewModel.isTranscribingAudio.collectAsState()

    var inputQuery by remember { mutableStateOf("") }
    var isRecordingAudio by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val audioRecorder = remember { AudioRecorderHelper(context) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = audioRecorder.startRecording()
            if (started) {
                isRecordingAudio = true
                Toast.makeText(context, "🎤 Recording audio... Tap mic again to stop & transcribe", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for audio input", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Mic Pulsating Animation
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val quickQuestions = listOf(
        "📍 Nearest 24/7 emergency hospital?",
        "💊 Check Amlodipine & Atorvastatin interaction",
        "🩸 What to do for severe arterial bleeding?",
        "🔬 How to interpret CBC blood report results?",
        "🌐 Latest clinical updates on Dengue fever in Nepal"
    )

    val systemRoles = listOf(
        "Clinical Triage Specialist" to Icons.Default.MedicalServices,
        "Pharmacology & Meds" to Icons.Default.Medication,
        "Emergency Protocol" to Icons.Default.Warning,
        "Preventive Coach" to Icons.Default.Favorite
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = TealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = "AI Chatbot",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Lifscan AI Assistant", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (selectedModel == GeminiHealthService.GeminiChatModel.COMPLEX_REASONING) Color(0xFF673AB7).copy(alpha = 0.2f) else TealPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        selectedModel.displayName,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedModel == GeminiHealthService.GeminiChatModel.COMPLEX_REASONING) Color(0xFF673AB7) else TealPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "Role: $selectedRole",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAIChatHistory() },
                        modifier = Modifier.testTag("chat_clear_history_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. GEMINI MODEL & GROUNDING CONTROL TOOLBAR
            Surface(
                color = if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    // Row A: Model Selector Chips (Pro, Flash, Flash-Lite)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "MODEL:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(GeminiHealthService.GeminiChatModel.values()) { model ->
                                val isSelected = selectedModel == model
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setChatModel(model) },
                                    label = {
                                        Text(
                                            model.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            when (model) {
                                                GeminiHealthService.GeminiChatModel.COMPLEX_REASONING -> Icons.Default.Psychology
                                                GeminiHealthService.GeminiChatModel.GENERAL_HEALTH -> Icons.Default.AutoAwesome
                                                GeminiHealthService.GeminiChatModel.FAST_TRIAGE -> Icons.Default.Bolt
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealPrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = TealPrimary,
                                        selectedLeadingIconColor = TealPrimary
                                    ),
                                    modifier = Modifier.testTag("model_chip_${model.name}")
                                )
                            }
                        }
                    }

                    // Row B: Grounding Tools & System Roles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Google Maps Grounding Toggle
                        FilterChip(
                            selected = isMapsGrounding,
                            onClick = { viewModel.toggleGoogleMapsGrounding() },
                            label = { Text("📍 Maps Data", fontSize = 10.5.sp, fontWeight = if (isMapsGrounding) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0288D1).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFF0288D1),
                                selectedLeadingIconColor = Color(0xFF0288D1)
                            ),
                            modifier = Modifier.testTag("maps_grounding_toggle")
                        )

                        // Google Search Grounding Toggle
                        FilterChip(
                            selected = isSearchGrounding,
                            onClick = { viewModel.toggleGoogleSearchGrounding() },
                            label = { Text("🌐 Search Data", fontSize = 10.5.sp, fontWeight = if (isSearchGrounding) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF388E3C).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFF388E3C),
                                selectedLeadingIconColor = Color(0xFF388E3C)
                            ),
                            modifier = Modifier.testTag("search_grounding_toggle")
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Role Selector Menu Dropdown
                        var showRoleMenu by remember { mutableStateOf(false) }
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clickable { showRoleMenu = true }
                                    .testTag("system_role_dropdown_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(13.dp), tint = TealPrimary)
                                    Text("Role", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                systemRoles.forEach { (roleName, roleIcon) ->
                                    DropdownMenuItem(
                                        text = { Text(roleName, fontSize = 12.sp, fontWeight = if (selectedRole == roleName) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(roleIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealPrimary) },
                                        onClick = {
                                            viewModel.setChatRole(roleName)
                                            showRoleMenu = false
                                        },
                                        modifier = Modifier.testTag("role_option_${roleName.replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. SCROLLABLE MULTI-TURN CONVERSATION THREAD
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("chat_messages_scrollable_thread"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(chatMessages) { (isUser, text) ->
                    val alignment = if (isUser) Alignment.End else Alignment.Start
                    val bubbleColor = if (isUser) {
                        TealPrimary
                    } else {
                        if (isDarkMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9)
                    }
                    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            color = bubbleColor,
                            shadowElevation = if (isUser) 2.dp else 1.dp,
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .testTag(if (isUser) "user_chat_bubble" else "ai_chat_bubble")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (!isUser) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = TealPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Lifscan AI ($selectedRole)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                    }
                                }

                                Text(
                                    text = text,
                                    color = textColor,
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(8.dp)
                                .testTag("chat_loading_indicator")
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TealPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (isMapsGrounding && isSearchGrounding) "Querying Google Maps & Google Search..."
                                else if (isMapsGrounding) "Grounding with Google Maps..."
                                else if (isSearchGrounding) "Grounding with Google Search..."
                                else "Gemini is analyzing medical context...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. QUICK SUGGESTED QUESTIONS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickQuestions) { question ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable {
                            inputQuery = question
                        }
                    ) {
                        Text(
                            question,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. MICROPHONE RECORDING & INPUT CONTROLLER
            AnimatedVisibility(visible = isRecordingAudio) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .scale(micPulse)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Text(
                                "Recording audio for gemini-3.5-transcribe...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Button(
                            onClick = {
                                val audioBytes = audioRecorder.stopRecording()
                                isRecordingAudio = false
                                if (audioBytes != null && audioBytes.isNotEmpty()) {
                                    viewModel.transcribeAudio(audioBytes) { transcribed ->
                                        inputQuery = transcribed
                                    }
                                } else {
                                    // Fallback sample clinical recording for testing/emulators
                                    val sampleBytes = "SAMPLE_CLINICAL_VOICE_NOTE".toByteArray()
                                    viewModel.transcribeAudio(sampleBytes) { transcribed ->
                                        inputQuery = transcribed
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("stop_recording_button")
                        ) {
                            Text("DONE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Transcribing indicator
            AnimatedVisibility(visible = isTranscribing) {
                Surface(
                    color = TealPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = TealPrimary)
                        Text(
                            "Transcribing audio with model gemini-3.5-transcribe...",
                            fontSize = 11.5.sp,
                            color = TealPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 5. INPUT TEXT FIELD WITH MICROPHONE & SEND BUTTONS
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Microphone input button (gemini-3.5-transcribe)
                    IconButton(
                        onClick = {
                            if (isRecordingAudio) {
                                val audioBytes = audioRecorder.stopRecording()
                                isRecordingAudio = false
                                if (audioBytes != null && audioBytes.isNotEmpty()) {
                                    viewModel.transcribeAudio(audioBytes) { transcribed ->
                                        inputQuery = transcribed
                                    }
                                }
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    val started = audioRecorder.startRecording()
                                    if (started) {
                                        isRecordingAudio = true
                                    } else {
                                        // Demo transcription fallback for virtual devices
                                        val sampleBytes = "SAMPLE_VOICE_NOTE".toByteArray()
                                        viewModel.transcribeAudio(sampleBytes) { transcribed ->
                                            inputQuery = transcribed
                                        }
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isRecordingAudio) Color(0xFFEF4444).copy(alpha = 0.2f) else TealPrimary.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .testTag("microphone_input_button")
                    ) {
                        Icon(
                            if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Microphone Transcription",
                            tint = if (isRecordingAudio) Color(0xFFEF4444) else TealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        placeholder = {
                            Text(
                                if (isMapsGrounding) "Ask with Google Maps..."
                                else if (isSearchGrounding) "Ask with Google Search..."
                                else "Ask symptoms, meds, or hospital...",
                                fontSize = 13.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_textfield"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputQuery.isNotBlank()) {
                                val q = inputQuery.trim()
                                inputQuery = ""
                                viewModel.sendAIChat(q)
                            }
                        },
                        enabled = inputQuery.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (inputQuery.isNotBlank() && !isLoading) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (inputQuery.isNotBlank() && !isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
