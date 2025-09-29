package com.nervesparks.axia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Axia Theme System - Modern Dark Theme for AI Interface
 * Inspired by GitHub Dark and VS Code themes
 */

// Color Palette
object AxiaColors {
    // Dark theme colors
    val Background = Color(0xFF0A0E1A)          // Deep space blue
    val Surface = Color(0xFF161B22)             // GitHub dark surface
    val SurfaceVariant = Color(0xFF21262D)      // Card background
    val OnSurface = Color(0xFFE6EDF3)           // Primary text
    val OnSurfaceVariant = Color(0xFF8B949E)    // Secondary text
    val OnSurfaceTertiary = Color(0xFF656D76)   // Tertiary text
    
    // Primary colors - Electric blue theme
    val Primary = Color(0xFF58A6FF)             // GitHub blue
    val PrimaryVariant = Color(0xFF1976D2)      // Darker blue
    val OnPrimary = Color(0xFFFFFFFF)           // White text on primary
    
    // Accent colors
    val Secondary = Color(0xFF00E5FF)           // Cyan accent
    val SecondaryVariant = Color(0xFF00BCD4)    // Teal accent
    val OnSecondary = Color(0xFF000000)         // Black text on secondary
    
    // Status colors
    val Success = Color(0xFF7EE787)             // Green
    val Warning = Color(0xFFFFAB40)             // Orange
    val Error = Color(0xFFFF6B6B)               // Red
    val Info = Color(0xFF64B5F6)                // Light blue
    
    // Mode-specific colors
    val AssistantColor = Color(0xFF58A6FF)      // Blue
    val AgentColor = Color(0xFF7EE787)          // Green
    val ResearchColor = Color(0xFFFFAB40)       // Orange
    val DevelopmentColor = Color(0xFF4CAF50)    // Green
    val SystemColor = Color(0xFFE91E63)         // Pink
    val ImageGenColor = Color(0xFF9C27B0)       // Purple
    
    // Border and outline colors
    val Outline = Color(0xFF30363D)             // Subtle border
    val OutlineVariant = Color(0xFF21262D)      // Even more subtle
}

// Typography
val AxiaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        color = AxiaColors.OnSurface
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
        color = AxiaColors.OnSurface
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        color = AxiaColors.OnSurface
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        color = AxiaColors.OnSurface
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = AxiaColors.OnSurface
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = AxiaColors.OnSurface
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = AxiaColors.OnSurface
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        color = AxiaColors.OnSurface
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = AxiaColors.OnSurface
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = AxiaColors.OnSurface
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = AxiaColors.OnSurface
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = AxiaColors.OnSurfaceVariant
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = AxiaColors.OnSurface
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = AxiaColors.OnSurfaceVariant
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = AxiaColors.OnSurfaceVariant
    )
)

// Dark Color Scheme
private val AxiaDarkColorScheme = darkColorScheme(
    primary = AxiaColors.Primary,
    onPrimary = AxiaColors.OnPrimary,
    primaryContainer = AxiaColors.PrimaryVariant,
    onPrimaryContainer = AxiaColors.OnPrimary,
    
    secondary = AxiaColors.Secondary,
    onSecondary = AxiaColors.OnSecondary,
    secondaryContainer = AxiaColors.SecondaryVariant,
    onSecondaryContainer = Color.White,
    
    tertiary = AxiaColors.Info,
    onTertiary = Color.White,
    tertiaryContainer = AxiaColors.Info.copy(alpha = 0.2f),
    onTertiaryContainer = AxiaColors.Info,
    
    background = AxiaColors.Background,
    onBackground = AxiaColors.OnSurface,
    
    surface = AxiaColors.Surface,
    onSurface = AxiaColors.OnSurface,
    surfaceVariant = AxiaColors.SurfaceVariant,
    onSurfaceVariant = AxiaColors.OnSurfaceVariant,
    
    outline = AxiaColors.Outline,
    outlineVariant = AxiaColors.OutlineVariant,
    
    error = AxiaColors.Error,
    onError = Color.White,
    errorContainer = AxiaColors.Error.copy(alpha = 0.2f),
    onErrorContainer = AxiaColors.Error,
    
    inverseSurface = AxiaColors.OnSurface,
    inverseOnSurface = AxiaColors.Surface,
    inversePrimary = AxiaColors.Primary,
    
    surfaceTint = AxiaColors.Primary
)

// Light color scheme (for future use)
private val AxiaLightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF1976D2),
    
    secondary = Color(0xFF00BCD4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF00695C),
    
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun AxiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        AxiaDarkColorScheme
    } else {
        AxiaLightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AxiaTypography,
        content = {
            CompositionLocalProvider(
                LocalAxiaColors provides AxiaColors,
                content = content
            )
        }
    )
}

// Extension properties for easier access
@Composable
fun getModeColor(mode: com.nervesparks.axia.data.AxiaMode): Color {
    return when (mode) {
        com.nervesparks.axia.data.AxiaMode.ASSISTANT -> AxiaColors.AssistantColor
        com.nervesparks.axia.data.AxiaMode.AUTONOMOUS_AGENT -> AxiaColors.AgentColor
        com.nervesparks.axia.data.AxiaMode.DEEP_RESEARCH -> AxiaColors.ResearchColor
        com.nervesparks.axia.data.AxiaMode.DEVELOPMENT -> AxiaColors.DevelopmentColor
        com.nervesparks.axia.data.AxiaMode.SYSTEM_CONTROL -> AxiaColors.SystemColor
        com.nervesparks.axia.data.AxiaMode.IMAGE_GENERATION -> AxiaColors.ImageGenColor
    }
}

// Custom composition local for Axia colors
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAxiaColors = staticCompositionLocalOf { AxiaColors }