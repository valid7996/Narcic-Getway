package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.cluvex.zedsecure.data.tools.IpScannerEngine
import dev.cluvex.zedsecure.data.tools.IpStatus
import dev.cluvex.zedsecure.data.tools.OperatorFilter
import dev.cluvex.zedsecure.data.tools.ScannedIp
import dev.cluvex.zedsecure.domain.model.AppSettings
import dev.cluvex.zedsecure.ui.components.PageHeader
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import kotlinx.coroutines.Job

@Composable
fun IpScannerScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val platform = LocalPlatform.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scope = rememberCoroutineScope()

    var selectedOperator by remember { mutableStateOf(OperatorFilter.All) }
    var portText by remember { mutableStateOf("443") }
    var customCidr by remember { mutableStateOf("") }

    var isScanning by remember { mutableStateOf(false) }
    var scannedCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }
    var currentIp by remember { mutableStateOf("") }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    val results = remember { mutableStateListOf<ScannedIp>() }

    fun stopScan() {
        activeJob?.cancel()
        activeJob = null
        isScanning = false
    }

    fun startScan() {
        stopScan()
        results.clear()
        scannedCount = 0
        val targetPort = portText.toIntOrNull() ?: 443
        val candidateIps = IpScannerEngine.getCandidateIps(selectedOperator, customCidr)
        totalCount = candidateIps.size
        isScanning = true

        activeJob = IpScannerEngine.startScan(
            scope = scope,
            ips = candidateIps,
            port = targetPort,
            operator = selectedOperator,
            onProgress = { scanned, total, ip, newResult ->
                scannedCount = scanned
                currentIp = ip
                if (newResult != null) {
                    if (newResult.status == IpStatus.Clean || newResult.status == IpStatus.Valid) {
                        results.add(0, newResult)
                    }
                }
            },
            onComplete = {
                isScanning = false
                currentIp = ""
            },
        )
    }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val accentColor = Color(0xFF007AFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7))
            .padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PageHeader(
                    title = "اسکنر آی‌پی تمیز (CDN Scanner)",
                    subtitle = "Cloudflare & CDN Clean IP Finder",
                )
            }

            // Operator Segmented Control
            item {
                Text(
                    text = "انتخاب اپراتور یا شبکه هدف",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(0.5.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OperatorFilter.entries.forEach { op ->
                            val isSelected = op == selectedOperator
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentColor else Color.Transparent)
                                    .clickable {
                                        selectedOperator = op
                                        if (isScanning) startScan()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = op.titleEn,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFFE5E5EA) else Color(0xFF1C1C1E)),
                                )
                            }
                        }
                    }
                }
            }

            // Config parameters (Port + Custom CIDR)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(0.5.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = portText,
                                onValueChange = { portText = it },
                                label = { Text("پورت اسکن") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = customCidr,
                                onValueChange = { customCidr = it },
                                label = { Text("رنج دلخواه (اختیاری)") },
                                placeholder = { Text("مثال: 104.16.0.0/16") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(2f),
                            )
                        }
                    }
                }
            }

            // Radar & Scanning Status Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(0.5.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isScanning) Color(0xFF34C759) else Color(0xFF8E8E93)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isScanning) "در حال اسکن زنده..." else "آماده برای اسکن",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFFE5E5EA) else Color(0xFF1C1C1E),
                                )
                            }
                            val cleanCount = results.count { it.status == IpStatus.Clean }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF34C759).copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "سالم: $cleanCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34C759),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        val progress = if (totalCount > 0) (scannedCount.toFloat() / totalCount) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accentColor,
                            trackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (currentIp.isNotEmpty()) "بررسی: $currentIp" else "اسکن‌شده: $scannedCount از $totalCount",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                            )
                        }
                    }
                }
            }

            // Quick Actions Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { if (isScanning) stopScan() else startScan() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) Color(0xFFFF3B30) else accentColor,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1.3f).height(46.dp),
                    ) {
                        Text(
                            text = if (isScanning) "توقف اسکن" else "شروع اسکن",
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        enabled = results.isNotEmpty(),
                        onClick = {
                            val top16 = results
                                .filter { it.pingMs > 0 }
                                .sortedBy { it.pingMs }
                                .take(16)
                                .joinToString("\n") { it.ip }
                            if (top16.isNotEmpty()) {
                                platform.copyToClipboard(top16)
                                platform.toast("۱۶ آی‌پی برتر کپی شد")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                            contentColor = if (isDark) Color.White else Color.Black,
                        ),
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) {
                        Text("کپی ۱۶ برتر", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        enabled = results.isNotEmpty(),
                        onClick = {
                            results.sortBy { if (it.pingMs > 0) it.pingMs else Long.MAX_VALUE }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                            contentColor = if (isDark) Color.White else Color.Black,
                        ),
                        modifier = Modifier.weight(0.9f).height(46.dp),
                    ) {
                        Text("مرتب‌سازی", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Scanned IPs List
            if (results.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isScanning) "در حال جستجو و پینگ آی‌پی‌ها..." else "دکمه «شروع اسکن» را بزنید تا آی‌پی‌های تمیز لیست شوند",
                            color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                items(results, key = { it.ip }) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = cardBg,
                        border = BorderStroke(0.5.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.ip,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = ":${item.port}",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                                    )
                                }
                                Text(
                                    text = item.operator,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val pillBg = when (item.status) {
                                    IpStatus.Clean -> Color(0xFF34C759).copy(alpha = 0.15f)
                                    IpStatus.Valid -> Color(0xFFFF9500).copy(alpha = 0.15f)
                                    IpStatus.Failed -> Color(0xFFFF3B30).copy(alpha = 0.15f)
                                }
                                val pillColor = when (item.status) {
                                    IpStatus.Clean -> Color(0xFF34C759)
                                    IpStatus.Valid -> Color(0xFFFF9500)
                                    IpStatus.Failed -> Color(0xFFFF3B30)
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = pillBg,
                                ) {
                                    Text(
                                        text = if (item.pingMs > 0) "${item.pingMs} ms" else "ناموفق",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }

                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                                        .clickable {
                                            platform.copyToClipboard(item.ip)
                                            platform.toast("آی‌پی ${item.ip} کپی شد")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "کپی",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else Color.Black,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
