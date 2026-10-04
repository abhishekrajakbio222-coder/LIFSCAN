package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VaultAuditLogEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(viewModel: LifscanViewModel) {
    val auditLogs by viewModel.vaultAuditLogs.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "ALL" to "All Events",
        "ADMIN" to "Admin & Facilities",
        "BIOMETRIC" to "Biometrics",
        "PDF" to "PDF Exports",
        "CLOUD" to "Cloud Sync",
        "EMERGENCY" to "Emergency SOS",
        "VIEW" to "Record Views"
    )

    val filteredLogs = remember(auditLogs, selectedFilter) {
        if (selectedFilter == "ALL") auditLogs
        else {
            auditLogs.filter { log ->
                when (selectedFilter) {
                    "ADMIN" -> log.accessType.contains("ADMIN", ignoreCase = true) || log.accessType.contains("FACILITY", ignoreCase = true) || log.accessType.contains("BED", ignoreCase = true)
                    "BIOMETRIC" -> log.accessType.contains("BIOMETRIC", ignoreCase = true)
                    "PDF" -> log.accessType.contains("PDF", ignoreCase = true)
                    "CLOUD" -> log.accessType.contains("CLOUD", ignoreCase = true)
                    "EMERGENCY" -> log.accessType.contains("EMERGENCY", ignoreCase = true)
                    "VIEW" -> log.accessType.contains("VIEW", ignoreCase = true) || log.accessType.contains("RECORD", ignoreCase = true)
                    else -> true
                }
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
                            color = TealPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SecurityUpdateGood,
                                    contentDescription = "Audit Logs",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Vault Security Audit Log",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Immutable Hardware-Verified Access Logs",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.MedicalVault) },
                        modifier = Modifier.testTag("audit_log_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle_audit")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_audit_logs_btn")
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // Security Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceVariantDark else TealPrimary.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "HIPAA & GDPR Access Accountability",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Every unlock, view, PDF report generation, emergency contact modification, and cloud sync event is timestamped and cryptographically logged to protect patient data sovereignty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("TOTAL EVENTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${auditLogs.size} Logged", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ENCRYPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("AES-256 GCM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("SECURE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InfoBlue)
                                }
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.take(3).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterOptions.drop(3).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No audit records matching this filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    AuditLogItemCard(log = log, isDarkMode = isDarkMode)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = EmergencyRed) },
            title = { Text("Clear Audit Logs?") },
            text = {
                Text("This will purge all local access logs. Future access events will continue to be recorded automatically.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearVaultAuditLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AuditLogItemCard(log: VaultAuditLogEntity, isDarkMode: Boolean) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault()) }
    val formattedDate = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    val (badgeColor, badgeIcon) = when {
        log.accessType.contains("BIOMETRIC") -> TealPrimary to Icons.Default.Fingerprint
        log.accessType.contains("PDF") -> InfoBlue to Icons.Default.PictureAsPdf
        log.accessType.contains("CLOUD") -> WarningAmber to Icons.Default.CloudSync
        log.accessType.contains("EMERGENCY") -> EmergencyRed to Icons.Default.Emergency
        log.accessType.contains("VIEW") -> SuccessGreen to Icons.Default.Visibility
        else -> MaterialTheme.colorScheme.primary to Icons.Default.Shield
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        log.accessType.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = badgeColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (log.status == "AUTHORIZED") SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)
                ) {
                    Text(
                        log.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.status == "AUTHORIZED") SuccessGreen else WarningAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                log.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (log.recordTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Target: ${log.recordTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    log.authenticatedUser,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
