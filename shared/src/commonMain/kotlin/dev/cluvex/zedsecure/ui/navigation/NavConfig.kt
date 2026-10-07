package dev.cluvex.zedsecure.ui.navigation

/**
 * The switches that hide parts of the interface without removing them. Flip one back to true and
 * the feature returns as it was.
 */
object NavConfig {

    /** The active-config card of the home screen. */
    const val SHOW_ACTIVE_CONFIG: Boolean = false

    /** The servers tab of the bottom bar; the list itself lives on the home screen. */
    const val SHOW_SERVERS_TAB: Boolean = false

    /** Every settings page; false leaves About, the VPN page and the core page. */
    const val SHOW_ALL_SETTINGS: Boolean = false
}
