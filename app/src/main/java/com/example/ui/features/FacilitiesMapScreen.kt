package com.example.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MedicalFacilityEntity
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import com.example.util.GoogleMapsHelper
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilitiesMapScreen(viewModel: LifscanViewModel) {
    val context = LocalContext.current
    val facilities by viewModel.allFacilities.collectAsState()
    val filterType by viewModel.facilityFilterType.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filterOptions = listOf(
        "ALL" to "All Facilities",
        "GOVT_HOSPITAL" to "Govt Hospitals",
        "PRIVATE_HOSPITAL" to "Private Hospitals",
        "LIVE_AMBULANCE" to "Live Ambulances",
        "PHARMACY" to "24/7 Pharmacies",
        "CLINIC" to "Specialist Clinics"
    )

    val filteredList = facilities.filter { fac ->
        val matchesCategory = (filterType == "ALL" || fac.facilityType == filterType)
        val matchesSearch = fac.name.contains(searchQuery, ignoreCase = true) ||
                fac.address.contains(searchQuery, ignoreCase = true) ||
                fac.services.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

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
                                Icon(Icons.Default.Map, contentDescription = "Map", tint = TealPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Medical Facilities & Live Map", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Real-time GPS Proximity Network", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("facilities_theme_toggle_btn")
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Toggle Health Green / Medical Blue",
                            tint = if (themeMode == HealthThemeMode.HEALTH_GREEN) HealthGreenPrimary else MedicalBluePrimary
                        )
                    }
                    IconButton(
                        onClick = {
                            val nearest = facilities.minByOrNull { it.distanceKm } ?: facilities.firstOrNull()
                            if (nearest != null) {
                                GoogleMapsHelper.openGoogleMaps(context, nearest.latitude, nearest.longitude, nearest.name)
                            }
                        },
                        modifier = Modifier.testTag("facilities_open_gmaps_header")
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Open Google Maps", tint = MaterialTheme.colorScheme.primary)
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
            // Interactive Map Simulator Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulated GPS Radar Grid
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "User Location", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("You are in Kathmandu Center", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${facilities.size} Facilities Active in 5km Radius", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Floating GPS live marker
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live GPS Sync", color = Color.White, fontSize = 10.sp)
                        }
                    }

                    // Quick launch buttons
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val nearest = facilities.minByOrNull { it.distanceKm } ?: facilities.firstOrNull()
                                if (nearest != null) {
                                    GoogleMapsHelper.openGoogleMaps(context, nearest.latitude, nearest.longitude, nearest.name)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Google Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val nearest = facilities.minByOrNull { it.distanceKm } ?: facilities.firstOrNull()
                                if (nearest != null) {
                                    viewModel.navigateTo(
                                        ScreenNav.MappingNavigation(
                                            facilityName = nearest.name,
                                            address = nearest.address,
                                            latitude = nearest.latitude,
                                            longitude = nearest.longitude,
                                            facilityType = nearest.facilityType,
                                            distanceKm = nearest.distanceKm,
                                            isEmergency = true,
                                            phone = nearest.phone
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("In-App Live Route", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search hospital, ambulance, or pharmacy...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("facility_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filterOptions) { (key, label) ->
                    val isSelected = filterType == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFacilityFilter(key) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Facilities List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredList) { facility ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when (facility.facilityType) {
                                        "GOVT_HOSPITAL" -> TealLight
                                        "PRIVATE_HOSPITAL" -> InfoBlueLight
                                        "LIVE_AMBULANCE" -> EmergencyRedLight
                                        "PHARMACY" -> SuccessGreenLight
                                        else -> WarningAmberLight
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (facility.facilityType) {
                                                "GOVT_HOSPITAL", "PRIVATE_HOSPITAL" -> Icons.Default.LocalHospital
                                                "LIVE_AMBULANCE" -> Icons.Default.Emergency
                                                "PHARMACY" -> Icons.Default.Medication
                                                else -> Icons.Default.MedicalServices
                                            },
                                            contentDescription = null,
                                            tint = when (facility.facilityType) {
                                                "GOVT_HOSPITAL" -> TealDark
                                                "PRIVATE_HOSPITAL" -> InfoBlue
                                                "LIVE_AMBULANCE" -> EmergencyRed
                                                "PHARMACY" -> SuccessGreen
                                                else -> WarningAmber
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(facility.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(facility.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TealPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "${facility.distanceKm} km",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TealPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(facility.services, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = WarningAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${facility.rating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    if (facility.isOpen24x7) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SuccessGreenLight
                                        ) {
                                            Text("24/7 OPEN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreenDark, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            GoogleMapsHelper.dialPhoneNumber(context, facility.phone)
                                            viewModel.showFeedback("Dialing ${facility.name}: ${facility.phone}")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp).testTag("call_facility_btn_${facility.id}")
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Call", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            GoogleMapsHelper.openGoogleMaps(context, facility.latitude, facility.longitude, facility.name)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp).testTag("gmaps_facility_btn_${facility.id}")
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = "Google Maps", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
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
                                                    isEmergency = facility.facilityType == "LIVE_AMBULANCE",
                                                    phone = facility.phone
                                                )
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp).testTag("navigate_to_facility_btn_${facility.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Directions, contentDescription = "Route", modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Route", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
