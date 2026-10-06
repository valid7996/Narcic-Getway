package dev.cluvex.zedsecure.ui.telemetry

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color

/**
 * Dark surface tokens shared by the sheets (log, subscriptions, vault). These keep the overlays
 * consistent with the dark reference UI; the tokens are styling only.
 */
object Tel {
    val bg = Color(0xFF000000)
    val panel = Color(0xFF0C0C0C)
    val panel2 = Color(0xFF111111)
    val border = Color(0xFF262626)
    val divider = Color(0xFF1C1C1C)
    val text = Color(0xFFE6E6E6)
    val text2 = Color(0xFF9A9A9A)
    val dim = Color(0xFF585858)
    val accent = Color(0xFF4ADE80)
    val info = Color(0xFF60A5FA)
    val warn = Color(0xFFFACC15)
    val error = Color(0xFFF87171)

    /** The mono style for technical values inside the dark sheets. */
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum")
}
