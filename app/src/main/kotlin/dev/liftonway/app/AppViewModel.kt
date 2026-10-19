package dev.liftonway.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.liftonway.app.push.Push
import dev.liftonway.app.push.PushRegistrar
import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.NotificationRepository
import dev.liftonway.core.data.Session
import dev.liftonway.core.data.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val notifications: NotificationRepository,
    private val push: PushRegistrar,
) : ViewModel() {
    /** Null until DataStore answers, so the first screen is chosen only once. */
    val session: StateFlow<Session?> = sessions.session.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Requests waiting for an answer plus anything unread, for the dot on the Notifications tab. */
    val pending: StateFlow<Int> = notifications.items
        .map { list -> list.count { it.actionable || !it.read } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** After sign-in: registers this phone for push, and says whether to ask for the notification permission (once). */
    suspend fun setUpPush(): Boolean {
        push.sync()
        if (!Push.enabled || sessions.askedNotifications()) return false
        sessions.markAskedNotifications()
        return true
    }

    /**
     * Keeps the notifications fresh while the app is on screen, as a fallback for phones where push is off.
     * The caller runs this only while started, so nothing polls in the background.
     */
    suspend fun keepNotificationsFresh() {
        while (true) {
            try {
                notifications.refresh()
            } catch (_: ApiError) {
                // Offline or signed out: try again next round.
            }
            delay(30.seconds)
        }
    }
}
