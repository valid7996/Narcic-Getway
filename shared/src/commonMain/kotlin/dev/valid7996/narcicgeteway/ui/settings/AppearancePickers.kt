package dev.valid7996.narcicgeteway.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.valid7996.narcicgeteway.domain.model.ConnectButtonStyle
import dev.valid7996.narcicgeteway.domain.model.NavBarStyle
import dev.valid7996.narcicgeteway.domain.model.SettingsTileLayout
import dev.valid7996.narcicgeteway.domain.model.TrafficCardStyle
import dev.valid7996.narcicgeteway.ui.theme.ThemeProfile
import dev.valid7996.narcicgeteway.ui.theme.ThemeProfiles
import dev.valid7996.narcicgeteway.ui.platform.draggableHorizontalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.ic_check
import dev.valid7996.narcicgeteway.ui.theme.ColorPalette32
import dev.valid7996.narcicgeteway.ui.theme.MasterNeonThemes
import dev.valid7996.narcicgeteway.ui.theme.MasterThemeSpec
import dev.valid7996.narcicgeteway.ui.theme.ColorSwatch
import dev.valid7996.narcicgeteway.ui.theme.argbLongToColor
import org.jetbrains.compose.resources.painterResource


@Composable
fun ThemeProfileRow(
    label: String,
    subtitle: String,
    selectedId: String?,
    onSelect: (ThemeProfile) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().draggableHorizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeProfiles.forEach { profile ->
                ProfileCard(
                    name = profile.name,
                    selected = selectedId == profile.id || (selectedId == null && profile.id == "noir"),
                    onClick = { onSelect(profile) },
                ) { ProfilePreview(profile) }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 92.dp, height = 148.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                .border(
                    width = if (selected) 2.dp else 0.5.dp,
                    color = if (selected) Color(0xFF007AFF)
                    else (if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable(onClick = onClick)
                .padding(8.dp),
        ) { preview() }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun MiniHome(
    hero: Color,
    downloadTile: Color,
    uploadTile: Color,
    download: Color,
    upload: Color,
    configCard: Color,
    configText: Color,
    connect: Color,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(hero))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            MiniTile(Modifier.weight(1f), downloadTile, download)
            MiniTile(Modifier.weight(1f), uploadTile, upload)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(configCard),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(start = 6.dp)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(configText),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(connect),
        )
    }
}

@Composable
private fun MiniTile(modifier: Modifier, surface: Color, accent: Color) {
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .size(width = 18.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun ProfilePreview(p: ThemeProfile) = MiniHome(
    hero = Color(p.connect),
    downloadTile = Color(p.downloadTile),
    uploadTile = Color(p.uploadTile),
    download = Color(p.download),
    upload = Color(p.upload),
    configCard = Color(p.configCard),
    configText = Color(p.configCardText),
    connect = Color(p.connect),
)


val AllowedConnectButtonStyles = listOf(
    ConnectButtonStyle.Hero,
    ConnectButtonStyle.Pill,
    ConnectButtonStyle.Slide,
)

@Composable
fun ConnectStyleRow(
    label: String,
    subtitle: String,
    selected: ConnectButtonStyle,
    nameOf: @Composable (ConnectButtonStyle) -> String,
    onSelect: (ConnectButtonStyle) -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().draggableHorizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AllowedConnectButtonStyles.forEach { style ->
                val isSelected = style == selected
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(width = 88.dp, height = 66.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) Color(0xFF007AFF)
                                else (if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelect(style) },
                        contentAlignment = Alignment.Center,
                    ) { StyleGlyph(style) }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        nameOf(style),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleGlyph(style: ConnectButtonStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val on = MaterialTheme.colorScheme.onPrimary
    when (style) {
        ConnectButtonStyle.Pill -> Box(
            Modifier.size(width = 58.dp, height = 22.dp)
                .clip(RoundedCornerShape(11.dp)).background(accent),
        )
        ConnectButtonStyle.Hero -> Box(
            Modifier.size(38.dp).clip(CircleShape).background(accent),
            contentAlignment = Alignment.Center,
        ) { Text("N", color = on, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        ConnectButtonStyle.Ring -> Box(
            Modifier.size(38.dp).clip(CircleShape).background(accent)
                .border(3.dp, on.copy(alpha = 0.5f), CircleShape),
        )
        ConnectButtonStyle.Bar -> Box(
            Modifier.size(width = 62.dp, height = 14.dp)
                .clip(RoundedCornerShape(4.dp)).background(accent),
        )
        ConnectButtonStyle.Icon -> Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent),
        )
        ConnectButtonStyle.Switch -> Box(
            Modifier.size(width = 58.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp)).background(accent),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier.padding(start = 4.dp).size(18.dp).clip(CircleShape)
                    .background(on.copy(alpha = 0.5f)),
            )
        }
        ConnectButtonStyle.Slide -> Box(
            Modifier.size(width = 58.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp)).background(accent),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier.padding(end = 4.dp).size(18.dp).clip(CircleShape)
                    .background(on),
            )
        }
    }
}


