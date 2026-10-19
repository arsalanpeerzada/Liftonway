package dev.liftonway.core.data

/**
 * A failure the screens can show. [code] is the backend's stable problem code (such as "CANCEL_WINDOW_CLOSED"),
 * and [message] is already fit to show to the user.
 */
class ApiError(val code: String, override val message: String) : Exception(message) {
    companion object {
        const val OFFLINE = "OFFLINE"
        const val UNKNOWN = "UNKNOWN"
    }
}
