package dev.liftonway.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.LiftColors

private val StarLabels = listOf("Poor", "Fair", "Good", "Great", "Excellent")

/** Read-only stars, e.g. the rating someone already gave. */
@Composable
fun Stars(stars: Int, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "$stars out of 5 stars" }, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i -> Icon(LiftIcons.Star, null, tint = if (i < stars) LiftColors.Amber else LiftColors.Muted, modifier = Modifier.size(size)) }
    }
}

/**
 * Asks for one to five stars. Each star springs as it lights up; once [done], the sheet thanks the user
 * with the success animation instead.
 */
@Composable
fun RatingSheet(
    title: String,
    busy: Boolean,
    failure: String?,
    done: Boolean,
    onSubmit: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var stars by rememberSaveable { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    LiftSheet(onDismiss) { dismiss ->
        AnimatedContent(done, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() }, label = "rating") { thanked ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (thanked) {
                    LiftLottie(LiftAnimation.Success, Modifier.size(140.dp))
                    Text("Thanks for rating!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Done", dismiss, Modifier.fillMaxWidth())
                } else {
                    Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(5) { i ->
                            val lit = i < stars
                            val scale by animateFloatAsState(if (lit) 1.12f else 1f, Motion.bouncy(), label = "star")
                            val tint by animateColorAsState(if (lit) LiftColors.Amber else LiftColors.Muted, label = "starTint")
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        role = Role.RadioButton,
                                        onClickLabel = "${i + 1} stars, ${StarLabels[i]}",
                                    ) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        stars = i + 1
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(LiftIcons.Star, "${i + 1} stars", tint = tint, modifier = Modifier.size(40.dp).scale(scale))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        failure ?: if (stars == 0) "Tap a star" else StarLabels[stars - 1],
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (failure != null) LiftColors.Coral else LiftColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlineButton("Later", dismiss, Modifier.weight(1f))
                        PrimaryButton("Submit", { onSubmit(stars) }, Modifier.weight(1f), enabled = stars > 0, loading = busy)
                    }
                }
            }
        }
    }
}
