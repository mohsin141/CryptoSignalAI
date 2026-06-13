package com.cryptosignalai.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cryptosignalai.data.model.Timeframe
import com.cryptosignalai.data.model.Token
import com.cryptosignalai.ui.components.IndicatorSummary
import com.cryptosignalai.ui.components.SignalCard
import com.cryptosignalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("CryptoSignalAI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark, titleContentColor = OnDark
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Token + timeframe selectors
            TokenSelector(selected = state.token, onSelect = vm::selectToken)
            Spacer(Modifier.height(8.dp))
            TimeframeSelector(selected = state.timeframe, onSelect = vm::selectTimeframe)
            Spacer(Modifier.height(16.dp))

            // Signal card
            if (state.loading) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Accent)
                        Spacer(Modifier.height(12.dp))
                        Text("Analyzing market…", color = MutedText)
                    }
                }
            } else {
                state.signal?.let { SignalCard(it) } ?: EmptySignalCard()
            }

            Spacer(Modifier.height(16.dp))
            IndicatorSummary(state.signal?.indicators)

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = SellRed)
            }

            Spacer(Modifier.height(20.dp))
            BottomActions(
                loading = state.loading,
                onRefresh = vm::refresh,
                onHistory = onOpenHistory,
                onSettings = onOpenSettings
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EmptySignalCard() {
    Card(
        Modifier.fillMaxWidth().height(180.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tap Refresh to generate a validated signal",
                color = MutedText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenSelector(selected: Token, onSelect: (Token) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "${selected.name} — ${selected.displayName}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Token") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnDark, unfocusedTextColor = OnDark,
                focusedBorderColor = Accent, unfocusedBorderColor = SurfaceVariantDark,
                focusedLabelColor = Accent, unfocusedLabelColor = MutedText
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Token.entries.forEach { token ->
                DropdownMenuItem(
                    text = { Text("${token.name} — ${token.displayName}") },
                    onClick = { onSelect(token); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun TimeframeSelector(selected: Timeframe, onSelect: (Timeframe) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Timeframe.entries.forEach { tf ->
            val active = tf == selected
            FilterChip(
                selected = active,
                onClick = { onSelect(tf) },
                label = { Text(tf.label) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Accent,
                    selectedLabelColor = OnDark,
                    containerColor = SurfaceDark,
                    labelColor = MutedText
                )
            )
        }
    }
}

@Composable
private fun BottomActions(
    loading: Boolean,
    onRefresh: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onRefresh,
            enabled = !loading,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp)); Text("Refresh")
        }
        OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.History, contentDescription = null)
            Spacer(Modifier.width(6.dp)); Text("History")
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Notifications, contentDescription = null)
            Spacer(Modifier.width(6.dp)); Text("Alerts")
        }
        OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Spacer(Modifier.width(6.dp)); Text("Settings")
        }
    }
}
