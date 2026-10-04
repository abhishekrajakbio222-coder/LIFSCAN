package com.example.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessageEntity
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    otherUserName: String,
    otherUserId: String,
    otherRole: UserRole,
    viewModel: LifscanViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Pre-seeded conversation messages
    var localMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessageEntity(
                    id = "msg_init_1",
                    conversationId = conversationId,
                    senderId = otherUserId,
                    senderName = otherUserName,
                    senderRole = otherRole,
                    receiverId = currentUser?.id ?: "me",
                    message = "Namaste! I have received your health record and recent AI skin scan. How can I assist you with your treatment today?",
                    timestamp = System.currentTimeMillis() - 600000L
                ),
                ChatMessageEntity(
                    id = "msg_init_2",
                    conversationId = conversationId,
                    senderId = currentUser?.id ?: "me",
                    senderName = currentUser?.name ?: "Patient",
                    senderRole = currentUser?.role ?: UserRole.PATIENT,
                    receiverId = otherUserId,
                    message = "Hello Doctor, the AI scan detected Atopic Dermatitis with moderate itching on my forearm. Should I use topical Hydrocortisone?",
                    attachmentType = "SKIN_SCAN",
                    attachmentName = "Atopic Dermatitis AI Scan (94% Match)",
                    timestamp = System.currentTimeMillis() - 300000L
                )
            )
        )
    }

    LaunchedEffect(localMessages.size) {
        if (localMessages.isNotEmpty()) {
            listState.animateScrollToItem(localMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (otherRole == UserRole.DOCTOR) DoctorPurple else TealPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (otherRole == UserRole.DOCTOR) Icons.Default.MedicalServices else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(otherUserName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Encrypted Telehealth • Online", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentUser?.role == UserRole.DOCTOR) {
                            viewModel.navigateTo(ScreenNav.DoctorDashboard)
                        } else {
                            viewModel.navigateTo(ScreenNav.PatientDashboard)
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.showFeedback("Starting secure telehealth video call with $otherUserName...")
                    }) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Video Call", tint = TealPrimary)
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
            // Security badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SuccessGreenLight.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Messages and health scans are end-to-end encrypted.", fontSize = 10.sp, color = Color(0xFF065F46))
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(localMessages) { msg ->
                    val isMe = msg.senderId == (currentUser?.id ?: "me")
                    val alignment = if (isMe) Alignment.End else Alignment.Start
                    val bubbleColor = if (isMe) TealPrimary else if (isDarkMode) SurfaceVariantDark else Color(0xFFF1F5F9)
                    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 14.dp,
                                bottomStart = if (isMe) 14.dp else 2.dp,
                                bottomEnd = if (isMe) 2.dp else 14.dp
                            ),
                            color = bubbleColor,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (msg.attachmentType != "NONE") {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isMe) Color.White.copy(alpha = 0.2f) else SkinScannerPinkLight,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = if (isMe) Color.White else SkinScannerPink,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                msg.attachmentName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color.White else SkinScannerPink
                                            )
                                        }
                                    }
                                }

                                Text(msg.message, color = textColor, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }

            // Quick attachment buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = {
                        val newMsg = ChatMessageEntity(
                            id = "msg_${System.currentTimeMillis()}",
                            conversationId = conversationId,
                            senderId = currentUser?.id ?: "me",
                            senderName = currentUser?.name ?: "Me",
                            senderRole = currentUser?.role ?: UserRole.PATIENT,
                            receiverId = otherUserId,
                            message = "Here is my latest encrypted Atopic Dermatitis scan from today.",
                            attachmentType = "SKIN_SCAN",
                            attachmentName = "Eczema Lesion Scan #8841",
                            timestamp = System.currentTimeMillis()
                        )
                        localMessages = localMessages + newMsg
                    },
                    label = { Text("Attach Skin Scan", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )

                AssistChip(
                    onClick = {
                        val newMsg = ChatMessageEntity(
                            id = "msg_${System.currentTimeMillis()}",
                            conversationId = conversationId,
                            senderId = currentUser?.id ?: "me",
                            senderName = currentUser?.name ?: "Me",
                            senderRole = currentUser?.role ?: UserRole.PATIENT,
                            receiverId = otherUserId,
                            message = "My current vitals: BP 120/80 mmHg, Pulse 74 bpm, SpO2 99%.",
                            attachmentType = "REPORT",
                            attachmentName = "Live Vitals Summary",
                            timestamp = System.currentTimeMillis()
                        )
                        localMessages = localMessages + newMsg
                    },
                    label = { Text("Share Vitals", fontSize = 10.sp) },
                    leadingIcon = { Icon(Icons.Default.MonitorHeart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            // Input Bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Type encrypted message...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val text = messageInput
                                messageInput = ""
                                val newMsg = ChatMessageEntity(
                                    id = "msg_${System.currentTimeMillis()}",
                                    conversationId = conversationId,
                                    senderId = currentUser?.id ?: "me",
                                    senderName = currentUser?.name ?: "Me",
                                    senderRole = currentUser?.role ?: UserRole.PATIENT,
                                    receiverId = otherUserId,
                                    message = text,
                                    timestamp = System.currentTimeMillis()
                                )
                                localMessages = localMessages + newMsg
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(TealPrimary, CircleShape)
                            .testTag("chat_send_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
