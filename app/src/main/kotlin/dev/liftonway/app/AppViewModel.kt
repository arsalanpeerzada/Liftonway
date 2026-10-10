package dev.liftonway.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.core.data.NotificationRepository
import dev.liftonway.core.data.Session
import dev.liftonway.core.data.SessionRepository
import dev.liftonway.core.model.NotificationKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(session: SessionRepository, notifications: NotificationRepository) : ViewModel() {
    /** Null until DataStore answers, so the first screen is chosen only once. */
    val session: StateFlow<Session?> = session.session.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Requests still waiting for an answer, for the dot on the Notifications tab. */
    val pending: StateFlow<Int> = notifications.items
        .map { list -> list.count { !it.answered && (it.kind == NotificationKind.RideRequest || it.kind == NotificationKind.PaymentRequest) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
