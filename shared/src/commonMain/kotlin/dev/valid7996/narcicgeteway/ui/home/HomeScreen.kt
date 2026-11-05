@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package dev.valid7996.narcicgeteway.ui.home
import dev.valid7996.narcicgeteway.ui.theme.NeonTheme
import dev.valid7996.narcicgeteway.ui.theme.NeonWaveAccent
import dev.valid7996.narcicgeteway.ui.theme.NeonAmbientBackground
import androidx.compose.ui.graphics.StrokeCap

import org.jetbrains.compose.resources.DrawableResource

import org.jetbrains.compose.resources.StringResource

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.valid7996.narcicgeteway.ui.components.CyberRadarIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.mutableFloatStateOf
import dev.valid7996.narcicgeteway.ui.theme.MotionBudget
import dev.valid7996.narcicgeteway.ui.theme.LocalMotionBudget
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.valid7996.narcicgeteway.ui.platform.LocalPlatform
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.*
import dev.valid7996.narcicgeteway.domain.model.ConnectButtonStyle
import dev.valid7996.narcicgeteway.domain.model.ConnectionState
import dev.valid7996.narcicgeteway.ui.components.MorphingBlob
import dev.valid7996.narcicgeteway.ui.components.NoteText
import dev.valid7996.narcicgeteway.data.net.NetworkInfo
import dev.valid7996.narcicgeteway.ui.components.FlagBadge
import dev.valid7996.narcicgeteway.ui.connection.ConnectionViewModel
import dev.valid7996.narcicgeteway.ui.format.formatBytes
import dev.valid7996.narcicgeteway.ui.format.formatElapsed
import dev.valid7996.narcicgeteway.ui.format.formatRate
import dev.valid7996.narcicgeteway.ui.theme.readableOn
import dev.valid7996.narcicgeteway.domain.model.TrafficCardStyle
import dev.valid7996.narcicgeteway.domain.model.TrafficTileSize
import dev.valid7996.narcicgeteway.ui.theme.Personalization
import dev.valid7996.narcicgeteway.ui.theme.NrgGradients
import dev.valid7996.narcicgeteway.ui.theme.NrgCyan
import dev.valid7996.narcicgeteway.ui.theme.NrgHotPink
import dev.valid7996.narcicgeteway.ui.theme.NrgLime
import dev.valid7996.narcicgeteway.ui.theme.NrgDeepViolet
import dev.valid7996.narcicgeteway.ui.theme.NrgViolet
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun HomeScreen(
    connectionVm: ConnectionViewModel,
    activeConfigName: String?,
    contentPadding: PaddingValues,

    activeConfigDetail: String? = null,

    exitGeneration: Int = 0,
    onToggleConnection: () -> Unit,
    onBrowseConfigs: () -> Unit,
    onSecretUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    lockedNote: String? = null,
    activeLocked: Boolean = false,
    activeExpiresAt: Long? = null,
    activeCountryCode: String? = null,
    reduceMotion: Boolean = false,

    repository: dev.valid7996.narcicgeteway.data.config.ConfigRepository? = null,
    realPingConcurrency: Int = 16,
    autoSortAfterTest: Boolean = false,
    autoTestAfterUpdate: Boolean = false,
    autoRemoveInvalidAfterTest: Boolean = false,

    showConnectionInfo: Boolean = true,

    ipApiUrl: String = "",

    delayTestUrl: String = "",

    personalization: Personalization = Personalization.Default,

    connectStyle: ConnectButtonStyle = ConnectButtonStyle.Pill,
    settings: dev.valid7996.narcicgeteway.domain.model.AppSettings? = null,
    onUpdateSettings: (((dev.valid7996.narcicgeteway.domain.model.AppSettings) -> dev.valid7996.narcicgeteway.domain.model.AppSettings) -> Unit)? = null,
    homeVm: HomeViewModel = viewModel { HomeViewModel() },
) {
    val ui by connectionVm.ui.collectAsStateWithLifecycle()
    val live = ui.state == ConnectionState.Connected
    val info by homeVm.info.collectAsStateWithLifecycle()
    val infoExpanded by homeVm.expanded.collectAsStateWithLifecycle()

    LaunchedEffect(live, ui.sessionId, exitGeneration, showConnectionInfo, ipApiUrl, delayTestUrl) {
        homeVm.preferredIpApiUrl = ipApiUrl.ifBlank { null }
        homeVm.preferredDelayUrl = delayTestUrl.ifBlank { null }
        if (live && showConnectionInfo) homeVm.refresh(settleDelayMs = 300) else homeVm.clear()
    }

    var burst by remember { mutableIntStateOf(0) }

    var laser by remember { mutableIntStateOf(0) }

    var statusTaps by remember { mutableIntStateOf(0) }
    var statusLastTap by remember { mutableLongStateOf(0L) }
    val haptics = LocalHapticFeedback.current
    val platform = LocalPlatform.current
    val scope = rememberCoroutineScope()

    val isNoir = personalization.themeProfileId == "noir"
    val isBlossom = personalization.themeProfileId == "blossom"
    val isModernTheme = true
    val showDashboard = (live || personalization.alwaysShowDashboard) && showConnectionInfo

    val baseBg = personalization.bgGradientBottom ?: MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg),
    ) {
        NeonAmbientBackground(personalization = personalization)
        DecorativeBackdrop(
            reduceMotion = reduceMotion,
            isNoir = isNoir,
            isBlossom = isBlossom,
            amoledBlack = personalization.amoledBlack,
            dynamicColor = personalization.dynamicColor,
        )

        ConnectingBackdrop(
            state = ui.state,
            sessionId = ui.sessionId,
            reduceMotion = reduceMotion,
            isNoir = isNoir,
            isBlossom = isBlossom,
            amoledBlack = personalization.amoledBlack,
            dynamicColor = personalization.dynamicColor,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 22.dp),
        ) {
            val heroSize = minOf(302.dp, maxHeight * 0.41f)

            val compact = maxHeight < 620.dp

            val upperScroll = rememberScrollState()
            // One scrollable page: header, tiles, the blob, the controls and the server list all
            // travel together, so nothing is pinned mid-screen or clipped by the status bar.
            StageColumn(
                viewport = if (isModernTheme) maxHeight * 0.65f else maxHeight * 0.58f,
                stageMin = if (isModernTheme) 280.dp else if (compact) 110.dp else STAGE_MIN,
                modifier = Modifier.fillMaxWidth().scrollFade(upperScroll).verticalScroll(upperScroll),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandHeader(
                    state = ui.state,
                    onSecretUnlocked = onSecretUnlocked,
                    personalization = personalization,
                )

                val fault = when {
                    ui.state == ConnectionState.Error ->
                        ui.error?.takeIf { it.isNotBlank() }
                            ?.let { dev.valid7996.narcicgeteway.core.ConnectionFault.classify(it) }
                    ui.state == ConnectionState.Connected -> info.fault
                    else -> null
                }
                AnimatedVisibility(visible = fault != null) {
                    fault?.let {
                        FaultBanner(
                            fault = it,
                            connected = ui.state == ConnectionState.Connected,
                        )
                    }
                }

                }

                // Optional: Traffic tiles above hero
                AnimatedVisibility(visible = showDashboard && personalization.trafficTilesAboveHero) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
                        TrafficStylesDisplay(
                            downloadBps = ui.downloadBps,
                            uploadBps = ui.uploadBps,
                            totalDownload = ui.totalDownload,
                            totalUpload = ui.totalUpload,
                            style = personalization.trafficCardStyle,
                            personalization = personalization,
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth().layoutId(STAGE_ID)) {
                    val stage = maxHeight
                    val tight = stage < 260.dp
                    val stageHero = minOf(heroSize, stage * 0.58f)
                    val modernHeroSize = minOf(260.dp, maxOf(220.dp, stage * 0.70f))
                    Column(
                      modifier = Modifier.fillMaxSize(),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center,
                    ) {
                      if (isBlossom) {
                        GirlHero(
                            state = ui.state,
                            size = modernHeroSize,
                            onTapHero = if (connectStyle == ConnectButtonStyle.Hero) {
                                {
                                    if (ui.state != ConnectionState.Disconnecting) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleConnection()
                                    }
                                }
                            } else null,
                        )
                      } else if (isNoir) {
                        MontaaHero(
                            state = ui.state,
                            size = modernHeroSize,
                            onTapHero = if (connectStyle == ConnectButtonStyle.Hero) {
                                {
                                    if (ui.state != ConnectionState.Disconnecting) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleConnection()
                                    }
                                }
                            } else null,
                        )

                      } else {
                        Hero(
                            state = ui.state,
                            sessionId = ui.sessionId,
                            size = stageHero,
                            onLongPress = {
                                burst++
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },

                            onTap = if (connectStyle == ConnectButtonStyle.Hero) {
                                {
                                    if (ui.state != ConnectionState.Disconnecting) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleConnection()
                                    }
                                }
                            } else null,
                        )
                      }

                      Spacer(Modifier.height(if (tight) 8.dp else 20.dp))

                      AnimatedContent(
                          targetState = ui.state,
                          transitionSpec = {
                              (fadeIn(animationSpec = tween(220)) +
                                  scaleIn(initialScale = 0.92f, animationSpec = tween(220))) togetherWith
                                  fadeOut(animationSpec = tween(140))
                          },
                          label = "status-text",
                          modifier = Modifier.clickable(
                              interactionSource = remember { MutableInteractionSource() },
                              indication = null,
                          ) {
                              val now = System.currentTimeMillis()
                              statusTaps = if (now - statusLastTap < TAP_WINDOW_MS) statusTaps + 1 else 1
                              statusLastTap = now
                              if (statusTaps >= 3) {
                                  statusTaps = 0
                                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                  laser++
                              }
                          },
                      ) { s ->
                          Column(
                              horizontalAlignment = Alignment.CenterHorizontally,
                              verticalArrangement = Arrangement.spacedBy(8.dp),
                          ) {
                              if (s == ConnectionState.Connected) {
                                  Box(
                                      modifier = Modifier
                                          .size(32.dp)
                                          .clip(RoundedCornerShape(9.dp))
                                          .background(Color(0x3022C55E))
                                          .border(1.5.dp, Color(0xFF22C55E), RoundedCornerShape(9.dp)),
                                      contentAlignment = Alignment.Center,
                                  ) {
                                      Icon(
                                          painter = painterResource(Res.drawable.ic_check),
                                          contentDescription = null,
                                          tint = Color.White,
                                          modifier = Modifier.size(18.dp),
                                      )
                                  }
                              }
                              Text(
                                  text = if (s == ConnectionState.Connected) stringResource(Res.string.status_connected_toast) else stringResource(s.labelRes()),
                                  style = MaterialTheme.typography.headlineMedium.copy(
                                      fontWeight = FontWeight.Bold,
                                      fontSize = if (tight) 24.sp else 30.sp,
                                  ),
                                  maxLines = 1,
                                  textAlign = TextAlign.Center,
                                  color = when {
                                      s == ConnectionState.Error -> MaterialTheme.colorScheme.error
                                      s.isActive || s == ConnectionState.Disconnecting -> Color.White
                                      else -> MaterialTheme.colorScheme.onSurface
                                  },
                              )
                          }
                      }

                      if (lockedNote != null) {
                          val noteScroll = rememberScrollState()
                          Spacer(Modifier.height(16.dp))
                          Surface(
                              shape = MaterialTheme.shapes.large,
                              color = MaterialTheme.colorScheme.surfaceContainerHigh,
                              modifier = Modifier.fillMaxWidth(),
                          ) {
                              NoteText(
                                  text = lockedNote,
                                  style = MaterialTheme.typography.bodyMedium,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  modifier = Modifier
                                      .fillMaxWidth()
                                      .heightIn(max = 140.dp)
                                      .verticalScroll(noteScroll)
                                      .padding(horizontal = 16.dp, vertical = 12.dp),
                              )
                          }
                      }
                    }
                }

                if (dev.valid7996.narcicgeteway.ui.navigation.NavConfig.SHOW_ACTIVE_CONFIG) {
                    ActiveConfigCard(
                        name = activeConfigName,
                        detail = activeConfigDetail,
                        locked = activeLocked,
                        expiresAt = activeExpiresAt,
                        countryCode = activeCountryCode,
                        onClick = onBrowseConfigs,
                        personalization = personalization,
                    )
                }

                Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                when (connectStyle) {
                    ConnectButtonStyle.Hero -> Unit
                    ConnectButtonStyle.Pill, ConnectButtonStyle.Switch -> {
                        if (isModernTheme) {
                            ModernConnectButton(
                                state = ui.state,
                                onClick = onToggleConnection,
                                isNoir = isNoir,
                                isBlossom = isBlossom,
                                amoledBlack = personalization.amoledBlack,
                                dynamicColor = personalization.dynamicColor,
                                customColor = personalization.connectColor,
                                customConnectedColor = personalization.connectedColor,
                            )
                        } else {
                            ConnectButton(
                                state = ui.state,
                                onToggle = onToggleConnection,
                                customColor = if (personalization.dynamicColor) MaterialTheme.colorScheme.primary else personalization.connectColor,
                                customActiveColor = if (personalization.dynamicColor) MaterialTheme.colorScheme.primary else personalization.connectedColor,
                                compact = compact,
                            )
                        }
                    }
                    ConnectButtonStyle.Slide -> SlideToConnect(
                        state = ui.state,
                        onToggle = onToggleConnection,
                        customColor = if (personalization.dynamicColor) MaterialTheme.colorScheme.primary else personalization.connectColor,
                        customActiveColor = if (personalization.dynamicColor) MaterialTheme.colorScheme.primary else personalization.connectedColor,
                        compact = compact,
                    )
                    else -> AltConnectControl(
                        style = connectStyle,
                        state = ui.state,
                        onToggle = onToggleConnection,
                        customColor = if (personalization.dynamicColor) MaterialTheme.colorScheme.primary else personalization.connectColor,
                        customActiveColor = if (personalization.dynamicColor) MaterialTheme.colorScheme.primary else personalization.connectedColor,
                        compact = compact,
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Modular Tiled Dashboard: Separate Live Route, Server Tile, Session Duration/Volume Tiles, and Traffic Styles
                AnimatedVisibility(visible = showDashboard) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // 0. Active Server Name & Protocol Tile
                        ActiveConfigDetailTile(
                            activeConfigName = activeConfigName,
                            activeConfigDetail = activeConfigDetail,
                            activeLocked = activeLocked,
                            activeExpiresAt = activeExpiresAt,
                            personalization = personalization,
                            onClick = onBrowseConfigs,
                        )

                        // 1. Separate Live Route Card
                        ConnectionMapTriggerButton(
                            expanded = infoExpanded,
                            destCountryCode = info.countryCode?.takeIf { it.isNotBlank() } ?: activeCountryCode,
                            onClick = homeVm::toggleExpanded,
                            personalization = personalization,
                            isNoir = isNoir,
                            isBlossom = isBlossom,
                            language = settings?.language,
                        )
                        AnimatedVisibility(visible = infoExpanded) {
                            ConnectionRouteMapCard(
                                info = info,
                                activeConfigName = activeConfigName,
                                fallbackCountryCode = activeCountryCode,
                                onClose = homeVm::toggleExpanded,
                                personalization = personalization,
                                isNoir = isNoir,
                                isBlossom = isBlossom,
                                language = settings?.language,
                            )
                        }

                        // 2. Active Server Connection Tile
                        ServerConnectionTile(
                            info = info,
                            onRefresh = homeVm::refresh,
                            onToggleMap = homeVm::toggleExpanded,
                            personalization = personalization,
                            activeLocked = activeLocked,
                        )

                        // 3. Modular Session Duration & Volume Tiles
                        SessionStatsTilesRow(
                            elapsedSeconds = ui.elapsedSeconds,
                            totalBytes = ui.totalDownload + ui.totalUpload,
                            personalization = personalization,
                        )

                        // 4. 8-Style Traffic Card (if not placed above hero)
                        if (!personalization.trafficTilesAboveHero) {
                            TrafficStylesDisplay(
                                downloadBps = ui.downloadBps,
                                uploadBps = ui.uploadBps,
                                totalDownload = ui.totalDownload,
                                totalUpload = ui.totalUpload,
                                style = personalization.trafficCardStyle,
                                personalization = personalization,
                            )
                        }
                    }
                }


                val deviceId = remember(repository) { repository?.nrgDeviceId().orEmpty() }
                if (deviceId.isNotBlank()) {
                    var isRevealed by remember { mutableStateOf(false) }
                    val copiedMsg = stringResource(Res.string.device_code_copied)
                    val keyBorder = (personalization.serverBorderColor ?: personalization.connectColor ?: (if (isNoir) Color(0xFF94A3B8) else Color(0xFFF72585))).copy(alpha = 0.5f)
                    val keyBg = personalization.cardBgColor ?: Color(0xE6140A1C)
                    val keyTint = personalization.connectColor ?: personalization.serverBorderColor ?: (if (isNoir) Color(0xFFE2E8F0) else Color(0xFFF72585))

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = {
                            platform.copyToClipboard(deviceId)
                            scope.launch { platform.toast(copiedMsg) }
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = keyBg,
                        border = BorderStroke(1.dp, keyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Left in RTL (Eye icon)
                            IconButton(
                                onClick = { isRevealed = !isRevealed },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    painter = painterResource(if (isRevealed) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off),
                                    contentDescription = null,
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            // Center: Masked or unmasked text
                            Text(
                                text = if (isRevealed) deviceId else "•••••••••••••",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                ),
                                color = Color(0xFF9CA3AF),
                            )

                            // Right in RTL: Key icon + "کلید دستگاه من"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.device_code_label),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                    ),
                                    color = Color.White,
                                )
                                Icon(
                                    painter = painterResource(Res.drawable.ic_key),
                                    contentDescription = null,
                                    tint = keyTint,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(32.dp))
                Spacer(Modifier.height(96.dp))
            }
        }
        ParticleBurst(trigger = if (reduceMotion) 0 else burst)

        dev.valid7996.narcicgeteway.ui.easteregg.LaserBounce(
            trigger = laser,
            color = personalization.connectColor ?: MaterialTheme.colorScheme.primary,
            reduceMotion = reduceMotion,
        )
    }
}

