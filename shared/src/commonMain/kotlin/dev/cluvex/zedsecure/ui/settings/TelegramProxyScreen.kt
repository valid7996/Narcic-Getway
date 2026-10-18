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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dev.cluvex.zedsecure.data.tools.TelegramProxyItem
import dev.cluvex.zedsecure.data.tools.TelegramProxyRepository
import dev.cluvex.zedsecure.domain.model.AppSettings
import dev.cluvex.zedsecure.ui.components.PageHeader
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import kotlinx.coroutines.launch

@Composable
fun TelegramProxyScreen(
    settings: AppSettings,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val platform = LocalPlatform.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scope = rememberCoroutineScope()

    val proxies = remember { mutableStateListOf<TelegramProxyItem>().apply { addAll(TelegramProxyRepository.getInitialProxies()) } }
    var isTestingAll by remember { mutableStateOf(false) }

    fun testAll() {
        if (isTestingAll) return
        isTestingAll = true
        scope.launch {
            val updated = TelegramProxyRepository.pingAll(proxies)
            proxies.clear()
            proxies.addAll(updated)
            isTestingAll = false
        }
    }

    fun testSingle(index: Int) {
        val target = proxies.getOrNull(index) ?: return
        scope.launch {
            proxies[index] = target.copy(isTesting = true)
            val res = TelegramProxyRepository.pingSingle(target)
            proxies[index] = res
        }
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
                    title = "پروکسی‌های تلگرام (Telegram Proxies)",
                    subtitle = "MTProto Fast Channels & Direct Connect",
                )
            }

            // Summary Info Card
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
                                        .background(Color(0xFF34C759)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${proxies.size} پروکسی آماده اتصال",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isDark) Color(0xFFE5E5EA) else Color(0xFF1C1C1E),
                                )
                            }

                            val bestPing = proxies.filter { it.pingMs > 0 }.minOfOrNull { it.pingMs }
                            if (bestPing != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF34C759).copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text = "بهترین: $bestPing ms",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34C759),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "با کلیک روی «اتصال در تلگرام»، اپلیکیشن تلگرام باز شده و پروکسی به‌طور خودکار فعال می‌گردد.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            // Controls Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { testAll() },
                        enabled = !isTestingAll,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1.3f).height(46.dp),
                    ) {
                        if (isTestingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("در حال تست...", fontSize = 13.sp)
                        } else {
                            Text("تست همه پینگ‌ها", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            proxies.sortWith(compareBy<TelegramProxyItem> {
                                if (it.pingMs > 0) 0 else 1
                            }.thenBy { if (it.pingMs > 0) it.pingMs else Long.MAX_VALUE })
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                            contentColor = if (isDark) Color.White else Color.Black,
                        ),
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) {
                        Text("مرتب‌سازی", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            val allLinks = proxies.joinToString("\n\n") { "${it.title}: ${it.webUri}" }
                            platform.copyToClipboard(allLinks)
                            platform.toast("همه پروکسی‌ها کپی شدند")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                            contentColor = if (isDark) Color.White else Color.Black,
                        ),
                        modifier = Modifier.weight(0.9f).height(46.dp),
                    ) {
                        Text("کپی همه", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Proxy Cards List
            items(proxies.indices.toList(), key = { proxies[it].id }) { index ->
                val proxy = proxies[index]
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(0.5.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = proxy.country,
                                    fontSize = 18.sp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = proxy.title.ifEmpty { "MTProto Proxy" },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000),
                                    )
                                    Text(
                                        text = "${proxy.server}:${proxy.port}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                                    )
                                }
                            }

                            // Ping badge
                            if (proxy.isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = accentColor,
                                )
                            } else {
                                val (bg, fg, text) = when {
                                    proxy.pingMs in 1..299 -> Triple(Color(0xFF34C759).copy(alpha = 0.15f), Color(0xFF34C759), "${proxy.pingMs} ms")
                                    proxy.pingMs in 300..799 -> Triple(Color(0xFFFF9500).copy(alpha = 0.15f), Color(0xFFFF9500), "${proxy.pingMs} ms")
                                    proxy.pingMs >= 800 -> Triple(Color(0xFFFF3B30).copy(alpha = 0.15f), Color(0xFFFF3B30), "${proxy.pingMs} ms")
                                    else -> Triple(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA), if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73), "تست نشده")
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = bg,
                                    modifier = Modifier.clickable { testSingle(index) },
                                ) {
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fg,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { platform.openUri(proxy.tgUri) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.weight(1.5f).height(38.dp),
                            ) {
                                Text("اتصال در تلگرام", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    platform.copyToClipboard(proxy.webUri)
                                    platform.toast("لینک پروکسی کپی شد")
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                                    contentColor = if (isDark) Color.White else Color.Black,
                                ),
                                modifier = Modifier.weight(1f).height(38.dp),
                            ) {
                                Text("کپی لینک", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
