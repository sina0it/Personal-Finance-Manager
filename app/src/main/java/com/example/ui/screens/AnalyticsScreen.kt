package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.analytics.FinancialInsight
import com.example.data.model.CategoryEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.TransactionEntity
import com.example.ui.components.CategorySlice
import com.example.ui.components.ComparisonBarChart
import com.example.ui.components.DonutExpenseChart
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter
import java.io.File

@Composable
fun AnalyticsScreen(
    currentProfile: FinancialProfileEntity?,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    insights: List<FinancialInsight>,
    onExportExcel: (onResult: (File?) -> Unit) -> Unit,
    onExportPdf: (dateRange: String, onResult: (File?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency = currentProfile?.currencyCode ?: "TOMAN"
    val catMap = categories.associateBy { it.id }

    var selectedPeriod by remember { mutableStateOf("THIS_MONTH") }
    var isExporting by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    // Filter transactions by period
    val filteredTx = remember(transactions, selectedPeriod) {
        val now = System.currentTimeMillis()
        when (selectedPeriod) {
            "THIS_MONTH" -> {
                val oneMonthAgo = now - 30L * 24 * 60 * 60 * 1000
                transactions.filter { it.timestamp >= oneMonthAgo }
            }
            "LAST_3_MONTHS" -> {
                val threeMonthsAgo = now - 90L * 24 * 60 * 60 * 1000
                transactions.filter { it.timestamp >= threeMonthsAgo }
            }
            else -> transactions
        }
    }

    val totalIncome = filteredTx.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = filteredTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val netCashFlow = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) (((totalIncome - totalExpense) / totalIncome) * 100).toInt().coerceIn(0, 100) else 0

    // Prepare Category Slices for Donut Chart
    val categorySlices = remember(filteredTx, categories) {
        filteredTx.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .map { (catId, amt) ->
                val cat = catMap[catId]
                CategorySlice(
                    name = cat?.name ?: "Other",
                    amount = amt,
                    color = try {
                        Color(android.graphics.Color.parseColor(cat?.colorHex ?: "#00C896"))
                    } catch (e: Exception) {
                        EmeraldPrimary
                    }
                )
            }
    }

    fun shareExportedFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Financial Export"))
        } catch (e: Exception) {
            // Handle error safely
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Financial Analytics",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Insights, spending breakdowns & exports",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Period filter pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "THIS_MONTH" to "30 Days",
                    "LAST_3_MONTHS" to "90 Days",
                    "ALL" to "All Time"
                ).forEach { (pKey, label) ->
                    val isSelected = selectedPeriod == pKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                            .clickable { selectedPeriod = pKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4 Metric cards in a grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(CurrencyFormatter.format(totalIncome, currency), style = MaterialTheme.typography.titleMedium, color = ColorIncome)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(CurrencyFormatter.format(totalExpense, currency), style = MaterialTheme.typography.titleMedium, color = ColorExpense)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Net Cash Flow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        val netColor = if (netCashFlow >= 0) ColorIncome else ColorExpense
                        Text(CurrencyFormatter.format(netCashFlow, currency), style = MaterialTheme.typography.titleMedium, color = netColor)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Savings Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$savingsRate%", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF3BA2F))
                    }
                }
            }
        }

        // Income vs Expense Comparison Bar Chart
        item {
            ComparisonBarChart(
                income = totalIncome,
                expense = totalExpense,
                currency = currency
            )
        }

        // Spending Breakdown Donut Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Expense by Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DonutExpenseChart(
                        slices = categorySlices,
                        currency = currency,
                        totalExpense = totalExpense
                    )
                }
            }
        }

        // Export Actions Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Export & Reports",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate professional financial spreadsheets and printable PDF statements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isExporting = true
                                onExportExcel { file ->
                                    isExporting = false
                                    if (file != null) {
                                        shareExportedFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel (.xlsx)", color = Color.Black)
                        }

                        FilledTonalButton(
                            onClick = {
                                isExporting = true
                                onExportPdf(selectedPeriod) { file ->
                                    isExporting = false
                                    if (file != null) {
                                        shareExportedFile(file, "application/pdf")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF Report")
                        }
                    }
                }
            }
        }
    }
}
