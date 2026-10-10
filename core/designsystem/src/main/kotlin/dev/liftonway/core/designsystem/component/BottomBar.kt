package dev.liftonway.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.LiftColors

data class BarItem(val label: String, val icon: ImageVector, val badge: Int = 0)

/**
 * Home / My trips / Notifications / Profile: each icon sits in a thin circle, as designed.
 * The selected one fills green with a little spring pop.
 */
@Composable
fun LiftBottomBar(items: List<BarItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Column(modifier.fillMaxWidth().background(Color.White)) {
        HorizontalDivider(color = LiftColors.Hairline, thickness = 0.5.dp)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(72.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, item ->
                val isSelected = i == selected
                val fill by animateColorAsState(if (isSelected) LiftColors.Green else Color.White, tween(Motion.SHORT), label = "fill")
                val tint by animateColorAsState(if (isSelected) Color.White else LiftColors.Ink, tween(Motion.SHORT), label = "tint")
                val label by animateColorAsState(if (isSelected) LiftColors.Green else LiftColors.Ink, tween(Motion.SHORT), label = "label")
                val pop by animateFloatAsState(if (isSelected) 1.08f else 1f, Motion.bouncy(), label = "pop")
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Tab) {
                            if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(i)
                        }
                        .semantics { this.selected = isSelected }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box {
                        Box(
                            Modifier
                                .size(30.dp)
                                .graphicsLayer {
                                    scaleX = pop
                                    scaleY = pop
                                }
                                .clip(CircleShape)
                                .background(fill)
                                .border(1.dp, if (isSelected) LiftColors.Green else LiftColors.Ink, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(item.icon, null, tint = tint, modifier = Modifier.size(18.dp))
                        }
                        if (item.badge > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).size(10.dp).clip(CircleShape).background(LiftColors.Coral)
                                    .border(1.5.dp, Color.White, CircleShape),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.label, style = MaterialTheme.typography.bodySmall, color = label)
                }
            }
        }
    }
}
