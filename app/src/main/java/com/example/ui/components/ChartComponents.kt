package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.util.CurrencyFormatter

data class CategorySlice(
    val name: String,
    val amount: Double,
    val color: Color
)

@Composable
fun DonutExpenseChart(
    slices: List<CategorySlice>,
    currency: String,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(190.dp)
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 26.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                if (slices.isEmpty() || totalExpense <= 0) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.2f),
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var startAngle = -90f
                    for (slice in slices) {
                        val sweepAngle = ((slice.amount / totalExpense) * 360f).toFloat() * progress.value
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.format(totalExpense, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend row
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (slice in slices.take(5)) {
                val pct = if (totalExpense > 0) ((slice.amount / totalExpense) * 100).toInt() else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = slice.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "$pct% • ${CurrencyFormatter.format(slice.amount, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ComparisonBarChart(
    income: Double,
    expense: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(income, expense) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val maxVal = maxOf(income, expense, 1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = "Income vs Expense",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Income Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Income",
                modifier = Modifier.width(64.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Gray.copy(alpha = 0.15f))
            ) {
                val ratio = (income / maxVal).toFloat().coerceIn(0f, 1f) * progress.value
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ColorIncome)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = CurrencyFormatter.format(income, currency),
                style = MaterialTheme.typography.labelSmall,
                color = ColorIncome,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Expense Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Expense",
                modifier = Modifier.width(64.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Gray.copy(alpha = 0.15f))
            ) {
                val ratio = (expense / maxVal).toFloat().coerceIn(0f, 1f) * progress.value
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ColorExpense)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = CurrencyFormatter.format(expense, currency),
                style = MaterialTheme.typography.labelSmall,
                color = ColorExpense,
                fontSize = 11.sp
            )
        }
    }
}
