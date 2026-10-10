package dev.liftonway.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LiftColorScheme = lightColorScheme(
    primary = LiftColors.Green,
    onPrimary = Color.White,
    primaryContainer = LiftColors.GreenSoft,
    onPrimaryContainer = LiftColors.Ink,
    secondary = LiftColors.Ink,
    onSecondary = Color.White,
    tertiary = LiftColors.Amber,
    background = LiftColors.Surface,
    onBackground = LiftColors.Ink,
    surface = LiftColors.Surface,
    onSurface = LiftColors.Ink,
    surfaceVariant = LiftColors.Canvas,
    onSurfaceVariant = LiftColors.TextSecondary,
    surfaceContainerLow = LiftColors.Surface,
    surfaceContainer = LiftColors.Surface,
    surfaceContainerHigh = LiftColors.Surface,
    outline = LiftColors.Ink,
    outlineVariant = LiftColors.Hairline,
    error = LiftColors.Coral,
    // Snackbar actions sit on the dark snackbar.
    inversePrimary = Color(0xFF6BE3A8),
)

object LiftShapes {
    val Control = RoundedCornerShape(10.dp)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Card = RoundedCornerShape(12.dp)
}

/** The designs are light only, so there is no dark scheme. */
@Composable
fun LiftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiftColorScheme,
        typography = LiftTypography,
        shapes = Shapes(small = LiftShapes.Control, medium = LiftShapes.Card, extraLarge = LiftShapes.Sheet),
        content = content,
    )
}
