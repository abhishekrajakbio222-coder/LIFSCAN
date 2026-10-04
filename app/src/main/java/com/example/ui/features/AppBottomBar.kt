package com.example.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

sealed class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val screen: ScreenNav
) {
    object Home : BottomNavItem(
        title = "Home",
        selectedIcon = Icons.Default.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "bottom_nav_home",
        screen = ScreenNav.PatientDashboard
    )

    object Scanner : BottomNavItem(
        title = "AI Scan",
        selectedIcon = Icons.Default.DocumentScanner,
        unselectedIcon = Icons.Outlined.DocumentScanner,
        testTag = "bottom_nav_scanner",
        screen = ScreenNav.SkinScanner
    )

    object Map : BottomNavItem(
        title = "Map",
        selectedIcon = Icons.Default.Map,
        unselectedIcon = Icons.Outlined.Map,
        testTag = "bottom_nav_map",
        screen = ScreenNav.FacilitiesMap
    )

    object AIChat : BottomNavItem(
        title = "AI Chat",
        selectedIcon = Icons.Default.SmartToy,
        unselectedIcon = Icons.Outlined.SmartToy,
        testTag = "bottom_nav_ai_chat",
        screen = ScreenNav.AIChat
    )

    object Profile : BottomNavItem(
        title = "Profile",
        selectedIcon = Icons.Default.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
        testTag = "bottom_nav_profile",
        screen = ScreenNav.ProfileAndSettings
    )
}