private const val STAGE_ID = "home-stage"

private val STAGE_MIN = 148.dp

private fun Modifier.scrollFade(state: ScrollState, length: Dp = 28.dp): Modifier = this
    .graphicsLayer {
        compositingStrategy = if (state.canScrollForward || state.canScrollBackward) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithContent {
        drawContent()
        val fade = length.toPx()
        if (state.canScrollForward) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black, Color.Transparent),
                    startY = size.height - fade,
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = fade),
                blendMode = BlendMode.DstIn,
            )
        }
    }

private const val MAX_STAGE_PX = 16_384

@Composable
private fun StageColumn(
    viewport: Dp,
    stageMin: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->

        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth

        val natural = Constraints(maxWidth = width)
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        var stageIndex = -1
        var fixed = 0
        measurables.forEachIndexed { i, child ->
            if (child.layoutId == STAGE_ID) {
                stageIndex = i
            } else {
                placeables[i] = child.measure(natural).also { fixed += it.height }
            }
        }

        val viewportPx = if (viewport.isFinite) viewport.roundToPx().coerceIn(0, MAX_STAGE_PX) else 0
        val stagePx = if (stageIndex < 0) 0 else maxOf(stageMin.roundToPx(), viewportPx - fixed).coerceIn(0, MAX_STAGE_PX)
        if (stageIndex >= 0) {
            placeables[stageIndex] = measurables[stageIndex].measure(Constraints.fixed(width, stagePx))
        }
        val total = placeables.fold(0) { sum, placeable -> sum + (placeable?.height ?: 0) }
        layout(width, maxOf(viewportPx, fixed + stagePx, total)) {
            var y = 0
            placeables.forEach { placeable ->
                if (placeable == null) return@forEach
                placeable.placeRelative((width - placeable.width) / 2, y)
                y += placeable.height
            }
        }
    }
}

