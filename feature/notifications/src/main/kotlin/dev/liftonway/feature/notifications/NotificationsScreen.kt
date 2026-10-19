package dev.liftonway.feature.notifications

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.AnswerResult
import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.NotificationRepository
import dev.liftonway.core.designsystem.animation.LiftAnimation
import dev.liftonway.core.designsystem.component.ConfirmSheetContent
import dev.liftonway.core.designsystem.component.EmptyState
import dev.liftonway.core.designsystem.component.IconAction
import dev.liftonway.core.designsystem.component.LiftSheet
import dev.liftonway.core.designsystem.component.LiftTopBar
import dev.liftonway.core.designsystem.component.MutedButton
import dev.liftonway.core.designsystem.component.PrimaryButton
import dev.liftonway.core.designsystem.component.TitleAlign
import dev.liftonway.core.designsystem.icon.LiftIcons
import dev.liftonway.core.designsystem.motion.entrance
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.model.AppNotification
import dev.liftonway.core.model.NotificationKind
import dev.liftonway.core.model.notificationStamp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repository: NotificationRepository) : ViewModel() {
    val items: StateFlow<List<AppNotification>> = repository.items
    val busy = mutableStateMapOf<String, Boolean>()
    var asking by mutableStateOf<AppNotification?>(null)

    /** Set when the list couldn't be loaded; shown when there's nothing else to show. */
    var failure by mutableStateOf<String?>(null)
        private set

    /** Unread when the screen opened; they keep their dot until the user leaves. */
    val fresh = mutableStateListOf<String>()

    /** Runs each time the tab is shown. */
    fun load() {
        viewModelScope.launch {
            fresh.clear()
            failure = try {
                repository.refresh()
                null
            } catch (e: ApiError) {
                e.message
            }
            fresh.addAll(items.value.filter { !it.read }.map { it.id })
            try {
                repository.markAllRead()
            } catch (_: ApiError) {
                // Harmless: they're marked next time.
            }
        }
    }

    fun ago(item: AppNotification): String {
        val d = Duration.between(item.at, repository.now())
        return when {
            d.toMinutes() < 1 -> "Just now"
            d.toHours() < 1 -> "${d.toMinutes()}m ago"
            d.toDays() < 1 -> "${d.toHours()}h ago"
            else -> "${d.toDays()}d ago"
        }
    }

    fun answer(item: AppNotification, accept: Boolean, onNoCredit: () -> Unit, onError: (String) -> Unit) {
        if (busy[item.id] == true) return
        viewModelScope.launch {
            busy[item.id] = true
            try {
                if (repository.answer(item.id, accept) == AnswerResult.NotEnoughCredit) onNoCredit()
            } catch (e: ApiError) {
                onError(e.message)
            } finally {
                busy.remove(item.id)
            }
        }
    }

    fun remove(item: AppNotification, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.remove(item.id)
            } catch (e: ApiError) {
                onError(e.message)
            }
        }
    }
}

