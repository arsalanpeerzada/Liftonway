package dev.liftonway.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.SessionRepository
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.component.OutlineButton
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.motion.entrance
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(session: SessionRepository) : ViewModel() {
    val firstName: StateFlow<String> = session.session
        .map { it.profile?.name?.substringBefore(' ').orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
}

/** "GOOD DAY, <name>", the carpool on the road, and the two main actions. */
@Composable
fun HomeRoute(onFindRide: () -> Unit, onPublishRide: () -> Unit, contentPadding: PaddingValues, viewModel: HomeViewModel = hiltViewModel()) {
    val name by viewModel.firstName.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(contentPadding)
            .padding(horizontal = 36.dp),
    ) {
        Spacer(Modifier.height(56.dp))
        Text("GOOD DAY,", style = MaterialTheme.typography.titleLarge, modifier = Modifier.entrance(0))
        Text(name, style = MaterialTheme.typography.headlineMedium.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize), modifier = Modifier.entrance(1))
        Spacer(Modifier.weight(0.5f))
        LiftLottie(LiftAnimation.TravelCar, Modifier.fillMaxWidth().aspectRatio(1.55f).entrance(2))
        Spacer(Modifier.weight(0.6f))
        PrimaryButton("Find a ride", onFindRide, Modifier.fillMaxWidth().entrance(3))
        Spacer(Modifier.height(18.dp))
        OutlineButton("Publish a ride", onPublishRide, Modifier.fillMaxWidth().entrance(4))
        Spacer(Modifier.weight(1f))
    }
}
