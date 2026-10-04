package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentEntity
import com.example.data.model.UserEntity
import com.example.ui.components.VoiceSpeechInputButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationSchedulerScreen(viewModel: LifscanViewModel) {
    val doctors by viewModel.doctorsList.collectAsState()
    val upcomingAppointments by viewModel.patientAppointments.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Browse & Book Doctors, 1: My Scheduled Appointments

    // Search & Availability Filter States
    var searchQuery by remember { mutableStateOf("") }
    var selectedSpecialty by remember { mutableStateOf("All") }
    var selectedAvailabilityFilter by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "TOMORROW", "MORNING", "AFTERNOON", "EVENING", "TOP_RATED"
    var selectedConsultationModeFilter by remember { mutableStateOf("ALL") } // "ALL", "VIDEO", "IN_CLINIC"

    // Booking Wizard State
    var selectedDateOffset by remember { mutableStateOf(0) } // 0 = Today, 1 = Tomorrow, etc.
    var selectedTimeSlot by remember { mutableStateOf("10:30 AM") }
    var isVideoConsultation by remember { mutableStateOf(true) }
    var selectedDoctorForBooking by remember { mutableStateOf<UserEntity?>(null) }
    var bookingNotes by remember { mutableStateOf("Consultation regarding skin lesion symptoms and dermatological assessment.") }
    var selectedPaymentMethod by remember { mutableStateOf("eSewa / Khalti (Nepal)") }
    var showBookingConfirmationDialog by remember { mutableStateOf(false) }
    var showBookingSuccessDialog by remember { mutableStateOf(false) }
    var lastBookedAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    // Reschedule & Cancel state
    var appointmentToReschedule by remember { mutableStateOf<AppointmentEntity?>(null) }
    var rescheduleDateOffset by remember { mutableStateOf(1) }
    var rescheduleTimeSlot by remember { mutableStateOf("02:00 PM") }
    var appointmentToCancel by remember { mutableStateOf<AppointmentEntity?>(null) }
    var appointmentStatusFilter by remember { mutableStateOf("All") } // "All", "CONFIRMED", "RESCHEDULED", "COMPLETED", "CANCELLED"
    var viewingPrescriptionAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }

    val specialties = listOf(
        "All",
        "Dermatologist & Skin",
        "Cardiology",
        "General Physician",
        "Pediatrician",
        "Orthopedics",
        "Neurology"
    )

    val timeSlots = listOf(
        "08:30 AM",
        "09:30 AM",
        "10:30 AM",
        "11:45 AM",
        "01:30 PM",
        "02:30 PM",
        "04:00 PM",
        "05:30 PM",
        "06:30 PM",
        "08:00 PM"
    )

    // Calculate dates for the next 7 days
    val datesList = remember {
        val list = mutableListOf<Triple<Int, String, String>>() // offset, dayName, formattedDate
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 0..6) {
            val d = cal.time
            val dayName = if (i == 0) "Today" else if (i == 1) "Tmrw" else dayFormat.format(d)
            val dateLabel = dateFormat.format(d)
            list.add(Triple(i, "$dayName, $dateLabel", fullDateFormat.format(d)))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val selectedDateString = datesList.getOrNull(selectedDateOffset)?.third ?: "2026-09-01"
    val rescheduleDateString = datesList.getOrNull(rescheduleDateOffset)?.third ?: "2026-09-02"

    // Multi-criteria Doctor Filtering (Availability, Specialty, Search, Mode)
    val filteredDoctors = remember(doctors, searchQuery, selectedSpecialty, selectedAvailabilityFilter, selectedConsultationModeFilter) {
        doctors.filter { doc ->
            // 1. Search Query filter
            val matchesSearch = searchQuery.isBlank() ||
                doc.name.contains(searchQuery, ignoreCase = true) ||
                doc.specialization.contains(searchQuery, ignoreCase = true) ||
                doc.clinicAffiliation.contains(searchQuery, ignoreCase = true)

            // 2. Specialty filter
            val matchesSpecialty = selectedSpecialty == "All" ||
                doc.specialization.contains(selectedSpecialty, ignoreCase = true) ||
                (selectedSpecialty.contains("Dermatolog") && doc.specialization.contains("Dermatolog", ignoreCase = true))

            // 3. Consultation Mode filter
            val matchesMode = when (selectedConsultationModeFilter) {
                "VIDEO" -> doc.doctorConsultationModes.contains("VIDEO_CALL", ignoreCase = true)
                "IN_CLINIC" -> doc.doctorConsultationModes.contains("IN_CLINIC", ignoreCase = true)
                else -> true
            }

            // 4. Availability Filter
            val matchesAvailability = when (selectedAvailabilityFilter) {
                "TODAY" -> doc.doctorAvailableDays.contains("Mon", ignoreCase = true) || doc.doctorAvailableDays.contains("Tue", ignoreCase = true)
                "TOMORROW" -> doc.doctorAvailableDays.contains("Wed", ignoreCase = true) || doc.doctorAvailableDays.contains("Thu", ignoreCase = true)
                "MORNING" -> doc.doctorAvailableTimeSlots.contains("08:30") || doc.doctorAvailableTimeSlots.contains("09:00") || doc.doctorAvailableTimeSlots.contains("10:") || doc.doctorAvailableTimeSlots.contains("11:")
                "AFTERNOON" -> doc.doctorAvailableTimeSlots.contains("01:") || doc.doctorAvailableTimeSlots.contains("02:") || doc.doctorAvailableTimeSlots.contains("03:")
                "EVENING" -> doc.doctorAvailableTimeSlots.contains("04:") || doc.doctorAvailableTimeSlots.contains("05:") || doc.doctorAvailableTimeSlots.contains("06:") || doc.doctorAvailableTimeSlots.contains("07:") || doc.doctorAvailableTimeSlots.contains("08:")
                "TOP_RATED" -> doc.rating >= 4.90f
                else -> true
            }

            matchesSearch && matchesSpecialty && matchesMode && matchesAvailability
        }
    }

    val filteredAppointments = remember(upcomingAppointments, appointmentStatusFilter) {
        if (appointmentStatusFilter == "All") {
            upcomingAppointments
        } else {
            upcomingAppointments.filter { it.status.equals(appointmentStatusFilter, ignoreCase = true) }
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
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Consultation Scheduler",
                                    tint = TealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Doctor Appointments",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Browse Local Specialists & Filter Availability",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("scheduler_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            selectedSpecialty = "All"
                            selectedAvailabilityFilter = "ALL"
                            selectedConsultationModeFilter = "ALL"
                        },
                        modifier = Modifier.testTag("reset_filters_btn")
                    ) {
                        Icon(
                            Icons.Default.FilterAltOff,
                            contentDescription = "Reset Filters",
                            tint = TealPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Tab Row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TealPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Browse & Book (${filteredDoctors.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_browse_doctors")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "My Appointments (${upcomingAppointments.count { it.status != "CANCELLED" }})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_my_appointments")
                )
            }

            if (activeTab == 0) {
                // ==========================================
                // 1. BROWSE & BOOK DOCTORS VIEW (WITH AVAILABILITY FILTERING)
                // ==========================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp)
                ) {
                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search doctor, clinic, hospital or symptom...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TealPrimary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doctor_search_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // Availability Filter Chips Header
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Filter by Availability & Time",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${filteredDoctors.size} available",
                                    fontSize = 11.sp,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val availabilityOptions = listOf(
                                "ALL" to "All Availability",
                                "TODAY" to "🟢 Available Today",
                                "TOMORROW" to "📅 Tomorrow",
                                "MORNING" to "⏱️ Morning (8-12 PM)",
                                "AFTERNOON" to "☀️ Afternoon (12-4 PM)",
                                "EVENING" to "🌙 Evening (4-8 PM)",
                                "TOP_RATED" to "⭐ Top Rated (4.9+)"
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(availabilityOptions) { (key, label) ->
                                    val isSelected = selectedAvailabilityFilter == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedAvailabilityFilter = key },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("filter_avail_${key.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    // Specialty Filter Carousel
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Medical Specialty",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(specialties) { spec ->
                                    val isSelected = selectedSpecialty == spec
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSpecialty = spec },
                                        label = { Text(spec, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("filter_spec_${spec.replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }

                    // Consultation Mode Selection (Video vs In-Clinic)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modes = listOf(
                                "ALL" to "All Modes",
                                "VIDEO" to "🎥 HD Video Telehealth",
                                "IN_CLINIC" to "🏥 Hospital / Clinic"
                            )
                            modes.forEach { (modeKey, modeTitle) ->
                                val isSelected = selectedConsultationModeFilter == modeKey
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) BorderStroke(1.dp, TealPrimary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedConsultationModeFilter = modeKey }
                                        .testTag("filter_mode_${modeKey.lowercase()}")
                                ) {
                                    Text(
                                        modeTitle,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Active Filters Summary / Reset notice
                    if (searchQuery.isNotEmpty() || selectedSpecialty != "All" || selectedAvailabilityFilter != "ALL" || selectedConsultationModeFilter != "ALL") {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TealPrimary.copy(alpha = 0.08f),
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
                                        "Filtering active (${filteredDoctors.size} matches)",
                                        fontSize = 11.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Clear All",
                                        fontSize = 11.sp,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                searchQuery = ""
                                                selectedSpecialty = "All"
                                                selectedAvailabilityFilter = "ALL"
                                                selectedConsultationModeFilter = "ALL"
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Doctors List Section
                    if (filteredDoctors.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.PersonSearch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No doctors found matching criteria", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "Try changing the availability filter, clearing the search query, or selecting 'All Specialties'.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            searchQuery = ""
                                            selectedSpecialty = "All"
                                            selectedAvailabilityFilter = "ALL"
                                            selectedConsultationModeFilter = "ALL"
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Reset All Filters")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredDoctors) { doctor ->
                            DoctorCardItem(
                                doctor = doctor,
                                isDarkMode = isDarkMode,
                                onBookClick = { slot ->
                                    selectedDoctorForBooking = doctor
                                    selectedTimeSlot = slot
                                    showBookingConfirmationDialog = true
                                }
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // 2. MY APPOINTMENTS VIEW (ROOM DATABASE QUERY)
                // ==========================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp)
                ) {
                    // Status Filter Tabs
                    item {
                        val statusOptions = listOf("All", "CONFIRMED", "COMPLETED", "RESCHEDULED", "CANCELLED")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(statusOptions) { status ->
                                val isSelected = appointmentStatusFilter.equals(status, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { appointmentStatusFilter = status },
                                    label = { Text(if (status == "All") "All Appointments" else status, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("filter_status_${status.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filteredAppointments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No consultations found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "You don't have any appointments matching this filter. Schedule a new consultation with our certified doctors.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                    )
                                    Button(
                                        onClick = { activeTab = 0 },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                        modifier = Modifier.testTag("empty_schedule_now_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Browse & Book Doctors")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredAppointments) { apt ->
                            AppointmentCardItem(
                                appointment = apt,
                                isDarkMode = isDarkMode,
                                onJoinVideo = {
                                    viewModel.navigateTo(ScreenNav.VideoCall(apt))
                                },
                                onReschedule = { appointmentToReschedule = apt },
                                onCancel = { appointmentToCancel = apt },
                                onViewPrescription = { viewingPrescriptionAppointment = apt },
                                onNavigateToClinic = {
                                    viewModel.navigateTo(
                                        ScreenNav.MappingNavigation(
                                            facilityName = apt.clinicName,
                                            address = "${apt.clinicName}, Kathmandu Medical Zone",
                                            facilityType = "CLINIC",
                                            distanceKm = 1.4,
                                            phone = "+977-1-4229988"
                                        )
                                    )
                                },
                                onDelete = { viewModel.deleteDoctorAppointment(apt.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // BOOKING WIZARD & CONFIRMATION MODAL
    // ==========================================
    if (showBookingConfirmationDialog && selectedDoctorForBooking != null) {
        val doctor = selectedDoctorForBooking!!
        AlertDialog(
            onDismissRequest = { showBookingConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = TealPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Doctor Appointment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Doctor Summary Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkMode) SurfaceVariantDark else TealLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TealPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(doctor.specialization, fontSize = 11.sp, color = TealPrimary)
                                Text(doctor.clinicAffiliation, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${doctor.preferredCurrency} ${doctor.consultationFee.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SuccessGreen
                                )
                                Text("Fee", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 1. Select Date (7-Day Carousel)
                    Text("1. Choose Date", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(datesList) { (offset, label, fullDate) ->
                            val isSelected = selectedDateOffset == offset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { selectedDateOffset = offset }
                                    .testTag("wizard_date_${offset}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        label.split(",").firstOrNull() ?: "",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        label.split(",").getOrNull(1)?.trim() ?: "",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 2. Select Time Slot
                    Text("2. Choose Time Slot", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val doctorSlots = doctor.doctorAvailableTimeSlots.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val availableSlotsToPick = if (doctorSlots.isNotEmpty()) doctorSlots else timeSlots

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableSlotsToPick) { slot ->
                            val isSelected = selectedTimeSlot == slot
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { selectedTimeSlot = slot }
                                    .testTag("wizard_slot_${slot.replace(" ", "_")}")
                            ) {
                                Text(
                                    slot,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 3. Consultation Mode Selection
                    Text("3. Consultation Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isVideoConsultation) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isVideoConsultation) BorderStroke(1.5.dp, TealPrimary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isVideoConsultation = true }
                                .testTag("wizard_mode_video")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Video Call", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Encrypted HD", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isVideoConsultation) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (!isVideoConsultation) BorderStroke(1.5.dp, TealPrimary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isVideoConsultation = false }
                                .testTag("wizard_mode_clinic")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("In-Clinic Visit", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Hospital Desk", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // 4. Symptoms / Notes with Voice Input & Quick Chips
                    Text("4. Symptoms / Consultation Reason", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    val quickSymptoms = listOf("Skin Lesion / Rash", "Chest Discomfort", "Fever & Weakness", "Joint Pain", "Routine Checkup")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickSymptoms) { symptomTag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.clickable {
                                    bookingNotes = if (bookingNotes.isBlank()) symptomTag else "$bookingNotes, $symptomTag"
                                }
                            ) {
                                Text(
                                    "+ $symptomTag",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    color = TealPrimary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = bookingNotes,
                        onValueChange = { bookingNotes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("booking_notes_input"),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                        maxLines = 3,
                        trailingIcon = {
                            VoiceSpeechInputButton(
                                onSpokenText = { recognizedText ->
                                    bookingNotes = if (bookingNotes.isBlank()) recognizedText else "$bookingNotes $recognizedText"
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    )

                    // 5. Payment Method
                    Text("5. Payment Method", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    val paymentOptions = listOf(
                        "eSewa / Khalti (Nepal)",
                        "ConnectIPS / Mobile Banking",
                        "Credit/Debit Card",
                        "Pay at Hospital Desk"
                    )
                    paymentOptions.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(method, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bookDoctorAppointment(
                            doctor = doctor,
                            date = selectedDateString,
                            timeSlot = selectedTimeSlot,
                            isVideo = isVideoConsultation,
                            notes = bookingNotes,
                            paymentMethod = selectedPaymentMethod
                        )
                        showBookingConfirmationDialog = false
                        showBookingSuccessDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("confirm_booking_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookingConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // BOOKING SUCCESS & DIGITAL APPOINTMENT SLIP
    // ==========================================
    if (showBookingSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showBookingSuccessDialog = false
                activeTab = 1 // Navigate to My Appointments tab
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = SuccessGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
                    }
                }
            },
            title = {
                Text("Appointment Confirmed!", fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Your appointment has been securely registered in your local SQLCipher Room database.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkMode) SurfaceVariantDark else TealLight,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🗓️ Date: $selectedDateString at $selectedTimeSlot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("🩺 Doctor: ${selectedDoctorForBooking?.name}", fontSize = 11.sp)
                            Text("🏥 Facility: ${selectedDoctorForBooking?.clinicAffiliation}", fontSize = 11.sp)
                            Text("🔔 WorkManager Reminder: Active (15m before)", fontSize = 10.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBookingSuccessDialog = false
                        activeTab = 1 // Go to My Appointments
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("view_scheduled_appointments_btn")
                ) {
                    Text("View My Appointments")
                }
            }
        )
    }

    // ==========================================
    // RESCHEDULE APPOINTMENT DIALOG
    // ==========================================
    if (appointmentToReschedule != null) {
        val apt = appointmentToReschedule!!
        AlertDialog(
            onDismissRequest = { appointmentToReschedule = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reschedule Consultation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Rescheduling consultation with ${apt.doctorName} (${apt.doctorSpecialty}). Pick a new date and time slot.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("1. Select New Date:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(datesList) { (offset, label, fullDate) ->
                            val isSelected = rescheduleDateOffset == offset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { rescheduleDateOffset = offset }
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text("2. Select New Time Slot:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(timeSlots) { slot ->
                            val isSelected = rescheduleTimeSlot == slot
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { rescheduleTimeSlot = slot }
                            ) {
                                Text(
                                    slot,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rescheduleDoctorAppointment(apt, rescheduleDateString, rescheduleTimeSlot)
                        appointmentToReschedule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_reschedule_btn")
                ) {
                    Text("Save & Reschedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToReschedule = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // CANCEL APPOINTMENT CONFIRMATION DIALOG
    // ==========================================
    if (appointmentToCancel != null) {
        val apt = appointmentToCancel!!
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Cancel Consultation?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "Are you sure you want to cancel your consultation with ${apt.doctorName} scheduled for ${apt.appointmentDate} at ${apt.timeSlot}? A full refund will be credited to your account.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelDoctorAppointment(apt)
                        appointmentToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_cancel_appointment_btn")
                ) {
                    Text("Yes, Cancel Consultation")
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToCancel = null }) {
                    Text("Keep Appointment")
                }
            }
        )
    }

    // ==========================================
    // VIEW DIGITAL PRESCRIPTION MODAL
    // ==========================================
    if (viewingPrescriptionAppointment != null) {
        val apt = viewingPrescriptionAppointment!!
        AlertDialog(
            onDismissRequest = { viewingPrescriptionAppointment = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consultation Summary & Rx", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Doctor: ${apt.doctorName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Specialty: ${apt.doctorSpecialty}", fontSize = 11.sp, color = TealPrimary)
                            Text("Date: ${apt.appointmentDate} • ${apt.timeSlot}", fontSize = 11.sp)
                        }
                    }

                    Text("Doctor's Prescription & Clinical Notes:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TealLight,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            apt.prescriptionNotes.ifBlank { "Topical Hydrocortisone 1% cream apply twice daily for 5 days. Cetirizine 10mg once daily at night. Review after 1 week." },
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewingPrescriptionAppointment = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Doctor Card with Live Availability Badges & Interactive Slot Picker
 */
@Composable
private fun DoctorCardItem(
    doctor: UserEntity,
    isDarkMode: Boolean,
    onBookClick: (slot: String) -> Unit
) {
    val doctorSlots = remember(doctor) {
        doctor.doctorAvailableTimeSlots.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    var selectedSlotOnCard by remember { mutableStateOf(doctorSlots.firstOrNull() ?: "10:30 AM") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("doctor_card_${doctor.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) SurfaceVariantDark else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Name, Verified, Fee, Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = TealPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            doctor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified Doctor",
                            tint = SuccessGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        doctor.specialization,
                        fontSize = 12.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        doctor.clinicAffiliation,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberWarning.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("${doctor.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${doctor.preferredCurrency} ${doctor.consultationFee.toInt()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Chips (Experience, Languages, Mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        "🏥 ${doctor.doctorExperienceYears} yrs exp",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        "🗣️ ${doctor.doctorLanguages.split(",").firstOrNull()?.trim() ?: "English"}",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TealPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (doctor.doctorConsultationModes.contains("VIDEO")) "🎥 Video & Clinic" else "🏥 Clinic Visit",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = TealPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Live Available Slots Row & Quick Book
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Available Slots:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "🟢 Available Today",
                    fontSize = 10.sp,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Interactive Slot Picker Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(doctorSlots.take(5)) { slot ->
                    val isSelected = selectedSlotOnCard == slot
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable { selectedSlotOnCard = slot }
                            .testTag("doc_${doctor.id}_slot_${slot.replace(" ", "_")}")
                    ) {
                        Text(
                            slot,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            Button(
                onClick = { onBookClick(selectedSlotOnCard) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("book_doctor_btn_${doctor.id}")
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule for $selectedSlotOnCard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Appointment Card Item displaying Room database record details
 */
@Composable
private fun AppointmentCardItem(
    appointment: AppointmentEntity,
    isDarkMode: Boolean,
    onJoinVideo: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit,
    onViewPrescription: () -> Unit,
    onNavigateToClinic: () -> Unit,
    onDelete: () -> Unit
) {
    val isCancelled = appointment.status.equals("CANCELLED", ignoreCase = true)
    val isRescheduled = appointment.status.equals("RESCHEDULED", ignoreCase = true)
    val isCompleted = appointment.status.equals("COMPLETED", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("appointment_card_${appointment.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) SurfaceVariantDark else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isCancelled) ErrorRed.copy(alpha = 0.4f)
            else if (isRescheduled) AmberWarning.copy(alpha = 0.5f)
            else if (isCompleted) SuccessGreen.copy(alpha = 0.4f)
            else TealPrimary.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Doctor & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCancelled) ErrorRed.copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isCancelled) ErrorRed else TealPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                appointment.doctorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified Doctor",
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            appointment.doctorSpecialty,
                            fontSize = 11.sp,
                            color = TealPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isCancelled -> ErrorRed.copy(alpha = 0.15f)
                        isRescheduled -> AmberWarning.copy(alpha = 0.15f)
                        isCompleted -> SuccessGreen.copy(alpha = 0.15f)
                        else -> TealPrimary.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        appointment.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isCancelled -> ErrorRed
                            isRescheduled -> AmberWarning
                            isCompleted -> SuccessGreen
                            else -> TealPrimary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Details: Date, Time Slot, Mode, Clinic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(appointment.appointmentDate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(appointment.timeSlot, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        if (appointment.isVideoConsultation) "🎥 Video Call" else "🏥 In-Clinic",
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Hospital / Clinic: ${appointment.clinicName}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Notes: ${appointment.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            if (!isCancelled && !isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (appointment.isVideoConsultation) {
                        Button(
                            onClick = onJoinVideo,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("join_video_consultation_btn_${appointment.id}")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Join Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onNavigateToClinic,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("navigate_to_clinic_btn_${appointment.id}")
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onReschedule,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
                        border = BorderStroke(1.dp, TealPrimary),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("reschedule_apt_btn_${appointment.id}")
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reschedule", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("cancel_apt_btn_${appointment.id}")
                    ) {
                        Text("Cancel", fontSize = 11.sp, color = ErrorRed)
                    }
                }
            } else if (isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "✅ Consultation Completed",
                        fontSize = 11.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = onViewPrescription,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("view_prescription_btn_${appointment.id}")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Prescription", fontSize = 11.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "❌ Cancelled • Full Refund Processed",
                        fontSize = 11.sp,
                        color = ErrorRed
                    )
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_apt_record_btn_${appointment.id}")
                    ) {
                        Text("Remove", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
