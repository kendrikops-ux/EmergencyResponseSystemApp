package com.example.emergencyresponsesystem.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmergencyColorScheme = darkColorScheme(
    primary = EmergencyRed,
    onPrimary = Color.White,
    primaryContainer = EmergencyRedDark,
    onPrimaryContainer = Color.White,
    secondary = EmergencyAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3A301C),
    onSecondaryContainer = EmergencyAmber,
    tertiary = EmergencyBlue,
    background = EmergencyDarkBg,
    onBackground = EmergencyTextLight,
    surface = EmergencySurface,
    onSurface = EmergencyTextLight,
    surfaceVariant = EmergencyCardBg,
    onSurfaceVariant = EmergencyTextGray,
    error = EmergencyRed,
    onError = Color.White
)

@Composable
fun EMERGENCYRESPONSESYSTEMTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EmergencyColorScheme,
        typography = Typography,
        content = content
    )
}
