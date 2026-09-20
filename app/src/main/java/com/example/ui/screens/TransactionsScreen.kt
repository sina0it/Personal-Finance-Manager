package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.AccountWithBalance
import com.example.ui.components.CategoryAvatar
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter
import com.example.util.DateHelper

@Composable
fun TransactionsScreen(
    currentProfile: FinancialProfileEntity?,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountWithBalance>,
    searchQuery: String,
    selectedTypeFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (String?) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onOpenAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = currentProfile?.currencyCode ?: "TOMAN"
    val isPersian = currentProfile?.languageCode == "fa"
    val catMap = categories.associateBy { it.id }
    val accMap = accounts.associateBy { it.account.id }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddTransaction,
                containerColor = EmeraldPrimary,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search transactions, notes, tags...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (All | Expense | Income | Transfer)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "ALL" to "All",
                    "EXPENSE" to "Expenses",
                    "INCOME" to "Income",
                    "TRANSFER" to "Transfers"
                )
                items(filters) { (typeKey, label) ->
                    val isSelected = (selectedTypeFilter ?: "ALL") == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTypeFilterChange(typeKey) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Group transactions by date string
            val groupedTransactions = remember(transactions, isPersian) {
                transactions.groupBy { DateHelper.formatDate(it.timestamp, isPersian) }
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedTransactions.forEach { (dateStr, dayTxList) ->
                        item {
                            // Date header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val dayExpense = dayTxList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                if (dayExpense > 0) {
                                    Text(
                                        text = "Spent: -${CurrencyFormatter.format(dayExpense, currency)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorExpense
                                    )
                                }
                            }
                        }

                        items(dayTxList) { tx ->
                            val cat = catMap[tx.categoryId]
                            val acc = accMap[tx.accountId]?.account
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
                                        val subtitle = buildString {
                                            append(cat?.name ?: "General")
                                            if (acc != null) append(" • ${acc.name}")
                                            if (tx.source == "SMS") append(" • 📱 SMS")
                                        }
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val prefix = if (isIncome) "+ " else if (isExpense) "- " else ""
                                        val amtColor = if (isIncome) ColorIncome else if (isExpense) ColorExpense else EmeraldPrimary

                                        Text(
                                            text = prefix + CurrencyFormatter.format(tx.amount, currency),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = amtColor
                                        )

                                        IconButton(
                                            onClick = { onDeleteTransaction(tx) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