@Composable
private fun DecorativeBackdrop(
    reduceMotion: Boolean,
    isNoir: Boolean = false,
    isBlossom: Boolean = false,
    amoledBlack: Boolean = false,
    dynamicColor: Boolean = false,
) {
    val budget = LocalMotionBudget.current
    val rotation = 0f
    val splash = if (amoledBlack) {
        val accent = when {
            dynamicColor -> MaterialTheme.colorScheme.primary
            isBlossom -> Color(0xFFF472B6)
            isNoir -> Color(0xFFFFFFFF)
            
            else -> NrgCyan
        }
        listOf(Color(0xFF000000), Color(0xFF000000), accent.copy(alpha = 0.35f))
    } else if (dynamicColor) {
        listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
    } else if (isBlossom) {
        listOf(Color(0xFF150510), Color(0xFF3B1229), Color(0xFFF472B6))
    } else if (isNoir) {
        listOf(Color(0xFF000000), Color(0xFF18181B), Color(0xFF3F3F46))
    } else {
        listOf(NrgDeepViolet, NrgViolet, NrgCyan)
    }
    // A quiet splash: one large disc with two droplets thrown off it.
    Box(
        Modifier
            .size(320.dp)
            .offset(x = 190.dp, y = (-140).dp)
            .graphicsLayer { rotationZ = rotation },
    ) {
        Box(
            Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Brush.linearGradient(splash)),
        )
        Box(
            Modifier
                .size(46.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .background(Brush.linearGradient(splash.map { it.copy(alpha = 0.75f) })),
        )
        Box(
            Modifier
                .size(22.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Brush.linearGradient(splash.map { it.copy(alpha = 0.5f) })),
        )
    }
}

/**
 * The home hero: a ringed planet — one large glossy sphere with a moonlet hugging it, a thin
 * orbit ring and a few droplets, all tinted by the connection state.
 */
@Composable
private fun PlanetBody(colors: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.47f
        val r = size.minDimension * 0.335f

        val ringW = r * 3.0f
        val ringH = r * 1.12f
        val ringTop = Offset(cx - ringW / 2f, cy - ringH / 2f)
        val ringSize = Size(ringW, ringH)

        // the orbit ring behind the sphere
        rotate(-16f, pivot = Offset(cx, cy)) {
            drawOval(
                color = colors[1].copy(alpha = 0.5f),
                topLeft = ringTop,
                size = ringSize,
                style = Stroke(width = r * 0.045f),
            )
        }

        // the planet
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors[0], colors[1], colors[2].copy(alpha = 0.85f)),
                center = Offset(cx - r * 0.35f, cy - r * 0.45f),
                radius = r * 1.9f,
            ),
            radius = r,
            center = Offset(cx, cy),
        )
        // shading toward the far side
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent),
                center = Offset(cx + r * 0.55f, cy + r * 0.6f),
                radius = r * 1.25f,
            ),
            radius = r,
            center = Offset(cx, cy),
        )

        // the moonlet hugging the lower-left
        val mr = r * 0.40f
        val mc = Offset(cx - r * 0.80f, cy + r * 0.60f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(colors[2], colors[0]),
                center = Offset(mc.x - mr * 0.3f, mc.y - mr * 0.35f),
                radius = mr * 1.6f,
            ),
            radius = mr,
            center = mc,
        )

        // droplets thrown off the planet
        drawCircle(colors[2].copy(alpha = 0.9f), radius = r * 0.055f, center = Offset(cx - r * 1.28f, cy - r * 0.92f))
        drawCircle(colors[1].copy(alpha = 0.8f), radius = r * 0.04f, center = Offset(cx + r * 1.32f, cy + r * 0.38f))
        drawCircle(Color.White.copy(alpha = 0.55f), radius = r * 0.03f, center = Offset(cx + r * 0.92f, cy - r * 1.06f))

        // specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                center = Offset(cx - r * 0.38f, cy - r * 0.5f),
                radius = r * 0.7f,
            ),
            radius = r,
            center = Offset(cx, cy),
        )

        // the orbit ring's front half, passing over the planet
        rotate(-16f, pivot = Offset(cx, cy)) {
            clipRect(left = 0f, top = cy, right = size.width, bottom = size.height) {
                drawOval(
                    color = colors[1].copy(alpha = 0.85f),
                    topLeft = ringTop,
                    size = ringSize,
                    style = Stroke(width = r * 0.05f),
                )
            }
        }
    }
}

@Composable
private fun Hero(
    state: ConnectionState,
    sessionId: Int,
    size: Dp,
    onLongPress: () -> Unit,

    onTap: (() -> Unit)? = null,
) {
    val budget = LocalMotionBudget.current
    val transitional = state == ConnectionState.Connecting || state == ConnectionState.Reconnecting
    val breatheState = if (budget == MotionBudget.Full || budget == MotionBudget.Throttled && transitional) {
        rememberInfiniteTransition(label = "breathe").animateFloat(
            initialValue = 0.97f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(2800), RepeatMode.Reverse),
            label = "breathe",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    val targetColors = when (state) {
        ConnectionState.Connected -> listOf(NrgViolet, NrgCyan, NrgLime)
        ConnectionState.Connecting, ConnectionState.Reconnecting -> listOf(Color(0xFF1E4A9E), NrgCyan, NrgLime)
        else -> listOf(NrgDeepViolet, Color(0xFF173B7C), NrgViolet)
    }
    val liquidColors = targetColors.mapIndexed { index, color ->
        animateColorAsState(color, tween(900), label = "liquid$index").value
    }

    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                val s = if (state.isActive) breatheState.value else 1f
                scaleX = s
                scaleY = s
            }

            .pointerInput(onTap) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = onTap?.let { handler -> { _ -> handler() } },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        PlanetBody(colors = liquidColors, modifier = Modifier.fillMaxSize())
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "hero-center",
        ) { s ->
            if (s == ConnectionState.Error) {
                Icon(
                    painter = painterResource(Res.drawable.ic_lock_open),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(size * 0.32f),
                )
            }
        }
    }
}

