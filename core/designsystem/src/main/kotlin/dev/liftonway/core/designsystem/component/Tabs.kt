package dev.liftonway.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.designsystem.theme.LiftShapes
import kotlin.math.roundToInt

/**
 * UPCOMING / COMPLETED / CANCELED. The green pill sits at [position] (a fractional tab index), so when
 * it's driven by a pager it tracks the finger frame by frame instead of jumping.
 */
@Composable
fun SegmentedTabs(labels: List<String>, position: () -> Float, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(40.dp),
    ) {
        val tabWidth = maxWidth / labels.size
        Box(
            Modifier
                // Read in the layout phase only, so dragging doesn't recompose the tabs.
                .offset { IntOffset((tabWidth.toPx() * position()).roundToInt(), 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .clip(LiftShapes.Control)
                .background(LiftColors.Green),
        )
        // Recomposes only when the nearest tab changes, not on every frame of a drag.
        val latest by rememberUpdatedState(position)
        val selected by remember { derivedStateOf { latest().roundToInt() } }
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            labels.forEachIndexed { i, label ->
                val color by animateColorAsState(if (i == selected) Color.White else LiftColors.TextSecondary, tween(Motion.SHORT), label = "tab")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(LiftShapes.Control)
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Tab) { onSelect(i) }
                        .semantics { this.selected = i == selected },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = color)
                }
            }
        }
    }
}
