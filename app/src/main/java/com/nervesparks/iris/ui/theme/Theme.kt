package com.nervesparks.iris.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

package com.nervesparks.iris.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Samsung Galaxy S24 Ultra optimized dark color scheme
private val SamsungDarkColorScheme = darkColorScheme(
    primary = IrisPrimary,
    onPrimary = OnPrimary,
    primaryContainer = IrisPrimaryVariant,
    onPrimaryContainer = OnPrimary,
    inversePrimary = IrisPrimary,
    secondary = IrisSecondary,
    onSecondary = OnSecondary,
    secondaryContainer = IrisSecondaryVariant,
    onSecondaryContainer = OnSecondary,
    tertiary = IrisTertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = IrisTertiaryVariant,
    onTertiaryContainer = OnTertiary,
    background = IrisDark, // Pure black for AMOLED power efficiency
    onBackground = OnBackground,
    surface = IrisSurface,
    onSurface = OnSurface,
    surfaceVariant = IrisSurfaceVariant,
    onSurfaceVariant = IrisTextSecondary,
    surfaceTint = IrisPrimary,
    inverseSurface = IrisLight,
    inverseOnSurface = IrisTextPrimaryLight,
    error = IrisError,
    onError = OnError,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor,
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3B3B3B),
    surfaceContainer = IrisCard,
    surfaceContainerHigh = IrisCardElevated,
    surfaceContainerHighest = Color(0xFF363636),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainerLowest = Color(0xFF0F0F0F),
    surfaceDim = Color(0xFF111111)
)

// Samsung Galaxy S24 Ultra optimized light color scheme
private val SamsungLightColorScheme = lightColorScheme(
    primary = IrisPrimary,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFDFE1FF),
    onPrimaryContainer = Color(0xFF001C3B),
    inversePrimary = IrisPrimary,
    secondary = IrisSecondary,
    onSecondary = OnSecondary,
    secondaryContainer = Color(0xFFE6DEFF),
    onSecondaryContainer = Color(0xFF1E0F47),
    tertiary = IrisTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB8F1FF),
    onTertiaryContainer = Color(0xFF001F26),
    background = IrisLightBackground,
    onBackground = IrisTextPrimaryLight,
    surface = IrisLightSurface,
    onSurface = IrisTextPrimaryLight,
    surfaceVariant = IrisLightSurfaceVariant,
    onSurfaceVariant = IrisTextSecondaryLight,
    surfaceTint = IrisPrimary,
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    error = IrisError,
    onError = OnError,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFDF8FD),
    surfaceContainer = IrisLightCard,
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDDD8E1)
)
    @Composable
fun IrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Samsung Galaxy S24 Ultra supports dynamic color (Android 12+)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Use dynamic color on Samsung Galaxy S24 Ultra for system integration
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Fallback to custom Samsung-optimized schemes
        darkTheme -> SamsungDarkColorScheme
        else -> SamsungLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Samsung Galaxy S24 Ultra status bar optimization
            window.statusBarColor = if (darkTheme) {
                StatusBarDark.toArgb() // Pure black for AMOLED
            } else {
                StatusBarLight.toArgb()
            }
            
            // Navigation bar optimization for Samsung's gesture navigation
            window.navigationBarColor = if (darkTheme) {
                NavigationBarDark.toArgb() // Pure black for AMOLED
            } else {
                NavigationBarLight.toArgb()
            }
            
            // Enhanced contrast for Samsung's high-resolution display
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SamsungTypography, // Samsung-optimized typography
        shapes = SamsungShapes, // Samsung-optimized shapes
        content = content
    )
}

@Composable
fun IrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent branding
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
