package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analytics.FinancialInsight
import com.example.data.model.CategoryEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.SmsDetectionEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinancialDashboardSummary
import com.example.ui.components.BalanceCard
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.QuickActionsRow
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.util.CurrencyFormatter
import com.example.util.DateHelper

@Composable
fun HomeScreen(
    currentProfile: FinancialProfileEntity?,
    summary: FinancialDashboardSummary,
    insights: List<FinancialInsight>,
    recentTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    pendingSmsList: List<SmsDetectionEntity>,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onOpenSmsReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = currentProfile?.currencyCode ?: "TOMAN"
    val currencySymbol = currentProfile?.currencySymbol ?: "تومان"
    val isPersian = currentProfile?.languageCode == "fa"
    val catMap = categories.associateBy { it.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // Balance Card
        item {
            BalanceCard(
                totalBalance = summary.totalBalance,
                monthIncome = summary.monthIncome,
                monthExpense = summary.monthExpense,
                savingsRate = summary.savingsRatePercent,
                currency = currency,
                currencySymbol = currencySymbol
            )
        }

        // Pending SMS Notification Alert Banner
        if (pendingSmsList.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenSmsReview),
                    colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${pendingSmsList.size} Bank SMS Detected",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to review and add to records",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Actions
        item {
            QuickActionsRow(
                onAddIncome = onAddIncome,
                onAddExpense = onAddExpense,
                onAddTransfer = onAddTransfer,
                onOpenAnalytics = onOpenAnalytics
            )
        }

        // Live Financial Insight
        if (insights.isNotEmpty()) {
            item {
                val insight = insights.first()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (insight.isPositive)
                            EmeraldPrimary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (insight.isPositive) EmeraldPrimary.copy(alpha = 0.2f)
                                    else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (insight.isPositive) Icons.Default.AutoGraph else Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = if (insight.isPositive) EmeraldPrimary else Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = insight.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = insight.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onViewAllTransactions) {
                    Text("View All", color = EmeraldPrimary)
                }
            }
        }

        // Recent Transactions List
        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No transactions recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentTransactions) { tx ->
                val cat = catMap[tx.categoryId]
                val isIncome = tx.type == "INCOME"
                val isExpense = tx.type == "EXPENSE"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryAvatar(
                            iconName = cat?.iconName ?: "category",
                            colorHex = cat?.colorHex ?: "#00C896",
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${cat?.name ?: "General"} • ${DateHelper.formatDate(tx.timestamp, isPersian)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val prefix = if (isIncome) "+ " else if (isExpense) "- " else ""
                        val amtColor = if (isIncome) ColorIncome else if (isExpense) ColorExpense else EmeraldPrimary

                        Text(
                            text = prefix + CurrencyFormatter.format(tx.amount, currency),
                            style = MaterialTheme.typography.titleSmall,
                            color = amtColor
                        )
                    }
                }
            }
        }
    }
}
