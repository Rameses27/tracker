package com.aro.expensetracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// A fixed, readable color palette so the same category always gets the same color
// across the pie chart and the legend.
val CHART_COLORS = listOf(
    Color(0xFF2E7D32), Color(0xFFEF6C00), Color(0xFF1565C0), Color(0xFFC62828),
    Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFF9E9D24), Color(0xFF4E342E)
)

@Composable
fun PieChart(data: Map<String, Double>, modifier: Modifier = Modifier) {
    val total = data.values.sum()
    Column(modifier = modifier) {
        if (total <= 0.0) {
            Text("No expenses yet this month", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(140.dp)) {
                var startAngle = -90f
                data.entries.forEachIndexed { index, (_, value) ->
                    val sweep = (value / total * 360.0).toFloat()
                    drawArc(
                        color = CHART_COLORS[index % CHART_COLORS.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweep
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                data.entries.forEachIndexed { index, (category, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = CHART_COLORS[index % CHART_COLORS.size])
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$category — %.0f%%".format(value / total * 100),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarChart(data: Map<String, Double>, modifier: Modifier = Modifier) {
    val maxValue = data.values.maxOrNull() ?: 0.0
    Column(modifier = modifier.fillMaxWidth()) {
        if (maxValue <= 0.0) {
            Text("No data to chart yet", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        data.entries.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(label, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall)
                Canvas(modifier = Modifier.weight(1f).height(18.dp)) {
                    val barWidth = (value / maxValue).toFloat() * size.width
                    drawRoundRect(
                        color = CHART_COLORS[index % CHART_COLORS.size],
                        size = Size(barWidth.coerceAtLeast(4f), size.height)
                    )
                }
                Text("%.0f".format(value), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
