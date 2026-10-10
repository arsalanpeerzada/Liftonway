package dev.liftonway.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.SessionRepository
import dev.liftonway.core.designsystem.component.Wordmark
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TermsViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    fun accept(then: () -> Unit) {
        viewModelScope.launch {
            session.finishOnboarding()
            then()
        }
    }
}

/**
 * The green terms page. Ticking the box draws the tick, then the card morphs into the START button.
 */
@Composable
fun TermsRoute(onStart: () -> Unit, onOpenTerms: () -> Unit, viewModel: TermsViewModel = hiltViewModel()) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    var ready by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(agreed) {
        if (agreed) {
            delay(420)
            ready = true
        } else {
            ready = false
        }
    }
    Box(Modifier.fillMaxSize().background(LiftColors.Green).padding(horizontal = 46.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Wordmark(Modifier.entrance(0))
            Spacer(Modifier.height(130.dp))
            Text(
                "In order to start using Lift On Way, please first agree to our",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.entrance(1),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "TERMS & CONDITIONS",
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                color = Color.White,
                modifier = Modifier.entrance(2).clip(RoundedCornerShape(4.dp)).clickable(role = Role.Button, onClick = onOpenTerms).padding(4.dp),
            )
            Spacer(Modifier.height(36.dp))
            AnimatedContent(
                targetState = ready,
                transitionSpec = {
                    (fadeIn(tween(Motion.MEDIUM, delayMillis = 90)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(Motion.SHORT)) using
                        SizeTransform(clip = false)
                },
                modifier = Modifier.entrance(3),
                label = "terms",
            ) { isReady ->
                if (isReady) {
                    StartButton { viewModel.accept(onStart) }
                } else {
                    AgreeCard(agreed) { agreed = !agreed }
                }
            }
        }
    }
}

@Composable
private fun AgreeCard(checked: Boolean, onToggle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val box by animateColorAsState(if (checked) LiftColors.Green else Color.White, tween(Motion.SHORT), label = "box")
    val tick by animateFloatAsState(if (checked) 1f else 0f, Motion.bouncy(), label = "tick")
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(6.dp, RoundedCornerShape(2.dp))
            .background(Color.White)
            .clickable(role = Role.Checkbox) {
                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                onToggle()
            }
            .semantics {
                contentDescription = "I agree to the ride sharing terms and conditions"
                stateDescription = if (checked) "Agreed" else "Not agreed"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(54.dp).fillMaxHeight().background(Color(0xFFD9D9D9)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).background(box).border(1.5.dp, LiftColors.Ink), contentAlignment = Alignment.Center) {
                Icon(
                    LiftIcons.Check,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp).graphicsLayer {
                        scaleX = tick
                        scaleY = tick
                        alpha = tick.coerceIn(0f, 1f)
                    },
                )
            }
        }
        Text(
            "I agree to the ride sharing terms and conditions",
            style = MaterialTheme.typography.bodyMedium,
            color = LiftColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(6.dp, RoundedCornerShape(2.dp))
            .background(Color.White)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("START", style = MaterialTheme.typography.bodyLarge, color = LiftColors.Ink)
    }
}
