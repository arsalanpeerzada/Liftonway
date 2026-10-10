package dev.liftonway.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.motion.pressScale
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.designsystem.theme.LiftShapes

/** The green, full-width button. While [loading], the label cross-fades into bouncing dots. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = LiftColors.Green,
    shape: Shape = LiftShapes.Control,
    height: Dp = 52.dp,
) {
    val container by animateColorAsState(if (enabled) color else LiftColors.GreenDisabled, tween(Motion.SHORT), label = "container")
    BaseButton(onClick, modifier.height(height), enabled && !loading, shape, container, border = null) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { (fadeIn(tween(160)) + scaleIn(initialScale = 0.85f)) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)) },
            label = "label",
        ) { busy ->
            if (busy) {
                LiftLottie(LiftAnimation.LoadingDots, Modifier.size(width = 72.dp, height = 24.dp))
            } else {
                Text(text, style = if (height < 40.dp) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

/** White button with a thin dark outline, such as "Publish a ride" and "History". */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = LiftShapes.Control,
    height: Dp = 52.dp,
) {
    BaseButton(onClick, modifier.height(height), enabled, shape, Color.White, BorderStroke(1.dp, LiftColors.Ink)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = LiftColors.Ink)
    }
}

/** Grey button, as in the empty states ("Book a ride") and "Decline". */
@Composable
fun MutedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 36.dp, enabled: Boolean = true) {
    BaseButton(onClick, modifier.height(height), enabled, RoundedSmall, LiftColors.Muted, border = null) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

/** The round green "next" button on the walkthrough. */
@Composable
fun NextFab(onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String = "Next") {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .size(48.dp)
            .pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .background(LiftColors.Green)
            .clickable(interaction, ripple(), role = Role.Button, onClickLabel = contentDescription) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(LiftIcons.ArrowForward, contentDescription, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/** Green text action, such as "+Add stop" and "+ADD CARD". */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = LiftColors.Green) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier
            .pressScale(interaction, 0.92f)
            .clip(RoundedSmall)
            .clickable(interaction, ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

private val RoundedSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)

@Composable
private fun BaseButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    container: Color,
    border: BorderStroke?,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .pressScale(interaction)
            .clip(shape)
            .background(container)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(interaction, ripple(), enabled = enabled, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}
