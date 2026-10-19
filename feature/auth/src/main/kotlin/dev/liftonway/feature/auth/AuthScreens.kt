package dev.liftonway.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.liftonway.core.data.BuildConfig
import dev.liftonway.core.data.PendingVerification
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.animation.LiftLottie
import dev.liftonway.core.designsystem.component.CelebrationContent
import dev.liftonway.core.designsystem.component.IconAction
import dev.liftonway.core.designsystem.component.LiftTextField
import dev.liftonway.core.designsystem.component.PhoneField
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.ProfilePhoto
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.motion.sharedElement
import dev.liftonway.core.designsystem.theme.LiftColors
import kotlinx.coroutines.delay

const val PROFILE_PHOTO_KEY = "profile-photo"

/** "Let's travel together!" with Sign up and Sign in. */
@Composable
fun AuthChoiceRoute(onSignUp: () -> Unit, onSignIn: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        LiftLottie(LiftAnimation.TravelCar, Modifier.fillMaxWidth().aspectRatio(1.5f).entrance(0))
        Spacer(Modifier.height(36.dp))
        Text("Let’s travel together!", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.entrance(1))
        Spacer(Modifier.weight(0.5f))
        PrimaryButton("Sign up", onSignUp, Modifier.fillMaxWidth().entrance(2))
        Spacer(Modifier.height(12.dp))
        Text(
            "Sign in",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.entrance(3).clip(CircleShape).clickable(role = Role.Button, onClick = onSignIn).padding(horizontal = 20.dp, vertical = 10.dp),
        )
        Spacer(Modifier.weight(0.7f))
    }
}

@Composable
private fun AuthScaffold(onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 38.dp),
    ) {
        Box(Modifier.height(64.dp).offset(x = (-12).dp), contentAlignment = Alignment.CenterStart) {
            if (onBack != null) IconAction(LiftIcons.ArrowBack, "Back", onBack)
        }
        content()
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Headline(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text, style = MaterialTheme.typography.displaySmall, modifier = Modifier.entrance(0))
}

@Composable
fun SignUpRoute(onBack: () -> Unit, onCodeSent: () -> Unit, viewModel: SignUpViewModel = hiltViewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(viewModel::onPhotoPicked) }
    val focus = LocalFocusManager.current
    AuthScaffold(onBack) {
        Headline("Hello!\nSignup to\nget started")
        Spacer(Modifier.height(40.dp))
        val pickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        // Only the photo is clipped round, so the camera badge can sit over its edge as designed.
        Box(Modifier.entrance(1)) {
            ProfilePhoto(
                viewModel.photoUri,
                Modifier.sharedElement(PROFILE_PHOTO_KEY).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Add a profile photo", onClick = pickPhoto),
            )
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = (-2).dp).size(30.dp).clip(CircleShape).background(Color.White)
                    .clickable(role = Role.Button, onClickLabel = "Add a profile photo", onClick = pickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LiftIcons.CameraAdd, null, tint = LiftColors.Ink, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(56.dp))
        LiftTextField(
            viewModel.name,
            { viewModel.name = it },
            "Full Name",
            Modifier.fillMaxWidth().entrance(2),
            isError = viewModel.nameError,
        )
        Spacer(Modifier.height(18.dp))
        PhoneField(
            viewModel.countryCode,
            { viewModel.countryCode = it },
            viewModel.phone,
            { viewModel.phone = it },
            Modifier.entrance(3),
            isError = viewModel.phoneError,
            onImeAction = {
                focus.clearFocus()
                viewModel.submit(onCodeSent)
            },
        )
        Spacer(Modifier.height(38.dp))
        PrimaryButton("Sign up", { viewModel.submit(onCodeSent) }, Modifier.fillMaxWidth().entrance(4), loading = viewModel.sending)
        Failure(viewModel.failure)
    }
}

