@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.valid7996.narcicgeteway.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.*
import dev.valid7996.narcicgeteway.ui.platform.LocalPlatform
import dev.valid7996.narcicgeteway.ui.platform.draggableHorizontalScroll
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class AdbFilter {
    All,
    Errors,
    Core,
}

@Composable
fun AdbLogSheet(onDismiss: () -> Unit) {
    val platform = LocalPlatform.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var rawLogs by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf(AdbFilter.All) }
    var query by remember { mutableStateOf("") }

    val logsCopied = stringResource(Res.string.adb_logs_copied)
    val shareFileName = stringResource(Res.string.adb_logs_share_filename)

    fun refreshLogs() {
        isLoading = true
        scope.launch {
            rawLogs = runCatching { platform.dumpAdbLogs() }.getOrDefault("")
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    val lines = remember(rawLogs, filter, query) {
        if (rawLogs.isBlank()) return@remember emptyList<String>()
        val all = rawLogs.lines()
        val q = query.trim()
        all.filter { line ->
            val matchesFilter = when (filter) {
                AdbFilter.All -> true
                AdbFilter.Errors -> {
                    line.contains(" E/") || line.contains(" F/") ||
                        line.contains("FATAL", ignoreCase = true) ||
                        line.contains("CRASH", ignoreCase = true) ||
                        line.contains("Exception", ignoreCase = true) ||
                        line.contains("Error", ignoreCase = true) ||
                        line.contains("SIGSEGV", ignoreCase = true) ||
                        line.contains("died", ignoreCase = true) ||
                        line.startsWith("🚨") || line.contains("CRASH REPORT")
                }
                AdbFilter.Core -> {
                    line.contains("Narcic", ignoreCase = true) ||
                        line.contains("Vpn", ignoreCase = true) ||
                        line.contains("Xray", ignoreCase = true) ||
                        line.contains("tun", ignoreCase = true) ||
                        line.contains("hev", ignoreCase = true) ||
                        line.contains("mhrv", ignoreCase = true) ||
                        line.contains("tor", ignoreCase = true) ||
                        line.contains("psiphon", ignoreCase = true) ||
                        line.contains("sing-box", ignoreCase = true)
                }
            }
            matchesFilter && (q.isEmpty() || line.contains(q, ignoreCase = true))
        }
    }

    val listState = rememberLazyListState()
    val atBottom by remember { derivedStateOf { !listState.canScrollForward } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_bug_report),
                    contentDescription = null,
                    tint = if (platform.hasRecordedCrash) Color(0xFFFF453A) else Color(0xFFE5A50A),
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    stringResource(Res.string.adb_logs_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.weight(1f),
                )
                if (lines.isNotEmpty()) {
                    Text(
                        "${lines.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Refresh button
                FilledTonalButton(
                    onClick = { refreshLogs() },
                    enabled = !isLoading,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_sync),
                        stringResource(Res.string.adb_logs_refresh),
                        Modifier.height(18.dp),
                    )
                }

                // Copy button
                FilledTonalButton(
                    onClick = {
                        val textToCopy = if (lines.isNotEmpty()) lines.joinToString("\n") else rawLogs
                        platform.copyToClipboard(textToCopy)
                        platform.toast(logsCopied)
                    },
                    enabled = rawLogs.isNotBlank(),
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_content_paste),
                        stringResource(Res.string.logs_copy),
                        Modifier.height(18.dp),
                    )
                }

                // Share button (sends full file)
                FilledTonalButton(
                    onClick = {
                        val textToShare = if (lines.isNotEmpty() && (filter != AdbFilter.All || query.isNotBlank())) {
                            lines.joinToString("\n")
                        } else {
                            rawLogs
                        }
                        platform.shareLogFile(shareFileName, textToShare)
                    },
                    enabled = rawLogs.isNotBlank(),
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_ios_share),
                        stringResource(Res.string.logs_share),
                        Modifier.height(18.dp),
                    )
                }
            }

            // Crash Recorded Banner
            if (platform.hasRecordedCrash) {
                Surface(
                    color = Color(0xFFFF453A).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_bug_report),
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(Res.string.adb_logs_crash_detected),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF453A),
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = filter == AdbFilter.Errors,
                            onClick = { filter = AdbFilter.Errors },
                            label = { Text(stringResource(Res.string.adb_logs_filter_errors)) },
                        )
                    }
                }
            }

            // Search input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(stringResource(Res.string.logs_search)) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_search), null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        FilledTonalIconButton(onClick = { query = "" }) {
                            Icon(painterResource(Res.drawable.ic_close), stringResource(Res.string.action_cancel), Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )

            // Filter Chips
            Row(
                Modifier.fillMaxWidth().draggableHorizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = filter == AdbFilter.All,
                    onClick = { filter = AdbFilter.All },
                    label = { Text(stringResource(Res.string.adb_logs_filter_all)) },
                )
                FilterChip(
                    selected = filter == AdbFilter.Errors,
                    onClick = { filter = AdbFilter.Errors },
                    label = { Text(stringResource(Res.string.adb_logs_filter_errors)) },
                )
                FilterChip(
                    selected = filter == AdbFilter.Core,
                    onClick = { filter = AdbFilter.Core },
                    label = { Text(stringResource(Res.string.adb_logs_filter_core)) },
                )
            }

            // Log Console Box
            Box(Modifier.fillMaxWidth().height(420.dp)) {
                if (isLoading) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            stringResource(Res.string.adb_logs_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (lines.isEmpty()) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr,
                    ) {
                        Text(
                            stringResource(
                                if (rawLogs.isBlank()) Res.string.adb_logs_empty else Res.string.logs_no_match,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp).align(Alignment.Center),
                        )
                    }
                } else {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr,
                    ) {
                        val logBoxShape = RoundedCornerShape(14.dp)
                        Surface(
                            color = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
                            shape = logBoxShape,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                            ),
                            modifier = Modifier.fillMaxWidth().height(420.dp),
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                itemsIndexed(lines, key = { i, line -> "$i:${line.hashCode()}" }) { _, line ->
                                    AdbLogRow(line)
                                }
                            }
                        }
                    }
                }

                if (!atBottom && lines.isNotEmpty() && !isLoading) {
                    FilledTonalButton(
                        onClick = { scope.launch { listState.scrollToItem(lines.lastIndex) } },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    ) {
                        Icon(painterResource(Res.drawable.ic_keyboard_arrow_down), null, Modifier.size(16.dp))
                        Box(Modifier.width(6.dp))
                        Text(stringResource(Res.string.logs_jump_latest), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdbLogRow(line: String) {
    val isHeader = line.startsWith("===") || line.startsWith("---")
    val isFatal = line.startsWith("🚨") || line.contains("CRASH") || line.contains("FATAL") || line.contains("SIGSEGV")
    val isError = isFatal || line.contains(" E/") || line.contains("Exception") || line.contains("Error")
    val isWarn = !isError && (line.contains(" W/") || line.contains("Warn", ignoreCase = true))

    val colour = when {
        isFatal -> Color(0xFFFF453A)
        isError -> MaterialTheme.colorScheme.error
        isWarn -> MaterialTheme.colorScheme.tertiary
        isHeader -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        if (isError || isWarn) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(colour, RoundedCornerShape(2.dp)),
            )
            Box(Modifier.width(6.dp))
        }
        Text(
            text = line,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isHeader || isFatal) FontWeight.Bold else FontWeight.Normal,
            color = colour,
        )
    }
}
