@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.settings

import org.jetbrains.compose.resources.DrawableResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.platform.AppInfo
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import dev.cluvex.zedsecure.ui.components.LiquidCircle
import dev.cluvex.zedsecure.ui.theme.ZedCyan
import dev.cluvex.zedsecure.ui.theme.ZedLime
import dev.cluvex.zedsecure.ui.theme.ZedViolet

private const val TELEGRAM_URL = "https://t.me/Narcic_team"
private const val GITHUB_URL = "https://github.com/valid7996/Narcic-Getway"

private const val XRAY_SOURCE_URL = "https://github.com/CluvexStudio/Xray-core"
private const val LIB_SOURCE_URL = "https://github.com/CluvexStudio/AndroidLibXrayLite"

@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val platform = LocalPlatform.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xF20B1C38) else Color(0xF8F8FAFD),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B).copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LiquidCircle(
                colors = listOf(ZedViolet, ZedCyan, ZedLime),
                modifier = Modifier.size(96.dp),
            )
            Text(
                stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
            )
            Text(
                stringResource(Res.string.about_version, AppInfo.versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color(0xFFA0B0C4) else Color(0xFF64748B),
            )
            Spacer(Modifier.size(4.dp))

            LinkRow(
                iconRes = Res.drawable.ic_add_link,
                title = stringResource(Res.string.about_telegram),
                subtitle = "t.me/Narcic_team",
                isDark = isDark,
                onClick = { platform.openUri(TELEGRAM_URL) },
            )
            LinkRow(
                iconRes = Res.drawable.ic_description,
                title = stringResource(Res.string.about_github),
                subtitle = "github.com/valid7996/Narcic-Getway",
                isDark = isDark,
                onClick = { platform.openUri(GITHUB_URL) },
            )

            Spacer(Modifier.size(8.dp))

            if (dev.cluvex.zedsecure.ui.navigation.NavConfig.SHOW_ALL_SETTINGS) {
                Text(
                    stringResource(Res.string.about_source_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF60E0B0) else Color(0xFF0D9488),
                )
                Text(
                    stringResource(Res.string.about_source_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFA0B0C4) else Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                )
                LinkRow(
                    iconRes = Res.drawable.ic_description,
                    title = stringResource(Res.string.about_source_xray),
                    subtitle = "github.com/CluvexStudio/Xray-core",
                    isDark = isDark,
                    onClick = { platform.openUri(XRAY_SOURCE_URL) },
                )
                LinkRow(
                    iconRes = Res.drawable.ic_description,
                    title = stringResource(Res.string.about_source_lib),
                    subtitle = "github.com/CluvexStudio/AndroidLibXrayLite",
                    isDark = isDark,
                    onClick = { platform.openUri(LIB_SOURCE_URL) },
                )
            }

            Text(
                stringResource(Res.string.about_thanks_cluvex),
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) Color(0xFF8899A6) else Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LinkRow(iconRes: DrawableResource, title: String, subtitle: String, isDark: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val cardBorder = if (isDark) {
        Brush.horizontalGradient(
            listOf(Color(0xFF2088FF).copy(alpha = 0.50f), Color(0xFF20D8C0).copy(alpha = 0.30f)),
        )
    } else {
        Brush.horizontalGradient(
            listOf(Color(0xFF3B82F6).copy(alpha = 0.25f), Color(0xFF0D9488).copy(alpha = 0.18f)),
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
        onClick = onClick,
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(iconRes),
                null,
                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF2563EB),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFA0B0C4) else Color(0xFF64748B),
                )
            }
            Icon(
                painterResource(Res.drawable.ic_chevron_right),
                null,
                tint = if (isDark) Color(0xFFC0C0D0) else Color(0xFF94A3B8),
            )
        }
    }
}
