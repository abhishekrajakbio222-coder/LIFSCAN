package com.example.ui.components

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Floating Feedback & Issue Reporting Button.
 * Allows users and clinicians to submit suggestions, bug reports,
 * or UI feedback directly to the engineering team.
 * Movable / draggable across the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingFeedbackButton(
    viewModel: LifscanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Draggable position state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Feedback Form State
    var feedbackType by remember { mutableStateOf("💡 Suggestion / Idea") }
    var selectedCategory by remember { mutableStateOf("AI Diagnostics") }
    var rating by remember { mutableIntStateOf(5) }
    var feedbackTitle by remember { mutableStateOf("") }
    var feedbackDescription by remember { mutableStateOf("") }
    var includeTelemetryLogs by remember { mutableStateOf(true) }

    val feedbackTypes = listOf(
        "💡 Suggestion / Idea",
        "🐛 Bug / Issue Report",
        "🩺 Clinical Content",
        "⭐ Experience"
    )

    val categoryTags = listOf(
        "AI Diagnostics",
        "CameraX Scanner",
        "AI Chat Bot",
        "Telehealth",
        "SOS & Maps",
        "UI & UX"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("floating_feedback_container")
    ) {
        // Floating Action Button
        Surface(
            shape = CircleShape,
            color = TealPrimary,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(48.dp)
                .border(2.dp, Color.White, CircleShape)
                .clickable { showFeedbackDialog = true }
                .testTag("floating_feedback_fab")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Feedback,
                    contentDescription = "Send Feedback or Report Issue (Drag to Move)",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Feedback & Bug Reporting Dialog
    if (showFeedbackDialog) {
        Dialog(
            onDismissRequest = {
                if (!isSubmitting) showFeedbackDialog = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f)
                    .padding(12.dp)
                    .testTag("feedback_reporting_modal")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = TealPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.RateReview,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Dev Team Feedback",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Help us improve Lifscan",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { showFeedbackDialog = false },
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable form content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Feedback Type Selector
                        Text(
                            "FEEDBACK TYPE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            feedbackTypes.take(2).forEach { type ->
                                val isSelected = feedbackType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { feedbackType = type },
                                    label = { Text(type, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            feedbackTypes.drop(2).forEach { type ->
                                val isSelected = feedbackType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { feedbackType = type },
                                    label = { Text(type, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Star Rating
                        Text(
                            "OVERALL EXPERIENCE RATING",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { rating = star },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "$star Stars",
                                        tint = if (star <= rating) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // Category Chips
                        Text(
                            "MODULE / AREA",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoryTags.take(3).forEach { cat ->
                                val isSelected = selectedCategory == cat
                                AssistChip(
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 10.5.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        labelColor = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // Summary / Title Input
                        OutlinedTextField(
                            value = feedbackTitle,
                            onValueChange = { feedbackTitle = it },
                            label = { Text("Short Summary / Title") },
                            placeholder = { Text("e.g., Camera tap-to-focus suggestion, or Vitals chart zoom...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_title_input")
                        )

                        // Detailed Feedback / Issue Text
                        OutlinedTextField(
                            value = feedbackDescription,
                            onValueChange = { feedbackDescription = it },
                            label = { Text("Detailed Description / Steps to Reproduce") },
                            placeholder = { Text("Describe what happened, what you expected, or how we can improve...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("feedback_desc_input")
                        )

                        // Diagnostic Logs Attachment Switch
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Attach Diagnostic Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Includes device model (${Build.MODEL}), OS ${Build.VERSION.RELEASE}, and build logs.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = includeTelemetryLogs,
                                    onCheckedChange = { includeTelemetryLogs = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSubmitting = true
                                val title = feedbackTitle.ifBlank { "User Feedback: $feedbackType" }
                                val desc = feedbackDescription.ifBlank { "Rating: $rating/5 on $selectedCategory." }

                                // Record audit log for developer dispatch
                                viewModel.recordVaultAuditLog(
                                    accessType = "USER_FEEDBACK_SUBMITTED",
                                    description = "[$feedbackType] ($selectedCategory) - $title: $desc. Rating: $rating/5. Device: ${Build.MODEL}",
                                    recordTitle = "Developer Team Feedback"
                                )

                                delay(800)
                                isSubmitting = false
                                showFeedbackDialog = false
                                viewModel.showFeedback("🎉 Feedback sent directly to the dev team! Thank you for helping us improve.")
                                Toast.makeText(context, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_feedback_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending to Engineering Team...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send to Dev Team", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
