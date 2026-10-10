package dev.liftonway.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.liftonway.core.designsystem.theme.LiftColors
import dev.liftonway.core.designsystem.theme.LiftShapes
import kotlinx.coroutines.launch

/** A bottom sheet in the app's style. Call [dismiss] from the content to slide it away before [onDismiss] runs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiftSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { if (!state.isVisible) onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = LiftShapes.Sheet,
        containerColor = Color.White,
        scrimColor = LiftColors.Ink.copy(alpha = 0.32f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = LiftColors.Hairline) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            content(dismiss)
        }
    }
}

/**
 * The two-button confirmation from the designs ("Request for a ride", "Are you sure you want to cancel?").
 * [primaryEnabled] false greys the primary action out, as when a ride is too close to cancel.
 */
@Composable
fun ColumnScope.ConfirmSheetContent(
    title: String,
    message: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String,
    onSecondary: () -> Unit,
    centred: Boolean = false,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
    primaryFirst: Boolean = true,
) {
    val align = if (centred) TextAlign.Center else TextAlign.Start
    Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = align, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Text(message, style = MaterialTheme.typography.bodyLarge, color = LiftColors.TextSecondary, textAlign = align, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        val primaryButton = @Composable { m: Modifier -> PrimaryButton(primary, onPrimary, m, enabled = primaryEnabled, loading = primaryLoading) }
        val secondaryButton = @Composable { m: Modifier -> OutlineButton(secondary, onSecondary, m) }
        if (primaryFirst) {
            primaryButton(Modifier.weight(1f))
            secondaryButton(Modifier.weight(1f))
        } else {
            secondaryButton(Modifier.weight(1f))
            primaryButton(Modifier.weight(1f))
        }
    }
}
