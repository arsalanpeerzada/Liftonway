package dev.liftonway.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.liftonway.app.push.Push
import dev.liftonway.core.designsystem.animation.LiftAnimation

@HiltAndroidApp
class LiftOnWayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Parse the animations while the splash plays, so no screen stalls on its first frame.
        LiftAnimation.preload(this)
        Push.start(this)
    }
}
