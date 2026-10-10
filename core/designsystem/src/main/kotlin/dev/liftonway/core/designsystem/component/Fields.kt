package dev.liftonway.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.designsystem.theme.LiftShapes
import dev.liftonway.core.model.Place

/**
 * The outlined input from the designs: 1dp dark border, 10dp corners. The border turns green while focused
 * and coral on error, and an error makes the field shake once.
 */
@Composable
fun LiftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isError: Boolean = false,
    enabled: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    leading: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        when {
            isError -> LiftColors.Coral
            focused -> LiftColors.Green
            else -> LiftColors.Ink
        },
        tween(Motion.SHORT),
        label = "border",
    )
    val placeholderAlpha by animateFloatAsState(if (value.isEmpty()) 1f else 0f, tween(120), label = "placeholder")
    val borderWidth by animateDpAsState(if (focused || isError) 1.5.dp else 1.dp, tween(Motion.SHORT), label = "width")
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle.copy(color = LiftColors.Ink, textAlign = textAlign),
        cursorBrush = SolidColor(LiftColors.Green),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        interactionSource = interactionSource,
        visualTransformation = visualTransformation,
        modifier = modifier.shakeOnError(isError),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(borderWidth, borderColor, LiftShapes.Control)
                    .padding(horizontal = if (leading != null) 8.dp else 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = if (textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart) {
                    Text(placeholder, style = textStyle, color = LiftColors.Hint, modifier = Modifier.graphicsLayer { alpha = placeholderAlpha })
                    inner()
                }
            }
        },
    )
}

/** The grey round search glyph that leads the location fields. */
@Composable
fun SearchGlyph() {
    Box(Modifier.size(32.dp).clip(CircleShape).background(LiftColors.Muted), contentAlignment = Alignment.Center) {
        Icon(LiftIcons.Search, null, tint = LiftColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

val CountryCodes = listOf("+92", "+44", "+49", "+31")

/** Country code box and the phone number, side by side, as on Sign in and Sign up. */
@Composable
fun PhoneField(
    countryCode: String,
    onCountryCode: (String) -> Unit,
    number: String,
    onNumber: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    var open by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (open) 180f else 0f, Motion.smooth(), label = "chevron")
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            Row(
                Modifier
                    .width(72.dp)
                    .height(50.dp)
                    .clip(LiftShapes.Control)
                    .border(1.dp, LiftColors.Ink, LiftShapes.Control)
                    .clickable(role = Role.DropdownList, onClickLabel = "Change country code") { open = true }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(countryCode, style = MaterialTheme.typography.bodyLarge, color = LiftColors.TextSecondary)
                Icon(LiftIcons.ChevronDown, null, tint = LiftColors.TextSecondary, modifier = Modifier.size(18.dp).rotate(chevron))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                CountryCodes.forEach { code ->
                    DropdownMenuItem(text = { Text(code) }, onClick = {
                        onCountryCode(code)
                        open = false
                    })
                }
            }
        }
        LiftTextField(
            value = number,
            onValueChange = { onNumber(it.filter(Char::isDigit).take(11)) },
            placeholder = "Phone Number",
            keyboardType = KeyboardType.Phone,
            imeAction = imeAction,
            onImeAction = onImeAction,
            isError = isError,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A tappable value with a leading icon, such as the date and seat pickers in the ride forms. */
@Composable
fun SelectorRow(icon: ImageVector, value: String, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxHeight()
            .clip(LiftShapes.Control)
            .clickable(interaction, ripple(), role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription, tint = LiftColors.Hint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        AnimatedContent(value, label = "value") { Text(it, style = MaterialTheme.typography.bodyLarge, color = LiftColors.Ink) }
    }
}

/** Shakes sideways each time [error] becomes true. */
@Composable
fun Modifier.shakeOnError(error: Boolean): Modifier {
    val offset = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { 8.dp.toPx() }
    LaunchedEffect(error) {
        if (error) {
            offset.animateTo(0f, keyframes {
                durationMillis = 360
                px at 50
                -px at 120
                px * 0.6f at 190
                -px * 0.4f at 260
                0f at 360
            })
        }
    }
    return graphicsLayer { translationX = offset.value }
}

/** Matching places listed under a location field. */
@Composable
fun PlaceSuggestions(places: List<Place>, onPick: (Place) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        if (places.isEmpty()) {
            Text("No matching places", style = MaterialTheme.typography.bodyMedium, color = LiftColors.Hint, modifier = Modifier.padding(12.dp))
        }
        places.forEach { place ->
            Row(
                Modifier.fillMaxWidth().clip(LiftShapes.Control).clickable(role = Role.Button) { onPick(place) }.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Icon(LiftIcons.Search, null, tint = LiftColors.Hint, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(place.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(place.area, style = MaterialTheme.typography.bodySmall, color = LiftColors.Hint)
                }
            }
        }
    }
}
