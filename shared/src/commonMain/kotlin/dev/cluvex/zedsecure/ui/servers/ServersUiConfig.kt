package dev.cluvex.zedsecure.ui.servers

/**
 * The switches that hide parts of the servers interface without removing them. Flip one back to
 * true and the feature returns as it was.
 */
object ServersUiConfig {

    /** The share-link and share-QR items of a server card's menu. */
    const val SHOW_SHARE_ACTIONS: Boolean = false

    /** Every import and engine option of the add sheet; false leaves the clipboard and subscriptions. */
    const val SHOW_ALL_ADD_OPTIONS: Boolean = false
}
