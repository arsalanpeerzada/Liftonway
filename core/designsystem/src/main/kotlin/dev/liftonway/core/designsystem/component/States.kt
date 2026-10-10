package dev.liftonway.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.Jakarta
import dev.liftonway.core.designsystem.theme.LiftColors

/** "You have not completed a ride yet!!" with the box animation and a grey action. */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    animation: LiftAnimation = LiftAnimation.EmptyBox,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LiftLottie(animation, Modifier.size(width = 150.dp, height = 130.dp).entrance(0))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium), textAlign = TextAlign.Center, modifier = Modifier.entrance(1))
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            MutedButton(action, onAction, Modifier.entrance(2))
        }
    }
}

/** A full-screen moment: animation, title and message, as on "Congratulations!". */
@Composable
fun CelebrationContent(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    animation: LiftAnimation = LiftAnimation.Success,
    size: Dp = 140.dp,
    onAnimationFinished: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LiftLottie(animation, Modifier.size(size), onFinished = onAnimationFinished)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.entrance(3))
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = LiftColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.entrance(4))
    }
}

/**
 * "lift ⏻n way" with "LET'S TRAVEL TOGETHER" beneath; the power-button O comes from the original Otere mark.
 * Pass [mark] to replace the static O, such as with the animated one on the splash.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier, color: Color = Color.White, mark: (@Composable () -> Unit)? = null) {
    val style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, color = color, letterSpacing = (-0.5).sp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("lift", style = style)
            Spacer(Modifier.width(10.dp))
            if (mark != null) mark() else PowerMark(Modifier.size(40.dp), color)
            Text("n way", style = style)
        }
        Text(
            "LET'S TRAVEL TOGETHER",
            style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 2.4.sp, color = color),
        )
    }
}

@Composable
fun PowerMark(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.15f
        val inset = stroke / 2 + size.minDimension * 0.04f
        drawArc(
            color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(inset, inset + size.height * 0.04f),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color,
            Offset(size.width / 2, size.height * 0.02f),
            Offset(size.width / 2, size.height * 0.42f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
