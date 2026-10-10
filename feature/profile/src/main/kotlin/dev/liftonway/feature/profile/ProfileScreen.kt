package dev.liftonway.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.liftonway.core.designsystem.component.ConfirmSheetContent
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.MenuRow
import dev.liftonway.core.designsystem.component.ProfilePhoto
import dev.liftonway.core.designsystem.component.TitleAlign
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors

/** Where a profile row leads. Pages without a design yet open a placeholder. */
enum class ProfileDestination(val title: String) {
    VerifyId("Verify ID"),
    Licence("Licence confirmation"),
    TravelPreferences("Travel preferences"),
    Help("Help"),
    Terms("Terms and conditions"),
}

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangeNumber: () -> Unit,
    onWallet: () -> Unit,
    onPaymentHistory: () -> Unit,
    onVehicle: () -> Unit,
    onInfo: (ProfileDestination) -> Unit,
    onSignedOut: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    var paymentsOpen by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (paymentsOpen) 90f else 0f, Motion.bouncy(), label = "chevron")

    Column(Modifier.fillMaxSize().background(Color.White).padding(contentPadding)) {
        LiftTopBar("Profile", onBack = onBack, align = TitleAlign.Center)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 30.dp, end = 28.dp, top = 8.dp).entrance(0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile?.name?.substringBefore(' ').orEmpty(), style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp))
                    Text(profile?.displayPhone.orEmpty(), style = MaterialTheme.typography.bodySmall, color = LiftColors.TextSecondary)
                }
                ProfilePhoto(profile?.photoUri, size = 86.dp)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onEditProfile).padding(horizontal = 30.dp, vertical = 8.dp).entrance(1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Edit personal details", style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary)
                Spacer(Modifier.width(8.dp))
                Icon(LiftIcons.ChevronRight, null, tint = LiftColors.TextSecondary, modifier = Modifier.size(16.dp))
            }
            HorizontalDivider(Modifier.padding(horizontal = 30.dp), color = LiftColors.Divider, thickness = 0.8.dp)
            MenuRow("Change Number", onChangeNumber, Modifier.entrance(2))
            if (vehicle != null) {
                Column(Modifier.entrance(3)) {
                    Text(
                        "VERIFICATION",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 32.dp, top = 18.dp, bottom = 6.dp),
                    )
                    VerificationRow("Verify ID") { onInfo(ProfileDestination.VerifyId) }
                    VerificationRow("Licence Confirmation") { onInfo(ProfileDestination.Licence) }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(Modifier.padding(horizontal = 30.dp), color = LiftColors.Divider, thickness = 0.8.dp)
                }
            }
            MenuRow("Payments", { paymentsOpen = !paymentsOpen }, Modifier.entrance(4)) {
                Icon(LiftIcons.ChevronRight, null, tint = LiftColors.TextSecondary, modifier = Modifier.size(22.dp).rotate(chevron))
            }
            AnimatedVisibility(paymentsOpen, enter = expandVertically(Motion.smooth()) + fadeIn(), exit = shrinkVertically(Motion.smooth()) + fadeOut()) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 8.dp).clip(RoundedCornerShape(10.dp)).background(LiftColors.Muted),
                ) {
                    SubItem("Wallet", onWallet)
                    SubItem("Payment history", onPaymentHistory)
                }
            }
            MenuRow("Vehicle Information", onVehicle, Modifier.entrance(5))
            MenuRow("Travel preferences", { onInfo(ProfileDestination.TravelPreferences) }, Modifier.entrance(6))
            MenuRow("Help", { onInfo(ProfileDestination.Help) }, Modifier.entrance(7))
            MenuRow("Terms and condition", { onInfo(ProfileDestination.Terms) }, Modifier.entrance(8))
            MenuRow("Logout", { confirmLogout = true }, Modifier.entrance(9))
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmLogout) {
        LiftSheet({ confirmLogout = false }) { dismiss ->
            ConfirmSheetContent(
                title = "Log out?",
                message = "You’ll need your phone number and a code to sign back in.",
                primary = "Logout",
                onPrimary = { viewModel.signOut(onSignedOut) },
                secondary = "Stay",
                onSecondary = dismiss,
            )
        }
    }
}

@Composable
private fun VerificationRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(start = 32.dp, end = 120.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LiftIcons.Close, "Not verified", tint = LiftColors.Ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(22.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(LiftIcons.ChevronRight, null, tint = LiftColors.TextSecondary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SubItem(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
    )
}