/**
 * 6 Coordinated Master Themes Row (قالب‌های آماده هماهنگ).
 */
@Composable
fun MasterThemesRow(
    selectedId: String?,
    onSelect: (MasterThemeSpec) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "قالب‌های آماده هماهنگ",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "ترکیب ۶ طرح کامل و هماهنگ رنگ کلید، داشبورد، سرورها و متن",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MasterNeonThemes.forEach { spec ->
                val isSelected = selectedId == spec.id
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    spec.cardBgColor,
                                    spec.cardBgColor.copy(alpha = 0.85f),
                                )
                            )
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) spec.primaryColor else spec.primaryColor.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelect(spec) }
                        .padding(12.dp),
                ) {
                    Column {
                        // Swatch circle preview
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(spec.primaryColor),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_check),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(spec.cardWaterColor),
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(spec.buttonColor),
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = spec.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = spec.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF9CA3AF),
                            maxLines = 2,
                            lineHeight = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 8 Distinct Traffic Card Styles Selector (انتخاب سبک کارت‌های ترافیک ۸ سبک مختلف).
 */
@Composable
fun TrafficStyleRow(
    selected: TrafficCardStyle,
    onSelect: (TrafficCardStyle) -> Unit,
) {
    val styles = listOf(
        TrafficCardStyle.Cards to ("کارت‌های دوگانه" to "دو کارت مجزا برای بارگذاری و دانلود با اکولایزر"),
        TrafficCardStyle.Duo to ("کپسول فشرده" to "کپسول دوگانه شیشه‌ای با فلش‌های جهت‌دار"),
        TrafficCardStyle.Rings to ("حلقه‌های نئونی" to "دو حلقه پالس‌دار برای پایش آپلود و دانلود"),
        TrafficCardStyle.Minimal to ("مینیمال باریک" to "نوار استاتوس فشرده تک‌خطی بدون اشغال فضا"),
        TrafficCardStyle.Graph to ("نمودار زنده" to "نمودار موجی پویای ترافیک شبکه"),
        TrafficCardStyle.NeonPills to ("کپسول‌های نئونی" to "کپسول‌های درخشان با نشانگر شناور وضعیت"),
        TrafficCardStyle.WaveEqualizer to ("اکولایزر آب روان" to "ستون‌های فرکانسی متحرک با افکت سیال"),
        TrafficCardStyle.CyberGauges to ("گیج‌های سایبری" to "دو نمایشگر تاکومتر دورانی سایبرپانک"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "سبک کارت‌های ترافیک (۸ سبک)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Text(
            text = "سبک نمایش سرعت و حجم مصرفی را شخصی‌سازی کنید",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            styles.forEach { (style, meta) ->
                val (title, sub) = meta
                val isSelected = selected == style
                Box(
                    modifier = Modifier
                        .size(width = 148.dp, height = 94.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0x35F72585) else Color(0x18FFFFFF))
                        .border(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) Color(0xFFF72585) else Color(0x25FFFFFF),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelect(style) }
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFF72585) else Color(0x40FFFFFF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_check),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF9CA3AF),
                            maxLines = 2,
                            lineHeight = 13.sp,
                        )
                    }
                }
            }
        }
    }
}


