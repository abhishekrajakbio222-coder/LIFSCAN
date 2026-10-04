package com.example.ui.features

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.MedicationEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScheduleScreen(viewModel: LifscanViewModel) {
    val context = LocalContext.current
    val medications by viewModel.patientMedications.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilterTimeSlot by remember { mutableStateOf("ALL") }

    // Check notification permission
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val totalMeds = medications.size
    val takenMeds = medications.count { it.isTakenToday }
    val adherencePercent = if (totalMeds > 0) ((takenMeds.toFloat() / totalMeds) * 100).toInt() else 100
    val maxStreak = medications.maxOfOrNull { it.streakDays } ?: 0

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
                                    Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Medication Schedule",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Encrypted Daily Prescriptions & Alerts",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("medication_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.triggerHealthWorkManagerCheck()
                            viewModel.sendTestPushNotification(
                                context,
                                "Cetirizine 10mg",
                                "Night Dose"
                            )
                        },
                        modifier = Modifier.testTag("med_test_push_btn")
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Test Notification",
                            tint = TealPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Medication") },
                containerColor = TealPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_medication_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            // Notification Permission Banner
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AmberWarningLight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsPaused, contentDescription = null, tint = AmberWarning)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Enable Dose Push Notifications",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    "Allow Lifscan to alert you when it is time to take your medicine.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F)
                                )
                            }
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Allow", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Adherence & Streak Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceDarkElevated else Color(0xFFF0FDF4)
                    ),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val todayStr = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                                Text("TODAY'S SCHEDULE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                Text(todayStr, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$maxStreak Day Streak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Daily Adherence", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$takenMeds of $totalMeds taken ($adherencePercent%)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (totalMeds > 0) takenMeds.toFloat() / totalMeds else 1f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SuccessGreen,
                            trackColor = SuccessGreen.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            // Time Slot Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val timeSlots = listOf("ALL" to "All Doses", "Morning" to "Morning", "Afternoon" to "Afternoon", "Night" to "Night")
                    timeSlots.forEach { (key, label) ->
                        val isSelected = selectedFilterTimeSlot == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterTimeSlot = key },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Medication List
            val filteredMeds = medications.filter {
                if (selectedFilterTimeSlot == "ALL") true else it.timeSlot.contains(selectedFilterTimeSlot, ignoreCase = true)
            }

            if (filteredMeds.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Medication, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Medications Scheduled", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Tap 'Add Medication' to schedule your prescribed medicine with push alerts.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                items(filteredMeds, key = { it.id }) { med ->
                    MedicationCard(
                        medication = med,
                        isDarkMode = isDarkMode,
                        onToggleTaken = { isTaken ->
                            viewModel.toggleMedicationTaken(med.id, isTaken)
                        },
                        onToggleReminder = { enabled ->
                            viewModel.toggleMedicationReminder(med.id, enabled)
                        },
                        onTriggerPush = {
                            viewModel.triggerMedicationReminderNotification(context, med)
                        },
                        onDelete = {
                            viewModel.deleteMedication(med.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, dosage, frequency, timeSlot, hour, min, doctor, instructions, totalDoses ->
                viewModel.addMedication(
                    name = name,
                    dosage = dosage,
                    frequency = frequency,
                    timeSlot = timeSlot,
                    scheduledHour = hour,
                    scheduledMinute = min,
                    doctorName = doctor,
                    instructions = instructions,
                    totalDoses = totalDoses
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MedicationCard(
    medication: MedicationEntity,
    isDarkMode: Boolean,
    onToggleTaken: (Boolean) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onTriggerPush: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("med_card_${medication.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) SurfaceDarkElevated else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (medication.isTakenToday) SuccessGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox / Taken Circle
                IconButton(
                    onClick = { onToggleTaken(!medication.isTakenToday) },
                    modifier = Modifier.testTag("med_check_${medication.id}")
                ) {
                    if (medication.isTakenToday) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Taken",
                            tint = SuccessGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Pending",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            medication.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textDecoration = if (medication.isTakenToday) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (medication.isTakenToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TealPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                medication.dosage,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${medication.timeSlot} • ${medication.frequency}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Push Notification Quick Trigger
                IconButton(
                    onClick = onTriggerPush,
                    modifier = Modifier.size(32.dp).testTag("med_push_test_${medication.id}")
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Trigger Push",
                        tint = if (medication.isReminderEnabled) TealPrimary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("med_delete_${medication.id}")
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instructions & Prescriber Doctor
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💡 ${medication.instructions}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Prescribed: ${medication.doctorName.take(16)}",
                        fontSize = 10.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, dosage: String, frequency: String, timeSlot: String, hour: Int, min: Int, doctor: String, instructions: String, totalDoses: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }
    var timeSlot by remember { mutableStateOf("Morning (08:00 AM)") }
    var doctor by remember { mutableStateOf("Dr. Sandeep Adhikari") }
    var instructions by remember { mutableStateOf("Take after meal with water") }
    var totalDoses by remember { mutableStateOf("14") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Medication, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule Medication", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name *") },
                    placeholder = { Text("e.g. Amoxicillin, Cetirizine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_med_name")
                )

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage *") },
                    placeholder = { Text("e.g. 500 mg, 1 tablet") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_med_dosage")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalDoses,
                        onValueChange = { totalDoses = it },
                        label = { Text("Total Doses") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = timeSlot,
                    onValueChange = { timeSlot = it },
                    label = { Text("Time Schedule") },
                    placeholder = { Text("Morning (08:00 AM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    placeholder = { Text("Take with water after food") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = doctor,
                    onValueChange = { doctor = it },
                    label = { Text("Prescribing Doctor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val doses = totalDoses.toIntOrNull() ?: 14
                        onAdd(name, dosage.ifBlank { "1 dose" }, frequency, timeSlot, 8, 0, doctor, instructions, doses)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("save_med_btn")
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
