package dev.cluvex.zedsecure.ui.navigation

/**
 * The switches that hide parts of the interface without removing them. Flip one back to true and
 * the feature returns as it was.
 */
object NavConfig {

    /** The vault tab of the bottom bar; false keeps the screen and everything behind it in place. */
    const val SHOW_VAULT: Boolean = false

    /** The active-config card of the home screen. */
    const val SHOW_ACTIVE_CONFIG: Boolean = false
}
