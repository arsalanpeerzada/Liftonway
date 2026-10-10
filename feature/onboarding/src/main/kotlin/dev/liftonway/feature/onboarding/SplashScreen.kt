package dev.liftonway.feature.onboarding

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.component.Wordmark
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.theme.LiftColors
import kotlinx.coroutines.delay

/** The green splash: the power-button mark draws itself, then the name rises in beneath it. */
@Composable
fun SplashRoute(onFinished: () -> Unit) {
    var showName by remember { mutableStateOf(false) }
    var markDone by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        delay(450)
        showName = true
    }
    LaunchedEffect(markDone) {
        if (markDone) {
            delay(350)
            finish()
        }
    }
    Box(Modifier.fillMaxSize().background(LiftColors.Green), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LiftLottie(LiftAnimation.SplashLogo, Modifier.size(120.dp), onFinished = { markDone = true })
            Spacer(Modifier.height(8.dp))
            Box(Modifier.height(76.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showName,
                    enter = fadeIn(tween(Motion.LONG)) + slideInVertically(tween(Motion.LONG, easing = Motion.Emphasized)) { it / 2 },
                ) {
                    Wordmark()
                }
            }
        }
    }
}
