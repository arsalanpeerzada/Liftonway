package dev.liftonway.feature.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.liftonway.core.designsystem.component.LiftTextField
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.ProfilePhoto
import dev.liftonway.core.designsystem.component.TextAction
import dev.liftonway.core.designsystem.component.TitleAlign
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.Motion
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.designsystem.theme.LiftShapes

private val pickImage = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

/** The photo and name editor ("Profile" with Update). */
@Composable
fun EditProfileRoute(onBack: () -> Unit, viewModel: EditProfileViewModel = hiltViewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::onPhotoPicked) }
    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        LiftTopBar("Profile", onBack = onBack, align = TitleAlign.Center)
        Column(Modifier.fillMaxWidth().padding(horizontal = 46.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            // Only the photo is clipped round, so the camera badge can sit over its edge as designed.
            Box(Modifier.entrance(0)) {
                AnimatedContent(viewModel.photoUri, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() }, label = "photo") {
                    ProfilePhoto(it, Modifier.clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Change photo") { picker.launch(pickImage) }, size = 92.dp)
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = 8.dp).size(34.dp).clip(CircleShape).background(Color.White).border(1.dp, LiftColors.Hairline, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Change photo") { picker.launch(pickImage) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LiftIcons.Camera, null, tint = LiftColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(36.dp))
            LiftTextField(
                viewModel.name,
                { viewModel.name = it },
                "Full Name",
                Modifier.fillMaxWidth().entrance(1),
                textAlign = TextAlign.Center,
                imeAction = ImeAction.Done,
                onImeAction = { viewModel.save(onBack) },
                isError = viewModel.loaded && viewModel.name.isBlank(),
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Update", { viewModel.save(onBack) }, Modifier.fillMaxWidth().entrance(2), enabled = viewModel.name.isNotBlank(), loading = viewModel.saving)
        }
    }
}

/** Two steps, the current number then the new one, before the code check. */
@Composable
fun ChangeNumberRoute(onBack: () -> Unit, onCodeSent: () -> Unit, viewModel: ChangeNumberViewModel = hiltViewModel()) {
    BackHandler(enabled = viewModel.enteringNew) { viewModel.back(onBack) }
    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        LiftTopBar("Change number", onBack = { viewModel.back(onBack) }, align = TitleAlign.Center)
        AnimatedContent(
            viewModel.enteringNew,
            transitionSpec = {
                val forward = targetState
                (slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { if (forward) -it / 3 else it / 3 } + fadeOut())
            },
            label = "step",
        ) { enteringNew ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 44.dp)) {
                Spacer(Modifier.height(80.dp))
                Text(
                    if (enteringNew) "Enter your new phone number here" else "Enter your existing phone number here",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                LiftTextField(
                    viewModel.number,
                    viewModel::onNumber,
                    "Phone number",
                    Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    onImeAction = { viewModel.next(onCodeSent) },
                    isError = viewModel.error != null,
                )
                AnimatedVisibility(viewModel.error != null) {
                    Text(viewModel.error.orEmpty(), style = MaterialTheme.typography.bodySmall, color = LiftColors.Coral, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(26.dp))
                PrimaryButton("Continue", { viewModel.next(onCodeSent) }, Modifier.fillMaxWidth(), enabled = viewModel.number.isNotEmpty(), loading = viewModel.busy)
            }
        }
    }
}

/** Add, view and edit the car. A saved car opens read-only; Edit unlocks it. */
@Composable
fun VehicleRoute(onBack: () -> Unit, viewModel: VehicleViewModel = hiltViewModel()) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(viewModel::onPhotoPicked) }
    val editing = viewModel.editing
    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        LiftTopBar(
            "Vehicle information",
            onBack = onBack,
            action = { if (!editing) TextAction("Edit", viewModel::edit) },
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 44.dp)) {
            VehiclePhoto(viewModel.photoUri, editing, Modifier.entrance(0)) { picker.launch(pickImage) }
            Spacer(Modifier.height(24.dp))
            Field("Car Name", viewModel.name, { viewModel.name = it }, "Toyota - Corolla", editing, viewModel.missing(viewModel.name), 1)
            Field("Model", viewModel.model, { viewModel.model = it }, "2020", editing, viewModel.missing(viewModel.model), 2, KeyboardType.Number)
            Field("Number Plate", viewModel.plate, { viewModel.plate = it }, "ABC 123", editing, viewModel.missing(viewModel.plate), 3)
            Field("Color", viewModel.color, { viewModel.color = it }, "Green", editing, viewModel.missing(viewModel.color), 4)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).entrance(5), horizontalArrangement = Arrangement.SpaceBetween) {
                Check("Air condition", viewModel.airConditioning, editing) { viewModel.airConditioning = it }
                Check("Heater", viewModel.heater, editing) { viewModel.heater = it }
                Spacer(Modifier.width(1.dp))
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                if (viewModel.isNew) "Save" else "Update",
                { viewModel.save(onBack) },
                Modifier.fillMaxWidth().entrance(6),
                enabled = editing,
                loading = viewModel.saving,
            )
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun VehiclePhoto(photoUri: String?, editing: Boolean, modifier: Modifier, onPick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(shape)
            .then(
                if (photoUri == null) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            LiftColors.Hint,
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(enabled = editing, role = Role.Button, onClickLabel = "Choose a photo of the car", onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri == null) {
            Icon(LiftIcons.Plus, null, tint = LiftColors.Hint, modifier = Modifier.size(28.dp))
        } else {
            AsyncImage(photoUri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            AnimatedVisibility(editing, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Icon(LiftIcons.Camera, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    editing: Boolean,
    error: Boolean,
    index: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(Modifier.padding(bottom = 14.dp).entrance(index)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        LiftTextField(value, onChange, placeholder, Modifier.fillMaxWidth(), enabled = editing, isError = error, keyboardType = keyboardType)
    }
}

@Composable
private fun Check(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.clip(LiftShapes.Control).clickable(enabled = enabled, role = Role.Checkbox) { onChange(!checked) }.padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).border(1.2.dp, LiftColors.Ink, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(checked, enter = scaleIn(Motion.bouncy()) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(LiftIcons.Check, null, tint = LiftColors.Ink, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** A plain page for profile links whose screens aren't in the designs yet. */
@Composable
fun InfoRoute(destination: ProfileDestination, onBack: () -> Unit) {
    val body = when (destination) {
        ProfileDestination.Terms -> "The terms and conditions will appear here once Legal has provided the final text."
        ProfileDestination.Help -> "Help articles and a way to contact support will appear here."
        ProfileDestination.TravelPreferences -> "Preferences such as music, smoking and pets will be set here."
        ProfileDestination.VerifyId, ProfileDestination.Licence ->
            "Document checks will happen here. They need a verification provider and a data protection review before they're built."
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        LiftTopBar(destination.title, onBack = onBack)
        Column(Modifier.padding(horizontal = 28.dp, vertical = 16.dp)) {
            Text("Not designed yet", style = MaterialTheme.typography.titleSmall, modifier = Modifier.entrance(0))
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = LiftColors.TextSecondary, modifier = Modifier.entrance(1))
        }
    }
}
