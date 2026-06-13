package com.cryptosignalai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cryptosignalai.data.model.IndicatorSnapshot
import com.cryptosignalai.ui.theme.*

@Composable
fun IndicatorSummary(ind: IndicatorSnapshot?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Indicator Summary", color = OnDark, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (ind == null) {
                Text("No data yet — tap Refresh.", color = MutedText)
                return@Column
            }
            IndicatorRow("RSI (14)", String.format("%.1f", ind.rsi), rsiColor(ind.rsi))
            IndicatorRow("MACD hist", String.format("%.4f", ind.macdHist),
                if (ind.macdHist >= 0) BuyGreen else SellRed)
            IndicatorRow("EMA trend", ind.emaTrend, trendColor(ind.emaTrend))
            IndicatorRow("ADX", String.format("%.1f", ind.adx),
                if (ind.adx >= 20) BuyGreen else MutedText)
            IndicatorRow("Volume", if (ind.volumeSpike) "SPIKE" else "Normal",
                if (ind.volumeSpike) BuyGreen else MutedText)
            IndicatorRow("VWAP", String.format("%.2f", ind.vwap), OnDark)
            IndicatorRow("Support / Resistance",
                "${trim(ind.support)} / ${trim(ind.resistance)}", OnDark)
        }
    }
}

@Composable
private fun IndicatorRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun trim(v: Double): String = if (v >= 1) String.format("%,.2f", v) else String.format("%.5f", v)
private fun rsiColor(rsi: Double): Color = when {
    rsi >= 70 -> SellRed
    rsi <= 30 -> BuyGreen
    else -> OnDark
}
private fun trendColor(t: String): Color = when (t) {
    "BULLISH" -> BuyGreen
    "BEARISH" -> SellRed
    else -> HoldAmber
}