@Composable
fun AppBottomBar(
    viewModel: LifscanViewModel,
    currentScreen: ScreenNav,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeSOS by viewModel.activeEmergencyAlert.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // Hide bottom navigation during immersive video calls or login
    if (currentScreen is ScreenNav.Login || currentScreen is ScreenNav.VideoCall) {
        return
    }

    val isPatient = currentUser == null || currentUser?.role == UserRole.PATIENT
    val isDoctor = currentUser?.role == UserRole.DOCTOR
    val isAmbulance = currentUser?.role == UserRole.AMBULANCE_DRIVER
    val isAdmin = currentUser?.role == UserRole.ADMIN

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_bottom_functional_bar"),
        containerColor = if (isDarkMode) SurfaceDark else MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        if (isAdmin) {
            val isFacilities = currentScreen is ScreenNav.AdminFacilitiesControl
            val isAudit = currentScreen is ScreenNav.AuditLogViewer
            val isMap = currentScreen is ScreenNav.FacilitiesMap
            val isEMS = currentScreen is ScreenNav.AmbulanceTracker

            NavigationBarItem(
                selected = isFacilities,
                onClick = { viewModel.navigateTo(ScreenNav.AdminFacilitiesControl) },
                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Facilities Control") },
                label = { Text("Facility Control", fontSize = 10.sp, fontWeight = if (isFacilities) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0284C7), selectedTextColor = Color(0xFF0284C7), indicatorColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
                modifier = Modifier.testTag("admin_nav_facilities")
            )
            NavigationBarItem(
                selected = isAudit,
                onClick = { viewModel.navigateTo(ScreenNav.AuditLogViewer) },
                icon = { Icon(Icons.Default.Security, contentDescription = "Security Audit") },
                label = { Text("App Logs", fontSize = 10.sp, fontWeight = if (isAudit) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0284C7), selectedTextColor = Color(0xFF0284C7), indicatorColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
                modifier = Modifier.testTag("admin_nav_audit")
            )
            NavigationBarItem(
                selected = isMap,
                onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) },
                icon = { Icon(Icons.Default.Map, contentDescription = "Live Map") },
                label = { Text("Regional Map", fontSize = 10.sp, fontWeight = if (isMap) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0284C7), selectedTextColor = Color(0xFF0284C7), indicatorColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
                modifier = Modifier.testTag("admin_nav_map")
            )
            NavigationBarItem(
                selected = isEMS,
                onClick = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) },
                icon = { Icon(Icons.Default.Emergency, contentDescription = "EMS Dispatch") },
                label = { Text("EMS Dispatch", fontSize = 10.sp, fontWeight = if (isEMS) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0284C7), selectedTextColor = Color(0xFF0284C7), indicatorColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
                modifier = Modifier.testTag("admin_nav_ems")
            )
            val isProfile = currentScreen is ScreenNav.ProfileAndSettings
            NavigationBarItem(
                selected = isProfile,
                onClick = { viewModel.navigateTo(ScreenNav.ProfileAndSettings) },
                icon = { Icon(if (isProfile) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle, contentDescription = "Admin Profile") },
                label = { Text("Profile", fontSize = 10.sp, fontWeight = if (isProfile) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF0284C7), selectedTextColor = Color(0xFF0284C7), indicatorColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
                modifier = Modifier.testTag("admin_nav_profile")
            )
        } else if (isPatient) {
            val items = listOf(
                BottomNavItem.Home,
                BottomNavItem.Scanner,
                BottomNavItem.Map,
                BottomNavItem.AIChat,
                BottomNavItem.Profile
            )

            items.forEach { item ->
                val isSelected = when (item) {
                    BottomNavItem.Home -> currentScreen is ScreenNav.PatientDashboard
                    BottomNavItem.Scanner -> currentScreen is ScreenNav.SkinScanner || currentScreen is ScreenNav.MedicalImageScan || currentScreen is ScreenNav.SymptomScanner || currentScreen is ScreenNav.GeneralHealthScan
                    BottomNavItem.Map -> currentScreen is ScreenNav.FacilitiesMap || currentScreen is ScreenNav.MappingNavigation || currentScreen is ScreenNav.AmbulanceTracker
                    BottomNavItem.AIChat -> currentScreen is ScreenNav.AIChat
                    BottomNavItem.Profile -> currentScreen is ScreenNav.ProfileAndSettings || currentScreen is ScreenNav.MedicalVault || currentScreen is ScreenNav.AuditLogViewer || currentScreen is ScreenNav.EmergencyContacts
                }

                val itemColor = MaterialTheme.colorScheme.primary

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            viewModel.navigateTo(item.screen)
                        }
                    },
                    icon = {
                        BadgedBox(badge = {
                            if (item == BottomNavItem.Map && activeSOS != null) {
                                Badge(containerColor = ErrorRed) {
                                    Text("GPS", fontSize = 8.sp, color = Color.White)
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            item.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = itemColor,
                        selectedTextColor = itemColor,
                        indicatorColor = itemColor.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        } else if (isDoctor) {
            // Doctor navigation tabs
            val isDashboard = currentScreen is ScreenNav.DoctorDashboard
            val isFacilities = currentScreen is ScreenNav.FacilitiesMap
            val isAI = currentScreen is ScreenNav.AIChat
            val isProfile = currentScreen is ScreenNav.ProfileAndSettings

            NavigationBarItem(
                selected = isDashboard,
                onClick = { viewModel.navigateTo(ScreenNav.DoctorDashboard) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Clinical Portal", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorPurple, selectedTextColor = DoctorPurple, indicatorColor = DoctorPurple.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("doctor_nav_dashboard")
            )
            NavigationBarItem(
                selected = isFacilities,
                onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) },
                icon = { Icon(Icons.Default.LocalHospital, contentDescription = "Hospitals") },
                label = { Text("Facilities", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorPurple, selectedTextColor = DoctorPurple, indicatorColor = DoctorPurple.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("doctor_nav_facilities")
            )
            NavigationBarItem(
                selected = isAI,
                onClick = { viewModel.navigateTo(ScreenNav.AIChat) },
                icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI") },
                label = { Text("Clinical AI", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorPurple, selectedTextColor = DoctorPurple, indicatorColor = DoctorPurple.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("doctor_nav_ai")
            )
            NavigationBarItem(
                selected = isProfile,
                onClick = { viewModel.navigateTo(ScreenNav.ProfileAndSettings) },
                icon = { Icon(if (isProfile) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle, contentDescription = "Doctor Profile") },
                label = { Text("Profile", fontSize = 10.sp, fontWeight = if (isProfile) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorPurple, selectedTextColor = DoctorPurple, indicatorColor = DoctorPurple.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("doctor_nav_profile")
            )
        } else if (isAmbulance) {
            // Ambulance navigation tabs
            val isAmbulanceDash = currentScreen is ScreenNav.AmbulanceDashboard
            val isTracker = currentScreen is ScreenNav.AmbulanceTracker
            val isFacilities = currentScreen is ScreenNav.FacilitiesMap
            val isProfile = currentScreen is ScreenNav.ProfileAndSettings

            NavigationBarItem(
                selected = isAmbulanceDash,
                onClick = { viewModel.navigateTo(ScreenNav.AmbulanceDashboard) },
                icon = { Icon(Icons.Default.Emergency, contentDescription = "Dispatch") },
                label = { Text("EMS Dispatch", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = EmergencyRed, selectedTextColor = EmergencyRed, indicatorColor = EmergencyRed.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("ambulance_nav_dispatch")
            )
            NavigationBarItem(
                selected = isTracker,
                onClick = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) },
                icon = { Icon(Icons.Default.Navigation, contentDescription = "Live GPS") },
                label = { Text("Live Tracker", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = EmergencyRed, selectedTextColor = EmergencyRed, indicatorColor = EmergencyRed.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("ambulance_nav_tracker")
            )
            NavigationBarItem(
                selected = isFacilities,
                onClick = { viewModel.navigateTo(ScreenNav.FacilitiesMap) },
                icon = { Icon(Icons.Default.LocalHospital, contentDescription = "Trauma Centers") },
                label = { Text("Hospitals", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = EmergencyRed, selectedTextColor = EmergencyRed, indicatorColor = EmergencyRed.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("ambulance_nav_facilities")
            )
            NavigationBarItem(
                selected = isProfile,
                onClick = { viewModel.navigateTo(ScreenNav.ProfileAndSettings) },
                icon = { Icon(if (isProfile) Icons.Default.AccountCircle else Icons.Outlined.AccountCircle, contentDescription = "Driver Profile") },
                label = { Text("Profile", fontSize = 10.sp, fontWeight = if (isProfile) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = EmergencyRed, selectedTextColor = EmergencyRed, indicatorColor = EmergencyRed.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("ambulance_nav_profile")
            )
        }
    }
}
