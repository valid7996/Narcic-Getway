package dev.valid7996.narcicgeteway.core

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import dev.valid7996.narcicgeteway.NrgSecureApp
import dev.valid7996.narcicgeteway.R
import dev.valid7996.narcicgeteway.core.platform.LocaleManager
import dev.valid7996.narcicgeteway.data.settings.SettingsRepository
import dev.valid7996.narcicgeteway.domain.model.ConnectionState
import dev.valid7996.narcicgeteway.ui.format.formatBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class NrgWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(ctx: Context) {
        super.onEnabled(ctx)
        hasWidgets = true
    }

    override fun onDisabled(ctx: Context) {
        super.onDisabled(ctx)
        checkHasWidgets(ctx)
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        hasWidgets = ids.isNotEmpty()
        val locCtx = localized(ctx)
        val views = renderForProvider(locCtx, this::class.java)
        ids.forEach { mgr.updateAppWidget(it, views) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_TOGGLE) {
            toggle(ctx)
        }
    }

    private fun toggle(ctx: Context) {
        val state = VpnManager.status.value.state
        if (state.isActive || state.isTransitioning) {
            if (Ikev2Controller.isActive) Ikev2Controller.stop(ctx) else AndroidVpn.stop(ctx)
            VpnManager.onDisconnected()
            refresh(ctx, force = true)
        } else {
            connectOrOpen(ctx)
        }
    }

    private fun connectOrOpen(ctx: Context) {
        val app = ctx.applicationContext as? NrgSecureApp
        val profile = runCatching { app?.container?.configRepository?.activeProfile() }.getOrNull()
        val needsConsent = runCatching { android.net.VpnService.prepare(ctx) != null }.getOrDefault(true)

        if (profile == null || needsConsent) {
            val intent = connectIntent(ctx)
            ctx.startActivity(intent)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val plan = runCatching { StartPlanner(ctx).plan(profile) }.getOrNull()
            when (plan) {
                is StartPlanner.Plan.Start -> {
                    AndroidVpn.start(
                        ctx,
                        plan.configJson,
                        plan.remark,
                        plan.socksPort,
                        plan.kind,
                        proxyOnly = plan.proxyOnly,
                    )
                    refresh(ctx, force = true)
                }
                is StartPlanner.Plan.Ikev2 -> {
                    val consent = runCatching { Ikev2Controller.provision(ctx, plan.profile) }.getOrNull()
                    if (consent == null) {
                        Ikev2Controller.startProvisioned(ctx, plan.remark)
                        refresh(ctx, force = true)
                    } else {
                        ctx.startActivity(connectIntent(ctx))
                    }
                }
                else -> {
                    ctx.startActivity(connectIntent(ctx))
                }
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "dev.valid7996.narcicgeteway.widget.TOGGLE"
        private const val REQ_TOGGLE = 100
        private const val REQ_OPEN = 101

        private const val MIN_INTERVAL_MS = 1_500L

        @Volatile private var lastPush = 0L
        @Volatile private var hasWidgets: Boolean? = null

        private val PROVIDER_CLASSES = listOf(
            NrgWidgetSmallProvider::class.java,
            NrgWidgetMediumProvider::class.java,
            NrgWidgetLargeProvider::class.java,
            NrgWidgetToggleProvider::class.java,
            NrgWidgetProvider::class.java,
        )

        fun refresh(ctx: Context, force: Boolean = false) {
            if (hasWidgets == false && !force) return
            val now = SystemClock.elapsedRealtime()
            if (!force && now - lastPush < MIN_INTERVAL_MS) return
            lastPush = now
            pushNow(ctx)
        }

        private fun checkHasWidgets(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx) ?: return
            hasWidgets = PROVIDER_CLASSES.any { cls ->
                mgr.getAppWidgetIds(ComponentName(ctx, cls)).isNotEmpty()
            }
        }

        private fun pushNow(ctx: Context) {
            runCatching {
                val mgr = AppWidgetManager.getInstance(ctx) ?: return
                var activeFound = false
                val locCtx = localized(ctx)
                for (cls in PROVIDER_CLASSES) {
                    val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
                    if (ids.isNotEmpty()) {
                        activeFound = true
                        val views = renderForProvider(locCtx, cls)
                        ids.forEach { mgr.updateAppWidget(it, views) }
                    }
                }
                hasWidgets = activeFound
            }
        }

        private fun localized(ctx: Context): Context =
            runCatching { LocaleManager.wrap(ctx, SettingsRepository.readLanguageTag(ctx)) }
                .getOrDefault(ctx)

        private fun connectIntent(ctx: Context): Intent =
            Intent(ctx, dev.valid7996.narcicgeteway.MainActivity::class.java).apply {
                action = VpnNotifications.ACTION_CONNECT
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

        private fun openIntent(ctx: Context): PendingIntent {
            val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: connectIntent(ctx)
            return PendingIntent.getActivity(
                ctx, REQ_OPEN, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun toggleIntent(ctx: Context): PendingIntent =
            PendingIntent.getBroadcast(
                ctx, REQ_TOGGLE,
                Intent(ctx, NrgWidgetProvider::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private data class Snapshot(
            val state: String,
            val server: String,
            val flag: String,
            val meta: String,
            val down: String,
            val up: String,
            val on: Boolean,
            val chronometerBase: Long,
            val durationText: String,
            val dotColor: Int,
        )

        private fun extractFlag(text: String): String? {
            var i = 0
            while (i <= text.length - 4) {
                if (text[i] == '\uD83C' && text[i + 1] in '\uDDE6'..'\uDDFF' &&
                    text[i + 2] == '\uD83C' && text[i + 3] in '\uDDE6'..'\uDDFF') {
                    return text.substring(i, i + 4)
                }
                i++
            }
            return null
        }

        private fun countryCodeToFlag(code: String): String {
            if (code.length != 2) return "⚡"
            val c1 = code[0].uppercaseChar()
            val c2 = code[1].uppercaseChar()
            if (c1 in 'A'..'Z' && c2 in 'A'..'Z') {
                val first = c1.code - 'A'.code + 0x1F1E6
                val second = c2.code - 'A'.code + 0x1F1E6
                return String(Character.toChars(first)) + String(Character.toChars(second))
            }
            return "⚡"
        }

        private fun cleanName(text: String): String {
            val sb = StringBuilder(text.length)
            var i = 0
            while (i < text.length) {
                if (i <= text.length - 4 &&
                    text[i] == '\uD83C' && text[i + 1] in '\uDDE6'..'\uDDFF' &&
                    text[i + 2] == '\uD83C' && text[i + 3] in '\uDDE6'..'\uDDFF') {
                    i += 4
                } else {
                    sb.append(text[i])
                    i++
                }
            }
            return sb.toString().replace(Regex("""\s+"""), " ").trim().ifBlank { text }
        }

        private fun snapshot(ctx: Context): Snapshot {
            val s = VpnManager.status.value
            val on = s.state.isActive || s.state.isTransitioning

            val activeProfile = runCatching {
                (ctx.applicationContext as? NrgSecureApp)?.container?.configRepository?.activeProfile()
            }.getOrNull()

            val rawName = s.serverName ?: activeProfile?.name ?: ""
            val flagFromCode = activeProfile?.countryCode?.let { countryCodeToFlag(it) }
            val flagFromName = extractFlag(rawName)
            val flag = flagFromCode ?: flagFromName ?: "⚡"

            val serverTitle = cleanName(rawName).ifBlank {
                ctx.getString(R.string.widget_no_server)
            }

            val kind = VpnManager.activeKind.value ?: activeProfile?.engine?.name ?: "VPN"
            val meta = if (on) {
                "$kind • ${ctx.getString(R.string.state_connected)}"
            } else {
                ctx.getString(R.string.widget_description)
            }

            val downStr = if (s.downloadBps > 0) "${formatBytes(s.downloadBps)}/s" else formatBytes(s.totalDownload)
            val upStr = if (s.uploadBps > 0) "${formatBytes(s.uploadBps)}/s" else formatBytes(s.totalUpload)

            val durationSeconds = s.durationSeconds
            val durationText = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60)
            val chronometerBase = SystemClock.elapsedRealtime() - (durationSeconds * 1000L)

            val stateText = ctx.getString(
                when (s.state) {
                    ConnectionState.Connected -> R.string.state_connected
                    ConnectionState.Connecting -> R.string.state_connecting
                    ConnectionState.Reconnecting -> R.string.state_reconnecting
                    ConnectionState.Disconnecting -> R.string.state_disconnecting
                    ConnectionState.Error -> R.string.state_error
                    ConnectionState.Idle -> R.string.state_idle
                },
            )

            val dotColor = ctx.getColor(
                when (s.state) {
                    ConnectionState.Connected -> R.color.widget_ok
                    ConnectionState.Error -> R.color.widget_warn
                    ConnectionState.Idle -> R.color.widget_text_dim
                    else -> R.color.widget_accent
                },
            )

            return Snapshot(
                state = stateText,
                server = BidiText.auto(serverTitle),
                flag = flag,
                meta = meta,
                down = BidiText.ltr(downStr),
                up = BidiText.ltr(upStr),
                on = on,
                chronometerBase = chronometerBase,
                durationText = durationText,
                dotColor = dotColor,
            )
        }

        private fun renderForProvider(ctx: Context, providerClass: Class<*>): RemoteViews {
            val snap = snapshot(ctx)
            val small = buildSmall(ctx, snap)
            val medium = buildMedium(ctx, snap)
            val large = buildLarge(ctx, snap)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return RemoteViews(
                    mapOf(
                        SizeF(60f, 60f) to small,
                        SizeF(180f, 60f) to medium,
                        SizeF(180f, 120f) to large,
                    ),
                )
            }

            return when (providerClass) {
                NrgWidgetSmallProvider::class.java, NrgWidgetToggleProvider::class.java -> small
                NrgWidgetMediumProvider::class.java -> medium
                else -> large
            }
        }

        private fun buildSmall(ctx: Context, snap: Snapshot): RemoteViews {
            val views = RemoteViews(ctx.packageName, R.layout.widget_bento_small)
            views.setOnClickPendingIntent(R.id.wb_root, openIntent(ctx))
            views.setOnClickPendingIntent(R.id.wb_power, toggleIntent(ctx))

            views.setTextViewText(R.id.wb_state, snap.state)

            if (snap.on) {
                views.setInt(R.id.wb_bg, "setColorFilter", ctx.getColor(R.color.wb_stage))
                views.setInt(R.id.wb_power_bg, "setColorFilter", ctx.getColor(R.color.wb_accent))
                views.setInt(R.id.wb_power_icon, "setColorFilter", ctx.getColor(R.color.wb_on_accent))
                views.setTextColor(R.id.wb_state, ctx.getColor(R.color.wb_on_stage))

                views.setViewVisibility(R.id.wb_timer, View.VISIBLE)
                views.setChronometer(R.id.wb_timer, snap.chronometerBase, null, true)
                views.setTextColor(R.id.wb_timer, ctx.getColor(R.color.wb_on_stage))

                views.setViewVisibility(R.id.wb_sub, View.GONE)
            } else {
                views.setInt(R.id.wb_bg, "setColorFilter", ctx.getColor(R.color.wb_surface))
                views.setInt(R.id.wb_power_bg, "setColorFilter", ctx.getColor(R.color.wb_chip))
                views.setInt(R.id.wb_power_icon, "setColorFilter", ctx.getColor(R.color.wb_ink))
                views.setTextColor(R.id.wb_state, ctx.getColor(R.color.wb_ink))

                views.setViewVisibility(R.id.wb_timer, View.GONE)
                views.setChronometer(R.id.wb_timer, snap.chronometerBase, null, false)

                views.setViewVisibility(R.id.wb_sub, View.VISIBLE)
                views.setTextViewText(R.id.wb_sub, snap.server)
                views.setTextColor(R.id.wb_sub, ctx.getColor(R.color.wb_muted))
            }
            return views
        }

        private fun buildMedium(ctx: Context, snap: Snapshot): RemoteViews {
            val views = RemoteViews(ctx.packageName, R.layout.widget_bento_medium)
            views.setOnClickPendingIntent(R.id.wb_root, openIntent(ctx))
            views.setOnClickPendingIntent(R.id.wb_power, toggleIntent(ctx))

            views.setInt(R.id.wb_bg, "setColorFilter", ctx.getColor(R.color.wb_surface))
            views.setTextViewText(R.id.wb_flag, snap.flag)
            views.setTextViewText(R.id.wb_server, snap.server)
            views.setTextColor(R.id.wb_server, ctx.getColor(R.color.wb_ink))

            views.setInt(R.id.wb_dot, "setColorFilter", snap.dotColor)

            val statusLine = if (snap.on) "${snap.state} • ${snap.durationText}" else snap.state
            views.setTextViewText(R.id.wb_line, statusLine)
            views.setTextColor(R.id.wb_line, ctx.getColor(R.color.wb_muted))

            if (snap.on) {
                views.setInt(R.id.wb_power_bg, "setColorFilter", ctx.getColor(R.color.wb_accent))
                views.setInt(R.id.wb_power_icon, "setColorFilter", ctx.getColor(R.color.wb_on_accent))
            } else {
                views.setInt(R.id.wb_power_bg, "setColorFilter", ctx.getColor(R.color.wb_chip))
                views.setInt(R.id.wb_power_icon, "setColorFilter", ctx.getColor(R.color.wb_ink))
            }
            return views
        }

        private fun buildLarge(ctx: Context, snap: Snapshot): RemoteViews {
            val views = RemoteViews(ctx.packageName, R.layout.widget_bento_large)
            views.setOnClickPendingIntent(R.id.wb_root, openIntent(ctx))
            views.setOnClickPendingIntent(R.id.wb_power, toggleIntent(ctx))

            views.setInt(R.id.wb_bg, "setColorFilter", ctx.getColor(R.color.wb_stage))

            views.setTextViewText(R.id.wb_state, snap.state)
            if (snap.on) {
                views.setInt(R.id.wb_chip_bg, "setColorFilter", ctx.getColor(R.color.wb_accent))
                views.setTextColor(R.id.wb_state, ctx.getColor(R.color.wb_on_accent))

                views.setViewVisibility(R.id.wb_timer, View.VISIBLE)
                views.setChronometer(R.id.wb_timer, snap.chronometerBase, null, true)
                views.setTextColor(R.id.wb_timer, ctx.getColor(R.color.wb_on_stage))
            } else {
                views.setInt(R.id.wb_chip_bg, "setColorFilter", ctx.getColor(R.color.wb_chip))
                views.setTextColor(R.id.wb_state, ctx.getColor(R.color.wb_on_stage))

                views.setViewVisibility(R.id.wb_timer, View.GONE)
                views.setChronometer(R.id.wb_timer, snap.chronometerBase, null, false)
            }

            views.setTextViewText(R.id.wb_flag, snap.flag)
            views.setTextViewText(R.id.wb_server, snap.server)
            views.setTextColor(R.id.wb_server, ctx.getColor(R.color.wb_on_stage))

            views.setTextViewText(R.id.wb_meta, snap.meta)
            views.setTextColor(R.id.wb_meta, ctx.getColor(R.color.wb_on_stage))

            views.setTextViewText(R.id.wb_down, snap.down)
            views.setTextViewText(R.id.wb_up, snap.up)

            if (snap.on) {
                views.setInt(R.id.wb_power_bg, "setColorFilter", ctx.getColor(R.color.wb_accent))
                views.setInt(R.id.wb_power_icon, "setColorFilter", ctx.getColor(R.color.wb_on_accent))
            } else {
                views.setInt(R.id.wb_power_bg, "setColorFilter", ctx.getColor(R.color.wb_chip))
                views.setInt(R.id.wb_power_icon, "setColorFilter", ctx.getColor(R.color.wb_on_stage))
            }

            return views
        }
    }
}

class NrgWidgetSmallProvider : NrgWidgetProvider()
class NrgWidgetMediumProvider : NrgWidgetProvider()
class NrgWidgetLargeProvider : NrgWidgetProvider()
class NrgWidgetToggleProvider : NrgWidgetProvider()
