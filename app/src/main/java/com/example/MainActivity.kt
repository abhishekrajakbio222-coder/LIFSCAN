package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.ui.admin.AdminFacilitiesControlScreen
import com.example.ui.ambulance.AmbulanceDashboardScreen
import com.example.ui.auth.BiometricGateScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.components.FloatingEmergencySOSButton
import com.example.ui.components.FloatingFeedbackButton
import com.example.ui.doctor.DoctorDashboardScreen
import com.example.ui.features.AIChatScreen
import com.example.ui.features.AmbulanceTrackerScreen
import com.example.ui.features.AppBottomBar
import com.example.ui.features.AuditLogScreen
import com.example.ui.features.ChatDetailScreen
import com.example.ui.features.ConsultationSchedulerScreen
import com.example.ui.features.EmergencyContactsScreen
import com.example.ui.features.EmergencySOSDashboardScreen
import com.example.ui.features.FacilitiesMapScreen
import com.example.ui.features.GeneralHealthScanScreen
import com.example.ui.features.HealthDashboardScreen
import com.example.ui.features.MappingNavigationScreen
import com.example.ui.features.MedicalDocumentScanScreen
import com.example.ui.features.MedicalImageScanScreen
import com.example.ui.features.MedicalVaultScreen
import com.example.ui.features.MedicationScheduleScreen
import com.example.ui.features.ProfileAndSettingsScreen
import com.example.ui.features.PrototypeHomeScreen
import com.example.ui.features.SkinScannerScreen
import com.example.ui.features.VideoCallScreen
import com.example.ui.patient.PatientDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

class MainActivity : FragmentActivity() {

    private val viewModel: LifscanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode, themeMode = themeMode) {
                LifscanApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LifscanApp(viewModel: LifscanViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomBar(viewModel = viewModel, currentScreen = currentScreen)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                when (screen) {
                    is ScreenNav.Login -> LoginScreen(viewModel = viewModel)
                    is ScreenNav.PatientDashboard -> PatientDashboardScreen(viewModel = viewModel)
                    is ScreenNav.DoctorDashboard -> DoctorDashboardScreen(viewModel = viewModel)
                    is ScreenNav.AmbulanceDashboard -> AmbulanceDashboardScreen(viewModel = viewModel)
                    is ScreenNav.AdminFacilitiesControl -> AdminFacilitiesControlScreen(viewModel = viewModel)
                    is ScreenNav.SkinScanner -> SkinScannerScreen(viewModel = viewModel)
                    is ScreenNav.MedicalImageScan -> MedicalImageScanScreen(viewModel = viewModel)
                    is ScreenNav.MedicalDocumentScan -> MedicalDocumentScanScreen(viewModel = viewModel)
                    is ScreenNav.SymptomScanner -> SkinScannerScreen(viewModel = viewModel)
                    is ScreenNav.ConsultationScheduler -> ConsultationSchedulerScreen(viewModel = viewModel)
                    is ScreenNav.MedicalVault -> MedicalVaultScreen(viewModel = viewModel)
                    is ScreenNav.ProfileAndSettings -> ProfileAndSettingsScreen(viewModel = viewModel)
                    is ScreenNav.MedicationSchedule -> MedicationScheduleScreen(viewModel = viewModel)
                    is ScreenNav.HealthDashboard -> HealthDashboardScreen(viewModel = viewModel)
                    is ScreenNav.EmergencyContacts -> EmergencyContactsScreen(viewModel = viewModel)
                    is ScreenNav.EmergencySOSDashboard -> EmergencySOSDashboardScreen(viewModel = viewModel)
                    is ScreenNav.AuditLogViewer -> AuditLogScreen(viewModel = viewModel)
                    is ScreenNav.AmbulanceTracker -> AmbulanceTrackerScreen(viewModel = viewModel)
                    is ScreenNav.GeneralHealthScan -> GeneralHealthScanScreen(viewModel = viewModel)
                    is ScreenNav.AIChat -> AIChatScreen(viewModel = viewModel)
                    is ScreenNav.FacilitiesMap -> FacilitiesMapScreen(viewModel = viewModel)
                    is ScreenNav.MappingNavigation -> MappingNavigationScreen(
                        viewModel = viewModel,
                        destinationName = screen.facilityName,
                        destinationAddress = screen.address,
                        destinationLat = screen.latitude,
                        destinationLng = screen.longitude,
                        facilityType = screen.facilityType,
                        initialDistanceKm = screen.distanceKm,
                        isEmergency = screen.isEmergency,
                        contactPhone = screen.phone
                    )
                    is ScreenNav.VideoCall -> VideoCallScreen(appointment = screen.appointment, viewModel = viewModel)
                    is ScreenNav.ChatDetail -> ChatDetailScreen(
                        conversationId = screen.conversationId,
                        otherUserName = screen.otherUserName,
                        otherUserId = screen.otherUserId,
                        otherRole = screen.otherRole,
                        viewModel = viewModel
                    )
                    else -> PatientDashboardScreen(viewModel = viewModel)
                }
            }

            // Global Movable Feedback & Dev Suggestions Button
            FloatingFeedbackButton(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 12.dp, start = 4.dp)
            )

            // Global High-Visibility Red Floating Emergency SOS Button (Movable)
            FloatingEmergencySOSButton(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 4.dp)
            )
        }
    }
}
