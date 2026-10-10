package dev.liftonway.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.theme.Jakarta
import dev.liftonway.core.designsystem.theme.LiftColors

private val AvatarTints = listOf(
    Color(0xFFDDF4E8) to Color(0xFF038A4B),
    Color(0xFFD7E5FF) to Color(0xFF2559C7),
    Color(0xFFFBEFDD) to Color(0xFFA9711C),
    Color(0xFFFDE3E3) to Color(0xFFC0392B),
    Color(0xFFEDE4FB) to Color(0xFF6B3FB8),
)

/**
 * A person's photo, or their initials on a tint picked from the name. The prototype ships no photos of
 * real people, so sample drivers always show initials.
 */
@Composable
fun Avatar(
    name: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        val (bg, fg) = AvatarTints[(name.hashCode() and Int.MAX_VALUE) % AvatarTints.size]
        val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        Box(modifier.size(size).clip(shape).background(bg), contentAlignment = Alignment.Center) {
            Text(initials, style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp, color = fg))
        }
    }
}

/** The large round photo used on Sign up and the profile, with an empty-person state. */
@Composable
fun ProfilePhoto(photoUri: String?, modifier: Modifier = Modifier, size: Dp = 92.dp) {
    if (photoUri != null) {
        AsyncImage(photoUri, null, contentScale = ContentScale.Crop, modifier = modifier.size(size).clip(CircleShape))
    } else {
        Box(
            modifier.size(size).clip(CircleShape).background(Color.White).border(1.dp, LiftColors.Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(LiftIcons.Person, null, tint = LiftColors.Ink, modifier = Modifier.size(size * 0.46f))
        }
    }
}
