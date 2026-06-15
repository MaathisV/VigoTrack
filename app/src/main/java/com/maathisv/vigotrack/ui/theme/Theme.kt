package com.maathisv.vigotrack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SkyBlue,
    tertiary = Rose,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF2D2D2D),
    surfaceVariant = Color(0xFF2D2D2D)

    // Old defaults:
    // primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SkyBlue,
    tertiary = Rose,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFF5F5F5),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)

    /* Old defaults:
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    */
)

@Composable
fun VigoTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
}
