@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.data.assets.GeoAsset
import dev.cluvex.zedsecure.data.assets.GeoAssets
import dev.cluvex.zedsecure.domain.model.AppSettings
import dev.cluvex.zedsecure.domain.model.GeoFilesSource
import dev.cluvex.zedsecure.ui.components.PageHeader
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import kotlinx.coroutines.launch

@Composable
fun AssetsScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit = {},
) {
    val platform = LocalPlatform.current
    val geo = platform.geoAssets
    val scope = androidx.compose.runtime.rememberCoroutineScope()

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

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Spacer(Modifier.height(8.dp))
            PageHeader(
                title = stringResource(Res.string.assets_title),
                subtitle = stringResource(Res.string.assets_subtitle),
                accentLine = true,
            )
            Spacer(Modifier.height(14.dp))

        if (geo == null) {
            Text(
                stringResource(Res.string.assets_subtitle),
                Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        var assets by remember { mutableStateOf(geo.state()) }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            assets.forEach { asset ->
                AssetCard(
                    asset = asset,
                    onImport = {
                        scope.launch {
                            val pick = platform.pickFileBytes() ?: return@launch
                            busy = true
                            val ok = runCatching { geo.importBytes(pick.bytes, asset.name) }.getOrDefault(false)
                            assets = geo.state()
                            busy = false
                            message = getString(if (ok) Res.string.assets_done else Res.string.assets_failed)
                        }
                    },
                )
            }

            Spacer(Modifier.height(4.dp))

            SettingsGroup {
                SettingsListRow(
                    title = stringResource(Res.string.assets_source),
                    options = GeoFilesSource.entries.map { it to it.repo },
                    selected = settings.geoFilesSource,
                    onSelected = { v -> onUpdate { it.copy(geoFilesSource = v) } },
                )
            }
            Text(
                stringResource(Res.string.assets_source_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.assets_bundled_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            if (busy) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ContainedLoadingIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.assets_downloading))
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            val ok = geo.downloadAll(settings.geoFilesSource)
                            assets = geo.state()
                            busy = false
                            message = getString(if (ok) Res.string.assets_done else Res.string.assets_failed)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) dev.cluvex.zedsecure.ui.theme.ZedGreen else Color(0xFF0D9488),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(Res.string.assets_download), fontWeight = FontWeight.Bold)
                }
            }

            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color(0xFF60E0B0) else Color(0xFF0D9488),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble() / 1024
    var i = 0
    while (value >= 1024 && i < units.size - 1) { value /= 1024; i++ }
    val rounded = (value * 10).toLong() / 10.0
    return "$rounded ${units[i]}"
}

@Composable
private fun AssetCard(asset: GeoAsset, onImport: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(22.dp)
    val cardBorder = if (isDark) {
        Brush.horizontalGradient(
            listOf(Color(0xFF20D8C0).copy(alpha = 0.50f), Color(0xFF2088FF).copy(alpha = 0.35f)),
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color(0xFF0D9488).copy(alpha = 0.28f), Color(0xFF3B82F6).copy(alpha = 0.20f)),
        )
    }
    val cardBg = if (isDark) {
        Brush.horizontalGradient(
            listOf(Color(0xFF0F243A).copy(alpha = 0.90f), Color(0xFF0A1828).copy(alpha = 0.82f)),
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.96f), Color(0xFFF1F5F9).copy(alpha = 0.90f)),
        )
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(Res.drawable.ic_description),
                contentDescription = null,
                tint = if (asset.present) (if (isDark) Color(0xFF60E0B0) else Color(0xFF0D9488))
                else (if (isDark) Color(0xFF8899A6) else Color(0xFF94A3B8)),
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                )
                Text(
                    if (asset.present) {
                        stringResource(Res.string.assets_size, formatBytes(asset.sizeBytes), "")
                    } else {
                        stringResource(Res.string.assets_missing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFA0B0C4) else Color(0xFF64748B),
                )
            }
            OutlinedButton(
                onClick = onImport,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(Res.string.assets_import))
            }
        }
    }
}