/**
 * 32 Color Picker with Category Filter Chips (۳۲ قالب رنگ با فیلتر رنگ اختیاری).
 */
@Composable
fun Color32PickerSection(
    title: String,
    subtitle: String,
    currentColorArgb: Long?,
    defaultColor: Color,
    onColorSelected: (Long?) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf("all") }

    val categories = listOf(
        "all" to "همه",
        "pink" to "صورتی/سرخابی",
        "purple" to "بنفش/نیلی",
        "cyan" to "فیروزه‌ای/آبی",
        "green" to "سبز/فسفری",
        "warm" to "طلایی/گرم",
        "mono" to "مونوکروم",
    )

    val filteredSwatches = if (selectedCategory == "all") {
        ColorPalette32
    } else {
        ColorPalette32.filter { it.category == selectedCategory }
    }

    val activeColor = currentColorArgb?.argbLongToColor() ?: defaultColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                )
            }

            // Current color preview pill + Reset
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (currentColorArgb != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable { onColorSelected(null) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("پیش‌فرض", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            categories.forEach { (catId, catLabel) ->
                val isSelected = selectedCategory == catId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0x40F72585) else Color(0x18FFFFFF))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFF72585) else Color(0x25FFFFFF),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { selectedCategory = catId }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = catLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                        ),
                        color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Color swatches row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            filteredSwatches.forEach { swatch ->
                val isCurrent = (currentColorArgb == swatch.argb) || (currentColorArgb == null && swatch.color == defaultColor)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onColorSelected(swatch.argb) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(swatch.color)
                            .border(
                                width = if (isCurrent) 2.5.dp else 1.dp,
                                color = if (isCurrent) Color.White else Color.Transparent,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCurrent) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = swatch.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (isCurrent) Color.White else Color(0xFF9CA3AF),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Compact, De-cluttered 32-Color Picker Row (کادر فشرده انتخاب رنگ با دایره پیش‌نمایش و بازنشانی).
 * Tapping the circle expands/collapses the color palette so the screen doesn't get cluttered.
 */
@Composable
fun CompactColorPickerRow(
    title: String,
    subtitle: String,
    currentColorArgb: Long?,
    defaultColor: Color,
    onColorSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("all") }

    val categories = listOf(
        "all" to "همه",
        "pink" to "صورتی/سرخابی",
        "purple" to "بنفش/نیلی",
        "cyan" to "فیروزه‌ای/آبی",
        "green" to "سبز/فسفری",
        "warm" to "طلایی/گرم",
        "mono" to "مونوکروم",
    )

    val filteredSwatches = if (selectedCategory == "all") {
        ColorPalette32
    } else {
        ColorPalette32.filter { it.category == selectedCategory }
    }

    val activeColor = currentColorArgb?.argbLongToColor() ?: defaultColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x14FFFFFF))
            .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = Color.White,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF9CA3AF),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (currentColorArgb != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x20EF4444))
                            .clickable { onColorSelected(null) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "بازنشانی",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFFFCA5A5),
                        )
                    }
                }

                // Circular Swatch with glowing border indicating current color
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(
                            width = 2.dp,
                            color = if (expanded) Color.White else Color.White.copy(alpha = 0.65f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (expanded) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { (catId, catLabel) ->
                        val isSelected = selectedCategory == catId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x40F72585) else Color(0x18FFFFFF))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFF72585) else Color(0x20FFFFFF),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { selectedCategory = catId }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                ),
                                color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Swatches row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    filteredSwatches.forEach { swatch ->
                        val isCurrent = (currentColorArgb == swatch.argb) || (currentColorArgb == null && swatch.color == defaultColor)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onColorSelected(swatch.argb) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(swatch.color)
                                    .border(
                                        width = if (isCurrent) 2.5.dp else 1.dp,
                                        color = if (isCurrent) Color.White else Color.Transparent,
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isCurrent) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_check),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = swatch.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isCurrent) Color.White else Color(0xFF9CA3AF),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Navigation Bar Style Selector Row (تغییر شکل‌های مختلف نوار ناوبری).
 */
