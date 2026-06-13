package com.cryptosignalai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cryptosignalai.data.model.AiProviderType
import com.cryptosignalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark, titleContentColor = OnDark,
                    navigationIconContentColor = OnDark
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text("AI Model", color = OnDark, style = MaterialTheme.typography.titleMedium)
            Text("Choose which model validates signals and add its API key.",
                color = MutedText, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            AiProviderType.entries.forEach { type ->
                val p = state.providers[type] ?: ProviderUiState(type)
                ProviderCard(
                    state = p,
                    isSelected = state.selectedProvider == type,
                    onSelect = { vm.selectProvider(type) },
                    onDraftChange = { vm.onDraftChange(type, it) },
                    onSave = { vm.saveKey(type) },
                    onEdit = { vm.editKey(type) },
                    onTest = { vm.testConnection(type) }
                )
                Spacer(Modifier.height(12.dp))
            }

            HorizontalDivider(color = SurfaceVariantDark)
            Spacer(Modifier.height(12.dp))
            Text("Alerts & Background", color = OnDark, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ToggleRow("Local notifications (confidence ≥ 80%)",
                state.notificationsEnabled, vm::setNotifications)
            ToggleRow("Background checks every 15 min",
                state.backgroundEnabled, vm::setBackground)

            Spacer(Modifier.height(24.dp))
            Text(
                "Keys are stored encrypted on-device (Android Keystore). " +
                    "No data is sent anywhere except the market APIs and the AI you select.",
                color = MutedText, style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProviderCard(
    state: ProviderUiState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(
                    selected = isSelected, onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = MutedText)
                )
                Text(state.type.displayName, color = OnDark, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (state.saved) Text("● configured", color = BuyGreen,
                    style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChange,
                enabled = state.editing,
                singleLine = true,
                label = { Text("API key") },
                visualTransformation = if (state.editing) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnDark, unfocusedTextColor = OnDark,
                    disabledTextColor = MutedText,
                    focusedBorderColor = Accent, unfocusedBorderColor = SurfaceVariantDark,
                    focusedLabelColor = Accent, unfocusedLabelColor = MutedText
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.editing) {
                    Button(onClick = onSave,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        Text("Save")
                    }
                } else {
                    OutlinedButton(onClick = onEdit) { Text("Edit") }
                }
                OutlinedButton(onClick = onTest, enabled = !state.testing) {
                    if (state.testing) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Test connection")
                }
            }
            state.testResult?.let {
                Spacer(Modifier.height(6.dp))
                val ok = it.contains("OK", true) || it == "Saved"
                Text(it, color = if (ok) BuyGreen else SellRed,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, color = OnDark, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnDark, checkedTrackColor = Accent
            )
        )
    }
}
