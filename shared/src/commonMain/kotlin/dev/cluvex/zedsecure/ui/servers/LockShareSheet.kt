@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cluvex.zedsecure.crypto.ZsxCrypto
import dev.cluvex.zedsecure.crypto.ZsxSealRequest
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.platform.currentTimeMillis
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.action_close
import dev.cluvex.zedsecure.shared.resources.action_copy
import dev.cluvex.zedsecure.shared.resources.action_share
import dev.cluvex.zedsecure.shared.resources.copied
import dev.cluvex.zedsecure.shared.resources.ic_check
import dev.cluvex.zedsecure.shared.resources.ic_content_paste
import dev.cluvex.zedsecure.shared.resources.ic_ios_share
import dev.cluvex.zedsecure.shared.resources.ic_key
import dev.cluvex.zedsecure.shared.resources.ic_lock
import dev.cluvex.zedsecure.shared.resources.ic_qr_code_2
import dev.cluvex.zedsecure.shared.resources.ic_schedule
import dev.cluvex.zedsecure.shared.resources.ic_sync
import dev.cluvex.zedsecure.shared.resources.qr_too_large
import dev.cluvex.zedsecure.ui.components.PageHeader
import dev.cluvex.zedsecure.ui.components.ProvidePageBack
import dev.cluvex.zedsecure.ui.components.QrDialog
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import kotlin.io.encoding.Base64
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private fun generateUserKey(advanced: Boolean = true): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return if (advanced) {
        val part1 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part3 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        "NG-$part1-$part2-$part3"
    } else {
        val rand = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        "NG-$rand"
    }
}

private fun formatExpiryTimestamp(epochMs: Long): String {
    val diffMs = epochMs - currentTimeMillis()
    if (diffMs <= 0) return "منقضی شده"
    val totalMinutes = diffMs / (60 * 1000L)
    val days = totalMinutes / (24 * 60)
    val remMinutesAfterDays = totalMinutes % (24 * 60)
    val hours = remMinutesAfterDays / 60
    val minutes = remMinutesAfterDays % 60
    return when {
        days > 0 && hours > 0 -> "$days روز و $hours ساعت دیگر"
        days > 0 -> "$days روز دیگر"
        hours > 0 && minutes > 0 -> "$hours ساعت و $minutes دقیقه دیگر"
        hours > 0 -> "$hours ساعت دیگر"
        else -> "$minutes دقیقه دیگر"
    }
}