@Composable
fun NavBarStyleRow(
    selected: NavBarStyle,
    onSelect: (NavBarStyle) -> Unit,
) {
    val styles = listOf(
        NavBarStyle.IosInteractive to ("شناور تعاملی (iOS)" to "داک شیشه‌ای شناور با کپسول لغزان فنری"),
        NavBarStyle.FloatingPill to ("کپسول جزیره‌ای" to "نوار گوشه‌گرد شناور با پدینگ متوازن"),
        NavBarStyle.FullBar to ("داک سرتاسری" to "نوار چسبیده به پایین با عرض کامل"),
        NavBarStyle.CompactDock to ("داک مینی" to "کپسول جمع‌وجور با ابعاد فشرده"),
        NavBarStyle.Underline to ("خط نئون زیرین" to "نوار مدرن با خط نئونی متحرک"),
        NavBarStyle.Minimal to ("مینیمال" to "باریک‌ترین طرح نوار ناوبری"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "شکل و سبک نوار ناوبری پایین",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Text(
            text = "تغییر شکل، ابعاد و ظاهر نوار پایین صفحه",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            styles.forEach { (style, meta) ->
                val (title, sub) = meta
                val isSelected = selected == style
                Box(
                    modifier = Modifier
                        .size(width = 150.dp, height = 90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0x35F72585) else Color(0x18FFFFFF))
                        .border(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) Color(0xFFF72585) else Color(0x25FFFFFF),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelect(style) }
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFF72585) else Color(0x40FFFFFF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_check),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF9CA3AF),
                            maxLines = 2,
                            lineHeight = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Settings Tiles Layout Selector Row (چیدمان کاشی‌های تنظیمات: عمودی، افقی یا شبکه ارگانیک).
 */
@Composable
fun SettingsTileLayoutRow(
    selected: SettingsTileLayout,
    onSelect: (SettingsTileLayout) -> Unit,
) {
    val layouts = listOf(
        SettingsTileLayout.MasonryGrid to ("شبکه ارگانیک (۲ ستونه)" to "طرح کاشی‌های مدرن مایع با فرم نامتقارن"),
        SettingsTileLayout.HorizontalCards to ("کارت‌های افقی" to "ردیف‌های اسکرول‌شونده برای دسترسی سریع"),
        SettingsTileLayout.SingleColumn to ("لیست عمودی یکپارچه" to "چیدمان تک‌ستونه کلاسیک و خوانا"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "چیدمان کاشی‌های تنظیمات",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Text(
            text = "تغییر نحوه چینش و فرم کاشی‌های صفحه تنظیمات (عمودی، افقی یا شبکه ارگانیک)",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF),
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            layouts.forEach { (layout, meta) ->
                val (title, sub) = meta
                val isSelected = selected == layout
                Box(
                    modifier = Modifier
                        .size(width = 150.dp, height = 90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0x35F72585) else Color(0x18FFFFFF))
                        .border(
                            width = if (isSelected) 1.5.dp else 0.8.dp,
                            color = if (isSelected) Color(0xFFF72585) else Color(0x25FFFFFF),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { onSelect(layout) }
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFF72585) else Color(0x40FFFFFF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_check),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF9CA3AF),
                            maxLines = 2,
                            lineHeight = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
