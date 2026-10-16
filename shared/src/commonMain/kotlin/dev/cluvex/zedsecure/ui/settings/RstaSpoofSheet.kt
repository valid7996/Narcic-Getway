@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.config.SniSpoofConfig
import dev.cluvex.zedsecure.domain.model.AppSettings
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.ui.components.PickerField
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * The no-root SNI spoof, laid out as RstaNG lays it out: ONE page with the connect IP, the connect port, the fake SNI
 * and the method, and the state of the engine with its log. Nothing is created per profile: every profile whose server
 * is 127.0.0.1:40443 goes through the engine with these settings, and the changes apply at once.
 */
@Composable
fun RstaSpoofSheet(settings: AppSettings, onUpdate: (AppSettings) -> Unit, onDismiss: () -> Unit) {
    val platform = LocalPlatform.current
    var ip by remember { mutableStateOf(settings.rstaSpoofConnectIp) }
    var port by remember { mutableStateOf(settings.rstaSpoofConnectPort.toString()) }
    var sni by remember { mutableStateOf(settings.rstaSpoofFakeSni) }
    var method by remember { mutableStateOf(SniSpoofConfig.method(settings.rstaSpoofMethod)) }
    var status by remember { mutableStateOf("") }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLog by remember { mutableStateOf(false) }

    // The engine runs in another process: what it says about itself is read from the files it writes.
    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) { status = platform.rstaSpoofStatus(); log = platform.rstaSpoofLog() }
            delay(1_000)
        }
    }

    fun save(
        newIp: String = ip, newPort: String = port, newSni: String = sni, newMethod: String = method,
    ) {
        onUpdate(
            settings.copy(
                rstaSpoofConnectIp = newIp.trim(),
                rstaSpoofConnectPort = newPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: 443,
                rstaSpoofFakeSni = newSni.trim(),
                rstaSpoofMethod = SniSpoofConfig.method(newMethod),
            ),
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.rsta_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.rsta_note), style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = ip, onValueChange = { ip = it; save(newIp = it) },
                label = { Text(stringResource(Res.string.rsta_connect_ip)) },
                supportingText = { Text(stringResource(Res.string.rsta_connect_ip_hint)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = port, onValueChange = { v -> val d = v.filter(Char::isDigit).take(5); port = d; save(newPort = d) },
                label = { Text(stringResource(Res.string.rsta_connect_port)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sni, onValueChange = { sni = it; save(newSni = it) },
                label = { Text(stringResource(Res.string.rsta_fake_sni)) },
                supportingText = { Text(stringResource(Res.string.rsta_fake_sni_hint)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            PickerField(
                label = stringResource(Res.string.rsta_method),
                options = SniSpoofConfig.METHODS.map { it to it },
                selected = method,
                onSelect = { method = it; save(newMethod = it) },
            )

            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.rsta_status), style = MaterialTheme.typography.labelMedium)
                    Text(status.ifEmpty { "…" }, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(Res.string.rsta_status_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = { showLog = !showLog }, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.rsta_view_log))
            }
            if (showLog) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        log.takeLast(60).joinToString("\n").ifEmpty { stringResource(Res.string.rsta_log_empty) },
                        style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}
