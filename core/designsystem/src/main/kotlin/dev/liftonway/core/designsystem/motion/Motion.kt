@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.liftonway.core.designsystem.motion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** One motion vocabulary for the app, so every screen moves the same way. */
object Motion {
    /** Material's emphasised decelerate: quick start, long soft landing. */
    val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val SHORT = 180
    const val MEDIUM = 320
    const val LONG = 480

    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
    fun <T> smooth(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> enter(duration: Int = MEDIUM): AnimationSpec<T> = tween(duration, easing = Emphasized)
}

/** Shrinks slightly while pressed and springs back, so every tap feels physical. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressed: Float = 0.96f): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, Motion.bouncy(), label = "press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Fades and rises into place the first time it's shown, `index` steps after the screen appears.
 * Remembered across configuration changes, so it doesn't replay on rotation.
 */
@Composable
fun Modifier.entrance(index: Int = 0, step: Long = 55L, rise: Int = 18): Modifier {
    var shown by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (shown) 1f else 0f) }
    val risePx = with(LocalDensity.current) { rise.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * step)
            progress.animateTo(1f, tween(Motion.LONG, easing = Motion.Emphasized))
            shown = true
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * risePx
    }
}

/** Provided by the app around NavDisplay, so screens can share elements across destinations. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Provided per destination with Navigation 3's animated content scope. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Flies this element between destinations that use the same key; does nothing in previews. */
@Composable
fun Modifier.sharedElement(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current
    val animated = LocalNavAnimatedScope.current
    if (shared == null || animated == null) return this
    return with(shared) {
        this@sharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
        )
    }
}
