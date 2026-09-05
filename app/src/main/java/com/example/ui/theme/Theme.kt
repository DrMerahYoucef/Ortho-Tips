package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = OrthoPrimaryDark,
    onPrimary = OrthoOnPrimaryDark,
    primaryContainer = OrthoPrimaryContainerDark,
    onPrimaryContainer = OrthoOnPrimaryContainerDark,
    secondary = OrthoSecondaryDark,
    onSecondary = OrthoOnSecondaryDark,
    secondaryContainer = OrthoSecondaryContainerDark,
    onSecondaryContainer = OrthoOnSecondaryContainerDark,
    tertiary = OrthoTertiaryDark,
    onTertiary = OrthoOnTertiaryDark,
    tertiaryContainer = OrthoTertiaryContainerDark,
    onTertiaryContainer = OrthoOnTertiaryContainerDark,
    background = OrthoBackgroundDark,
    onBackground = OrthoOnBackgroundDark,
    surface = OrthoSurfaceDark,
    onSurface = OrthoOnSurfaceDark,
    surfaceVariant = OrthoSurfaceVariantDark,
    onSurfaceVariant = OrthoOnSurfaceVariantDark,
    outline = OrthoOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = OrthoPrimaryLight,
    onPrimary = OrthoOnPrimaryLight,
    primaryContainer = OrthoPrimaryContainerLight,
    onPrimaryContainer = OrthoOnPrimaryContainerLight,
    secondary = OrthoSecondaryLight,
    onSecondary = OrthoOnSecondaryLight,
    secondaryContainer = OrthoSecondaryContainerLight,
    onSecondaryContainer = OrthoOnSecondaryContainerLight,
    tertiary = OrthoTertiaryLight,
    onTertiary = OrthoOnTertiaryLight,
    tertiaryContainer = OrthoTertiaryContainerLight,
    onTertiaryContainer = OrthoOnTertiaryContainerLight,
    background = OrthoBackgroundLight,
    onBackground = OrthoOnBackgroundLight,
    surface = OrthoSurfaceLight,
    onSurface = OrthoOnSurfaceLight,
    surfaceVariant = OrthoSurfaceVariantLight,
    onSurfaceVariant = OrthoOnSurfaceVariantLight,
    outline = OrthoOutlineLight
)

@Composable
fun OrthoTipsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted clinical colors by default
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
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
