package dev.cluvex.zedsecure.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.auto_title_for
import dev.cluvex.zedsecure.domain.model.AppLanguage
import dev.cluvex.zedsecure.domain.model.AppSettings
import dev.cluvex.zedsecure.ui.servers.ServersScreen
import dev.cluvex.zedsecure.ui.theme.toPersonalization
import dev.cluvex.zedsecure.ui.connection.ConnectionViewModel
import dev.cluvex.zedsecure.ui.easteregg.SpaceScreen
import dev.cluvex.zedsecure.ui.home.HomeScreen
import dev.cluvex.zedsecure.ui.motion.transitionDecoration
import dev.cluvex.zedsecure.ui.navigation.TopDestination
import dev.cluvex.zedsecure.ui.platform.BackHandler
import dev.cluvex.zedsecure.ui.settings.SettingsScreen
import dev.cluvex.zedsecure.ui.update.NudgeHost
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainScaffold(
    settings: AppSettings,
    configRepository: ConfigRepository,
    onToggleConnection: () -> Unit,

    onActiveServerChanged: () -> Unit = {},
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onLanguage: (AppLanguage) -> Unit,

    aiBridge: dev.cluvex.zedsecure.domain.ai.AiAppBridge? = null,
    appVersion: String = "",
) {
    val platform = dev.cluvex.zedsecure.ui.platform.LocalPlatform.current
    var current by rememberSaveable { mutableStateOf(TopDestination.Home) }

    var spaceUnlocked by rememberSaveable { mutableStateOf(false) }

    val connectionVm: ConnectionViewModel = viewModel { ConnectionViewModel() }

    val profiles by configRepository.profiles.collectAsStateWithLifecycle()
    val subscriptions by configRepository.subscriptions.collectAsStateWithLifecycle()
    val activeId by configRepository.activeId.collectAsStateWithLifecycle()

    val activeProfile = remember(profiles, subscriptions, activeId) {
        activeId?.let { configRepository.profile(it) }
    }
    val autoSession by dev.cluvex.zedsecure.core.AutoSelect.session.collectAsStateWithLifecycle()

    val lockedNote = activeProfile?.takeIf { it.isLocked }?.note

    var activeCountryCode by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(activeProfile) {
        activeCountryCode = null
        val profile = activeProfile?.takeIf { !it.isLocked && !it.isAutoSelect } ?: return@LaunchedEffect
        val link = (profile.source as? dev.cluvex.zedsecure.domain.config.ProfileSource.Link)?.link
        val parsed = link?.let {
            runCatching { dev.cluvex.zedsecure.domain.config.ConfigParser.parse(it) }.getOrNull()
        }
        val host = parsed?.address?.takeIf { it.isNotBlank() }
            ?: profile.address.takeIf { it.isNotBlank() }
        if (host != null) {
            activeCountryCode = dev.cluvex.zedsecure.data.net.GeoLookup.countryOf(host)
        }
    }

    val mtuHint = remember(activeProfile) {
        val link = (activeProfile?.source as? dev.cluvex.zedsecure.domain.config.ProfileSource.Link)?.link
        val parsed = link?.let {
            runCatching { dev.cluvex.zedsecure.domain.config.ConfigParser.parse(it) }.getOrNull()
        }
        val host = parsed?.address?.takeIf { it.isNotBlank() } ?: activeProfile?.address.orEmpty()
        dev.cluvex.zedsecure.data.net.MtuServerHint(
            host = host,
            overhead = parsed
                ?.let { dev.cluvex.zedsecure.data.net.MtuOverheads.ofServer(it) }
                ?: dev.cluvex.zedsecure.data.net.MtuOverheads.worstCase(),
        )
    }

    dev.cluvex.zedsecure.ui.servers.DeepLinkImportHost(
        repository = configRepository,
        onImported = { current = TopDestination.Servers },
    )

    NudgeHost(settings = settings, onUpdateSettings = onUpdateSettings)

    BackHandler(enabled = current != TopDestination.Home) { current = TopDestination.Home }

    if (spaceUnlocked) {
        SpaceScreen(onExit = { spaceUnlocked = false })
        return
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            dev.cluvex.zedsecure.ui.navigation.ZedNavBar(
                style = settings.navBarStyle,
                current = current,
                onSelect = { current = it },
            )
        },
    ) { innerPadding ->
      Box(Modifier.fillMaxSize()) {
        val sign = dev.cluvex.zedsecure.ui.motion.layoutSign()
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                dev.cluvex.zedsecure.ui.motion.screenTransition(
                    style = settings.screenTransition,
                    forward = targetState.ordinal >= initialState.ordinal,
                    direction = sign,
                    reduceMotion = settings.reduceMotion,
                ).using(SizeTransform(clip = false))
            },
            label = "top-destination",
            modifier = Modifier.fillMaxSize(),
        ) { dest ->

            val decoration = transitionDecoration(
                style = settings.screenTransition,
                forward = dest.ordinal >= current.ordinal,
                direction = sign,
                reduceMotion = settings.reduceMotion,
            )
            Box(decoration) {
            when (dest) {
                TopDestination.Home -> HomeScreen(
                    connectionVm = connectionVm,
                    activeConfigName = activeProfile?.let { p ->
                        if (p.isAutoSelect) {
                            stringResource(
                                Res.string.auto_title_for,
                                dev.cluvex.zedsecure.ui.servers.autoSelectGroupLabel(p),
                            )
                        } else p.name
                    },
                    exitGeneration = autoSession?.takeIf { it.profileId == activeProfile?.id }?.exitGeneration ?: 0,
                    activeConfigDetail = activeProfile?.takeIf { it.isAutoSelect }?.let { p ->
                        autoSession?.takeIf { it.profileId == p.id }?.let { session ->
                            dev.cluvex.zedsecure.ui.servers.autoSelectLiveLine(session) { id ->
                                profiles.firstOrNull { it.id == id }?.name
                            }
                        }
                    },
                    lockedNote = lockedNote,
                    activeLocked = activeProfile?.isLocked == true,
                    activeCountryCode = activeCountryCode,
                    reduceMotion = settings.reduceMotion,

                    showConnectionInfo = activeProfile?.isDnsTunnel != true,
                    ipApiUrl = settings.ipApiUrl,
                    delayTestUrl = settings.delayTestUrl,

                    personalization = settings.toPersonalization(),
                    connectStyle = dev.cluvex.zedsecure.domain.model.ConnectButtonStyle.Slide,
                    repository = configRepository,
                    realPingConcurrency = settings.realPingConcurrency,
                    autoSortAfterTest = settings.autoSortAfterTest,
                    autoTestAfterUpdate = settings.autoTestAfterUpdate,
                    autoRemoveInvalidAfterTest = settings.autoRemoveInvalidAfterTest,
                    contentPadding = innerPadding,
                    onToggleConnection = onToggleConnection,

                    onBrowseConfigs = { current = TopDestination.Servers },
                    onSecretUnlocked = { spaceUnlocked = true },
                )
                TopDestination.Servers -> ServersScreen(
                    repository = configRepository,
                    contentPadding = innerPadding,
                    twoColumns = settings.doubleColumnDisplay,
                    showAllGroup = settings.groupAllDisplay,
                    confirmRemove = settings.confirmRemove,
                    realPingConcurrency = settings.realPingConcurrency,
                    delayTestUrl = settings.delayTestUrl,
                    autoTestAfterUpdate = settings.autoTestAfterUpdate,
                    autoRemoveInvalidAfterTest = settings.autoRemoveInvalidAfterTest,
                    autoSortAfterTest = settings.autoSortAfterTest,
                    personalization = settings.toPersonalization(),
                    onServerActivated = {
                        if (settings.homeAfterSelect) current = TopDestination.Home

                        onActiveServerChanged()
                    },
                )
                TopDestination.Settings -> SettingsScreen(
                    settings = settings,
                    contentPadding = innerPadding,
                    onUpdate = onUpdateSettings,
                    onLanguage = onLanguage,

                    serverTargets = profiles
                        .filter { it.source is dev.cluvex.zedsecure.domain.config.ProfileSource.Link }
                        .map { it.id to it.name },

                    mtuHint = mtuHint,
                    ai = aiBridge?.let { bridge ->
                        dev.cluvex.zedsecure.ui.settings.AiSection(
                            settings = settings.ai,
                            onUpdate = { next -> onUpdateSettings { it.copy(ai = next) } },
                            bridge = bridge,
                            onOpenUrl = { url -> platform.openUri(url) },

                            languageName = languageEnglishName(settings.language),
                            languageNative = languageNativeName(settings.language),
                            platform = "Android",
                            appVersion = appVersion,
                        )
                    },
                )
            }
            }
        }
      }
    }
    }
}

private fun languageEnglishName(language: AppLanguage): String = when (language) {
    AppLanguage.Persian -> "Persian"
    AppLanguage.Chinese -> "Chinese"
    AppLanguage.Russian -> "Russian"
    AppLanguage.English -> "English"

    AppLanguage.System -> "the language the user writes in"
}

private fun languageNativeName(language: AppLanguage): String = when (language) {
    AppLanguage.Persian -> "فارسی"
    AppLanguage.Chinese -> "中文"
    AppLanguage.Russian -> "Русский"
    AppLanguage.English -> "English"
    AppLanguage.System -> ""
}
