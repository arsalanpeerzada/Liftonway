package dev.liftonway.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.pressScale
import dev.liftonway.core.designsystem.theme.LiftColors

enum class TitleAlign { Start, Center }

/**
 * The screens' header: a back (or close) button and a title, either beside it ("Available rides")
 * or centred ("Profile"), with an optional action on the right.
 */
@Composable
fun LiftTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navIcon: ImageVector = LiftIcons.ArrowBack,
    align: TitleAlign = TitleAlign.Start,
    action: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp)
            .padding(horizontal = 12.dp),
    ) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconAction(navIcon, if (navIcon == LiftIcons.Close) "Close" else "Back", onBack)
            if (align == TitleAlign.Start) {
                Spacer(Modifier.width(if (onBack != null) 12.dp else 8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
        }
        if (align == TitleAlign.Center) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center).semantics { heading() })
        }
        if (action != null) Box(Modifier.align(Alignment.CenterEnd)) { action() }
    }
}

@Composable
fun IconAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = LiftColors.Ink) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(44.dp)
            .pressScale(interaction, 0.88f)
            .clip(CircleShape)
            .clickable(interaction, ripple(bounded = false, radius = 22.dp), role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/** A white band on the pale canvas, as used to group the ride forms. */
@Composable
fun Section(modifier: Modifier = Modifier, label: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (label != null) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = LiftColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/** A profile menu line: label, chevron, and a hairline beneath. */
@Composable
fun MenuRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable BoxScope.() -> Unit)? = null) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 30.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Box(contentAlignment = Alignment.Center) {
                if (trailing != null) trailing() else Icon(LiftIcons.ChevronRight, null, tint = LiftColors.TextSecondary, modifier = Modifier.size(22.dp))
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 30.dp), color = LiftColors.Divider, thickness = 0.8.dp)
    }
}