@Composable
fun NotificationsRoute(onBack: () -> Unit, onOpenWallet: () -> Unit, contentPadding: PaddingValues, viewModel: NotificationsViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noCredit: () -> Unit = {
        scope.launch {
            if (snackbar.showSnackbar("Not enough credit in your wallet", actionLabel = "Top up") == SnackbarResult.ActionPerformed) onOpenWallet()
        }
    }
    val showError: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    LaunchedEffect(Unit) { viewModel.load() }

    Box(Modifier.fillMaxSize().background(LiftColors.Canvas).padding(contentPadding)) {
        Column(Modifier.fillMaxSize()) {
            LiftTopBar("Notifications", Modifier.background(Color.White), onBack = onBack, align = TitleAlign.Center)
            val failure = viewModel.failure
            if (items.isEmpty() && failure != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(failure, Modifier.padding(bottom = 80.dp), action = "Try again", onAction = viewModel::load)
                }
            } else if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState("You’re all caught up", Modifier.padding(bottom = 80.dp), animation = LiftAnimation.Bell)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(items, key = { _, it -> it.id }) { i, item ->
                        SwipeAway(onRemove = { viewModel.remove(item, showError) }, modifier = Modifier.animateItem().entrance(i.coerceAtMost(5))) {
                            NotificationCard(
                                item,
                                viewModel.ago(item),
                                fresh = item.id in viewModel.fresh,
                                busy = viewModel.busy[item.id] == true,
                                onAnswer = { accept -> viewModel.answer(item, accept, noCredit, showError) },
                                onOpen = { if (item.kind == NotificationKind.RideRequest && item.actionable) viewModel.asking = item },
                                onDelete = { viewModel.remove(item, showError) },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    viewModel.asking?.let { item ->
        LiftSheet({ viewModel.asking = null }) { dismiss ->
            ConfirmSheetContent(
                title = "Request for a ride",
                message = "${item.personName} ${item.message}\n${item.at.notificationStamp()}",
                primary = "Yes",
                onPrimary = {
                    viewModel.answer(item, true, noCredit, showError)
                    dismiss()
                },
                secondary = "No",
                onSecondary = {
                    viewModel.answer(item, false, noCredit, showError)
                    dismiss()
                },
                centred = true,
                primaryFirst = false,
            )
        }
    }
}

/** Swipe a notification sideways to clear it. */
@Composable
private fun SwipeAway(onRemove: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) onRemove()
    }
    SwipeToDismissBox(
        state,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(LiftColors.RedSoft).padding(horizontal = 28.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(LiftIcons.Trash, "Delete", tint = LiftColors.Coral)
            }
        },
        modifier = modifier,
    ) { content() }
}

private data class Look(val icon: ImageVector, val tint: Color, val bg: Color, val title: String, val confirm: Color)

private fun look(kind: NotificationKind) = when (kind) {
    NotificationKind.RideRequest -> Look(LiftIcons.Check, LiftColors.Blue, LiftColors.BlueSoft, "Request for a ride", LiftColors.Blue)
    NotificationKind.RequestAccepted -> Look(LiftIcons.Check, LiftColors.Green, LiftColors.GreenSoft, "Request accepted", LiftColors.Green)
    NotificationKind.RequestDeclined -> Look(LiftIcons.Close, LiftColors.Coral, LiftColors.RedSoft, "Request declined", LiftColors.Coral)
    NotificationKind.RequestWithdrawn -> Look(LiftIcons.Close, LiftColors.Amber, LiftColors.AmberSoft, "Request canceled", LiftColors.Amber)
    NotificationKind.RideCancelled -> Look(LiftIcons.Alert, LiftColors.Coral, LiftColors.RedSoft, "Ride canceled", LiftColors.Coral)
    NotificationKind.RideCompleted -> Look(LiftIcons.Car, LiftColors.Blue, LiftColors.BlueSoft, "Ride completed", LiftColors.Blue)
    NotificationKind.PaymentRequest -> Look(LiftIcons.Card, LiftColors.Amber, LiftColors.AmberSoft, "Payment confirmation", LiftColors.Amber)
    NotificationKind.PaymentSent -> Look(LiftIcons.ArrowUpLeft, LiftColors.Coral, LiftColors.RedSoft, "Payment done", LiftColors.Coral)
    NotificationKind.PaymentReceived -> Look(LiftIcons.ArrowDownRight, LiftColors.Green, LiftColors.GreenSoft, "Payment received", LiftColors.Green)
}

@Composable
private fun NotificationCard(
    item: AppNotification,
    ago: String,
    fresh: Boolean,
    busy: Boolean,
    onAnswer: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val look = look(item.kind)
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(Color.White).clickable(role = Role.Button, onClick = onOpen).padding(start = 18.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(look.bg), contentAlignment = Alignment.Center) {
            Icon(look.icon, null, tint = look.tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(look.title, style = MaterialTheme.typography.titleSmall)
                if (fresh) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(LiftColors.Green).semantics { contentDescription = "New" })
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LiftColors.Ink)) { append(item.personName) }
                    append(" ${item.message}")
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Text(item.at.notificationStamp(), style = MaterialTheme.typography.bodySmall)
            Text(ago, style = MaterialTheme.typography.bodySmall, color = LiftColors.Hint)
            if (item.actionable || item.outcome != null) {
                AnimatedContent(
                    item.outcome,
                    transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform() },
                    label = "answer",
                ) { outcome ->
                    if (outcome != null) {
                        val positive = outcome == "Accepted" || outcome == "Paid"
                        val tint = if (positive) LiftColors.Green else LiftColors.Hint
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (positive) LiftIcons.Check else LiftIcons.Close, null, tint = tint, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(outcome, style = MaterialTheme.typography.bodySmall, color = tint)
                        }
                    } else {
                        Row(Modifier.padding(top = 12.dp, end = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton("Confirm", { onAnswer(true) }, Modifier.weight(1f), color = look.confirm, height = 32.dp, loading = busy)
                            MutedButton("Decline", { onAnswer(false) }, Modifier.weight(1f), height = 32.dp, enabled = !busy)
                        }
                    }
                }
            }
        }
        Box {
            IconAction(LiftIcons.More, "More options", { menu = true }, tint = LiftColors.Hint)
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Delete") }, onClick = {
                    menu = false
                    onDelete()
                })
            }
        }
    }
}
