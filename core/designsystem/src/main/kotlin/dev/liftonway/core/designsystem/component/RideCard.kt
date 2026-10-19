package dev.liftonway.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.Ride
import dev.liftonway.core.model.asPkr
import dev.liftonway.core.model.rideHeader
import java.util.Locale

/** The white ride card from the lists: driver, date, route, then whatever [footer] adds. */
@Composable
fun RideCard(
    ride: Ride,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    dateTrailing: (@Composable () -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        DriverHeader(ride.driver.name, ride.driver.rating, ride.driver.photoUri, onOpen)
        Spacer(Modifier.height(14.dp))
        if (dateTrailing == null) {
            DateDivider(ride.departure.rideHeader())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ride.departure.rideHeader(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                dateTrailing()
            }
        }
        Spacer(Modifier.height(16.dp))
        RouteStops(ride.allStops.map { it.label })
        footer()
    }
}

@Composable
fun DriverHeader(name: String, rating: Float?, photoUri: String?, onOpen: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClickLabel = "See stops", onClick = onOpen) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name, photoUri, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name.uppercase(Locale.UK), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(LiftIcons.Star, null, tint = LiftColors.Amber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(rating?.let { "(${"%.1f".format(Locale.UK, it)})" } ?: "(New)", style = MaterialTheme.typography.bodySmall, color = LiftColors.Hint)
            }
        }
        if (onOpen != null) Icon(LiftIcons.ChevronRight, null, tint = LiftColors.Divider, modifier = Modifier.size(20.dp))
    }
}

/** "———— April 3, 2026 | 8:00 AM ————" */
@Composable
fun DateDivider(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = LiftColors.Hairline)
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 10.dp))
        HorizontalDivider(Modifier.weight(1f), color = LiftColors.Hairline)
    }
}

/** Start (green), any stops, and destination (black), joined by a dotted line. */
@Composable
fun RouteStops(labels: List<String>, modifier: Modifier = Modifier, emphasiseLast: Boolean = false) {
    Column(modifier) {
        labels.forEachIndexed { i, label ->
            val first = i == 0
            val last = i == labels.lastIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                StopMarker(if (first) StopKind.Start else if (last) StopKind.End else StopKind.Middle)
                Spacer(Modifier.width(12.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (emphasiseLast && last) LiftColors.Ink else LiftColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!last) DottedConnector()
        }
    }
}

enum class StopKind { Start, Middle, End }

@Composable
fun StopMarker(kind: StopKind) {
    when (kind) {
        StopKind.Middle -> Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(LiftColors.Hint))
        }
        else -> Box(
            Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(if (kind == StopKind.Start) LiftColors.Green else LiftColors.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LiftIcons.Star, null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
    }
}

@Composable
private fun DottedConnector() {
    Canvas(Modifier.padding(start = 6.dp).size(width = 2.dp, height = 22.dp)) {
        drawLine(
            LiftColors.Ink,
            Offset(size.width / 2, 2f),
            Offset(size.width / 2, size.height - 2f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 3.dp.toPx())),
        )
    }
}

/** Vehicle / Total seats / Per seat. */
@Composable
fun RideStats(ride: Ride, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat("Vehicle", ride.vehicleName, TextAlign.Start, Modifier.weight(1.3f))
        Stat("Total seats", ride.totalSeats.toString(), TextAlign.Center, Modifier.weight(1f))
        Stat("Per seat", ride.pricePerSeat.asPkr(), TextAlign.End, Modifier.weight(1.1f))
    }
}

@Composable
private fun Stat(label: String, value: String, align: TextAlign, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LiftColors.Hint, textAlign = align, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
    }
}

/** The pill on Requested rides; it cross-fades colour and text when the status changes. */
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(color, tween(Motion.MEDIUM), label = "badge")
    Box(modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 18.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(text, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith fadeOut() }, label = "badgeText") {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}
