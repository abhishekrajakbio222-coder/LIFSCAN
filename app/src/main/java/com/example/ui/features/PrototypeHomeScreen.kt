package com.example.ui.features

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

/**
 * Prototype Home Page Modes matching the 3 mockup screens:
 * 1. FACILITIES (Center Screen): 2-column grid of rounded cards (Equipment, Shut Offs, Inspections, Training, Documents, Contacts)
 * 2. CMMS (Left Screen): Issue by Status bar charts, Work Order by Type concentric radial chart, Weekly Tasks
 * 3. SAFETY (Right Screen): Health Check verified records, compliance status, facility timestamps
 */
enum class PrototypeHomeView {
    FACILITIES,
    CMMS,
    SAFETY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrototypeHomeScreen(viewModel: LifscanViewModel) {
    var currentView by remember { mutableStateOf(PrototypeHomeView.FACILITIES) }
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activeSOS by viewModel.activeEmergencyAlert.collectAsState()

    val user = currentUser ?: UserEntity(
        id = "demo_patient",
        phone = "+977-9841234567",
        countryCode = "+977",
        countryName = "Nepal",
        name = "Aayush Shrestha",
        role = UserRole.PATIENT
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF4F6F8))
            .testTag("prototype_home_root")
    ) {
        // Quick Screen Switcher Pill Tabs (Facilities, CMMS, Safety)
        Surface(
            color = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFE2E8F0),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple(PrototypeHomeView.CMMS, "CMMS Analytics", Icons.Default.BarChart),
                    Triple(PrototypeHomeView.FACILITIES, "Facilities Hub", Icons.Default.Apartment),
                    Triple(PrototypeHomeView.SAFETY, "Safety & Health", Icons.Default.HealthAndSafety)
                ).forEach { (viewMode, title, icon) ->
                    val isSelected = currentView == viewMode
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) {
                            if (viewMode == PrototypeHomeView.FACILITIES) Color(0xFF22C55E) else Color(0xFF1E293B)
                        } else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { currentView = viewMode }
                            .testTag("tab_${viewMode.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = title,
                                tint = if (isSelected) Color.White else if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // Active Emergency SOS Notice Banner
        if (activeSOS != null) {
            Surface(
                color = EmergencyRed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CRITICAL SOS ACTIVE: Immediate emergency dispatch broadcast in progress",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.navigateTo(ScreenNav.AmbulanceTracker) }) {
                        Text("TRACK", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Animated View Transition
        AnimatedContent(
            targetState = currentView,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "prototype_view_anim"
        ) { viewState ->
            when (viewState) {
                PrototypeHomeView.FACILITIES -> FacilitiesGridView(viewModel = viewModel, user = user, isDarkMode = isDarkMode)
                PrototypeHomeView.CMMS -> CMMSDashboardView(viewModel = viewModel, user = user, isDarkMode = isDarkMode)
                PrototypeHomeView.SAFETY -> SafetyHealthCheckView(viewModel = viewModel, user = user, isDarkMode = isDarkMode)
            }
        }
    }
}

// =========================================================================
// VIEW 1: FACILITIES HUB (Center Screen from Prototype Mockup)
// =========================================================================
@Composable
fun FacilitiesGridView(
    viewModel: LifscanViewModel,
    user: UserEntity,
    isDarkMode: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF4F6F8))
    ) {
        // Curved Green Header Banner (Matching Center Phone)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF22C55E),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Icon Badge / Logo
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Apartment,
                            contentDescription = "Facilities Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Centered Title
                Text(
                    text = "Facilities",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Right Menu Button / Profile
                IconButton(
                    onClick = { viewModel.navigateTo(ScreenNav.ProfileAndSettings) },
                    modifier = Modifier.testTag("facilities_header_menu_btn")
                ) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Options Menu",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Role Indicator Sub-banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome, ${user.name}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF1E293B)
                )
                Text(
                    text = "${user.role.displayName} Portal • ${user.countryName}",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF22C55E).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.3f))
            ) {
                Text(
                    text = "ONLINE",
                    color = Color(0xFF16A34A),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Direct Call System (Emergency Hotlines & Contact Speed-Dial)
        HomeDirectCallSystemCard(
            viewModel = viewModel,
            isDarkMode = isDarkMode
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2-Column Grid of Rounded Elevated Cards (Directly matching mockup)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Row 1: Equipment & Shut Offs
            Row(modifier = Modifier.fillMaxWidth()) {
                FacilityActionCard(
                    title = "Equipment",
                    icon = Icons.Default.Settings,
                    iconTint = Color(0xFF0284C7),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_equipment"),
                    onClick = {
                        when (user.role) {
                            UserRole.DOCTOR -> viewModel.navigateTo(ScreenNav.SkinScanner)
                            UserRole.AMBULANCE_DRIVER -> viewModel.navigateTo(ScreenNav.AmbulanceDashboard)
                            else -> viewModel.navigateTo(ScreenNav.MedicalDocumentScan)
                        }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                FacilityActionCard(
                    title = "Shut Offs",
                    icon = Icons.Default.Emergency,
                    iconTint = Color(0xFFEF4444),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_shut_offs"),
                    onClick = {
                        viewModel.navigateTo(ScreenNav.EmergencyContacts)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Inspections & Emergency Training
            Row(modifier = Modifier.fillMaxWidth()) {
                FacilityActionCard(
                    title = "Inspections",
                    icon = Icons.Default.Search,
                    iconTint = Color(0xFF0284C7),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_inspections"),
                    onClick = {
                        viewModel.navigateTo(ScreenNav.ConsultationScheduler)
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                FacilityActionCard(
                    title = "Emergency Training",
                    icon = Icons.Default.AccountTree,
                    iconTint = Color(0xFFF59E0B),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_emergency_training"),
                    onClick = {
                        viewModel.navigateTo(ScreenNav.AIChat)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 3: Emergency Documents & Emergency Contact
            Row(modifier = Modifier.fillMaxWidth()) {
                FacilityActionCard(
                    title = "Emergency Documents",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconTint = Color(0xFFEC4899),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_emergency_documents"),
                    onClick = {
                        viewModel.navigateTo(ScreenNav.MedicalVault)
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                FacilityActionCard(
                    title = "Emergency Contact",
                    icon = Icons.Default.Phone,
                    iconTint = Color(0xFFEAB308),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_emergency_contact"),
                    onClick = {
                        viewModel.navigateTo(ScreenNav.EmergencyContacts)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 4: Hospital Facilities Map & Live Telemetry
            Row(modifier = Modifier.fillMaxWidth()) {
                FacilityActionCard(
                    title = "Hospital Facilities",
                    icon = Icons.Default.LocalHospital,
                    iconTint = Color(0xFF10B981),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_facilities_map"),
                    onClick = {
                        viewModel.navigateTo(ScreenNav.FacilitiesMap)
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                FacilityActionCard(
                    title = "Ambulance Tracker",
                    icon = Icons.Default.Navigation,
                    iconTint = Color(0xFF8B5CF6),
                    isDarkMode = isDarkMode,
                    modifier = Modifier.weight(1f).testTag("card_ambulance_tracker"),
                    onClick = {
                        viewModel.navigateTo(ScreenNav.AmbulanceTracker)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Individual Facility Square Raised Card with icon & label
 */
@Composable
fun FacilityActionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color.White else Color(0xFF334155),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================================
// VIEW 2: CMMS ANALYTICS (Left Screen from Prototype Mockup)
// =========================================================================
@Composable
fun CMMSDashboardView(
    viewModel: LifscanViewModel,
    user: UserEntity,
    isDarkMode: Boolean
) {
    val scrollState = rememberScrollState()
    val bgCanvasColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF4F6F8)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(bgCanvasColor)
    ) {
        // Dark Navy Header (Matching Left Phone)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                }

                Text(
                    text = "CMMS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { viewModel.navigateTo(ScreenNav.AuditLogViewer) }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Back / Subtitle: "<- Dashboard"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dashboard", tint = textSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Dashboard", fontSize = 14.sp, color = textSecondary, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // =========================================================================
        // CARD 1: ISSUE BY STATUS (BAR CHART)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("chart_issue_by_status")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Issue by Status",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Chart Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(color = Color(0xFF3B82F6), label = "Fire Alarms/Sprinklers")
                    LegendItem(color = Color(0xFF10B981), label = "HVAC")
                    LegendItem(color = Color(0xFFF59E0B), label = "Gen. Life")
                    LegendItem(color = Color(0xFFEF4444), label = "Emergency")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Multi-bar Mockup Stacked Bar Chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val barData = listOf(
                        Triple(0.2f, 0.4f, 0.3f),
                        Triple(0.6f, 0.3f, 0.5f),
                        Triple(0.4f, 0.7f, 0.6f),
                        Triple(0.8f, 0.5f, 0.4f),
                        Triple(0.5f, 0.8f, 0.7f),
                        Triple(0.3f, 0.6f, 0.9f),
                        Triple(0.7f, 0.4f, 0.3f),
                        Triple(0.9f, 0.6f, 0.5f),
                        Triple(0.4f, 0.3f, 0.2f)
                    )

                    barData.forEachIndexed { index, (h1, h2, h3) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // Stacked bar segments
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(h1)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(h2 * 0.5f)
                                    .background(Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .fillMaxHeight(h3 * 0.4f)
                                    .background(Color(0xFFEF4444))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // CARD 2: WORK ORDER BY TYPE (RADIAL CONCENTRIC CHART)
        // =========================================================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("chart_work_order_by_type")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Work Order by Type",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        val strokeWidth = 10.dp.toPx()

                        // Outer ring (Blue 0.8)
                        drawArc(
                            color = Color(0xFFE2E8F0),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )
                        drawArc(
                            color = Color(0xFF38BDF8),
                            startAngle = -90f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = Size(size.width, size.height)
                        )

                        // Middle ring (Green 0.6)
                        val inset1 = 18.dp.toPx()
                        drawArc(
                            color = Color(0xFF10B981),
                            startAngle = -90f,
                            sweepAngle = 190f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(inset1, inset1),
                            size = Size(size.width - inset1 * 2, size.height - inset1 * 2)
                        )

                        // Inner ring (Yellow 0.4)
                        val inset2 = 36.dp.toPx()
                        drawArc(
                            color = Color(0xFFF59E0B),
                            startAngle = -90f,
                            sweepAngle = 130f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(inset2, inset2),
                            size = Size(size.width - inset2 * 2, size.height - inset2 * 2)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("86%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                        Text("Resolved", fontSize = 10.sp, color = textSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // CARD 3: WEEKLY TASKS BY PRIORITY
        // =========================================================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("chart_weekly_tasks")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Tasks by Priority",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                listOf(
                    Triple("Critical Emergency Response", "4 Active Tasks", EmergencyRed),
                    Triple("High Priority Inspections", "9 Pending Tasks", Color(0xFFF59E0B)),
                    Triple("Routine Facility Maintenance", "18 Scheduled Tasks", Color(0xFF10B981))
                ).forEach { (task, count, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(task, fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                        }
                        Text(count, fontSize = 12.sp, color = textSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            label,
            fontSize = 9.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =========================================================================
// VIEW 3: SAFETY & HEALTH CHECK (Right Screen from Prototype Mockup)
// =========================================================================
@Composable
fun SafetyHealthCheckView(
    viewModel: LifscanViewModel,
    user: UserEntity,
    isDarkMode: Boolean
) {
    val scrollState = rememberScrollState()
    val bgCanvasColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF4F6F8)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(bgCanvasColor)
    ) {
        // Navy Header (Matching Right Phone)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                }

                Text(
                    text = "Safety",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { viewModel.navigateTo(ScreenNav.ProfileAndSettings) }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Back Navigation: "<- Health Check"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Health Check", tint = textSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Health Check", fontSize = 14.sp, color = textSecondary, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Patient / Staff Health Check Verification Records (Matching Right Screen)
        val safetyRecords = listOf(
            SafetyRecordItem(
                name = "Linda L. Humphreys",
                dateTime = "Fri 24 Jul 2026 01:21 PM",
                isCompliant = false,
                facility = "Bir Hospital Trauma Hub",
                roleTitle = "Senior Clinical Staff"
            ),
            SafetyRecordItem(
                name = "Richard R. Lockhart",
                dateTime = "Sat 18 Jul 2026 05:21 PM",
                isCompliant = true,
                facility = "Kathmandu Medical Center",
                roleTitle = "Emergency Resuscitation Unit"
            ),
            SafetyRecordItem(
                name = user.name,
                dateTime = "Today 09:15 AM",
                isCompliant = true,
                facility = "Don Thornton Critical Hub",
                roleTitle = "${user.role.displayName} Triage Entry"
            ),
            SafetyRecordItem(
                name = "Dr. Sandeep Adhikari, MD",
                dateTime = "Yesterday 04:30 PM",
                isCompliant = true,
                facility = "Nepal Skin & Burn Care",
                roleTitle = "Attending Physician"
            )
        )

        safetyRecords.forEach { record ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("safety_record_${record.name.replace(" ", "_")}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = record.name,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = record.roleTitle,
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }

                        // Pass / Fail badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (record.isCompliant) Color(0xFF10B981).copy(alpha = 0.15f) else EmergencyRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (record.isCompliant) "PASSED" else "PENDING",
                                color = if (record.isCompliant) Color(0xFF10B981) else EmergencyRed,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Date & Time",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                    Text(
                        text = record.dateTime,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Compliant on COVID-19 / Health Check",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                    Text(
                        text = if (record.isCompliant) "Yes (Verified Temperature & Mask)" else "No (Requires Rapid Screening)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record.isCompliant) Color(0xFF10B981) else EmergencyRed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Facility",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                    Text(
                        text = record.facility,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

data class SafetyRecordItem(
    val name: String,
    val dateTime: String,
    val isCompliant: Boolean,
    val facility: String,
    val roleTitle: String
)

/**
 * Direct Emergency Call System in Home Screen
 * Provides instant 1-tap dialer triggers for emergency hotlines and saved emergency contacts.
 */
@Composable
fun HomeDirectCallSystemCard(
    viewModel: LifscanViewModel,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryContact by viewModel.primaryEmergencyContact.collectAsState()
    val emergencyContacts by viewModel.patientEmergencyContacts.collectAsState()
    var showCustomDialer by remember { mutableStateOf(false) }

    fun launchDirectCall(number: String, label: String) {
        try {
            val cleanNumber = number.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
            context.startActivity(intent)
            Toast.makeText(context, "Opening Phone Dialer: $label ($number)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1F2430) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            1.dp,
            if (isDarkMode) Color(0xFFEF4444).copy(alpha = 0.35f) else Color(0xFFFCA5A5).copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_direct_call_system_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmergencyRed,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhoneInTalk,
                                contentDescription = "Direct Call",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DIRECT CALL SYSTEM",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = EmergencyRed,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF22C55E).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "24/7 LIVE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Instant 1-Tap Emergency Hotlines & Contacts",
                            fontSize = 10.5.sp,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCustomDialer = true }
                        .testTag("open_custom_dialer_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Dialpad,
                            contentDescription = "Custom Dial Pad",
                            tint = if (isDarkMode) Color.White else Color(0xFF334155),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dial Pad",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color.White else Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Direct Call Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Ambulance 102
                DirectCallPillButton(
                    title = "102",
                    subtitle = "Ambulance",
                    icon = Icons.Default.AirportShuttle,
                    bgColor = EmergencyRed,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f).testTag("direct_call_102"),
                    onClick = { launchDirectCall("102", "Ambulance EMS") }
                )

                // Button 2: Police 100
                DirectCallPillButton(
                    title = "100",
                    subtitle = "Police",
                    icon = Icons.Default.LocalPolice,
                    bgColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f).testTag("direct_call_100"),
                    onClick = { launchDirectCall("100", "Nepal Police") }
                )

                // Button 3: Disaster/EMS 112
                DirectCallPillButton(
                    title = "112",
                    subtitle = "Disaster",
                    icon = Icons.Default.Emergency,
                    bgColor = Color(0xFFD97706),
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f).testTag("direct_call_112"),
                    onClick = { launchDirectCall("112", "National Disaster EMS") }
                )

                // Button 4: Primary Emergency Contact or Doctor
                val contactTarget = primaryContact ?: emergencyContacts.firstOrNull()
                val targetPhone = contactTarget?.phone ?: "+977-9841234567"
                val targetName = contactTarget?.name?.split(" ")?.firstOrNull() ?: "Contact"
                DirectCallPillButton(
                    title = targetName,
                    subtitle = "Emergency",
                    icon = Icons.Default.ContactPhone,
                    bgColor = Color(0xFF10B981),
                    contentColor = Color.White,
                    modifier = Modifier.weight(1.1f).testTag("direct_call_contact"),
                    onClick = { launchDirectCall(targetPhone, targetName) }
                )
            }
        }
    }

    if (showCustomDialer) {
        DirectCustomDialerDialog(
            onDismiss = { showCustomDialer = false },
            onCall = { number ->
                launchDirectCall(number, "Custom Emergency Call")
                showCustomDialer = false
            },
            isDarkMode = isDarkMode
        )
    }
}

/**
 * Individual High-Contrast Speed Dial Tile
 */
@Composable
fun DirectCallPillButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = "$title $subtitle",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = contentColor.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/**
 * Custom Direct Emergency Dial Pad Dialog
 */
@Composable
fun DirectCustomDialerDialog(
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    isDarkMode: Boolean
) {
    var dialInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = EmergencyRed,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhoneForwarded, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Direct Phone Dialer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Instant call to any hospital or contact", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dialInput,
                    onValueChange = { dialInput = it },
                    placeholder = { Text("Enter number (e.g. 102 or +977-...)") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Dialpad, contentDescription = null, tint = EmergencyRed)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("custom_dial_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Presets:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "102" to "EMS",
                        "100" to "Police",
                        "112" to "Disaster",
                        "01-4221119" to "Bir Hospital"
                    ).forEach { (preset, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { dialInput = preset }
                        ) {
                            Text(
                                text = "$label ($preset)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dialInput.isNotBlank()) {
                        onCall(dialInput)
                    }
                },
                enabled = dialInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("launch_custom_direct_call_btn")
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        }
    )
}
