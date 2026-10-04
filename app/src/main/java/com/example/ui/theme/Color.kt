package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Primary (Teal / Medical Cyan)
val TealPrimary = Color(0xFF007A87)
val TealDark = Color(0xFF00545D)
val TealLight = Color(0xFFE0F2F1)
val TealAccent = Color(0xFF00B4D8)

// Emergency / SOS Crimson
val EmergencyRed = Color(0xFFD90429)
val EmergencyRedDark = Color(0xFF9B0014)
val EmergencyRedLight = Color(0xFFFFEBEE)
val ErrorRed = EmergencyRed
val ErrorRedLight = EmergencyRedLight

// Status & Accent Colors
val SuccessGreen = Color(0xFF0F9D58)
val SuccessGreenLight = Color(0xFFE8F5E9)
val WarningAmber = Color(0xFFF59E0B)
val WarningAmberLight = Color(0xFFFEF3C7)
val AmberWarning = WarningAmber
val AmberWarningLight = WarningAmberLight
val InfoBlue = Color(0xFF2563EB)
val InfoBlueLight = Color(0xFFEFF6FF)

// Neutrals & Surfaces (Light)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)
val BorderLight = Color(0xFFE2E8F0)

// Neutrals & Surfaces (Dark)
val BackgroundDark = Color(0xFF0B132B)
val SurfaceDark = Color(0xFF1C2541)
val SurfaceVariantDark = Color(0xFF2A3656)
val SurfaceDarkElevated = Color(0xFF243054)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFFCBD5E1)
val TextMutedDark = Color(0xFF64748B)
val BorderDark = Color(0xFF334155)

// Specialty Colors
val DoctorPurple = Color(0xFF7C3AED)
val DoctorPurpleLight = Color(0xFFF3E8FF)
val AmbulanceOrange = Color(0xFFEA580C)
val SkinScannerPink = Color(0xFFE11D48)
val SkinScannerPinkLight = Color(0xFFFFE4E6)
val NepalCrimson = Color(0xFFDC143C)
val SuccessGreenDark = Color(0xFF065F46)

// Dynamic Color Scheme Modes
enum class HealthThemeMode(val displayName: String, val description: String) {
    HEALTH_GREEN("Health Green", "Calming emerald and restorative teal"),
    MEDICAL_BLUE("Medical Blue", "High-contrast clinical and royal blue")
}

// Calming Health Green Palette
val HealthGreenPrimary = Color(0xFF059669)
val HealthGreenDark = Color(0xFF065F46)
val HealthGreenLight = Color(0xFFD1FAE5)
val HealthGreenAccent = Color(0xFF10B981)
val HealthGreenContainer = Color(0xFFECFDF5)

// High-Contrast Medical Blue Palette
val MedicalBluePrimary = Color(0xFF1D4ED8)
val MedicalBlueDark = Color(0xFF1E3A8A)
val MedicalBlueLight = Color(0xFFDBEAFE)
val MedicalBlueAccent = Color(0xFF3B82F6)
val MedicalBlueContainer = Color(0xFFEFF6FF)

