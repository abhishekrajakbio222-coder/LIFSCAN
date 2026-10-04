package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MedicalFacilityEntity
import com.example.data.model.UserRole
import com.example.ui.components.LifscanAppBarBrand
import com.example.ui.components.LifscanMiniBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import java.text.SimpleDateFormat
import java.util.*

val AdminNavy = Color(0xFF0D1129)
val AdminAccentBlue = Color(0xFF0284C7)
val AdminEmerald = Color(0xFF10B981)
val AdminAmber = Color(0xFFF59E0B)
val AdminPurple = Color(0xFF8B5CF6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFacilitiesControlScreen(
    viewModel: LifscanViewModel
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val facilities by viewModel.allFacilities.collectAsState()
    val activeSOSAlerts by viewModel.activeSOSAlerts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var facilityToEditCapacity by remember { mutableStateOf<MedicalFacilityEntity?>(null) }
    var facilityToEditFull by remember { mutableStateOf<MedicalFacilityEntity?>(null) }
    var facilityToDelete by remember { mutableStateOf<MedicalFacilityEntity?>(null) }
    var showAuditLogsSheet by remember { mutableStateOf(false) }

    // Filter facilities based on search & category
    val filteredFacilities = remember(facilities, searchQuery, selectedTypeFilter) {
        facilities.filter { fac ->
            val matchesType = when (selectedTypeFilter) {
                "ALL" -> true
                "GOVT" -> fac.facilityType == "GOVT_HOSPITAL"
                "PRIVATE" -> fac.facilityType == "PRIVATE_HOSPITAL"
                "CLINIC" -> fac.facilityType == "CLINIC"
                "LAB" -> fac.facilityType == "DIAGNOSTIC_LAB" || fac.facilityType.contains("LAB")
                "PHARMACY" -> fac.facilityType == "PHARMACY"
                "AMBULANCE" -> fac.facilityType == "LIVE_AMBULANCE"
                "24x7" -> fac.isOpen24x7
                "EMERGENCY" -> fac.emergencyAvailable
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                fac.name.contains(searchQuery, ignoreCase = true) ||
                        fac.address.contains(searchQuery, ignoreCase = true) ||
                        fac.services.contains(searchQuery, ignoreCase = true) ||
                        fac.phone.contains(searchQuery, ignoreCase = true)
            }
            matchesType && matchesQuery
        }
    }

    // Network Telemetry Calculations
    val totalFacilitiesCount = facilities.size
    val total24x7Count = facilities.count { it.isOpen24x7 }
    val totalEmergencyCount = facilities.count { it.emergencyAvailable }
    val totalIcuBeds = facilities.sumOf { it.availableIcuBeds }
    val totalVentilators = facilities.sumOf { it.availableVentilators }
    val totalAmbulanceUnits = facilities.sumOf { it.ambulanceFleetCount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    LifscanAppBarBrand(
                        title = "Facility Command Center",
                        subtitle = "Admin Network & Capacity Control"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("admin_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.AuditLogViewer) },
                        modifier = Modifier.testTag("admin_audit_logs_btn")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Security Audit Logs", tint = AdminAccentBlue)
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.Login) },
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Exit Admin", tint = EmergencyRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                text = { Text("Register Facility", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("admin_add_facility_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 12.dp)
        ) {
            // ============================================================
            // 1. ADMIN HEADER & LIVE TELEMETRY BANNER
            // ============================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("admin_telemetry_banner"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AdminNavy)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AdminAccentBlue.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = AdminAccentBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "LIFSCAN HEALTHCARE NETWORK CONTROL",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    currentUser?.name ?: "Chief Health Administrator",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AdminEmerald.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, AdminEmerald.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(AdminEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "LIVE DISPATCH",
                                        color = AdminEmerald,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric Stat Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AdminMetricTile(
                                label = "Total Facilities",
                                value = "$totalFacilitiesCount",
                                icon = Icons.Default.LocalHospital,
                                tint = AdminAccentBlue
                            )
                            AdminMetricTile(
                                label = "24/7 Active",
                                value = "$total24x7Count",
                                icon = Icons.Default.AccessTimeFilled,
                                tint = AdminEmerald
                            )
                            AdminMetricTile(
                                label = "Available ICU",
                                value = "$totalIcuBeds",
                                icon = Icons.Default.Bed,
                                tint = AdminAmber
                            )
                            AdminMetricTile(
                                label = "Ventilators",
                                value = "$totalVentilators",
                                icon = Icons.Default.Air,
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }

            // ============================================================
            // 2. ACTIVE REGIONAL SOS NOTIFICATION (If Any)
            // ============================================================
            if (activeSOSAlerts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("admin_active_sos_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmergencyRed.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Active Emergency Alerts (${activeSOSAlerts.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = EmergencyRed
                                )
                                Text(
                                    "Latest: ${activeSOSAlerts.first().patientName} (${activeSOSAlerts.first().emergencyType})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Track SOS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ============================================================
            // 3. SEARCH & CATEGORY FILTER CHIPS
            // ============================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search hospital, lab, clinic, contact...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("admin_facility_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf(
                            "ALL" to "All Facilities",
                            "GOVT" to "Govt Hospitals",
                            "PRIVATE" to "Super Specialty",
                            "EMERGENCY" to "Emergency Trauma",
                            "24x7" to "24/7 Open",
                            "LAB" to "Diagnostic Labs",
                            "CLINIC" to "Clinics",
                            "PHARMACY" to "Pharmacies",
                            "AMBULANCE" to "Ambulance Units"
                        )
                        items(filters) { (key, label) ->
                            FilterChip(
                                selected = selectedTypeFilter == key,
                                onClick = { selectedTypeFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedTypeFilter == key) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (selectedTypeFilter == key) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // ============================================================
            // 4. FACILITIES LIST HEADER
            // ============================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Managed Facilities (${filteredFacilities.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Tap card controls to edit live",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ============================================================
            // 5. FACILITY CARDS WITH REAL-TIME ADMIN CONTROLS
            // ============================================================
            if (filteredFacilities.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No matching facilities found", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Try adjusting your search terms or filter chip.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredFacilities, key = { it.id }) { facility ->
                    AdminFacilityCard(
                        facility = facility,
                        onToggleEmergency = { is24x7, emergency ->
                            viewModel.toggleFacilityEmergency(facility.id, is24x7, emergency)
                        },
                        onToggleVerification = { isVerified ->
                            viewModel.toggleFacilityVerification(facility.id, isVerified)
                        },
                        onEditCapacity = { facilityToEditCapacity = facility },
                        onEditFull = { facilityToEditFull = facility },
                        onDelete = { facilityToDelete = facility },
                        onNavigate = {
                            viewModel.navigateTo(
                                ScreenNav.MappingNavigation(
                                    facilityName = facility.name,
                                    address = facility.address,
                                    latitude = facility.latitude,
                                    longitude = facility.longitude,
                                    facilityType = facility.facilityType,
                                    distanceKm = facility.distanceKm,
                                    isEmergency = facility.emergencyAvailable,
                                    phone = facility.phone
                                )
                            )
                        },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${facility.phone}"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    // ============================================================
    // DIALOG: REGISTER NEW FACILITY
    // ============================================================
    if (showAddDialog) {
        AdminAddFacilityDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, address, distance, phone, services, is24x7, emergency, lat, lng, totalBeds, icuBeds, vents, oxygen, doctors, fleet ->
                viewModel.addMedicalFacility(
                    name = name,
                    facilityType = type,
                    address = address,
                    distanceKm = distance,
                    phone = phone,
                    services = services,
                    isOpen24x7 = is24x7,
                    emergencyAvailable = emergency,
                    latitude = lat,
                    longitude = lng,
                    totalBeds = totalBeds,
                    icuBeds = icuBeds,
                    ventilators = vents,
                    oxygenStatus = oxygen,
                    doctorsOnDuty = doctors,
                    ambulanceFleet = fleet
                )
                showAddDialog = false
            }
        )
    }

    // ============================================================
    // DIALOG: EDIT BED & EQUIPMENT CAPACITY
    // ============================================================
    facilityToEditCapacity?.let { facility ->
        AdminEditCapacityDialog(
            facility = facility,
            onDismiss = { facilityToEditCapacity = null },
            onConfirm = { totalBeds, icuBeds, vents, oxygen, doctors ->
                viewModel.updateFacilityBedCapacity(
                    facilityId = facility.id,
                    totalBeds = totalBeds,
                    icuBeds = icuBeds,
                    ventilators = vents,
                    oxygenStatus = oxygen,
                    activeDoctors = doctors
                )
                facilityToEditCapacity = null
            }
        )
    }

    // ============================================================
    // DIALOG: FULL EDIT FACILITY
    // ============================================================
    facilityToEditFull?.let { facility ->
        AdminFullEditFacilityDialog(
            facility = facility,
            onDismiss = { facilityToEditFull = null },
            onConfirm = { updated ->
                viewModel.updateMedicalFacility(updated)
                facilityToEditFull = null
            }
        )
    }

    // ============================================================
    // DIALOG: CONFIRM DELETE
    // ============================================================
    facilityToDelete?.let { facility ->
        AlertDialog(
            onDismissRequest = { facilityToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Decommission Facility?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove '${facility.name}' from the active Lifscan Emergency Directory? This action will be recorded in the security audit ledger.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedicalFacility(facility.id, facility.name)
                        facilityToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Delete Facility")
                }
            },
            dismissButton = {
                TextButton(onClick = { facilityToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminMetricTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.2f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        Text(label, fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun AdminFacilityCard(
    facility: MedicalFacilityEntity,
    onToggleEmergency: (Boolean, Boolean) -> Unit,
    onToggleVerification: (Boolean) -> Unit,
    onEditCapacity: () -> Unit,
    onEditFull: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: () -> Unit,
    onCall: () -> Unit
) {
    val typeColor = when (facility.facilityType) {
        "GOVT_HOSPITAL" -> Color(0xFF0284C7)
        "PRIVATE_HOSPITAL" -> Color(0xFF7C3AED)
        "DIAGNOSTIC_LAB" -> Color(0xFF0D9488)
        "PHARMACY" -> Color(0xFF059669)
        "LIVE_AMBULANCE" -> EmergencyRed
        else -> TealPrimary
    }

    val typeLabel = when (facility.facilityType) {
        "GOVT_HOSPITAL" -> "Govt Hospital"
        "PRIVATE_HOSPITAL" -> "Super Specialty"
        "DIAGNOSTIC_LAB" -> "Diagnostic Lab"
        "PHARMACY" -> "24/7 Pharmacy"
        "LIVE_AMBULANCE" -> "Ambulance Base"
        "CLINIC" -> "Polyclinic"
        else -> facility.facilityType
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_facility_card_${facility.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (facility.emergencyAvailable) EmergencyRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Badges & Accreditation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            typeLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }

                    if (facility.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = SuccessGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("VERIFIED", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }
                }

                // Action Menu: Edit / Delete
                Row {
                    IconButton(onClick = onEditFull, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = EmergencyRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Facility Title & Address
            Text(
                facility.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${facility.address} • ${facility.distanceKm} km away",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Services Description
            Text(
                facility.services,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Live Capacity & Resource Indicator Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Beds", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${facility.totalBeds}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ICU Beds", fontSize = 9.5.sp, color = EmergencyRed)
                        Text("${facility.availableIcuBeds}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmergencyRed)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vents", fontSize = 9.5.sp, color = AdminAccentBlue)
                        Text("${facility.availableVentilators}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AdminAccentBlue)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("O2 Level", fontSize = 9.5.sp, color = if (facility.oxygenSupplyStatus == "OPTIMAL") SuccessGreen else EmergencyRed)
                        Text(facility.oxygenSupplyStatus, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = if (facility.oxygenSupplyStatus == "OPTIMAL") SuccessGreen else EmergencyRed)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Doctors", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${facility.activeDoctorsOnDuty}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Status Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 24/7 Open Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = facility.isOpen24x7,
                        onCheckedChange = { onToggleEmergency(it, facility.emergencyAvailable) },
                        modifier = Modifier.testTag("toggle_24x7_${facility.id}")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (facility.isOpen24x7) "24/7 Open" else "Closed/Shift",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Emergency Trauma Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = facility.emergencyAvailable,
                        onCheckedChange = { onToggleEmergency(facility.isOpen24x7, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmergencyRed, checkedTrackColor = EmergencyRed.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("toggle_emergency_${facility.id}")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (facility.emergencyAvailable) "Emergency On" else "No Emergency",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (facility.emergencyAvailable) EmergencyRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditCapacity,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f).testTag("btn_adjust_capacity_${facility.id}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adjust Beds", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigate,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.9f).testTag("btn_map_${facility.id}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map View", fontSize = 11.5.sp)
                }

                Button(
                    onClick = onCall,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.weight(0.8f).testTag("btn_call_${facility.id}"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// DIALOG: ADD NEW MEDICAL FACILITY
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddFacilityDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: String,
        address: String,
        distance: Double,
        phone: String,
        services: String,
        is24x7: Boolean,
        emergency: Boolean,
        lat: Double,
        lng: Double,
        totalBeds: Int,
        icuBeds: Int,
        vents: Int,
        oxygen: String,
        doctors: Int,
        fleet: Int
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("GOVT_HOSPITAL") }
    var address by remember { mutableStateOf("Kathmandu, Nepal") }
    var distance by remember { mutableStateOf("1.5") }
    var phone by remember { mutableStateOf("+977-1-4221111") }
    var services by remember { mutableStateOf("24/7 Emergency Trauma, ICU, Operation Theaters, Blood Bank, CT Scan, Pharmacy") }
    var isOpen24x7 by remember { mutableStateOf(true) }
    var emergencyAvailable by remember { mutableStateOf(true) }
    var latitude by remember { mutableStateOf("27.7058") }
    var longitude by remember { mutableStateOf("85.3142") }
    var totalBeds by remember { mutableStateOf("150") }
    var icuBeds by remember { mutableStateOf("12") }
    var ventilators by remember { mutableStateOf("6") }
    var oxygenStatus by remember { mutableStateOf("OPTIMAL") }
    var doctorsOnDuty by remember { mutableStateOf("18") }
    var ambulanceFleet by remember { mutableStateOf("4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Register Medical Facility", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Facility Name *") },
                        placeholder = { Text("e.g. Kathmandu National Super Specialty Hospital") },
                        modifier = Modifier.fillMaxWidth().testTag("add_fac_name_input"),
                        singleLine = true
                    )
                }

                item {
                    Text("Facility Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    val types = listOf(
                        "GOVT_HOSPITAL" to "Govt Hospital",
                        "PRIVATE_HOSPITAL" to "Private Super Specialty",
                        "DIAGNOSTIC_LAB" to "Diagnostic Lab",
                        "CLINIC" to "Clinic / Polyclinic",
                        "PHARMACY" to "24/7 Pharmacy",
                        "LIVE_AMBULANCE" to "Ambulance Station"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(types) { (key, label) ->
                            FilterChip(
                                selected = selectedType == key,
                                onClick = { selectedType = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address & City *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Helpline / Phone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = distance,
                            onValueChange = { distance = it },
                            label = { Text("Distance (Km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = totalBeds,
                            onValueChange = { totalBeds = it },
                            label = { Text("Total Beds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = icuBeds,
                            onValueChange = { icuBeds = it },
                            label = { Text("ICU Beds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ventilators,
                            onValueChange = { ventilators = it },
                            label = { Text("Vents") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = doctorsOnDuty,
                            onValueChange = { doctorsOnDuty = it },
                            label = { Text("Doctors on Duty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ambulanceFleet,
                            onValueChange = { ambulanceFleet = it },
                            label = { Text("Ambulances") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = services,
                        onValueChange = { services = it },
                        label = { Text("Services & Facilities List") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isOpen24x7, onCheckedChange = { isOpen24x7 = it })
                            Text("24/7 Open", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = emergencyAvailable, onCheckedChange = { emergencyAvailable = it })
                            Text("Emergency Ready", fontSize = 12.sp, color = EmergencyRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            selectedType,
                            address,
                            distance.toDoubleOrNull() ?: 1.0,
                            phone,
                            services,
                            isOpen24x7,
                            emergencyAvailable,
                            latitude.toDoubleOrNull() ?: 27.7058,
                            longitude.toDoubleOrNull() ?: 85.3142,
                            totalBeds.toIntOrNull() ?: 100,
                            icuBeds.toIntOrNull() ?: 10,
                            ventilators.toIntOrNull() ?: 4,
                            oxygenStatus,
                            doctorsOnDuty.toIntOrNull() ?: 12,
                            ambulanceFleet.toIntOrNull() ?: 3
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("admin_save_facility_btn")
            ) {
                Text("Register Facility")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================
// DIALOG: EDIT BED & EQUIPMENT CAPACITY ONLY
// ============================================================
@Composable
fun AdminEditCapacityDialog(
    facility: MedicalFacilityEntity,
    onDismiss: () -> Unit,
    onConfirm: (totalBeds: Int, icuBeds: Int, vents: Int, oxygen: String, doctors: Int) -> Unit
) {
    var totalBeds by remember { mutableStateOf(facility.totalBeds.toString()) }
    var icuBeds by remember { mutableStateOf(facility.availableIcuBeds.toString()) }
    var ventilators by remember { mutableStateOf(facility.availableVentilators.toString()) }
    var oxygenStatus by remember { mutableStateOf(facility.oxygenSupplyStatus) }
    var doctorsOnDuty by remember { mutableStateOf(facility.activeDoctorsOnDuty.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Adjust Bed & Resource Capacity", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(facility.name, fontSize = 12.sp, color = TealPrimary, maxLines = 1)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Real-time changes reflect immediately across all Lifscan Patient & Ambulance SOS modules.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalBeds,
                        onValueChange = { totalBeds = it },
                        label = { Text("Total Beds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_total_beds_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = icuBeds,
                        onValueChange = { icuBeds = it },
                        label = { Text("ICU Beds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_icu_beds_input"),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ventilators,
                        onValueChange = { ventilators = it },
                        label = { Text("Ventilators") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_vents_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = doctorsOnDuty,
                        onValueChange = { doctorsOnDuty = it },
                        label = { Text("Doctors On Duty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Oxygen Reserve Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("OPTIMAL", "ADEQUATE", "LOW", "CRITICAL").forEach { status ->
                        val isSel = oxygenStatus == status
                        val statusColor = when (status) {
                            "OPTIMAL" -> SuccessGreen
                            "ADEQUATE" -> AdminAccentBlue
                            "LOW" -> AdminAmber
                            else -> EmergencyRed
                        }
                        FilterChip(
                            selected = isSel,
                            onClick = { oxygenStatus = status },
                            label = { Text(status, fontSize = 10.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = statusColor.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        totalBeds.toIntOrNull() ?: facility.totalBeds,
                        icuBeds.toIntOrNull() ?: facility.availableIcuBeds,
                        ventilators.toIntOrNull() ?: facility.availableVentilators,
                        oxygenStatus,
                        doctorsOnDuty.toIntOrNull() ?: facility.activeDoctorsOnDuty
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.testTag("admin_confirm_capacity_btn")
            ) {
                Text("Update Capacity")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================
// DIALOG: FULL EDIT FACILITY
// ============================================================
@Composable
fun AdminFullEditFacilityDialog(
    facility: MedicalFacilityEntity,
    onDismiss: () -> Unit,
    onConfirm: (MedicalFacilityEntity) -> Unit
) {
    var name by remember { mutableStateOf(facility.name) }
    var address by remember { mutableStateOf(facility.address) }
    var phone by remember { mutableStateOf(facility.phone) }
    var services by remember { mutableStateOf(facility.services) }
    var distance by remember { mutableStateOf(facility.distanceKm.toString()) }
    var isOpen24x7 by remember { mutableStateOf(facility.isOpen24x7) }
    var emergencyAvailable by remember { mutableStateOf(facility.emergencyAvailable) }
    var isVerified by remember { mutableStateOf(facility.isVerified) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Facility Details", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Facility Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Helpline Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Distance (Km)") },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = services,
                    onValueChange = { services = it },
                    label = { Text("Services") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isOpen24x7, onCheckedChange = { isOpen24x7 = it })
                        Text("24/7", fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = emergencyAvailable, onCheckedChange = { emergencyAvailable = it })
                        Text("Emergency", fontSize = 12.sp, color = EmergencyRed)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isVerified, onCheckedChange = { isVerified = it })
                        Text("Verified", fontSize = 12.sp, color = SuccessGreen)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        facility.copy(
                            name = name,
                            address = address,
                            phone = phone,
                            services = services,
                            distanceKm = distance.toDoubleOrNull() ?: facility.distanceKm,
                            isOpen24x7 = isOpen24x7,
                            emergencyAvailable = emergencyAvailable,
                            isVerified = isVerified,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
