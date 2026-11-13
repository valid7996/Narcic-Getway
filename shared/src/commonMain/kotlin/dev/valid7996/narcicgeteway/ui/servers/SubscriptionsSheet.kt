@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.valid7996.narcicgeteway.ui.servers

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material3.HorizontalDivider
import dev.valid7996.narcicgeteway.shared.resources.ic_keyboard_arrow_down
import dev.valid7996.narcicgeteway.shared.resources.bento_move_earlier
import dev.valid7996.narcicgeteway.shared.resources.bento_move_later
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.*
import dev.valid7996.narcicgeteway.data.config.ConfigRepository
import dev.valid7996.narcicgeteway.ui.platform.LocalPlatform
import org.jetbrains.compose.resources.getString
import dev.valid7996.narcicgeteway.ui.components.SectionTitle
import dev.valid7996.narcicgeteway.ui.format.formatBytes
import androidx.compose.ui.graphics.luminance
import kotlinx.coroutines.launch

@Composable
fun SubscriptionsSheet(
    repository: ConfigRepository,
    onDismiss: () -> Unit,
) {
    val platform = LocalPlatform.current
    val scope = rememberCoroutineScope()
    val subs by repository.subscriptions.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var busyId by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<dev.valid7996.narcicgeteway.domain.config.Subscription?>(null) }
    var deleteTarget by remember { mutableStateOf<dev.valid7996.narcicgeteway.domain.config.Subscription?>(null) }
    var qrTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.subs_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(vertical = 4.dp),
            )

            SectionTitle(stringResource(Res.string.subs_add))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.subs_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(Res.string.subs_url)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            UserAgentPicker(userAgent) { userAgent = it }
            Button(
                onClick = {
                    val sub = repository.addSubscription(name, url, userAgent)
                    name = ""; url = ""; userAgent = ""
                    busyId = sub.id
                    scope.launch {
                        repository.updateSubscription(sub.id)
                            .onSuccess {
                                platform.toast(getString(Res.string.servers_imported, it))

                                onDismiss()
                            }
                            .onFailure {
                                platform.toast(getString(Res.string.subs_failed))
                            }
                        busyId = null
                    }
                },
                enabled = url.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(stringResource(Res.string.subs_add_and_fetch), fontWeight = FontWeight.Bold)
            }

            SectionTitle(stringResource(Res.string.groups_add))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(stringResource(Res.string.groups_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        repository.addGroup(groupName)
                        groupName = ""
                    },
                    enabled = groupName.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.height(50.dp),
                ) { Text(stringResource(Res.string.action_add), fontWeight = FontWeight.Bold) }
            }

            if (subs.isNotEmpty()) {
                SectionTitle(stringResource(Res.string.subs_existing))
                val subCardBorder = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
                val subCardBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                val subCardShape = RoundedCornerShape(14.dp)
                subs.forEachIndexed { index, sub ->
                    Surface(
                        shape = subCardShape,
                        color = subCardBg,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, subCardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(
                          Modifier.fillMaxWidth(),
                      ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        sub.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    sub.formatQuotaBadge()?.let { badge ->
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0x2610B981),
                                            shape = CircleShape,
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                        ) {
                                            Text(
                                                badge,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    when {
                                        sub.serverCount > 0 -> stringResource(Res.string.servers_count, sub.serverCount)

                                        sub.url.isBlank() -> stringResource(Res.string.groups_add)
                                        else -> sub.url
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Switch(
                                checked = sub.enabled,
                                onCheckedChange = { repository.setSubscriptionEnabled(sub.id, it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF34C759),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = if (isDark) Color(0xFF39393D) else Color(0xFFE9E9EA),
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                            )
                            if (busyId == sub.id) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = Color(0xFF007AFF))
                                }
                            } else {
                                IconButton(onClick = {
                                    busyId = sub.id
                                    scope.launch {
                                        repository.updateSubscription(sub.id)
                                        busyId = null
                                    }
                                }) {
                                    Icon(
                                        painterResource(Res.drawable.ic_sync),
                                        contentDescription = stringResource(Res.string.subs_update),
                                    )
                                }
                            }

                            // جابه‌جایی سابسکرپشن به بالا
                            IconButton(
                                onClick = { repository.moveSubscription(sub.id, moveUp = true) },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    painterResource(Res.drawable.ic_keyboard_arrow_down),
                                    contentDescription = stringResource(Res.string.bento_move_earlier),
                                    modifier = Modifier.size(20.dp).rotate(180f),
                                    tint = if (index > 0) (if (isDark) Color.White else Color(0xFF333333)) else Color.Gray.copy(alpha = 0.25f),
                                )
                            }

                            // جابه‌جایی سابسکرپشن به پایین
                            IconButton(
                                onClick = { repository.moveSubscription(sub.id, moveUp = false) },
                                enabled = index < subs.lastIndex,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    painterResource(Res.drawable.ic_keyboard_arrow_down),
                                    contentDescription = stringResource(Res.string.bento_move_later),
                                    modifier = Modifier.size(20.dp),
                                    tint = if (index < subs.lastIndex) (if (isDark) Color.White else Color(0xFF333333)) else Color.Gray.copy(alpha = 0.25f),
                                )
                            }

                            if (!ConfigRepository.isDefaultSubscription(sub.id)) {
                                IconButton(onClick = { qrTarget = sub.name to sub.url }) {
                                    Icon(
                                        painterResource(Res.drawable.ic_qr_code_2),
                                        contentDescription = stringResource(Res.string.action_share_qr),
                                    )
                                }
                                IconButton(onClick = { renameTarget = sub }) {
                                    Icon(
                                        painterResource(Res.drawable.ic_edit),
                                        contentDescription = stringResource(Res.string.subs_rename),
                                    )
                                }
                            }
                            IconButton(onClick = { deleteTarget = sub }) {
                                Icon(
                                    painterResource(Res.drawable.ic_delete),
                                    contentDescription = stringResource(Res.string.action_delete),
                                    tint = Color(0xFFFF3B30),
                                )
                            }
                        }
                        SubscriptionUsage(sub, onOpen = { platform.openUri(it) })
                      }
                    }
                }
            }
        }
    }

    qrTarget?.let { (qrTitle, qrText) ->
        dev.valid7996.narcicgeteway.ui.components.QrDialog(
            title = qrTitle,
            text = qrText,
            copyLabel = stringResource(Res.string.action_copy),
            shareLabel = stringResource(Res.string.action_share),
            closeLabel = stringResource(Res.string.action_close),
            unsupportedLabel = stringResource(Res.string.qr_too_large),
            onCopy = { platform.copyToClipboard(qrText) },
            onShare = { platform.shareText(qrText) },
            onDismiss = { qrTarget = null },
        )
    }

    renameTarget?.let { target ->
        SubscriptionEditDialog(
            target = target,
            repository = repository,
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { target ->
        SubscriptionDeleteConfirmDialog(
            target = target,
            repository = repository,
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun PresetField(label: org.jetbrains.compose.resources.StringResource, value: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UserAgentPicker(value: String, onChange: (String) -> Unit) {
    val default = ConfigRepository.DEFAULT_UA
    val alt = ConfigRepository.ALT_UA
    val singBox = ConfigRepository.SING_BOX_UA
    val isDefault = value.isBlank() || value == default
    val isAlt = value == alt
    val isSingBox = value == singBox
    var custom by remember(value) { mutableStateOf(!isDefault && !isAlt && !isSingBox) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(Res.string.subs_ua_preset),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = isDefault && !custom,
                onClick = { custom = false; onChange("") },
                label = { Text(stringResource(Res.string.subs_ua_v2rayng), maxLines = 1) },
            )
            FilterChip(
                selected = isAlt && !custom,
                onClick = { custom = false; onChange(alt) },
                label = { Text(stringResource(Res.string.subs_ua_narcicgeteway), maxLines = 1) },
            )
            FilterChip(
                selected = isSingBox && !custom,
                onClick = { custom = false; onChange(singBox) },
                label = { Text("sing-box", maxLines = 1) },
            )
            FilterChip(
                selected = custom,
                onClick = { custom = true },
                label = { Text(stringResource(Res.string.subs_ua_custom), maxLines = 1) },
            )
        }
        if (custom) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(stringResource(Res.string.subs_user_agent)) },
                placeholder = { Text(default) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            stringResource(Res.string.subs_ua_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubscriptionUsage(
    sub: dev.valid7996.narcicgeteway.domain.config.Subscription,
    onOpen: (String) -> Unit,
) {
    val used = sub.used
    val total = sub.total
    val expireAt = sub.expireAt
    if (used == null && total == null && expireAt == null && sub.supportUrl == null && sub.webPageUrl == null) return

    val now = dev.valid7996.narcicgeteway.platform.currentTimeMillis()
    val fraction = if (used != null && total != null && total > 0) (used.toDouble() / total).toFloat() else null
    val expired = expireAt != null && expireAt <= now
    val daysLeft = expireAt?.let { ((it - now) / 86_400_000L).toInt() }
    val exhausted = fraction != null && fraction >= 1f
    val warning = !expired && !exhausted && ((fraction != null && fraction >= 0.8f) || (daysLeft != null && daysLeft <= 3))
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val stateColor = when {
        expired || exhausted -> MaterialTheme.colorScheme.error
        warning -> if (dark) androidx.compose.ui.graphics.Color(0xFFFFB74D) else androidx.compose.ui.graphics.Color(0xFF9A5B00)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val usageText = when {
                used != null && total != null ->
                    stringResource(Res.string.subs_usage, formatBytes(used), formatBytes(total))
                used != null -> stringResource(Res.string.subs_usage_unlimited, formatBytes(used))
                else -> null
            }
            Text(
                usageText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val expiryText = when {
                expired -> stringResource(Res.string.subs_expired)
                exhausted -> stringResource(Res.string.subs_quota_exhausted)
                daysLeft == null -> null
                daysLeft <= 0 -> stringResource(Res.string.subs_expires_today)
                daysLeft == 1 -> stringResource(Res.string.subs_one_day_left)
                else -> stringResource(Res.string.subs_days_left, daysLeft)
            }
            if (expiryText != null) {
                Text(
                    expiryText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expired || exhausted || warning) stateColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (fraction != null) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                color = stateColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
        }
        if (sub.supportUrl != null || sub.webPageUrl != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sub.supportUrl?.let { url ->
                    androidx.compose.material3.AssistChip(
                        onClick = { onOpen(url) },
                        label = { Text(stringResource(Res.string.subs_support)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.ic_info), null, Modifier.size(18.dp)) },
                    )
                }
                sub.webPageUrl?.let { url ->
                    androidx.compose.material3.AssistChip(
                        onClick = { onOpen(url) },
                        label = { Text(stringResource(Res.string.subs_website)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.ic_public), null, Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionEditDialog(
    target: dev.valid7996.narcicgeteway.domain.config.Subscription,
    repository: ConfigRepository,
    onDismiss: () -> Unit,
    onSaved: ((dev.valid7996.narcicgeteway.domain.config.Subscription) -> Unit)? = null,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scope = rememberCoroutineScope()
    var newName by remember(target.id) { mutableStateOf(target.name) }
    var newUrl by remember(target.id) { mutableStateOf(target.url) }
    var newUa by remember(target.id) { mutableStateOf(target.userAgent.orEmpty()) }
    var preset by remember(target.id) { mutableStateOf(target.preset) }
    var presetOpen by remember(target.id) { mutableStateOf(!target.preset.isEmpty) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        title = {
            Text(
                stringResource(Res.string.subs_edit),
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color.Black,
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(Res.string.subs_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text(stringResource(Res.string.subs_url)) },
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                UserAgentPicker(newUa) { newUa = it }
                TextButton(onClick = { presetOpen = !presetOpen }) {
                    Text(stringResource(Res.string.subs_preset_title))
                }
                if (presetOpen) {
                    Text(
                        stringResource(Res.string.subs_preset_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PresetField(Res.string.subs_preset_address, preset.address) { preset = preset.copy(address = it) }
                    PresetField(Res.string.subs_preset_fingerprint, preset.fingerprint) { preset = preset.copy(fingerprint = it) }
                    PresetField(Res.string.subs_preset_alpn, preset.alpn) { preset = preset.copy(alpn = it) }
                    PresetField(Res.string.subs_preset_finalmask, preset.finalMask, singleLine = false) {
                        preset = preset.copy(finalMask = it)
                    }
                    PresetField(Res.string.subs_preset_ciphers, preset.cipherSuites) { preset = preset.copy(cipherSuites = it) }
                    PresetField(Res.string.subs_preset_ech, preset.echConfigList) { preset = preset.copy(echConfigList = it) }
                    dev.valid7996.narcicgeteway.ui.components.PickerField(
                        label = stringResource(Res.string.subs_preset_core),
                        options = listOf(
                            "" to stringResource(Res.string.subs_preset_core_link),
                            dev.valid7996.narcicgeteway.domain.config.SubscriptionPreset.CORE_XRAY to "Xray",
                            dev.valid7996.narcicgeteway.domain.config.SubscriptionPreset.CORE_SINGBOX to stringResource(Res.string.subs_preset_core_singbox),
                        ),
                        selected = preset.core,
                        onSelect = { preset = preset.copy(core = it) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val changed = preset != target.preset
                    repository.editSubscription(target.id, newName, newUa, newUrl, preset)
                    onDismiss()
                    if (changed) {
                        scope.launch {
                            repository.updateSubscription(target.id)
                        }
                    }
                    val updated = target.copy(
                        name = newName.trim().ifEmpty { target.name },
                        url = newUrl.trim().ifEmpty { target.url },
                        userAgent = newUa.trim().takeIf { it.isNotBlank() },
                        preset = preset,
                    )
                    onSaved?.invoke(updated)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF007AFF)),
            ) { Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF007AFF)),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
fun SubscriptionDeleteConfirmDialog(
    target: dev.valid7996.narcicgeteway.domain.config.Subscription,
    repository: ConfigRepository,
    onDismiss: () -> Unit,
    onDeleted: (() -> Unit)? = null,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        icon = { Icon(painterResource(Res.drawable.ic_delete), null, tint = Color(0xFFFF3B30)) },
        title = {
            Text(
                stringResource(Res.string.subs_delete_confirm_title),
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color.Black,
            )
        },
        text = {
            Text(
                stringResource(Res.string.subs_delete_confirm_body, target.name),
                color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    repository.removeSubscription(target.id, alsoRemoveServers = true)
                    onDismiss()
                    onDeleted?.invoke()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30)),
            ) { Text(stringResource(Res.string.action_delete), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF007AFF)),
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

/**
 * منوی اختصاصی عملیات سابسکرپشن با فشار دادن روی تب یا کارت ساب در لیست کانفیگ
 */
@Composable
fun SubscriptionActionSheet(
    sub: dev.valid7996.narcicgeteway.domain.config.Subscription,
    isFirst: Boolean,
    isLast: Boolean,
    serverCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onUpdate: () -> Unit,
    onShareQr: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // سربرگ مشخصات سابسکرپشن
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (serverCount > 0) stringResource(Res.string.servers_count, serverCount) else sub.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                sub.formatQuotaBadge()?.let { badge ->
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0x2610B981),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                modifier = Modifier.padding(bottom = 6.dp),
            )

            // عملیات ویرایش
            ActionSheetRow(
                icon = Res.drawable.ic_edit,
                title = stringResource(Res.string.subs_edit),
                onClick = { onDismiss(); onEdit() },
            )

            // عملیات جابه‌جایی به قبل (چپ)
            ActionSheetRow(
                icon = Res.drawable.ic_keyboard_arrow_down,
                iconRotate = 90f,
                title = stringResource(Res.string.bento_move_earlier),
                enabled = !isFirst,
                onClick = { onMoveEarlier() },
            )

            // عملیات جابه‌جایی به بعد (راست)
            ActionSheetRow(
                icon = Res.drawable.ic_keyboard_arrow_down,
                iconRotate = -90f,
                title = stringResource(Res.string.bento_move_later),
                enabled = !isLast,
                onClick = { onMoveLater() },
            )

            // عملیات به‌روزرسانی
            ActionSheetRow(
                icon = Res.drawable.ic_sync,
                title = stringResource(Res.string.subs_update),
                onClick = { onDismiss(); onUpdate() },
            )

            // عملیات اشتراک‌گذاری QR
            if (sub.url.isNotBlank()) {
                ActionSheetRow(
                    icon = Res.drawable.ic_qr_code_2,
                    title = stringResource(Res.string.action_share_qr),
                    onClick = { onDismiss(); onShareQr() },
                )
            }

            // عملیات حذف سابسکرپشن
            ActionSheetRow(
                icon = Res.drawable.ic_delete,
                title = stringResource(Res.string.action_delete),
                isDestructive = true,
                onClick = { onDismiss(); onDelete() },
            )
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: org.jetbrains.compose.resources.DrawableResource,
    title: String,
    iconRotate: Float = 0f,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val contentColor = when {
        !enabled -> Color.Gray.copy(alpha = 0.4f)
        isDestructive -> Color(0xFFFF3B30)
        isDark -> Color.White
        else -> Color(0xFF0F172A)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp).rotate(iconRotate),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp,
                ),
                color = contentColor,
            )
        }
    }
}
