package com.cryptosignalai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptosignalai.data.model.FinalSignal
import com.cryptosignalai.ui.theme.*

@Composable
fun SignalCard(signal: FinalSignal, modifier: Modifier = Modifier) {
    val color = decisionColor(signal.decision)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${signal.token.name} • ${signal.timeframe.label}",
                color = MutedText,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = signal.decision.replace("_", " "),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Confidence", color = MutedText, style = MaterialTheme.typography.labelMedium)
            Text(
                "${signal.confidence}%",
                color = OnDark,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            LinearProgressIndicator(
                progress = { signal.confidence / 100f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = color,
                trackColor = SurfaceVariantDark
            )

            if (signal.decision == "BUY" || signal.decision == "SELL") {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PriceCell("Entry", signal.entry, OnDark)
                    PriceCell("Stop Loss", signal.stopLoss, SellRed)
                    PriceCell("Take Profit", signal.takeProfit, BuyGreen)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Risk: ", color = MutedText)
                    Text(signal.risk, color = riskColor(signal.risk), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(16.dp))
                    Text("Backtest: ", color = MutedText)
                    Text(
                        "${(signal.backtestWinRate * 100).toInt()}% (${signal.backtestSamples})",
                        color = OnDark, fontWeight = FontWeight.Medium
                    )
                }
            }

            if (signal.reason.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    signal.reason,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun RowScope.PriceCell(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelMedium)
        Text(formatPrice(value), color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatPrice(v: Double): String = when {
    v == 0.0 -> "—"
    v >= 100 -> String.format("%,.2f", v)
    v >= 1 -> String.format("%.3f", v)
    else -> String.format("%.5f", v)
}

fun decisionColor(decision: String): Color = when (decision) {
    "BUY" -> BuyGreen
    "SELL" -> SellRed
    "HOLD" -> HoldAmber
    else -> NoTradeGray
}

private fun riskColor(risk: String): Color = when (risk) {
    "LOW" -> BuyGreen
    "MEDIUM" -> HoldAmber
    "HIGH" -> SellRed
    else -> MutedText
}
