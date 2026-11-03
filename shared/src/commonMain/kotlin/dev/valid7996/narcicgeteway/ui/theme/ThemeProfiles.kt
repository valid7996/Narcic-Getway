package dev.valid7996.narcicgeteway.ui.theme

import androidx.compose.ui.graphics.Color
import dev.valid7996.narcicgeteway.domain.model.AppSettings

data class ThemeProfile(
    val id: String,

    val name: String,

    val accentIndex: Int,
    val amoled: Boolean = false,
    val download: Long,
    val upload: Long,
    val downloadTile: Long,
    val uploadTile: Long,
    val connect: Long,
    val connected: Long,
    val tapHint: Long,
    val serverCard: Long,
    val serverActive: Long,
    val configCard: Long,
    val configCardText: Long,
) {
    val swatches: List<Color>
        get() = listOf(Color(connect), Color(download), Color(upload))
}

val ThemeProfiles: List<ThemeProfile> = listOf(
    ThemeProfile(
        id = "noir",
        name = "Noir",
        accentIndex = 9,
        amoled = true,
        download = 0xFFFFFFFF,
        upload = 0xFF94A3B8,
        downloadTile = 0xFF121214,
        uploadTile = 0xFF18181B,
        connect = 0xFFE4E4E7,
        connected = 0xFF27272A,
        tapHint = 0xFFE4E4E7,
        serverCard = 0xFF121214,
        serverActive = 0xFF27272A,
        configCard = 0xFF16161A,
        configCardText = 0xFFF4F4F5,
    ),
    ThemeProfile(
        id = "blossom",
        name = "Blossom",
        accentIndex = 5,
        amoled = true,
        download = 0xFFFF85A1,
        upload = 0xFFFFC2D6,
        downloadTile = 0xFF230D18,
        uploadTile = 0xFF2A101D,
        connect = 0xFFF472B6,
        connected = 0xFFDB2777,
        tapHint = 0xFFFFD1DC,
        serverCard = 0xFF180A12,
        serverActive = 0xFFDB2777,
        configCard = 0xFF210E1A,
        configCardText = 0xFFFFF1F5,
    ),
)

private val HiddenThemeProfiles: List<ThemeProfile> = emptyList()

fun themeProfileOf(id: String?): ThemeProfile? =
    (id?.let { wanted -> ThemeProfiles.firstOrNull { it.id == wanted } })
        ?: ThemeProfiles.firstOrNull()

fun AppSettings.applyThemeProfile(profile: ThemeProfile): AppSettings = copy(
    accentColor = profile.accentIndex,
    dynamicColor = false,
    amoledBlack = if (profile.id == "noir") true else amoledBlack,
    customDownloadColor = profile.download,
    customUploadColor = profile.upload,
    customDownloadTileColor = profile.downloadTile,
    customUploadTileColor = profile.uploadTile,
    customConnectColor = profile.connect,
    customConnectedColor = profile.connected,
    customTapHintColor = profile.tapHint,
    customServerCardColor = profile.serverCard,
    customServerActiveColor = profile.serverActive,
    customConfigCardColor = profile.configCard,
    customConfigCardTextColor = profile.configCardText,
    themeProfileId = profile.id,
)


fun AppSettings.applyMasterTheme(spec: MasterThemeSpec): AppSettings = copy(
    customConnectColor = spec.buttonColor.toArgbLong(),
    customServerBorderColor = spec.serverBorderColor.toArgbLong(),
    customCardWaterColor = spec.cardWaterColor.toArgbLong(),
    customCardBgColor = spec.cardBgColor.toArgbLong(),
    customDashboardBorderColor = spec.dashboardBorderColor.toArgbLong(),
    customDashboardBgColor = spec.dashboardBgColor.toArgbLong(),
    customDashboardTextColor = spec.textColor.toArgbLong(),
    customTextColor = spec.textColor.toArgbLong(),
    customNavActiveColor = spec.navActiveColor.toArgbLong(),
    customNavBgColor = spec.navBgColor.toArgbLong(),
    customNavBorderColor = spec.serverBorderColor.toArgbLong(),
    customAmbientGlowColor = spec.ambientGlowColor.toArgbLong(),
    customBgGradientTop = spec.bgGradientTop.toArgbLong(),
    customBgGradientBottom = spec.bgGradientBottom.toArgbLong(),
    themeProfileId = spec.heroCharacter,
)
