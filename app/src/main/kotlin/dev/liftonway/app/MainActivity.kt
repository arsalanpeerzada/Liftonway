package dev.liftonway.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.liftonway.app.push.Push
import dev.liftonway.core.designsystem.theme.LiftTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Set when the app was opened from a push notification; the shell then shows the Notifications tab. */
    private var openNotifications by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        openNotifications = intent.opensNotifications()
        // The app is light only, so bar icons are always dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            LiftTheme { LiftOnWayApp(openNotifications = openNotifications, onOpened = { openNotifications = false }) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.opensNotifications()) openNotifications = true
    }

    private fun Intent.opensNotifications() = getStringExtra(Push.EXTRA_OPEN) == Push.OPEN_NOTIFICATIONS
}
