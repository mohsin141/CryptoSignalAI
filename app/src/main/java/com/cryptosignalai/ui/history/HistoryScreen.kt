package com.cryptosignalai.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cryptosignalai.data.local.SignalEntity
import com.cryptosignalai.ui.components.decisionColor
import com.cryptosignalai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, vm: HistoryViewModel = viewModel()) {
    val signals by vm.signals.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = { Text("Signal History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::clear) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark, titleContentColor = OnDark,
                    navigationIconContentColor = OnDark, actionIconContentColor = MutedText
                )
            )
        }
    ) { padding ->
        if (signals.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No signals yet", color = MutedText)
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(signals) { HistoryRow(it) }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryRow(s: SignalEntity) {
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${s.token} • ${s.timeframe}", color = MutedText,
                    style = MaterialTheme.typography.labelMedium)
                Text(s.decision.replace("_", " "),
                    color = decisionColor(s.decision), fontWeight = FontWeight.Bold)
                Text(fmt.format(Date(s.createdAt)), color = MutedText,
                    style = MaterialTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${s.confidence}%", color = OnDark, fontWeight = FontWeight.SemiBold)
                if (s.decision == "BUY" || s.decision == "SELL") {
                    Text("WR ${(s.backtestWinRate * 100).toInt()}%",
                        color = MutedText, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
