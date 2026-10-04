package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthReportEntity
import com.example.data.model.MedicalFacilityEntity
import com.example.data.model.SkinScanEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.auth.BiometricAuthGateDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav
import com.example.util.GoogleMapsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalVaultScreen(viewModel: LifscanViewModel) {
    val context = LocalContext.current
    val isVaultAuthenticated by viewModel.isVaultAuthenticated.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val facilities by viewModel.allFacilities.collectAsState()
    val reports by viewModel.patientHealthReports.collectAsState()
    val skinScans by viewModel.patientSkinScans.collectAsState()
    val appointments by viewModel.patientAppointments.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val lastAuditTime by viewModel.lastSecurityAuditTime.collectAsState()
    val lastCloudBackupTime by viewModel.lastCloudBackupTime.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val searchQuery by viewModel.vaultSearchQuery.collectAsState()
    val selectedCategoryFilter by viewModel.vaultCategoryFilter.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSimulatedOffline by viewModel.isSimulatedOffline.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Profile & Facilities, 1: All Records, 2: Vitals & Labs, 3: Skin Scans, 4: Prescriptions, 5: Consults
    var showAddReportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedReportForDetails by remember { mutableStateOf<HealthReportEntity?>(null) }
    var selectedScanForDetails by remember { mutableStateOf<SkinScanEntity?>(null) }

    val tabTitles = listOf("Profile & Facilities", "All Records", "Diagnostic & Vitals", "Skin Scans", "Prescriptions", "Consultations")

    // Filter data by search query and category
    val filteredReports = reports.filter { report ->
        val matchesQuery = searchQuery.isBlank() ||
            report.title.contains(searchQuery, ignoreCase = true) ||
            report.doctorOrLabName.contains(searchQuery, ignoreCase = true) ||
            report.category.contains(searchQuery, ignoreCase = true) ||
            report.summary.contains(searchQuery, ignoreCase = true) ||
            report.vitalsBloodPressure.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "All" || report.category.equals(selectedCategoryFilter, ignoreCase = true)
        matchesQuery && matchesCategory
    }

    val filteredSkinScans = skinScans.filter { scan ->
        val matchesQuery = searchQuery.isBlank() ||
            scan.conditionName.contains(searchQuery, ignoreCase = true) ||
            scan.affectedArea.contains(searchQuery, ignoreCase = true) ||
            scan.riskLevel.contains(searchQuery, ignoreCase = true) ||
            scan.aiAnalysisSummary.contains(searchQuery, ignoreCase = true) ||
            scan.recommendedTreatment.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "All" || selectedCategoryFilter.equals("Dermatology", ignoreCase = true)
        matchesQuery && matchesCategory
    }

    val filteredAppointments = appointments.filter { apt ->
        searchQuery.isBlank() ||
            apt.doctorName.contains(searchQuery, ignoreCase = true) ||
            apt.doctorSpecialty.contains(searchQuery, ignoreCase = true) ||
            apt.notes.contains(searchQuery, ignoreCase = true) ||
            apt.prescriptionNotes.contains(searchQuery, ignoreCase = true)
    }

    // If vault is locked, present the Biometric Authentication Gate
    if (!isVaultAuthenticated) {
        BiometricAuthGateDialog(
            vaultTitle = "Encrypted Medical Vault",
            onAuthenticated = {
                viewModel.setVaultAuthenticated(true)
            },
            onDismiss = {
                viewModel.navigateTo(ScreenNav.PatientDashboard)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.EnhancedEncryption,
                                    contentDescription = "Medical Vault",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Medical Data Vault",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SuccessGreen
                                ) {
                                    Text(
                                        "256-BIT AES",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                "SQLCipher Encrypted at Rest • TEE Protected",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("vault_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.AuditLogViewer) },
                        modifier = Modifier.testTag("vault_audit_logs_btn")
                    ) {
                        Icon(Icons.Default.SecurityUpdateGood, contentDescription = "Security Audit Logs", tint = TealPrimary)
                    }
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("vault_dark_mode_btn")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.MedicalDocumentScan) },
                        modifier = Modifier.testTag("vault_scan_document_btn")
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Scan Medical Document / Rx",
                            tint = Color(0xFF0EA5E9)
                        )
                    }
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("vault_export_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export Encrypted Records")
                    }
                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier.testTag("vault_lock_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = AmberWarning)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddReportDialog = true },
                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                text = { Text("Add Vital / Record") },
                containerColor = TealPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_vital_record_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Security Header Banner
            Surface(
                color = if (isDarkMode) SurfaceVariantDark else SuccessGreenLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SQLCipher Hardware Keystore Encrypted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    }
                    Text(
                        "Audit: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastAuditTime))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Search Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setVaultSearchQuery(it) },
                        placeholder = { Text("Search records, doctor notes, labs, vitals...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search Vault", tint = TealPrimary, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setVaultSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_search_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf("All", "Dermatology", "Blood Test", "General")
                        categories.forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setVaultCategoryFilter(cat) },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Categorized Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TealPrimary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("vault_tab_$index")
                    )
                }
            }

            // Records List View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search result badge
                if (searchQuery.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Filtered for '$searchQuery'", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                Text("${filteredReports.size + filteredSkinScans.size + filteredAppointments.size} records found", fontSize = 11.sp, color = TealPrimary)
                            }
                        }
                    }
                }

                when (selectedTab) {
                    // TAB 0: PROFILE & MEDICAL FACILITIES
                    0 -> {
                        // Patient Identity Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("vault_profile_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentUser?.name ?: "Aayush Shrestha",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Health ID: ${currentUser?.id?.take(12)?.uppercase() ?: "LIF-NP-882194"}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = EmergencyRed.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "Blood: ${currentUser?.bloodGroup ?: "O+"}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = EmergencyRed
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Profile details grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Age / Gender", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${currentUser?.age ?: 32} yrs • ${currentUser?.gender ?: "Male"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text("Phone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(currentUser?.phone ?: "+977 9841234567", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text("Emergency Contact", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(currentUser?.emergencyContactPhone ?: "+977-9841234567", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Medical History & Allergies", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(currentUser?.medicalHistory ?: "No known drug allergies. Up-to-date immunizations.", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }

                        // Nearby Medical Facilities Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verified Facilities & Hospitals", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                TextButton(
                                    onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("View Live Map", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Facilities List
                        items(facilities) { facility ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("vault_facility_${facility.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(facility.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(facility.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${facility.distanceKm} km",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (facility.isOpen24x7) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (facility.isOpen24x7) "24/7 OPEN" else facility.facilityType,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (facility.isOpen24x7) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text("📞 ${facility.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                GoogleMapsHelper.dialPhoneNumber(context, facility.phone)
                                            },
                                            modifier = Modifier.height(32.dp).weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Call", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                GoogleMapsHelper.openGoogleMaps(context, facility.latitude, facility.longitude, facility.name)
                                            },
                                            modifier = Modifier.height(32.dp).weight(1.3f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Google Map", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.navigateTo(
                                                    ScreenNav.MappingNavigation(
                                                        facilityName = facility.name,
                                                        address = facility.address,
                                                        latitude = facility.latitude,
                                                        longitude = facility.longitude,
                                                        facilityType = facility.facilityType,
                                                        distanceKm = facility.distanceKm,
                                                        isEmergency = facility.isOpen24x7,
                                                        phone = facility.phone
                                                    )
                                                )
                                            },
                                            modifier = Modifier.height(32.dp).weight(1.2f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Route", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: ALL RECORDS
                    1 -> {
                        // Cloud Backup & Recovery Card with Room DB Offline Sync
                        item {
                            FirestoreCloudBackupCard(
                                lastBackupTime = lastCloudBackupTime,
                                isSyncing = isCloudSyncing,
                                isOnline = isOnline,
                                isSimulatedOffline = isSimulatedOffline,
                                onToggleSimulatedOffline = { viewModel.toggleSimulatedOffline() },
                                onSync = { viewModel.triggerSyncNow() },
                                onRestore = { viewModel.restoreVaultFromCloud() }
                            )
                        }

                        // Diagnostic Vitals Card Summary
                        item {
                            BiomarkersVitalsOverviewCard(filteredReports.ifEmpty { reports })
                        }

                        // List Health Reports
                        items(filteredReports) { report ->
                            HealthReportItemCard(
                                report = report,
                                onClick = {
                                    selectedReportForDetails = report
                                    viewModel.recordVaultAuditLog("VIEW_REPORT", "Viewed medical report: ${report.title}", report.title)
                                }
                            )
                        }

                        // List Skin Scans
                        items(filteredSkinScans) { scan ->
                            SkinScanItemCard(
                                scan = scan,
                                onClick = {
                                    selectedScanForDetails = scan
                                    viewModel.recordVaultAuditLog("VIEW_SKIN_SCAN", "Viewed dermatology scan: ${scan.conditionName}", scan.conditionName)
                                }
                            )
                        }
                    }

                    // TAB 2: DIAGNOSTIC REPORTS & VITALS
                    2 -> {
                        item {
                            BiomarkersVitalsOverviewCard(filteredReports.ifEmpty { reports })
                        }
                        if (filteredReports.isEmpty()) {
                            item {
                                EmptyVaultCategoryNotice(
                                    title = "No Matching Reports",
                                    subtitle = "No diagnostic records match your search query '$searchQuery'."
                                )
                            }
                        } else {
                            items(filteredReports) { report ->
                                HealthReportItemCard(report = report, onClick = { selectedReportForDetails = report })
                            }
                        }
                    }

                    // TAB 3: SKIN SCANS & BIOPSIES
                    3 -> {
                        if (filteredSkinScans.isEmpty()) {
                            item {
                                EmptyVaultCategoryNotice(
                                    title = "No Matching Skin Scans",
                                    subtitle = if (searchQuery.isNotBlank()) "No skin scans match '$searchQuery'." else "Use the AI Skin Scanner to capture lesions and sync diagnostic assessments."
                                )
                            }
                        } else {
                            items(filteredSkinScans) { scan ->
                                SkinScanItemCard(scan = scan, onClick = { selectedScanForDetails = scan })
                            }
                        }
                    }

                    // TAB 4: PRESCRIPTIONS
                    4 -> {
                        val prescriptionApts = filteredAppointments.filter { it.prescriptionNotes.isNotBlank() || it.status == "COMPLETED" }
                        if (prescriptionApts.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) SurfaceVariantDark else Color(0xFFF0FDF4)),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Medication, contentDescription = null, tint = SuccessGreen)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Active E-Prescriptions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("1. Hydrocortisone 1% Topical Cream (Apply 2x daily after washing)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("2. Cetirizine 10mg Tablets (1 tablet orally at bedtime for itching)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("3. Ceramide-based Barrier Moisturizer (Apply liberally 3x daily)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Prescribed by: Dr. Sandeep Adhikari, MD • Digitally Signed & Encrypted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            items(prescriptionApts) { apt ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Medication, contentDescription = null, tint = SuccessGreen)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Rx Prescribed by ${apt.doctorName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Text(apt.appointmentDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            apt.prescriptionNotes.ifBlank { "Hydrocortisone Cream 1% + Cetirizine 10mg. Maintain skin hydration." },
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TAB 5: DOCTOR CONSULT SUMMARIES
                    5 -> {
                        if (filteredAppointments.isEmpty()) {
                            item {
                                EmptyVaultCategoryNotice(
                                    title = "No Consultation Summaries",
                                    subtitle = if (searchQuery.isNotBlank()) "No doctor consultations match '$searchQuery'." else "Schedule a video consultation to receive clinical summaries & ICD-10 diagnoses."
                                )
                            }
                        } else {
                            items(filteredAppointments) { apt ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(apt.doctorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(apt.appointmentDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(apt.doctorSpecialty, fontSize = 11.sp, color = TealPrimary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Clinical Notes: ${apt.notes}", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }

    // ADD VITAL / HEALTH REPORT MODAL
    if (showAddReportDialog) {
        var reportTitle by remember { mutableStateOf("Routine Vitals & Blood Panel") }
        var category by remember { mutableStateOf("Blood Test") }
        var doctorOrLab by remember { mutableStateOf("Kathmandu Clinical Lab / Bir Hospital") }
        var bpInput by remember { mutableStateOf("120/80 mmHg") }
        var hrInput by remember { mutableStateOf("74 bpm") }
        var spO2Input by remember { mutableStateOf("99%") }
        var sugarInput by remember { mutableStateOf("96 mg/dL") }
        var summaryNotes by remember { mutableStateOf("Normal lipid profile. Blood glucose fasting in safe range.") }

        AlertDialog(
            onDismissRequest = { showAddReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Encrypted Health Record", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = reportTitle,
                        onValueChange = { reportTitle = it },
                        label = { Text("Report Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vital_title_input")
                    )

                    OutlinedTextField(
                        value = doctorOrLab,
                        onValueChange = { doctorOrLab = it },
                        label = { Text("Lab or Hospital Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bpInput,
                            onValueChange = { bpInput = it },
                            label = { Text("Blood Pressure") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vital_bp_input")
                        )
                        OutlinedTextField(
                            value = hrInput,
                            onValueChange = { hrInput = it },
                            label = { Text("Heart Rate") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vital_hr_input")
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = spO2Input,
                            onValueChange = { spO2Input = it },
                            label = { Text("SpO2 %") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vital_spo2_input")
                        )
                        OutlinedTextField(
                            value = sugarInput,
                            onValueChange = { sugarInput = it },
                            label = { Text("Blood Sugar") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vital_sugar_input")
                        )
                    }

                    OutlinedTextField(
                        value = summaryNotes,
                        onValueChange = { summaryNotes = it },
                        label = { Text("Clinical Notes / Summary") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vital_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNewHealthReport(
                            title = reportTitle,
                            category = category,
                            doctorOrLabName = doctorOrLab,
                            summary = summaryNotes,
                            bp = bpInput,
                            heartRate = hrInput,
                            spO2 = spO2Input,
                            bloodSugar = sugarInput
                        )
                        showAddReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.testTag("confirm_add_vital_btn")
                ) {
                    Text("Encrypt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // EXPORT DIALOG
    if (showExportDialog) {
        ExportHealthDataDialog(viewModel = viewModel, onDismiss = { showExportDialog = false })
    }

    // SECURE BIOMETRIC DOCUMENT VIEWER MODAL (Room DB Decryption via AndroidX Biometric)
    if (selectedReportForDetails != null) {
        val report = selectedReportForDetails!!
        SecureDocumentViewerDialog(
            report = report,
            viewModel = viewModel,
            onDismiss = { selectedReportForDetails = null },
            onExportPdf = {
                selectedReportForDetails = null
                showExportDialog = true
            },
            onDeleteRecord = {
                viewModel.deleteHealthReport(report.id, report.title)
                selectedReportForDetails = null
            }
        )
    }

    // SKIN SCAN DETAILS MODAL
    if (selectedScanForDetails != null) {
        val scan = selectedScanForDetails!!
        AlertDialog(
            onDismissRequest = { selectedScanForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = SkinScannerPink)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(scan.conditionName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Risk Level: ${scan.riskLevel}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (scan.riskLevel == "High") ErrorRed else SuccessGreen)
                        Text("Confidence: ${(scan.confidenceScore * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Affected Area: ${scan.affectedArea}", fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("AI Clinical Assessment:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(scan.aiAnalysisSummary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Recommended Treatment:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(scan.recommendedTreatment, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Medication Advice:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(scan.medicationAdvice, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedScanForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun BiomarkersVitalsOverviewCard(reports: List<HealthReportEntity>) {
    val latestReport = reports.firstOrNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vitals_overview_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Key Biomarkers & Vitals History", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Text("SYNCED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VitalMetricTile(title = "Blood Pressure", value = latestReport?.vitalsBloodPressure ?: "120/80", unit = "mmHg", modifier = Modifier.weight(1f))
                VitalMetricTile(title = "Heart Rate", value = latestReport?.vitalsHeartRate ?: "72", unit = "bpm", modifier = Modifier.weight(1f))
                VitalMetricTile(title = "SpO2 Level", value = latestReport?.vitalsSpO2 ?: "98%", unit = "Oxygen", modifier = Modifier.weight(1f))
                VitalMetricTile(title = "Blood Sugar", value = latestReport?.vitalsBloodSugar ?: "95", unit = "mg/dL", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VitalMetricTile(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value.split(" ").firstOrNull() ?: value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TealPrimary)
            Text(unit, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HealthReportItemCard(report: HealthReportEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("health_report_card_${report.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = TealPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(report.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (report.syncStatus == "PENDING_SYNC") WarningAmber.copy(alpha = 0.18f) else SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (report.syncStatus == "PENDING_SYNC") "⏳ Pending Sync" else "💾 Room DB • Offline",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.syncStatus == "PENDING_SYNC") WarningAmber else SuccessGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text("${report.doctorOrLabName} • ${report.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("BP: ${report.vitalsBloodPressure} | Sugar: ${report.vitalsBloodSugar}", fontSize = 10.sp, color = TealPrimary)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SkinScanItemCard(scan: SkinScanEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("skin_scan_card_${scan.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = SkinScannerPink.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = SkinScannerPink, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(scan.conditionName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (scan.riskLevel == "High") ErrorRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            scan.riskLevel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scan.riskLevel == "High") ErrorRed else SuccessGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (scan.syncStatus == "PENDING_SYNC") WarningAmber.copy(alpha = 0.18f) else SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (scan.syncStatus == "PENDING_SYNC") "⏳ Pending" else "💾 Room DB",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (scan.syncStatus == "PENDING_SYNC") WarningAmber else SuccessGreen,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
                Text("Confidence: ${(scan.confidenceScore * 100).toInt()}% • Area: ${scan.affectedArea}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyVaultCategoryNotice(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun FirestoreCloudBackupCard(
    lastBackupTime: Long,
    isSyncing: Boolean,
    isOnline: Boolean = true,
    isSimulatedOffline: Boolean = false,
    onToggleSimulatedOffline: () -> Unit = {},
    onSync: () -> Unit,
    onRestore: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(lastBackupTime) {
        if (lastBackupTime > 0) dateFormat.format(Date(lastBackupTime)) else "Never Synced (Local Encrypted Only)"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("firestore_cloud_backup_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isOnline) TealPrimary else WarningAmber,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isOnline) Icons.Default.CloudSync else Icons.Default.Storage,
                                contentDescription = "Room DB Sync Status",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isOnline) "Room DB & Cloud Sync" else "Offline Room Database",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isOnline) SuccessGreen else WarningAmber
                            ) {
                                Text(
                                    text = if (isOnline) "ONLINE • AUTO-SYNC" else "OFFLINE ACTIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isOnline) "SQLCipher Room DB • Firestore multi-device backup" else "100% offline access via encrypted Room database",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = TealPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Offline Mode Test Switch
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isSimulatedOffline) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSimulatedOffline) WarningAmber else SuccessGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Simulate Offline Mode (Test Room DB)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "Disconnect cloud to test viewing medical records offline",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSimulatedOffline,
                        onCheckedChange = { onToggleSimulatedOffline() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = WarningAmber,
                            checkedTrackColor = WarningAmber.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("simulate_offline_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Last Backup Timestamp Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Last Cloud Sync:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSync,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isOnline) TealPrimary else WarningAmber),
                    modifier = Modifier.weight(1.2f).testTag("backup_to_cloud_btn")
                ) {
                    Icon(
                        if (isOnline) Icons.Default.CloudUpload else Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isOnline) "Sync Now" else "Save to Room DB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRestore,
                    enabled = !isSyncing && isOnline,
                    modifier = Modifier.weight(1f).testTag("restore_from_cloud_btn")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