@Composable
private fun BrandHeader(
    state: ConnectionState,
    onSecretUnlocked: () -> Unit,
    personalization: Personalization = Personalization.Default,
) {
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    val haptics = LocalHapticFeedback.current
    val isNoir = personalization.themeProfileId == "noir"
    val brandAccent = personalization.serverBorderColor ?: personalization.connectColor ?: (if (isNoir) Color(0xFFE2E8F0) else Color(0xFFF72585))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // StatusChip on the right in RTL (Child 1 in RTL Row)
        StatusChip(state = state, personalization = personalization)

        // Branding on the left in RTL (Child 2 in RTL Row)
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    val now = System.currentTimeMillis()
                    taps = if (now - lastTap < TAP_WINDOW_MS) taps + 1 else 1
                    lastTap = now
                    if (taps >= TAPS_TO_UNLOCK) {
                        taps = 0
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSecretUnlocked()
                    } else if (taps > 1) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
            ) {
                // Styled Hexagon Emblem with "N" matching reference UI
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.5f, 1f)
                            lineTo(w - 1f, h * 0.25f)
                            lineTo(w - 1f, h * 0.75f)
                            lineTo(w * 0.5f, h - 1f)
                            lineTo(1f, h * 0.75f)
                            lineTo(1f, h * 0.25f)
                            close()
                        }
                        drawPath(path, color = brandAccent.copy(alpha = 0.18f), style = androidx.compose.ui.graphics.drawscope.Fill)
                        drawPath(
                            path,
                            color = brandAccent,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.8.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                            ),
                        )
                    }
                    Text(
                        text = "N",
                        color = brandAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 15.sp,
                    )
                }
                Spacer(Modifier.width(9.dp))
                Row {
                    Text(
                        text = "Narcic ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = "Gateway",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        ),
                        color = brandAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun FaultBanner(
    fault: dev.valid7996.narcicgeteway.core.ConnectionFault,
    connected: Boolean,
) {
    var expanded by remember(fault.raw) { mutableStateOf(false) }
    val known = fault.cause != dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Unknown
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clickable { expanded = !expanded },
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(
                    if (connected) Res.string.fault_heading_health else Res.string.fault_heading_connect,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )
            if (known) {
                Text(
                    stringResource(faultTitle(fault.cause)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    stringResource(faultHint(fault.cause)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (fault.raw.isNotBlank()) {
                Text(
                    fault.raw,
                    style = if (known) MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ) else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = if (known) 0.75f else 1f),

                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun faultTitle(c: dev.valid7996.narcicgeteway.core.ConnectionFault.Cause) = when (c) {
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Timeout -> Res.string.fault_timeout
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.ServerClosed -> Res.string.fault_server_closed
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Refused -> Res.string.fault_refused
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Reset -> Res.string.fault_reset
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Unreachable -> Res.string.fault_unreachable
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.DnsFailed -> Res.string.fault_dns
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.TlsHandshake -> Res.string.fault_tls
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Certificate -> Res.string.fault_certificate
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Reality -> Res.string.fault_reality
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Auth -> Res.string.fault_auth
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Transport -> Res.string.fault_transport
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Unknown -> Res.string.fault_unknown
}

private fun faultHint(c: dev.valid7996.narcicgeteway.core.ConnectionFault.Cause) = when (c) {
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Timeout -> Res.string.fault_timeout_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.ServerClosed -> Res.string.fault_server_closed_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Refused -> Res.string.fault_refused_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Reset -> Res.string.fault_reset_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Unreachable -> Res.string.fault_unreachable_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.DnsFailed -> Res.string.fault_dns_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.TlsHandshake -> Res.string.fault_tls_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Certificate -> Res.string.fault_certificate_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Reality -> Res.string.fault_reality_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Auth -> Res.string.fault_auth_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Transport -> Res.string.fault_transport_hint
    dev.valid7996.narcicgeteway.core.ConnectionFault.Cause.Unknown -> Res.string.fault_unknown
}

private const val TAPS_TO_UNLOCK = 5

private const val TAP_WINDOW_MS = 1_200L

@Composable
private fun StatusChip(
    state: ConnectionState,
    personalization: Personalization = Personalization.Default,
) {
    val isConnected = state == ConnectionState.Connected
    val isConnecting = state == ConnectionState.Connecting || state == ConnectionState.Reconnecting
    val isError = state == ConnectionState.Error

    val dotColor = when {
        isConnected -> Color(0xFF22C55E)
        isConnecting -> Color(0xFFF59E0B)
        isError -> Color(0xFFEF4444)
        else -> Color(0xFFEF4444)
    }

    val label = when (state) {
        ConnectionState.Connected -> stringResource(Res.string.state_connected)
        ConnectionState.Connecting, ConnectionState.Reconnecting ->
            stringResource(Res.string.state_connecting)
        ConnectionState.Error -> stringResource(Res.string.state_error)
        else -> stringResource(Res.string.status_disconnected_label)
    }

    val chipBg = personalization.cardBgColor ?: Color(0xE6121118)
    val chipBorder = (personalization.serverBorderColor ?: Color(0x35FFFFFF)).copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                ),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ActiveConfigCard(
    name: String?,
    detail: String? = null,
    locked: Boolean,
    expiresAt: Long? = null,
    countryCode: String? = null,
    onClick: () -> Unit,
    personalization: Personalization = Personalization.Default,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "config-scale",
    )
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isAmoled = personalization.amoledBlack
    val isNoir = personalization.themeProfileId == "noir"
    val isBlossom = personalization.themeProfileId == "blossom"

    val shape = RoundedCornerShape(16.dp)
    val cardBg = when {
        isAmoled -> Color(0xFF000000)
        personalization.configCardColor != null -> personalization.configCardColor
        isBlossom -> Color(0xFF1F0B16)
        isNoir -> Color(0xFF121214)
        
        isDark -> Color(0xFF1C1C1E)
        else -> Color(0xFFFFFFFF)
    }
    val cardBorder = when {
        personalization.dynamicColor -> MaterialTheme.colorScheme.primary.copy(alpha = if (isAmoled) 0.50f else 0.35f)
        isBlossom -> Color(0xFFF472B6).copy(alpha = if (isAmoled) 0.50f else 0.35f)
        isNoir -> Color(0xFFFFFFFF).copy(alpha = if (isAmoled) 0.35f else 0.20f)
        
        isAmoled -> Color(0xFF10B981).copy(alpha = 0.45f)
        isDark -> Color(0xFF2C2C2E)
        else -> Color(0xFFE5E5EA)
    }
    val titleColor = when {
        personalization.configCardTextColor != null -> personalization.configCardTextColor
        isBlossom -> Color(0xFFFFF1F5)
        isNoir -> Color(0xFFFFFFFF)
        
        isDark -> Color(0xFFF2F2F7)
        else -> Color(0xFF000000)
    }
    val subtitleColor = when {
        personalization.tapHintColor != null -> personalization.tapHintColor
        isBlossom -> Color(0xFFFFD1DC)
        isNoir -> Color(0xFF8E8E93)
        
        isDark -> Color(0xFF8E8E93)
        else -> Color(0xFF6E6E73)
    }
    val tileBg = when {
        personalization.dynamicColor -> MaterialTheme.colorScheme.primary
        isBlossom -> Color(0xFFFF85A1)
        isNoir -> Color(0xFF2C2C2E)
        
        personalization.configCardTextColor != null -> personalization.configCardTextColor
        else -> Color(0xFF007AFF)
    }

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        shape = shape,
        color = cardBg,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .border(0.5.dp, cardBorder, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (locked) Res.drawable.ic_encrypted else Res.drawable.ic_public),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (countryCode != null && !locked) {
                        dev.valid7996.narcicgeteway.ui.components.FlagBadge(
                            countryCode = countryCode,
                            size = 18.dp,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                    Text(
                        text = name ?: stringResource(Res.string.home_select_config),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (locked && expiresAt != null) {
                        Spacer(Modifier.width(8.dp))
                        dev.valid7996.narcicgeteway.ui.servers.ExpiryCountdownBadge(
                            expiresAt = expiresAt,
                            isDark = isDark,
                            fontSize = 11.sp,
                        )
                    }
                }
                Text(
                    text = detail ?: stringResource(
                        when {
                            name == null -> Res.string.home_no_config_body
                            locked -> Res.string.home_tap_to_change_vault
                            else -> Res.string.home_tap_to_change
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val direction = androidx.compose.ui.platform.LocalLayoutDirection.current
            Icon(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = if (isDark) Color(0xFF545458) else Color(0xFFC7C7CC),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        if (direction == LayoutDirection.Rtl) {
                            rotationY = 180f
                        }
                    },
            )
        }
    }
}

@Composable
private fun ConnectButton(
    state: ConnectionState,
    onToggle: () -> Unit,
    customColor: Color? = null,
    customActiveColor: Color? = null,

    compact: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "connect-scale",
    )

    val glowTarget = when {
        state == ConnectionState.Error -> 0f
        state.isActive -> 1f
        else -> 0.45f
    }
    val glow by animateFloatAsState(
        targetValue = if (LocalMotionBudget.current == MotionBudget.Paused) 0f else glowTarget,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "connect-glow",
    )

    val idleContent = if (customColor == null) MaterialTheme.colorScheme.onPrimary
    else if (customColor.luminance() > 0.5f) Color(0xFF10131A) else Color.White
    val activeContent = if (customActiveColor == null) MaterialTheme.colorScheme.onPrimaryContainer
    else if (customActiveColor.luminance() > 0.5f) Color(0xFF10131A) else Color.White
    val content = when {
        state == ConnectionState.Error -> MaterialTheme.colorScheme.onErrorContainer
        state.isActive -> activeContent
        else -> idleContent
    }

    Box(Modifier.fillMaxWidth()) {
        // Aurora glow behind the button — the signature of the new design.
        if (glow > 0.01f) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(if (compact) 56.dp else 72.dp)
                    .graphicsLayer {
                        alpha = glow * 0.5f
                        val spread = 1f + 16.dp.toPx() * glow / size.height.coerceAtLeast(1f)
                        scaleX = spread
                        scaleY = spread
                    }
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(NrgViolet.copy(alpha = 0.55f), NrgCyan.copy(alpha = 0.55f)),
                        ),
                    ),
            )
        }

        Surface(
            onClick = onToggle,

            enabled = state != ConnectionState.Disconnecting,
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            contentColor = content,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 56.dp else 72.dp)
                .scale(scale)
                .clip(RoundedCornerShape(50))
                .background(
                    when {
                        state == ConnectionState.Error -> Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.errorContainer,
                            ),
                        )
                        state.isActive && customActiveColor != null -> Brush.horizontalGradient(
                            listOf(customActiveColor, customActiveColor.copy(alpha = 0.82f)),
                        )
                        state.isActive -> Brush.horizontalGradient(
                            listOf(NrgViolet, NrgCyan),
                        )
                        customColor != null -> Brush.horizontalGradient(
                            listOf(customColor, customColor.copy(alpha = 0.82f)),
                        )
                        else -> Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                            ),
                        )
                    },
                ),
        ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = state.isTransitioning,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) +
                        scaleIn(initialScale = 0.9f, animationSpec = tween(200))) togetherWith
                        fadeOut(animationSpec = tween(120))
                },
                label = "connect-label",
            ) { busy ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        CyberRadarIndicator(
                            modifier = Modifier.size(26.dp),
                        )
                    } else {
                        Icon(
                            painterResource(
                                if (state.isActive) Res.drawable.ic_lock_open else Res.drawable.ic_bolt
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(
                            when {
                                state == ConnectionState.Disconnecting -> Res.string.state_disconnecting

                                busy -> Res.string.action_cancel
                                state.isActive -> Res.string.action_disconnect
                                else -> Res.string.action_connect
                            }
                        ),
                        style = if (compact) {
                            MaterialTheme.typography.titleMediumEmphasized
                        } else {
                            MaterialTheme.typography.titleLargeEmphasized
                        },
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ConnectingBackdrop(
    state: ConnectionState,
    sessionId: Int,
    reduceMotion: Boolean,
    isNoir: Boolean = false,
    isBlossom: Boolean = false,
    amoledBlack: Boolean = false,
    dynamicColor: Boolean = false,
) {
    val intensity by animateFloatAsState(
        targetValue = when (state) {
            ConnectionState.Connecting, ConnectionState.Reconnecting -> 0.72f
            ConnectionState.Connected -> 1f
            ConnectionState.Disconnecting -> 0.35f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "backdrop-intensity",
    )

    val budget = LocalMotionBudget.current
    val pulseRuns = !reduceMotion && when (budget) {
        MotionBudget.Full -> true
        MotionBudget.Throttled -> state == ConnectionState.Connecting || state == ConnectionState.Reconnecting
        MotionBudget.Paused -> false
    }
    val pulse by if (!pulseRuns) {
        remember { mutableStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "backdrop-life").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(8500), RepeatMode.Restart),
            label = "pulse",
        )
    }

    val stops = if (amoledBlack) {
        if (state == ConnectionState.Connected) {
            val primary = when {
                dynamicColor -> MaterialTheme.colorScheme.primary
                isBlossom -> Color(0xFFDB2777)
                isNoir -> Color(0xFF3F3F46)
                
                else -> dev.valid7996.narcicgeteway.ui.theme.NrgGreen
            }
            val accent = when {
                dynamicColor -> MaterialTheme.colorScheme.tertiary
                isBlossom -> Color(0xFFF472B6)
                isNoir -> Color(0xFFFFFFFF)
                
                else -> dev.valid7996.narcicgeteway.ui.theme.NrgLime
            }
            listOf(Color(0xFF000000), Color(0xFF000000), primary.copy(alpha = 0.30f), accent.copy(alpha = 0.18f))
        } else {
            listOf(Color(0xFF000000), Color(0xFF000000), Color(0xFF000000))
        }
    } else if (dynamicColor) {
        if (state == ConnectionState.Connected) {
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    } else if (isBlossom) {
        if (state == ConnectionState.Connected) {
            listOf(Color(0xFF150510), Color(0xFF260D1A), Color(0xFFDB2777).copy(alpha = 0.35f), Color(0xFFF472B6).copy(alpha = 0.2f))
        } else {
            listOf(Color(0xFF0D0209), Color(0xFF1A0812), Color(0xFF2A0E1E))
        }
    } else if (isNoir) {
        if (state == ConnectionState.Connected) {
            listOf(Color(0xFF000000), Color(0xFF0C0C0E), Color(0xFF18181B).copy(alpha = 0.5f), Color(0xFF27272A).copy(alpha = 0.25f))
        } else {
            listOf(Color(0xFF000000), Color(0xFF09090B), Color(0xFF121214))
        }
    } else if (state == ConnectionState.Connected) {
        NrgGradients.connectedStops(sessionId)
    } else {
        NrgGradients.connectingStops
    }

    // Circle.json floating particles in background for Default, Azkaban, Noir, and Blossom
    val particleColors = when {
        dynamicColor -> listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primaryContainer,
        )
        isBlossom -> listOf(
            Color(0xFFFF85A1), // Candy Rose
            Color(0xFFF472B6), // Sakura Pink
            Color(0xFFFFD1DC), // Soft Petal
            Color(0xFFC084FC), // Lavender Orchid
        )
        isNoir -> listOf(
            Color(0xFFFFFFFF), // Crisp White
            Color(0xFFE4E4E7), // Silver
            Color(0xFFA1A1AA), // Slate Gray
            Color(0xFF71717A), // Charcoal
        )
        else -> listOf(
            Color(0xFF10B981), // Emerald
            Color(0xFF38BDF8), // Cyan
            Color(0xFFA78BFA), // Soft Violet
            Color(0xFF34D399), // Mint Green
        )
    }

    Canvas(Modifier.fillMaxSize()) {
        if (intensity <= 0.002f) return@Canvas
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(colors = stops, startY = 0f, endY = h),
            alpha = 0.9f * intensity,
        )

        // Particle definitions from Circle.json: (relativeX, radiusDp, isStroke)
        val particles = listOf(
            Triple(0.825f, 9f, false),
            Triple(0.704f, 6.5f, false),
            Triple(0.185f, 10f, false),
            Triple(0.492f, 10f, false),
            Triple(0.496f, 15f, true),
            Triple(0.235f, 8f, true),
            Triple(0.308f, 8f, true),
            Triple(0.937f, 14f, true),
            Triple(0.114f, 14f, true),
        )

        val strokeStyle = Stroke(width = 1.8f)

        // Draw Wave 1 and Wave 2 (offset by 0.5f)
        for (wave in 0..1) {
            val waveOffset = wave * 0.5f
            particles.forEachIndexed { index, (relX, baseRadius, isStroke) ->
                val p = (pulse + waveOffset + (index * 0.04f)) % 1f
                val y = h * (1.08f - p * 1.18f)
                val drift = sin((p * PI * 2).toFloat()) * (12f * (index % 3 + 1))
                val x = (w * relX + drift).coerceIn(0f, w)

                // Smooth sinusoidal alpha: gentle fade-in at bottom, gentle fade-out at top
                val alphaCurve = sin((p * PI).toFloat()).coerceIn(0f, 1f)
                val particleAlpha = (0.22f * alphaCurve * intensity).coerceIn(0f, 1f)
                val color = particleColors[index % particleColors.size].copy(alpha = particleAlpha)

                if (isStroke) {
                    drawCircle(
                        color = color,
                        radius = baseRadius * density,
                        center = Offset(x, y),
                        style = strokeStyle,
                    )
                } else {
                    drawCircle(
                        color = color,
                        radius = baseRadius * density,
                        center = Offset(x, y),
                    )
                }
            }
        }
    }
}

private class BurstParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val radius: Float,
    val square: Boolean,
    var life: Float = 1f,
)

