package dev.liftonway.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

private const val PACKAGE = "io.github.arsalanpeerzada.liftonway"
private const val TIMEOUT = 5_000L
private const val SETTLE_MS = 700L // A little longer than the app's 480 ms screen transition.

/**
 * Walks the journeys people use most, so their code is compiled ahead of time in release builds:
 * cold start and onboarding, sign-up, a ride search, trips, notifications, profile and wallet.
 *
 * Generate with an emulator or device on API 33+ connected:  ./gradlew :app:generateBaselineProfile
 *
 * The class name is short on purpose: it goes into the output file name, and on Windows the full path back
 * from the device can otherwise pass the 260-character limit and the profile silently fails to copy.
 */
@RunWith(AndroidJUnit4::class)
class Journeys {
    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE) {
        pressHome()
        startActivityAndWait()
        device.reachHome()
        device.searchForARide()
        device.browseTabs()
    }
}

private fun UiDevice.waitFor(selector: BySelector, timeout: Long = TIMEOUT) = wait(Until.findObject(selector), timeout)
    ?: error("Not on screen: $selector. Showing: ${findObjects(By.text(Pattern.compile(".+"))).joinToString { it.text }}")

private fun UiDevice.tapDesc(description: String) {
    waitFor(By.desc(description)).click()
    waitForIdle()
    Thread.sleep(SETTLE_MS)
}

/** Taps, then lets the app settle, since taps that land mid-transition are ignored. */
private fun UiDevice.tap(text: String) {
    waitFor(By.text(text)).click()
    waitForIdle()
    Thread.sleep(SETTLE_MS)
}

/**
 * Waits for [marker] and for the slide-in to finish, so typing can't land in the screen that's leaving
 * (both are on screen while a transition runs).
 */
private fun UiDevice.arriveAt(marker: String) {
    waitFor(By.text(marker))
    waitForIdle()
    Thread.sleep(SETTLE_MS)
}

/** Taps a place in the suggestion list, not the text field that may hold the same words. */
private fun UiDevice.pickSuggestion(title: String) = waitFor(By.clazz("android.widget.TextView").text(title)).click()

/** Fills the nth text field on screen; Compose text fields show up as EditTexts. */
private fun UiDevice.type(index: Int, text: String) {
    wait(Until.hasObject(By.clazz("android.widget.EditText")), TIMEOUT)
    val field = findObjects(By.clazz("android.widget.EditText"))[index]
    field.click() // Focus it first: the place suggestions only unfold under a focused field.
    field.text = text
}

/**
 * The rule runs the journey several times and reinstalls over existing data, so the app may open on the
 * walkthrough, on Sign up / Sign in, or straight on Home. Each case is walked from wherever it lands.
 */
private fun UiDevice.reachHome() {
    // The splash plays first, and a cold start on a busy emulator can be slow, so allow longer here.
    val landing = waitFor(By.text(Pattern.compile("Skip|Sign in|Find a ride")), timeout = 15_000).text
    waitForIdle()
    Thread.sleep(SETTLE_MS) // The splash fades out on top of the first screen; let it finish.
    when (landing) {
        "Skip" -> {
            repeat(2) { tapDesc("Next") }
            tapDesc("Get started")
            tapDesc("I agree to the ride sharing terms and conditions")
            tap("START")
            signUp()
        }
        "Sign in" -> signUp()
    }
    // Verifying, then the success screen, take a few seconds before Home.
    waitFor(By.text("Find a ride"), timeout = 15_000)
    waitForIdle()
    Thread.sleep(SETTLE_MS)
}

private fun UiDevice.signUp() {
    tap("Sign up")
    arriveAt("Full Name")
    type(0, "Profile Run")
    type(1, "3001234567") // Text set this way opens no keyboard, so there's nothing to dismiss.
    tap("Sign up")
    arriveAt("Verify")
    type(0, "1234")
    tap("Verify")
}

/** Taps the screen's own back or close button, which is more dependable than a system back during animations. */
private fun UiDevice.goBack(desc: String = "Back") {
    waitFor(By.desc(desc)).click()
    waitForIdle()
    Thread.sleep(SETTLE_MS)
}

private fun UiDevice.searchForARide() {
    tap("Find a ride")
    arriveAt("Search a ride")
    type(0, "F-7")
    pickSuggestion("F-7 Markaz")
    type(1, "Saddar")
    pickSuggestion("Saddar")
    tap("Search a ride")
    // Late in the day there may be nothing left today; the "No rides available" page is a valid result too.
    waitFor(By.text(Pattern.compile("Request|No rides available")))
    findObject(By.scrollable(true))?.fling(Direction.DOWN)
    waitForIdle()
    goBack()
    goBack("Close")
}

private fun UiDevice.browseTabs() {
    tap("My trips")
    tap("Requested rides")
    arriveAt("UPCOMING")
    swipe(displayWidth * 4 / 5, displayHeight / 2, displayWidth / 5, displayHeight / 2, 12)
    waitForIdle()
    goBack()
    tap("Notifications")
    waitFor(By.text("Request for a ride"))
    tap("Profile")
    tap("Payments")
    tap("Wallet")
    arriveAt("Available credit")
    tap("History")
    arriveAt("Transaction history")
    goBack()
    goBack()
}
