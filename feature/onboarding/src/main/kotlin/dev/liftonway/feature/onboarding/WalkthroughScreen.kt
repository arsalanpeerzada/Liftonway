package dev.liftonway.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.component.IconAction
import dev.liftonway.core.designsystem.component.NextFab
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class Page(val animation: LiftAnimation, val title: String, val body: String? = null, val points: List<String> = emptyList())

private val Pages = listOf(
    Page(LiftAnimation.TravelCar, "Let’s travel together", points = listOf("Save money", "Make passive income", "Make new friends", "Overcome traffic congestion")),
    Page(LiftAnimation.FindRide, "Find a ride", body = "Request a ride and be picked up on the same route by the driver"),
    Page(LiftAnimation.PostRide, "Post a ride", body = "Publish a ride and pick up passengers along the way"),
)

/** The three walkthrough pages. Illustrations drift at a different speed from the text as you swipe. */
@Composable
fun WalkthroughRoute(onDone: () -> Unit) {
    val pager = rememberPagerState { Pages.size }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(LiftColors.Surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
            androidx.compose.animation.AnimatedVisibility(pager.currentPage > 0, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                IconAction(LiftIcons.ArrowBack, "Previous", { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } })
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { index ->
            PageContent(Pages[index], index, pager)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Skip",
                style = MaterialTheme.typography.bodyLarge,
                color = LiftColors.Hint,
                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onDone).padding(8.dp),
            )
            Spacer(Modifier.weight(1f))
            NextFab(
                onClick = { if (pager.currentPage == Pages.lastIndex) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                contentDescription = if (pager.currentPage == Pages.lastIndex) "Get started" else "Next",
            )
        }
    }
}

@Composable
private fun PageContent(page: Page, index: Int, pager: PagerState) {
    // -1..1 as the page moves across the screen; 0 when it's centred.
    val offset = { (pager.currentPage - index) + pager.currentPageOffsetFraction }
    // Clipped so a neighbouring page's drifting text never peeks in at the edge.
    Column(Modifier.fillMaxSize().clipToBounds().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.2f))
        LiftLottie(
            page.animation,
            playing = pager.settledPage == index || pager.targetPage == index,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .graphicsLayer {
                    val o = offset()
                    translationX = o * size.width * 0.45f
                    alpha = lerp(1f, 0f, o.absoluteValue.coerceIn(0f, 1f))
                    val s = lerp(1f, 0.86f, o.absoluteValue.coerceIn(0f, 1f))
                    scaleX = s
                    scaleY = s
                },
        )
        Spacer(Modifier.height(28.dp))
        Column(
            Modifier.graphicsLayer { translationX = offset() * size.width * 0.12f },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            if (page.body != null) {
                Text(page.body, style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary, textAlign = TextAlign.Center)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                page.points.forEachIndexed { i, point ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.entrance(i + 2, step = 90L)) {
                        Icon(LiftIcons.Check, null, tint = LiftColors.Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(point, style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.weight(0.3f))
    }
}
