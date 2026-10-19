package dev.liftonway.app.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.liftonway.app.BuildConfig
import dev.liftonway.app.MainActivity
import dev.liftonway.app.R
import dev.liftonway.core.data.ApiError
import dev.liftonway.core.data.AppScope
import dev.liftonway.core.data.NotificationRepository
import dev.liftonway.core.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase, started by hand from the values build.gradle.kts reads out of google-services.json. Builds without
 * that file have push switched off and everything else works as usual.
 */
object Push {
    const val CHANNEL = "rides"
    const val EXTRA_OPEN = "liftonway.open"
    const val OPEN_NOTIFICATIONS = "notifications"

    val enabled: Boolean get() = BuildConfig.FIREBASE_APP_ID.isNotEmpty()

    fun start(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Rides and requests")
                .setDescription("Seat requests, answers and changes to your rides")
                .build(),
        )
        if (!enabled || FirebaseApp.getApps(context).isNotEmpty()) return
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build(),
        )
    }

    /** Whether the app may post notifications (always before Android 13, which added the permission). */
    fun allowed(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Shows one notification. Tapping it opens the Notifications tab. On a locked phone only "New activity"
     * shows until it is unlocked, since the text names people and places.
     */
    @SuppressLint("MissingPermission") // Checked by allowed().
    fun show(context: Context, tag: String, title: String, body: String) {
        if (!allowed(context)) return
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN, OPEN_NOTIFICATIONS)
        val tap = PendingIntent.getActivity(context, tag.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val brand = ContextCompat.getColor(context, R.color.brand_green)
        val locked = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_lift)
            .setColor(brand)
            .setContentTitle("Lift On Way")
            .setContentText("New activity on your rides")
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_lift)
            .setColor(brand)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(locked)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(tag, 0, notification)
    }
}

/** Gets this install's push token and registers it for the signed-in user. */
@Singleton
class PushRegistrar @Inject constructor(private val session: SessionRepository) {
    suspend fun sync() {
        if (!Push.enabled) return
        try {
            session.registerDevice(FirebaseMessaging.getInstance().token.await())
        } catch (_: ApiError) {
            // Offline: Firebase hands the token over again on the next start, and onNewToken on any change.
        } catch (_: Exception) {
            // Play services missing or Firebase unreachable: push simply stays off on this phone.
        }
    }
}

/** Receives pushes and token changes from Firebase. */
@AndroidEntryPoint
class LiftMessagingService : FirebaseMessagingService() {
    @Inject lateinit var session: SessionRepository

    @Inject lateinit var notifications: NotificationRepository

    @Inject
    @AppScope
    lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        scope.launch {
            try {
                session.registerDevice(token)
            } catch (_: ApiError) {
                // Registered on the next start instead.
            }
        }
    }

    /** The server sends data messages, so this runs in the foreground and the background alike. */
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: return
        Push.show(this, data["notificationId"] ?: title, title, data["body"].orEmpty())
        scope.launch {
            try {
                notifications.refresh()
            } catch (_: ApiError) {
                // The list refreshes when the app is next opened.
            }
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (task.isSuccessful) continuation.resume(task.result) else continuation.resumeWithException(error ?: IllegalStateException("Task failed"))
    }
}
