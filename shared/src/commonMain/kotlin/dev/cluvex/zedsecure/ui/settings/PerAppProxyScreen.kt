@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.domain.model.AppSettings
import dev.cluvex.zedsecure.ui.components.PageHeader
import dev.cluvex.zedsecure.ui.components.PickerField
import dev.cluvex.zedsecure.ui.platform.InstalledApp
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PerAppProxyScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val platform = LocalPlatform.current
    var query by rememberSaveable { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }

    if (!platform.supportsPerAppProxy) {
        Column(modifier.fillMaxSize().padding(contentPadding)) {
            Spacer(Modifier.height(8.dp))
            PageHeader(
                title = stringResource(Res.string.per_app_title),
                subtitle = stringResource(Res.string.per_app_subtitle),
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                Text(
                    stringResource(Res.string.per_app_unsupported),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        return
    }

    val importFailMsg = stringResource(Res.string.per_app_import_failed)
    val exportOkMsg = stringResource(Res.string.per_app_export_ok)

    var imported by remember { mutableStateOf<Int?>(null) }
    imported?.let { count ->
        val msg = stringResource(Res.string.per_app_import_ok, count)
        LaunchedEffect(count) {
            platform.toast(msg)
            imported = null
        }
    }

    val apps by produceState<List<InstalledApp>?>(initialValue = null, showSystem) {
        value = withContext(Dispatchers.IO) {
            platform.installedApps().filter { showSystem || !it.isSystem }
        }
    }

    val mode = when {
        !settings.perAppProxyEnabled -> PerAppMode.Off
        settings.perAppBypassMode -> PerAppMode.Exclude
        else -> PerAppMode.Include
    }

    val visible = remember(apps, query, settings.perAppPackages) {
        val list = apps.orEmpty()
        val filtered = if (query.isBlank()) list else list.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
        filtered.sortedWith(
            compareByDescending<InstalledApp> { it.packageName in settings.perAppPackages }
                .thenBy { it.label.lowercase() },
        )
    }

    fun setSelection(next: Set<String>) {
        onUpdate { it.copy(perAppPackages = next) }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glowColors = if (isDark) {
        listOf(
            Color(0xFF0168D1).copy(alpha = 0.40f),
            Color(0xFF08C6AB).copy(alpha = 0.18f),
            Color.Transparent,
        )
    } else {
        listOf(
            Color(0xFF60A5FA).copy(alpha = 0.20f),
            Color(0xFF2DD4BF).copy(alpha = 0.14f),
            Color.Transparent,
        )
    }

    Box(
        modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF030D18) else Color(0xFFF8FAFC)),
    ) {
        Box(
            Modifier
                .size(360.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = glowColors,
                        radius = 500f,
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Spacer(Modifier.height(8.dp))
            PageHeader(
                title = stringResource(Res.string.per_app_title),
                subtitle = stringResource(Res.string.per_app_subtitle),
                accentLine = true,
            )
            Spacer(Modifier.height(12.dp))

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PickerField(
                label = stringResource(Res.string.per_app_title),
                options = listOf(
                    PerAppMode.Off to stringResource(Res.string.per_app_mode_off),
                    PerAppMode.Include to stringResource(Res.string.per_app_mode_include),
                    PerAppMode.Exclude to stringResource(Res.string.per_app_mode_exclude),
                ),
                selected = mode,
                onSelect = { selected ->
                    onUpdate {
                        it.copy(
                            perAppProxyEnabled = selected != PerAppMode.Off,
                            perAppBypassMode = selected == PerAppMode.Exclude,
                        )
                    }
                },
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.per_app_search)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.per_app_show_system),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = showSystem, onCheckedChange = { showSystem = it })
            }
        }

        Spacer(Modifier.height(8.dp))

        if (apps == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                ContainedLoadingIndicator()
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(key = "per-app-actions") {
                    Text(
                        stringResource(Res.string.per_app_selected, settings.perAppPackages.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { setSelection(settings.perAppPackages + visible.map { it.packageName }) },
                            label = { Text(stringResource(Res.string.per_app_select_all)) },
                        )
                        AssistChip(
                            onClick = { setSelection(settings.perAppPackages - visible.map { it.packageName }.toSet()) },
                            label = { Text(stringResource(Res.string.per_app_select_none)) },
                        )
                        AssistChip(
                            onClick = {
                                val shown = visible.map { it.packageName }.toSet()
                                val kept = settings.perAppPackages - shown
                                setSelection(kept + shown.filterNot { it in settings.perAppPackages })
                            },
                            label = { Text(stringResource(Res.string.per_app_invert)) },
                        )
                        AssistChip(
                            onClick = {
                                val parsed = platform.readClipboard().orEmpty()
                                    .split('\n', ',')
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() && it.none(Char::isWhitespace) }
                                if (parsed.isEmpty()) platform.toast(importFailMsg)
                                else {
                                    setSelection(parsed.toSet())
                                    imported = parsed.size
                                }
                            },
                            label = { Text(stringResource(Res.string.per_app_import)) },
                        )
                        AssistChip(
                            onClick = {
                                platform.copyToClipboard(settings.perAppPackages.sorted().joinToString("\n"))
                                platform.toast(exportOkMsg)
                            },
                            label = { Text(stringResource(Res.string.per_app_export)) },
                        )
                    }

                    Text(
                        stringResource(Res.string.per_app_reconnect_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                items(visible, key = { it.packageName }) { app ->
                    val checked = app.packageName in settings.perAppPackages
                    AppRow(
                        app = app,
                        checked = checked,
                        enabled = settings.perAppProxyEnabled,
                        onToggle = {
                            onUpdate { current ->
                                val next = current.perAppPackages.toMutableSet()
                                if (checked) next.remove(app.packageName)
                                else next.add(app.packageName)
                                current.copy(perAppPackages = next)
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
    }
}

internal enum class PerAppMode { Off, Include, Exclude }

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(16.dp)
    val cardBorder = if (isDark) {
        if (checked) {
            Brush.horizontalGradient(
                listOf(Color(0xFF20D8C0).copy(alpha = 0.60f), Color(0xFF2088FF).copy(alpha = 0.45f)),
            )
        } else {
            Brush.horizontalGradient(
                listOf(Color(0xFF28384F).copy(alpha = 0.25f), Color(0xFF1E293B).copy(alpha = 0.20f)),
            )
        }
    } else {
        if (checked) {
            Brush.horizontalGradient(
                listOf(Color(0xFF0D9488).copy(alpha = 0.40f), Color(0xFF3B82F6).copy(alpha = 0.30f)),
            )
        } else {
            Brush.horizontalGradient(
                listOf(Color(0xFFE2E8F0), Color(0xFFE2E8F0)),
            )
        }
    }
    val cardBg = if (isDark) {
        if (checked) Color(0xFF102840).copy(alpha = 0.85f) else Color(0xFF0A1828).copy(alpha = 0.65f)
    } else {
        if (checked) Color(0xFFF0FDF4) else Color.White
    }

    Surface(
        shape = shape,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, shape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(cardBg)
                .clickable(enabled = enabled, onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val platform = LocalPlatform.current
            val icon by produceState<ByteArray?>(app.iconPng, app.packageName) {
                if (value == null) value = withContext(Dispatchers.IO) { platform.appIcon(app.packageName) }
            }
            AppIcon(iconPng = icon, label = app.label)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFE0E0F0) else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDark) Color(0xFFA0B0C4) else Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
        }
    }
}

@Composable
private fun AppIcon(iconPng: ByteArray?, label: String) {
    if (iconPng != null) {
        val platformContext = LocalPlatformContext.current
        AsyncImage(
            model = remember(iconPng) {
                ImageRequest.Builder(platformContext).data(iconPng).build()
            },
            contentDescription = null,
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)),
        )
    } else {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    label.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
