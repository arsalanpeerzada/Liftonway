package dev.liftonway.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.liftonway.core.designsystem.R

private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/**
 * The designs use Gilroy, which needs a paid licence. Plus Jakarta Sans (SIL Open Font Licence) is the
 * closest free match: geometric, with the same double-storey "a" and open counters.
 */
val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) =
    TextStyle(fontFamily = Jakarta, fontSize = size.sp, fontWeight = weight, lineHeight = line.sp, letterSpacing = spacing.em)

val LiftTypography = Typography(
    // "Hello! Sign in to get started", "Congratulations!"
    displaySmall = style(28, FontWeight.Medium, 34, -0.01),
    // "Let's travel together", "GOOD DAY, Siffat"
    headlineMedium = style(26, FontWeight.Medium, 32, -0.01),
    headlineSmall = style(22, FontWeight.Medium, 28),
    // Screen titles: "Available rides", "Profile"
    titleLarge = style(20, FontWeight.Medium, 26),
    // Driver names on cards
    titleMedium = style(16, FontWeight.ExtraBold, 22),
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 22),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(16, FontWeight.Medium, 20),
    // Section labels: "DEPARTURE DATE", tab labels
    labelMedium = style(12, FontWeight.Medium, 16, 0.02),
    labelSmall = style(10, FontWeight.Medium, 14, 0.03),
)
