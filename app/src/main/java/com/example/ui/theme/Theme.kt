package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val GreenDarkColorScheme =
  darkColorScheme(
    primary = HealthGreenAccent,
    onPrimary = BackgroundDark,
    primaryContainer = HealthGreenDark,
    onPrimaryContainer = HealthGreenLight,
    secondary = TealAccent,
    onSecondary = SurfaceLight,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = WarningAmber,
    error = EmergencyRed,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
  )

private val GreenLightColorScheme =
  lightColorScheme(
    primary = HealthGreenPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = HealthGreenLight,
    onPrimaryContainer = HealthGreenDark,
    secondary = TealPrimary,
    onSecondary = SurfaceLight,
    secondaryContainer = HealthGreenContainer,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = WarningAmber,
    error = EmergencyRed,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
  )

private val BlueDarkColorScheme =
  darkColorScheme(
    primary = MedicalBlueAccent,
    onPrimary = BackgroundDark,
    primaryContainer = MedicalBlueDark,
    onPrimaryContainer = MedicalBlueLight,
    secondary = InfoBlue,
    onSecondary = SurfaceLight,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = WarningAmber,
    error = EmergencyRed,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
  )

private val BlueLightColorScheme =
  lightColorScheme(
    primary = MedicalBluePrimary,
    onPrimary = SurfaceLight,
    primaryContainer = MedicalBlueLight,
    onPrimaryContainer = MedicalBlueDark,
    secondary = InfoBlue,
    onSecondary = SurfaceLight,
    secondaryContainer = MedicalBlueContainer,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = WarningAmber,
    error = EmergencyRed,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeMode: HealthThemeMode = HealthThemeMode.HEALTH_GREEN,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      themeMode == HealthThemeMode.MEDICAL_BLUE -> {
        if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
      }
      else -> {
        if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
      }
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

