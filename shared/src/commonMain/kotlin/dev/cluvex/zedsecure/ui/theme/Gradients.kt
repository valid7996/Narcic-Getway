package dev.cluvex.zedsecure.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ZedGradients {
    val connected: Brush
        get() = Brush.linearGradient(
            colors = listOf(ZedViolet, ZedCyan, ZedLime),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )

    private val sessionPalettes: List<List<Color>> = listOf(
        listOf(ZedViolet, ZedCyan, ZedLime),
        listOf(Color(0xFF2E5FE8), ZedCyan, ZedLime),
        listOf(ZedDeepViolet, ZedViolet, ZedCyan),
        listOf(ZedCyan, ZedMint, ZedLime),
        listOf(Color(0xFF1D3A8F), ZedViolet, ZedCyan),
    )

    fun forSession(sessionId: Int): Brush = Brush.linearGradient(
        colors = paletteFor(sessionId),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    private fun paletteFor(sessionId: Int): List<Color> =
        sessionPalettes[((sessionId % sessionPalettes.size) + sessionPalettes.size) % sessionPalettes.size]

    fun connectedStops(sessionId: Int): List<Color> =
        listOf(ZedDeepViolet) + paletteFor(sessionId)

    val connectingStops: List<Color> = listOf(ZedDeepViolet, Color(0xFF1D3A8F), ZedCyan)

    val idle: Brush
        get() = Brush.linearGradient(
            colors = listOf(ZedDeepViolet, Color(0xFF14285C), ZedViolet.copy(alpha = 0.80f)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )

    val connecting: Brush
        get() = Brush.linearGradient(
            colors = listOf(Color(0xFF1D3A8F), ZedCyan, ZedLime),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
}
