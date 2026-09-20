package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.util.CurrencyFormatter

@Composable
fun BalanceCard(
    totalBalance: Double,
    monthIncome: Double,
    monthExpense: Double,
    savingsRate: Int,
    currency: String,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var isBalanceVisible by remember { mutableStateOf(true) }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F382E),
            Color(0xFF062019),
            Color(0xFF0B141E)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradientBrush)
            .padding(20.dp)
    ) {
        Column {
            // Header Row: Label + Eye Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C896))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Total Net Worth",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA7F3D0)
                    )
                }

                IconButton(
                    onClick = { isBalanceVisible = !isBalanceVisible },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Balance",
                        tint = Color(0xFFA7F3D0).copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Balance Text
            Text(
                text = if (isBalanceVisible) {
                    CurrencyFormatter.format(totalBalance, currency)
                } else {
                    "•••••••• $currencySymbol"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row (Income | Expense | Savings Rate)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Income
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ColorIncome.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = ColorIncome,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (isBalanceVisible) CurrencyFormatter.format(monthIncome, currency) else "••••",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorIncome
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Expense
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(ColorExpense.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = ColorExpense,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (isBalanceVisible) CurrencyFormatter.format(monthExpense, currency) else "••••",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorExpense
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Savings rate
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Savings",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "$savingsRate%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF3BA2F)
                    )
                }
            }
        }
    }
}