@Composable
private fun ParticleBurst(trigger: Int) {
    val colors = listOf(NrgViolet, NrgHotPink, NrgLime, NrgCyan)
    val particles = remember { mutableStateListOf<BurstParticle>() }
    var tick by remember { mutableLongStateOf(0L) }
    var canvas by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect

        var guard = 0
        while (canvas == Size.Zero && guard < 5) {
            withInfiniteAnimationFrameNanos { }
            guard++
        }
        if (canvas == Size.Zero) return@LaunchedEffect
        val cx = canvas.width / 2f
        val cy = canvas.height * 0.42f
        repeat(56) {
            val angle = Random.nextFloat() * (2f * Math.PI.toFloat())
            val speed = 8f + Random.nextFloat() * 20f
            particles += BurstParticle(
                x = cx,
                y = cy,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 8f,
                color = colors[Random.nextInt(colors.size)],
                radius = 7f + Random.nextFloat() * 11f,
                square = Random.nextBoolean(),
            )
        }
        var last = 0L
        while (particles.isNotEmpty()) {
            withInfiniteAnimationFrameNanos { now ->
                val step = if (last == 0L) 1f else ((now - last) / 16_666_666f).coerceIn(0f, 3f)
                last = now
                particles.forEach { p ->
                    p.vy += 0.9f * step
                    p.vx *= 1f - 0.02f * step
                    p.x += p.vx * step
                    p.y += p.vy * step
                    p.life -= 0.014f * step
                }
                particles.removeAll { it.life <= 0f || it.y > canvas.height + 60f }
                tick = now
            }
        }
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvas = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        tick
        particles.forEach { p ->
            val color = p.color.copy(alpha = p.life.coerceIn(0f, 1f))
            if (p.square) {
                drawRect(
                    color = color,
                    topLeft = Offset(p.x - p.radius / 2f, p.y - p.radius / 2f),
                    size = Size(p.radius, p.radius),
                )
            } else {
                drawCircle(color = color, radius = p.radius / 2f, center = Offset(p.x, p.y))
            }
        }
    }
}