@Composable
fun LockShareSheet(
    profile: VpnProfile,
    rawPayload: String,
    onDismiss: () -> Unit,
) {
    val platform = LocalPlatform.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var configTitle by remember { mutableStateOf(profile.name) }
    var userNote by remember { mutableStateOf("") }
    var requireKey by remember { mutableStateOf(true) }
    var isAdvancedKey by remember { mutableStateOf(true) }
    var userKey by remember { mutableStateOf(generateUserKey(advanced = true)) }
    var selectedExpiryOption by remember { mutableIntStateOf(5) } // 0: None, 1: 5m, 2: 1h, 3: 6h, 4: 24h, 5: 3d, 6: 7d, 7: 30d, 8: custom
    var customDays by remember { mutableIntStateOf(0) }
    var customHours by remember { mutableIntStateOf(0) }
    var customMinutes by remember { mutableIntStateOf(5) }

    var generatedNrgLink by remember { mutableStateOf<String?>(null) }
    var generatedExpiresAt by remember { mutableStateOf<Long?>(null) }
    var generatedKey by remember { mutableStateOf<String?>(null) }

    var linkCopied by remember { mutableStateOf(false) }
    var keyCopied by remember { mutableStateOf(false) }
    var packageCopied by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.3f) else Color(0xFF3C3C43).copy(alpha = 0.3f)),
            )
        },
    ) {
        ProvidePageBack(onBack = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PageHeader(
                    title = "اشتراک امن و قفل‌شده (NRG)",
                    subtitle = "قفل کانفیگ با کلید یک‌بار مصرف کاربر و زمان انقضا",
                )

                // ─── Settings Card ────────────────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF242426) else Color(0xFFF2F2F7),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = configTitle,
                            onValueChange = { configTitle = it },
                            label = { Text("عنوان کانفیگ") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = userNote,
                            onValueChange = { userNote = it },
                            label = { Text("یادداشت / نام خریدار (اختیاری)") },
                            placeholder = { Text("مثلاً: اختصاصی برای مهندس احمدی") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                        )

                        HorizontalDivider(color = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA))

                        // User Key Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("کلید خصوصی یک‌بار مصرف (User Key)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("فقط یک بار جهت فعال‌سازی روی دستگاه مقصد استفاده می‌شود", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(checked = requireKey, onCheckedChange = { requireKey = it })
                        }

                        AnimatedVisibility(visible = requireKey) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    FilterChip(
                                        selected = isAdvancedKey,
                                        onClick = {
                                            isAdvancedKey = true
                                            userKey = generateUserKey(advanced = true)
                                        },
                                        label = { Text("کلید پیشرفته (۱۲ کاراکتر)", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFF9500).copy(alpha = 0.2f),
                                            selectedLabelColor = Color(0xFFFF9500),
                                        ),
                                    )
                                    FilterChip(
                                        selected = !isAdvancedKey,
                                        onClick = {
                                            isAdvancedKey = false
                                            userKey = generateUserKey(advanced = false)
                                        },
                                        label = { Text("کلید ساده (۶ کاراکتر)", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFFF9500).copy(alpha = 0.2f),
                                            selectedLabelColor = Color(0xFFFF9500),
                                        ),
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = userKey,
                                        onValueChange = { userKey = it },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("کلید خصوصی اختصاصی") },
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9500),
                                        ),
                                    )
                                    OutlinedButton(
                                        onClick = { userKey = generateUserKey(advanced = isAdvancedKey) },
                                        modifier = Modifier.height(52.dp),
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Icon(painter = painterResource(Res.drawable.ic_sync), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("تولید", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA))

                        // Expiration Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = painterResource(Res.drawable.ic_schedule), contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("مدت اعتبار و تاریخ انقضا (Expiration):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OptIn(ExperimentalLayoutApi::class)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val expiryOptions = listOf(
                                    0 to "دائمی (بدون انقضا)",
                                    1 to "۵ دقیقه (تست سریع)",
                                    2 to "۱ ساعت (تست)",
                                    3 to "۶ ساعت",
                                    4 to "۲۴ ساعت (۱ روز)",
                                    5 to "۳ روز",
                                    6 to "۷ روز (یک هفته)",
                                    7 to "۳۰ روز (یک ماه)",
                                    8 to "سفارشی",
                                )
                                expiryOptions.forEach { (code, label) ->
                                    val isSelected = selectedExpiryOption == code
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedExpiryOption = code },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF007AFF).copy(alpha = 0.2f),
                                            selectedLabelColor = Color(0xFF007AFF),
                                        ),
                                    )
                                }
                            }

                            AnimatedVisibility(visible = selectedExpiryOption == 8) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("تنظیم دقیق روز، ساعت و دقیقه اعتبار:", fontSize = 11.sp, color = Color.Gray)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = if (customDays > 0) customDays.toString() else "",
                                            onValueChange = { customDays = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 },
                                            label = { Text("روز") },
                                            placeholder = { Text("0") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true,
                                        )
                                        OutlinedTextField(
                                            value = if (customHours > 0) customHours.toString() else "",
                                            onValueChange = { customHours = it.filter { c -> c.isDigit() }.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                                            label = { Text("ساعت") },
                                            placeholder = { Text("0") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true,
                                        )
                                        OutlinedTextField(
                                            value = if (customMinutes > 0) customMinutes.toString() else "",
                                            onValueChange = { customMinutes = it.filter { c -> c.isDigit() }.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                            label = { Text("دقیقه") },
                                            placeholder = { Text("5") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true,
                                        )
                                    }
                                }
                            }
                        }

                        // Generate Button
                        Button(
                            onClick = {
                                val now = currentTimeMillis()
                                val expiresAt: Long? = when (selectedExpiryOption) {
                                    0 -> null
                                    1 -> now + 5 * 60 * 1000L
                                    2 -> now + 60 * 60 * 1000L
                                    3 -> now + 6 * 3600 * 1000L
                                    4 -> now + 24 * 3600 * 1000L
                                    5 -> now + 3 * 24 * 3600 * 1000L
                                    6 -> now + 7 * 24 * 3600 * 1000L
                                    7 -> now + 30 * 24 * 3600 * 1000L
                                    8 -> {
                                        val totalMs = (customDays * 24 * 3600L + customHours * 3600L + customMinutes * 60L) * 1000L
                                        if (totalMs > 0) now + totalMs else null
                                    }
                                    else -> null
                                }

                                val finalKey = if (requireKey) userKey.trim().ifBlank { null } else null
                                val sealReq = ZsxSealRequest(
                                    configPayload = rawPayload,
                                    nameEn = configTitle,
                                    nameFa = configTitle,
                                    note = userNote,
                                    expiresAt = expiresAt,
                                    password = finalKey,
                                )

                                val sealedBytes = ZsxCrypto.seal(sealReq)
                                val base64 = Base64.encode(sealedBytes)
                                generatedNrgLink = "nrg://$base64"
                                generatedExpiresAt = expiresAt
                                generatedKey = finalKey
                                linkCopied = false
                                keyCopied = false
                                packageCopied = false
                                platform.toast("کانفیگ امن و قفل‌شده با موفقیت تولید شد")
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                        ) {
                            Icon(painter = painterResource(Res.drawable.ic_lock), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تولید کانفیگ قفل‌شده (NRG)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // ─── Result Output Card ────────────────────────────────────────────────────
                AnimatedVisibility(visible = generatedNrgLink != null) {
                    val nrgLink = generatedNrgLink ?: ""
                    val expiresAt = generatedExpiresAt
                    val currentKey = generatedKey

                    val fullPackageText = buildString {
                        appendLine("🔒 کانفیگ اختصاصی و قفل‌شده Narcic Gateway")
                        appendLine("🏷️ نام کانفیگ: $configTitle")
                        appendLine("⏳ تاریخ انقضا: ${if (expiresAt != null) formatExpiryTimestamp(expiresAt) else "دائمی (بدون انقضا)"}")
                        if (currentKey != null) {
                            appendLine("🔑 کلید خصوصی یک‌بار مصرف شما: $currentKey")
                            appendLine("⚠️ توجه: این کلید اختصاصی و یک‌بار مصرف است و فقط روی دستگاه شما فعال می‌شود.")
                        }
                        if (userNote.isNotBlank()) {
                            appendLine("📝 یادداشت: $userNote")
                        }
                        appendLine()
                        appendLine("🔗 لینک کانفیگ:")
                        appendLine(nrgLink)
                        appendLine()
                        appendLine("جهت فعال‌سازی، لینک بالا را کپی کرده و در برنامه Narcic Gateway وارد نمایید.")
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) Color(0xFF1E261E) else Color(0xFFEDF8ED),
                        border = BorderStroke(1.dp, Color(0xFF34C759).copy(alpha = 0.4f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("✔ کانفیگ آماده اشتراک است", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF34C759))
                                Text(
                                    if (expiresAt != null) "انقضا: ${formatExpiryTimestamp(expiresAt)}" else "دائمی",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                )
                            }

                            // Link Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF161618) else Color(0xFFFFFFFF),
                                border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        nrgLink,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 2,
                                        color = if (isDark) Color.LightGray else Color.DarkGray,
                                    )
                                    IconButton(
                                        onClick = {
                                            platform.copyToClipboard(nrgLink)
                                            linkCopied = true
                                            platform.toast("لینک کانفیگ کپی شد")
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            painter = painterResource(if (linkCopied) Res.drawable.ic_check else Res.drawable.ic_content_paste),
                                            contentDescription = "Copy Link",
                                            tint = Color(0xFFFF9500),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }

                            // Key Box (if password protected)
                            if (currentKey != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0xFF28241D) else Color(0xFFFFF8EC),
                                    border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.3f)),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(painter = painterResource(Res.drawable.ic_key), contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text("کلید اختصاصی این کاربر:", fontSize = 11.sp, color = Color.Gray)
                                                Text(currentKey, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                platform.copyToClipboard(currentKey)
                                                keyCopied = true
                                                platform.toast("کلید کاربر کپی شد")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp),
                                        ) {
                                            Text(if (keyCopied) "کپی شد" else "کپی کلید", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // Share and QR Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        platform.copyToClipboard(fullPackageText)
                                        packageCopied = true
                                        platform.toast("پیام کامل اشتراک در کلیپ‌بورد کپی شد")
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                ) {
                                    Icon(painter = painterResource(if (packageCopied) Res.drawable.ic_check else Res.drawable.ic_content_paste), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (packageCopied) "کپی شد" else "کپی پیام برای کاربر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { platform.shareText(fullPackageText) },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(painter = painterResource(Res.drawable.ic_ios_share), contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                OutlinedButton(
                                    onClick = { showQrDialog = true },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(painter = painterResource(Res.drawable.ic_qr_code_2), contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog && generatedNrgLink != null) {
        val link = generatedNrgLink!!
        QrDialog(
            title = configTitle,
            text = link,
            copyLabel = stringResource(Res.string.action_copy),
            shareLabel = stringResource(Res.string.action_share),
            closeLabel = stringResource(Res.string.action_close),
            unsupportedLabel = stringResource(Res.string.qr_too_large),
            onCopy = {
                platform.copyToClipboard(link)
                platform.toast("لینک کپی شد")
            },
            onShare = { platform.shareText(link) },
            onDismiss = { showQrDialog = false },
        )
    }
}
