package dev.cluvex.zedsecure.ui.telemetry

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cluvex.zedsecure.domain.model.ConnectionState

/**
 * The telemetry design tokens: pure black, hairline panels, a single phosphor accent, and mono
 * type for every technical value. Nothing here reads from MaterialTheme — the console keeps its
 * own language.
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

    /** The mono style every technical value uses: tabular, always legible. */
    val mono = TextStyle(fontFamily = FontFamily.Monospace, fontFeatureSettings = "tnum")

    fun stateColor(state: ConnectionState): Color = when (state) {
        ConnectionState.Connected -> accent
        ConnectionState.Connecting, ConnectionState.Reconnecting, ConnectionState.Disconnecting -> info
        ConnectionState.Error -> error
        else -> dim
    }

    fun stateWord(state: ConnectionState): String = when (state) {
        ConnectionState.Connected -> "LINK UP"
        ConnectionState.Connecting -> "HANDSHAKE"
        ConnectionState.Reconnecting -> "RELINK"
        ConnectionState.Disconnecting -> "CLOSING"
        ConnectionState.Error -> "LINK FAULT"
        else -> "LINK DOWN"
    }
}

/**
 * A bordered panel with a machine-style header: an accent block, the module title, and optional
 * header actions on the right.
 */
@Composable
fun TelPanel(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Tel.panel)
            .border(1.dp, Tel.border),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).background(Tel.accent))
            Spacer(Modifier.width(8.dp))
            Text(
                title.uppercase(),
                style = TextStyle(fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                color = Tel.text2,
            )
            Spacer(Modifier.weight(1f))
            actions()
        }
        HorizontalDivider(color = Tel.border)
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** The top segmented control: text cells, the selected one marked with a caret and underline. */
@Composable
fun TelTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().background(Tel.panel).border(1.dp, Tel.border)) {
        items.forEachIndexed { index, label ->
            val selected = index == selected
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = (if (selected) "▸ " else "") + label,
                    style = TextStyle(
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = if (selected) Tel.accent else Tel.dim,
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(2.dp)
                        .background(if (selected) Tel.accent else Color.Transparent),
                )
            }
        }
    }
}

/** A key/value line of the console: dim label, mono value. */
@Composable
fun TelKV(label: String, value: String, valueColor: Color = Tel.text, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label.uppercase(),
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
            color = Tel.dim,
            modifier = Modifier.width(86.dp),
        )
        Text(
            value,
            style = Tel.mono.copy(fontSize = 13.sp),
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A flat bordered action cell — the console's button. */
@Composable
fun TelAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    filled: Boolean = false,
) {
    val color = when {
        !enabled -> Tel.dim
        danger -> Tel.error
        filled -> Color(0xFF04120C)
        else -> Tel.accent
    }
    Box(
        modifier
            .border(
                1.dp,
                when {
                    filled -> Tel.accent
                    danger -> Tel.error.copy(alpha = 0.6f)
                    else -> Tel.border
                },
            )
            .background(if (filled && enabled) Tel.accent else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text.uppercase(),
            style = TextStyle(
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            ),
            color = color,
        )
    }
}

/** The live state line: dot + machine word, pulsing while a handshake runs. */
@Composable
fun TelStatusLine(state: ConnectionState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "tel-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "tel-alpha",
    )
    val shown = if (state == ConnectionState.Connecting || state == ConnectionState.Reconnecting) alpha else 1f
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .graphicsLayer { this.alpha = shown }
                .background(Tel.stateColor(state), androidx.compose.foundation.shape.CircleShape),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            Tel.stateWord(state),
            style = Tel.mono.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = Tel.stateColor(state),
        )
    }
}


/** The console input: a dark bordered cell with a dim label above and mono values inside. */
@Composable
fun TelField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    mono: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
            color = Tel.dim,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .background(Tel.panel2)
                .border(1.dp, Tel.border)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                minLines = minLines,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    color = Tel.text,
                ),
                cursorBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Tel.accent, Tel.accent),
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
                ),
                visualTransformation = visualTransformation,
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) {
                Text(
                    text = "—",
                    style = Tel.mono.copy(fontSize = 13.sp),
                    color = Tel.dim,
                )
            }
        }
    }
}

/** The console selector: a cell that opens a machine dropdown. */
@Composable
fun <T> TelPicker(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: ""

    Column(modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
            color = Tel.dim,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .background(Tel.panel2)
                .border(1.dp, Tel.border)
                .clickable { open = true }
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Text(
                text = selectedLabel,
                style = Tel.mono.copy(fontSize = 13.sp),
                color = Tel.text,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "▾",
                style = Tel.mono.copy(fontSize = 12.sp),
                color = Tel.dim,
            )
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                containerColor = Tel.panel2,
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text,
                                style = Tel.mono.copy(fontSize = 12.sp),
                                color = if (value == selected) Tel.accent else Tel.text,
                            )
                        },
                        onClick = {
                            onSelect(value)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

/** The console switch: a square indicator cell. */
@Composable
fun TelToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(),
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
            color = Tel.dim,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(38.dp)
                .height(18.dp)
                .border(1.dp, if (checked) Tel.accent else Tel.border)
                .background(if (checked) Tel.accent.copy(alpha = 0.15f) else Tel.panel2)
                .clickable { onChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(12.dp)
                    .background(if (checked) Tel.accent else Tel.dim),
            )
        }
    }
}