private fun ConnectionState.labelRes(): StringResource = when (this) {
    ConnectionState.Idle -> Res.string.state_idle
    ConnectionState.Connecting -> Res.string.state_connecting
    ConnectionState.Connected -> Res.string.state_connected
    ConnectionState.Reconnecting -> Res.string.state_reconnecting
    ConnectionState.Disconnecting -> Res.string.state_disconnecting
    ConnectionState.Error -> Res.string.state_error
}

private val ConnectedGreen = dev.valid7996.narcicgeteway.ui.theme.NrgGreen

/**
 * The one merged dashboard: location, address and live traffic in a single always-open panel.
 * The green wash is the "connected" colour of the app at its 60% focus strength.
 */
@Composable

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}

/**
 * 2-Column Modular Session Stats Tiles: Duration and Session Volume (کادر مدت اتصال و کادر حجم اتصال).
 */
@Composable
private fun SessionStatsTilesRow(
    elapsedSeconds: Int,
    totalBytes: Long,
    personalization: Personalization,
    modifier: Modifier = Modifier,
) {
    val isNoir = personalization.themeProfileId == "noir"
    val cardBg = personalization.dashboardBgColor ?: personalization.cardBgColor ?: (if (isNoir) Color(0xE616161A) else Color(0xE6140A1C))
    val cardBorder = personalization.dashboardBorderColor ?: personalization.serverBorderColor ?: (if (isNoir) Color(0xFFCBD5E1) else Color(0x60F72585))
    val waterColor = personalization.cardWaterColor ?: personalization.serverBorderColor ?: (if (isNoir) Color(0xFF64748B) else NeonTheme.PrimaryPink)
    val textColor = personalization.dashboardTextColor ?: Color.White

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Tile 1: کادر مدت اتصال
        Box(
            modifier = Modifier
                .weight(1f)
                .height(86.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
        ) {
            NeonWaveAccent(
                color = waterColor,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.12f,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_schedule),
                        contentDescription = null,
                        tint = NeonTheme.SecondaryPink,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.connection_duration_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFF9CA3AF),
                    )
                }

                Text(
                    text = formatDuration(elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    ),
                    color = textColor,
                )
            }
        }

        // Tile 2: کادر حجم اتصال
        Box(
            modifier = Modifier
                .weight(1f)
                .height(86.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, cardBorder.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
        ) {
            NeonWaveAccent(
                color = Color(0xFF06B6D4),
                modifier = Modifier.fillMaxSize(),
                alpha = 0.12f,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_sync),
                        contentDescription = null,
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.traffic_consumed_label),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFF9CA3AF),
                    )
                }

                Text(
                    text = formatBytes(totalBytes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    ),
                    color = textColor,
                )
            }
        }
    }
}

/**
 * Active Server Connection Card Tile (کادر اطلاعات سرور فعال مجزا از نقشه).
 */
@Composable
private fun ServerConnectionTile(
    info: NetworkInfo,
    onRefresh: () -> Unit,
    onToggleMap: (() -> Unit)? = null,
    personalization: Personalization = Personalization.Default,
    activeLocked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isNoir = personalization.themeProfileId == "noir"
    val cardBg = personalization.dashboardBgColor ?: personalization.cardBgColor ?: (if (isNoir) Color(0xE616161A) else Color(0xE6140A1C))
    val cardBorder = personalization.dashboardBorderColor ?: personalization.serverBorderColor ?: (if (isNoir) Color(0xFFCBD5E1) else Color(0x90F72585))
    val waterColor = personalization.cardWaterColor ?: personalization.serverBorderColor ?: (if (isNoir) Color(0xFF64748B) else NeonTheme.PrimaryPink)
    val textColor = personalization.dashboardTextColor ?: Color.White
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(1.2.dp, cardBorder, shape),
    ) {
        NeonWaveAccent(
            color = waterColor,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.14f,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Right in RTL: Flag + City, Country + IP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlagBadge(countryCode = info.countryCode, size = 32.dp)
                Column {
                    Text(
                        text = listOfNotNull(info.city, info.country).joinToString(", ")
                            .ifBlank { null }
                            ?: "Atlanta, United States",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (activeLocked) stringResource(Res.string.server_locked_live) else (info.ipv4 ?: "198.44.138.67"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                        ),
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                    )
                }
            }

            // Left in RTL: Ping pill + Refresh
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val ping = info.pingMs ?: 1245
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBorder.copy(alpha = 0.22f))
                        .border(0.8.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "ms $ping",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = if (ping < 120) Color(0xFF10B981) else if (ping < 300) Color(0xFFF59E0B) else Color(0xFFFF80BF),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x20FFFFFF))
                        .clickable(enabled = !info.loading) { onRefresh() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (info.loading) {
                        CyberRadarIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sync),
                            contentDescription = "Refresh",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 8 Traffic Card Styles Display (نمایش سبک‌های هشت‌گانه کارت ترافیک).
 */
@Composable
private fun TrafficStylesDisplay(
    downloadBps: Long,
    uploadBps: Long,
    totalDownload: Long,
    totalUpload: Long,
    style: TrafficCardStyle,
    personalization: Personalization,
    modifier: Modifier = Modifier,
) {
    val (downVal, downUnit) = formatRate(downloadBps)
    val (upVal, upUnit) = formatRate(uploadBps)
    val cardBg = personalization.dashboardBgColor ?: Color(0xE612081C)
    val cardBorder = personalization.dashboardBorderColor ?: Color(0x60F72585)
    val textColor = personalization.dashboardTextColor ?: Color.White

    when (style) {
        TrafficCardStyle.Graph -> {
            // Live Real-Time Moving Bezier Wave Graph
            LiveTrafficWaveGraph(
                downloadBps = downloadBps,
                uploadBps = uploadBps,
                downVal = downVal,
                downUnit = downUnit,
                upVal = upVal,
                upUnit = upUnit,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textColor = textColor,
                personalization = personalization,
                modifier = modifier,
            )
        }

        TrafficCardStyle.CyberGauges -> {
            // Live Dual Tachometer Cyber Gauges with sweeping arcs
            CyberDualGauges(
                upVal = upVal,
                upUnit = upUnit,
                downVal = downVal,
                downUnit = downUnit,
                downloadBps = downloadBps,
                uploadBps = uploadBps,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textColor = textColor,
                personalization = personalization,
                modifier = modifier,
            )
        }

        TrafficCardStyle.WaveEqualizer -> {
            // Live Wave Equalizer with responsive bouncing frequency bars
            WaveEqualizerCard(
                upVal = upVal,
                upUnit = upUnit,
                downVal = downVal,
                downUnit = downUnit,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textColor = textColor,
                personalization = personalization,
                modifier = modifier,
            )
        }

        TrafficCardStyle.Rings -> {
            // Concentric Neon Pulsing Rings
            ConcentricRingsCard(
                upVal = upVal,
                upUnit = upUnit,
                downVal = downVal,
                downUnit = downUnit,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textColor = textColor,
                personalization = personalization,
                modifier = modifier,
            )
        }

        TrafficCardStyle.Cards -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Upload Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(82.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(Res.string.traffic_upload_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            MiniSoundBars(color = personalization.connectColor ?: Color(0xFFF72585))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↑ ", fontWeight = FontWeight.Bold, color = personalization.connectColor ?: Color(0xFFF72585), fontSize = 15.sp)
                            Text("$upVal $upUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                        }
                    }
                }

                // Download Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(82.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(Res.string.traffic_download_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            MiniSoundBars(color = Color(0xFF06B6D4))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("↓ ", fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4), fontSize = 15.sp)
                            Text("$downVal $downUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        TrafficCardStyle.Duo -> {
            // Duo Split Pill
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(29.dp))
                    .background(cardBg)
                    .border(1.2.dp, cardBorder, RoundedCornerShape(29.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("↑", fontWeight = FontWeight.Bold, color = personalization.connectColor ?: Color(0xFFF72585), fontSize = 16.sp)
                        Text("$upVal $upUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                    }

                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0x30FFFFFF)))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$downVal $downUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                        Text("↓", fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4), fontSize = 16.sp)
                    }
                }
            }
        }

        TrafficCardStyle.NeonPills -> {
            // Neon Pills
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x30F72585))
                        .border(1.5.dp, personalization.connectColor ?: Color(0xFFF72585), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("↑", fontWeight = FontWeight.Bold, color = personalization.connectColor ?: Color(0xFFF72585), fontSize = 18.sp)
                        Column {
                            Text(stringResource(Res.string.traffic_upload_label), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFD1D5DB))
                            Text("$upVal $upUnit", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x3006B6D4))
                        .border(1.5.dp, Color(0xFF06B6D4), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("↓", fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4), fontSize = 18.sp)
                        Column {
                            Text(stringResource(Res.string.traffic_download_label), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFD1D5DB))
                            Text("$downVal $downUnit", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }

        TrafficCardStyle.Minimal -> {
            // Minimal single row
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg.copy(alpha = 0.7f))
                    .border(0.8.dp, cardBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("↑ $upVal $upUnit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = personalization.connectColor ?: Color(0xFFF72585))
                    Text(stringResource(Res.string.traffic_total_rate_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text("↓ $downVal $downUnit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF06B6D4))
                }
            }
        }
    }
}

