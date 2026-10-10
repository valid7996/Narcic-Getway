@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.domain.config.PsiphonConfigBuilder
import dev.cluvex.zedsecure.domain.config.PsiphonProfile
import dev.cluvex.zedsecure.ui.components.PickerField

@Composable
fun PsiphonSheet(
    initial: PsiphonProfile? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, PsiphonProfile) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var country by remember { mutableStateOf(initial?.country ?: "") }
    var mode by remember { mutableStateOf(initial?.mode ?: PsiphonConfigBuilder.MODE_AUTO) }
    var cdnIps by remember { mutableStateOf(initial?.cdnIps ?: "") }
    var cdnSni by remember { mutableStateOf(initial?.cdnSni ?: "") }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xF20B1C38) else Color(0xF8F8FAFD),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B).copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.psiphon_add_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.padding(vertical = 4.dp),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.manual_remark)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            PsiphonCountryField(
                selected = country,
                onSelect = { country = it },
            )
            PickerField(
                label = stringResource(Res.string.psiphon_mode),
                options = PsiphonConfigBuilder.MODES.map { it to it },
                selected = mode,
                onSelect = { mode = it },
            )
            if (mode == PsiphonConfigBuilder.MODE_CDN) {
                OutlinedTextField(
                    value = cdnIps,
                    onValueChange = { cdnIps = it },
                    label = { Text(stringResource(Res.string.psiphon_cdn_ips)) },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cdnSni,
                    onValueChange = { cdnSni = it },
                    label = { Text(stringResource(Res.string.psiphon_cdn_sni)) },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        PsiphonProfile(
                            country = country.trim(),
                            mode = mode,
                            cdnIps = cdnIps.trim(),
                            cdnSni = cdnSni.trim(),
                        ),
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) dev.cluvex.zedsecure.ui.theme.ZedGreen else Color(0xFF0D9488),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