@Composable
fun SignInRoute(onBack: () -> Unit, onCodeSent: () -> Unit, viewModel: SignInViewModel = hiltViewModel()) {
    val focus = LocalFocusManager.current
    AuthScaffold(onBack) {
        Headline("Hello!\nSign in to\nget started")
        Spacer(Modifier.height(72.dp))
        PhoneField(
            viewModel.countryCode,
            { viewModel.countryCode = it },
            viewModel.phone,
            { viewModel.phone = it },
            Modifier.entrance(1),
            isError = viewModel.phoneError,
            onImeAction = {
                focus.clearFocus()
                viewModel.submit(onCodeSent)
            },
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Sign in", { viewModel.submit(onCodeSent) }, Modifier.fillMaxWidth().entrance(2), loading = viewModel.sending)
        Failure(viewModel.failure)
    }
}

/**
 * "Welcome, <name>" with the photo and the code field. Used for sign-up, sign-in and changing number;
 * the latter uses the plainer "Verify OTP" layout from the profile designs.
 */
@Composable
fun OtpRoute(onBack: () -> Unit, onVerified: () -> Unit, viewModel: OtpViewModel = hiltViewModel()) {
    val pending = viewModel.pending
    val back by rememberUpdatedState(onBack)
    LaunchedEffect(pending) { if (pending == null) back() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(350) // After the screen's slide-in, so the keyboard doesn't fight the transition.
        focus.requestFocus()
    }
    AuthScaffold(onBack) {
        when (pending) {
            is PendingVerification.SignUp -> {
                Spacer(Modifier.height(16.dp))
                Text("Welcome,", style = MaterialTheme.typography.displaySmall, modifier = Modifier.entrance(0))
                Text(pending.name.substringBefore(' '), style = MaterialTheme.typography.displaySmall, modifier = Modifier.entrance(0))
                Spacer(Modifier.height(36.dp))
                ProfilePhoto(pending.photoUri, Modifier.sharedElement(PROFILE_PHOTO_KEY), size = 100.dp)
                Spacer(Modifier.height(48.dp))
            }
            is PendingVerification.SignIn -> {
                Headline("Welcome\nback")
                Spacer(Modifier.height(64.dp))
            }
            else -> {
                Text("Verify OTP", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(80.dp))
            }
        }
        Text(
            "Enter the OTP we’ve sent to ${pending?.let { "${it.countryCode} ${it.phone}" } ?: "your phone number"}.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.entrance(2),
        )
        Spacer(Modifier.height(10.dp))
        LiftTextField(
            viewModel.code,
            viewModel::onCode,
            "OTP",
            Modifier.fillMaxWidth().entrance(3).focusRequester(focus),
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            onImeAction = { viewModel.verify(onVerified) },
            isError = viewModel.error != null,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            viewModel.error ?: if (BuildConfig.USE_FAKE_BACKEND) "Demo: any 4-digit code works." else "The code can take a moment to arrive.",
            style = MaterialTheme.typography.bodySmall,
            color = if (viewModel.error != null) LiftColors.Coral else LiftColors.Hint,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            "Verify",
            { viewModel.verify(onVerified) },
            Modifier.fillMaxWidth().entrance(4),
            enabled = viewModel.code.length == 4,
            loading = viewModel.verifying,
        )
    }
}

/** A server or connection problem under the form's button, sliding in when there is one. */
@Composable
private fun Failure(message: String?) {
    androidx.compose.animation.AnimatedVisibility(message != null) {
        Text(
            message.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = LiftColors.Coral,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
    }
}

/** "Congratulations! Your verification is successful", which moves on by itself. */
@Composable
fun VerifiedRoute(onContinue: () -> Unit) {
    val next by rememberUpdatedState(onContinue)
    Box(
        Modifier.fillMaxSize().background(Color.White).clickable(role = Role.Button, onClickLabel = "Continue") { next() },
        contentAlignment = Alignment.Center,
    ) {
        CelebrationContent("Congratulations!", "Your verification is successful")
    }
    LaunchedEffect(Unit) {
        delay(2200)
        next()
    }
}