/**
 * Real animated Canvas spline wave chart for TrafficCardStyle.Graph
 */
@Composable
private fun LiveTrafficWaveGraph(
    downloadBps: Long,
    uploadBps: Long,
    downVal: String,
    downUnit: String,
    upVal: String,
    upUnit: String,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    personalization: Personalization,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "traffic-graph-phase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * kotlin.math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "graph-phase",
    )

    val downColor = Color(0xFF06B6D4)
    val upColor = personalization.connectColor ?: Color(0xFFF72585)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(106.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(1.2.dp, cardBorder, RoundedCornerShape(22.dp)),
    ) {
        // Live animated spline curves on Canvas (high efficiency smooth Bezier)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // Download Wave Area (Cyan / Blue)
            val baseDown = h * 0.55f
            val ampDown = h * 0.20f
            val dY0 = baseDown + kotlin.math.sin(phase) * ampDown
            val dY1 = baseDown + kotlin.math.sin(phase + 1.8f) * (ampDown * 1.1f)
            val dY2 = baseDown + kotlin.math.sin(phase + 3.6f) * (ampDown * 0.9f)
            val dY3 = baseDown + kotlin.math.sin(phase + 5.2f) * ampDown

            val downPath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, dY0)
                cubicTo(w * 0.25f, dY0, w * 0.25f, dY1, w * 0.5f, dY1)
                cubicTo(w * 0.75f, dY1, w * 0.75f, dY2, w, dY3)
                lineTo(w, h)
                close()
            }

            drawPath(
                path = downPath,
                brush = Brush.verticalGradient(
                    listOf(downColor.copy(alpha = 0.35f), downColor.copy(alpha = 0.05f)),
                    startY = baseDown - ampDown,
                    endY = h,
                ),
            )

            // Upload Wave Line (Pink / Theme Accent)
            val baseUp = h * 0.40f
            val ampUp = h * 0.18f
            val uY0 = baseUp + kotlin.math.sin(phase * 1.2f + 1.2f) * ampUp
            val uY1 = baseUp + kotlin.math.sin(phase * 1.2f + 2.8f) * (ampUp * 1.15f)
            val uY2 = baseUp + kotlin.math.sin(phase * 1.2f + 4.5f) * (ampUp * 0.85f)
            val uY3 = baseUp + kotlin.math.sin(phase * 1.2f + 6.0f) * ampUp

            val upPath = Path().apply {
                moveTo(0f, uY0)
                cubicTo(w * 0.25f, uY0, w * 0.25f, uY1, w * 0.5f, uY1)
                cubicTo(w * 0.75f, uY1, w * 0.75f, uY2, w, uY3)
            }

            drawPath(
                path = upPath,
                brush = Brush.horizontalGradient(listOf(upColor.copy(alpha = 0.85f), downColor.copy(alpha = 0.85f))),
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }

        // Overlaid Data Metrics
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(downColor))
                    Text(stringResource(Res.string.traffic_download_rate_label), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFD1D5DB))
                }
                Text(stringResource(Res.string.wave_chart_layout), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF9CA3AF))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("↑", fontWeight = FontWeight.Bold, color = upColor, fontSize = 16.sp)
                    Text("$upVal $upUnit", fontWeight = FontWeight.ExtraBold, color = textColor, fontSize = 15.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$downVal $downUnit", fontWeight = FontWeight.ExtraBold, color = textColor, fontSize = 15.sp)
                    Text("↓", fontWeight = FontWeight.Bold, color = downColor, fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * Dual animated tachometer gauges for TrafficCardStyle.CyberGauges
 */
@Composable
private fun CyberDualGauges(
    upVal: String,
    upUnit: String,
    downVal: String,
    downUnit: String,
    downloadBps: Long,
    uploadBps: Long,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    personalization: Personalization,
    modifier: Modifier = Modifier,
) {
    val upColor = personalization.connectColor ?: Color(0xFFF72585)
    val downColor = Color(0xFF06B6D4)

    val upPct = ((uploadBps.toFloat() / 2_000_000f).coerceIn(0.08f, 1f))
    val downPct = ((downloadBps.toFloat() / 5_000_000f).coerceIn(0.08f, 1f))

    val animUpSweep by animateFloatAsState(targetValue = upPct * 220f, animationSpec = tween(400), label = "up-sweep")
    val animDownSweep by animateFloatAsState(targetValue = downPct * 220f, animationSpec = tween(400), label = "down-sweep")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.2.dp, cardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Upload Tachometer Gauge
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 4.dp.toPx()
                        // Track
                        drawArc(
                            color = Color(0x30FFFFFF),
                            startAngle = 160f,
                            sweepAngle = 220f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                        // Active Arc
                        drawArc(
                            color = upColor,
                            startAngle = 160f,
                            sweepAngle = animUpSweep,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                    }
                    Text("↑", fontWeight = FontWeight.Bold, color = upColor, fontSize = 16.sp)
                }
                Column {
                    Text(stringResource(Res.string.traffic_upload_label), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF9CA3AF))
                    Text("$upVal $upUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                }
            }

            Box(modifier = Modifier.height(38.dp).width(1.dp).background(Color(0x25FFFFFF)))

            // Download Tachometer Gauge
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 4.dp.toPx()
                        // Track
                        drawArc(
                            color = Color(0x30FFFFFF),
                            startAngle = 160f,
                            sweepAngle = 220f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                        // Active Arc
                        drawArc(
                            color = downColor,
                            startAngle = 160f,
                            sweepAngle = animDownSweep,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                    }
                    Text("↓", fontWeight = FontWeight.Bold, color = downColor, fontSize = 16.sp)
                }
                Column {
                    Text(stringResource(Res.string.traffic_download_label), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF9CA3AF))
                    Text("$downVal $downUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Responsive bouncing equalizer bars for TrafficCardStyle.WaveEqualizer
 */
@Composable
private fun WaveEqualizerCard(
    upVal: String,
    upUnit: String,
    downVal: String,
    downUnit: String,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    personalization: Personalization,
    modifier: Modifier = Modifier,
) {
    val upColor = personalization.connectColor ?: Color(0xFFF72585)
    val downColor = Color(0xFF06B6D4)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.2.dp, cardBorder.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
    ) {
        NeonWaveAccent(
            color = personalization.cardWaterColor ?: upColor,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.16f,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniSoundBars(color = upColor, barCount = 6)
                Column {
                    Text("↑ $upVal $upUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.5.sp)
                    Text(stringResource(Res.string.traffic_upload_rate_label), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF9CA3AF))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("↓ $downVal $downUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.5.sp)
                    Text(stringResource(Res.string.traffic_download_rate_short_label), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF9CA3AF))
                }
                MiniSoundBars(color = downColor, barCount = 6)
            }
        }
    }
}

/**
 * Concentric pulsing rings for TrafficCardStyle.Rings
 */
@Composable
private fun ConcentricRingsCard(
    upVal: String,
    upUnit: String,
    downVal: String,
    downUnit: String,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    personalization: Personalization,
    modifier: Modifier = Modifier,
) {
    val upColor = personalization.connectColor ?: Color(0xFFF72585)
    val downColor = Color(0xFF06B6D4)
    val transition = rememberInfiniteTransition(label = "rings-pulse")
    val ringPulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring-scale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.2.dp, cardBorder.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer { scaleX = ringPulse; scaleY = ringPulse }
                        .clip(CircleShape)
                        .background(upColor.copy(alpha = 0.20f))
                        .border(2.5.dp, upColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↑", fontWeight = FontWeight.ExtraBold, color = upColor, fontSize = 16.sp)
                }
                Column {
                    Text(stringResource(Res.string.traffic_upload_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text("$upVal $upUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer { scaleX = ringPulse; scaleY = ringPulse }
                        .clip(CircleShape)
                        .background(downColor.copy(alpha = 0.20f))
                        .border(2.5.dp, downColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↓", fontWeight = FontWeight.ExtraBold, color = downColor, fontSize = 16.sp)
                }
                Column {
                    Text(stringResource(Res.string.traffic_download_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text("$downVal $downUnit", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * Dedicated tile displaying current connected server name, protocol & transport badge
 */
@Composable
private fun ActiveConfigDetailTile(
    activeConfigName: String?,
    activeConfigDetail: String?,
    activeLocked: Boolean,
    activeExpiresAt: Long? = null,
    personalization: Personalization,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = personalization.dashboardBgColor ?: Color(0xE6140A1C)
    val cardBorder = personalization.dashboardBorderColor ?: Color(0x60F72585)
    val textColor = personalization.dashboardTextColor ?: Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(1.2.dp, cardBorder.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        NeonWaveAccent(
            color = personalization.cardWaterColor ?: NeonTheme.PrimaryPink,
            modifier = Modifier.fillMaxWidth().height(36.dp).align(Alignment.BottomCenter),
            alpha = 0.16f,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Right in RTL: Config Icon/Badge + Server Name & Protocol
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                val isNoirTile = personalization.themeProfileId == "noir"
                val tileAccent = personalization.serverBorderColor ?: personalization.connectColor ?: (if (isNoirTile) Color(0xFFE2E8F0) else NeonTheme.PrimaryPink)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tileAccent.copy(alpha = 0.18f))
                        .border(1.dp, tileAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_dns),
                        contentDescription = null,
                        tint = tileAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = activeConfigName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.server_not_selected),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (activeLocked) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_lock),
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp),
                            )
                            if (activeExpiresAt != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x30F59E0B))
                                        .border(0.6.dp, Color(0x60F59E0B), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    dev.valid7996.narcicgeteway.ui.servers.ExpiryCountdownBadge(
                                        expiresAt = activeExpiresAt,
                                        isDark = isDark,
                                        fontSize = 9.5.sp,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (activeLocked) stringResource(Res.string.server_protected_sub) else (activeConfigDetail?.takeIf { it.isNotBlank() } ?: "VLESS • TCP • TLS"),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Left in RTL: Change Server Button / Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x25FFFFFF))
                    .border(0.8.dp, Color(0x40FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stringResource(Res.string.change_server),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun MergedDashboardCard(
    info: NetworkInfo,
    downloadBps: Long,
    uploadBps: Long,
    totalDownload: Long,
    totalUpload: Long,
    onRefresh: () -> Unit,
    onToggleMap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    connectedColor: Color = ConnectedGreen,
    personalization: Personalization = Personalization.Default,
    isNoir: Boolean = false,
    isBlossom: Boolean = false,
) {
    val shape = RoundedCornerShape(26.dp)

    val dashBorder = personalization.dashboardBorderColor ?: personalization.serverBorderColor ?: (if (isNoir) Color(0xFFCBD5E1) else Color(0x90F72585))
    val dashBg = personalization.dashboardBgColor ?: personalization.cardBgColor ?: (if (isNoir) Color(0xE616161A) else Color(0xE612081C))
    val dashWave = personalization.cardWaterColor ?: personalization.serverBorderColor ?: (if (isNoir) Color(0xFF64748B) else Color(0xFFF72585))
    val upColor = personalization.connectColor ?: (if (isNoir) Color(0xFFE2E8F0) else Color(0xFFF72585))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(dashBg)
            .border(1.2.dp, dashBorder, shape),
    ) {
        // Neon wave accent at the bottom
        NeonWaveAccent(
            color = dashWave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter),
            alpha = 0.22f,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            // Top Row: Server Location, IP, Flag on the right in RTL; Ping, Map, Refresh on the left in RTL
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Right in RTL: Flag + City, Country + IP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FlagBadge(countryCode = info.countryCode, size = 30.dp)
                    Column {
                        Text(
                            text = listOfNotNull(info.city, info.country).joinToString(", ")
                                .ifBlank { null }
                                ?: "Atlanta, United States",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = info.ipv4 ?: "198.44.138.67",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                            ),
                            color = Color(0xFF9CA3AF),
                            maxLines = 1,
                        )
                    }
                }

                // Left in RTL: Ping pill + Map button + Refresh button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val ping = info.pingMs ?: 1245
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x35F72585))
                            .border(0.8.dp, Color(0x60F72585), RoundedCornerShape(12.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "ms $ping",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = Color(0xFFFF80BF),
                        )
                    }

                    if (onToggleMap != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x20FFFFFF))
                                .clickable { onToggleMap() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_map),
                                contentDescription = "Map",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x20FFFFFF))
                            .clickable(enabled = !info.loading) { onRefresh() },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (info.loading) {
                            CyberRadarIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.ic_sync),
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0x25F72585))
            Spacer(Modifier.height(10.dp))

            // Bottom Row: Traffic Statistics with Equalizer Mini-Bars
            val (downVal, downUnit) = formatRate(downloadBps)
            val (upVal, upUnit) = formatRate(uploadBps)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Upload side: Mini-bars + ↑ Speed + بارگذاری
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiniSoundBars(color = upColor)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "↑ ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = upColor,
                            )
                            Text(
                                text = "$upVal $upUnit",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                ),
                                color = Color.White,
                            )
                        }
                        Text(
                            text = stringResource(Res.string.traffic_upload_label),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9CA3AF),
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(Color(0x25FFFFFF)),
                )

                // Download side: ↓ Speed + دانلود + Cyan mini-bars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "↓ ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = Color(0xFF06B6D4),
                            )
                            Text(
                                text = "$downVal $downUnit",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                ),
                                color = Color.White,
                            )
                        }
                        Text(
                            text = stringResource(Res.string.traffic_download_label),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9CA3AF),
                        )
                    }
                    MiniSoundBars(color = Color(0xFF06B6D4))
                }
            }
        }
    }
}

@Composable
private fun MiniSoundBars(
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
) {
    val factors = listOf(0.40f, 0.85f, 0.55f, 0.95f, 0.65f, 0.45f)
    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (i in 0 until barCount) {
            val h = 4.dp + 14.dp * factors[i % factors.size]
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun DashboardReading(
    icon: DrawableResource,
    label: String,
    bytesPerSecond: Long,
    total: Long,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    accentColor: Color? = null,
) {
    val (value, unit) = formatRate(bytesPerSecond)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = if (bytesPerSecond > 0) (accentColor ?: primaryColor) else secondaryColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = primaryColor,
                    maxLines = 1,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Text(
                formatBytes(total),
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor,
                maxLines = 1,
            )
        }
    }
}

