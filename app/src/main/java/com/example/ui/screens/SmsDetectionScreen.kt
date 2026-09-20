package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.SmsDetectionEntity
import com.example.data.repository.AccountWithBalance
import com.example.ui.components.CategoryAvatar
import com.example.ui.theme.ColorExpense
import com.example.ui.theme.ColorIncome
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.util.CurrencyFormatter
import com.example.util.DateHelper

@Composable
fun SmsDetectionScreen(
    currentProfile: FinancialProfileEntity?,
    pendingList: List<SmsDetectionEntity>,
    historyList: List<SmsDetectionEntity>,
    accounts: List<AccountWithBalance>,
    categories: List<CategoryEntity>,
    onToggleDetection: (Boolean) -> Unit,
    onConfirmSms: (detection: SmsDetectionEntity, accountId: String, categoryId: String) -> Unit,
    onIgnoreSms: (SmsDetectionEntity) -> Unit,
    onScanInbox: (onCount: (Int) -> Unit) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEnabled = currentProfile?.isSmsDetectionEnabled == true
    val currency = currentProfile?.currencyCode ?: "TOMAN"
    val isPersian = currentProfile?.languageCode == "fa"

    var isScanning by remember { mutableStateOf(false) }
    var scanResultMessage by remember { mutableStateOf<String?>(null) }

    // Dialog state for confirming an SMS
    var confirmingDetection by remember { mutableStateOf<SmsDetectionEntity?>(null) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.account?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleDetection(true)
            scanResultMessage = "SMS banking permission granted."
        } else {
            onToggleDetection(false)
            scanResultMessage = "SMS permission was not granted. Detection remains disabled."
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bank SMS Detection",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
        ) {
            // Main Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Detect Bank SMS",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Listen for bank transaction alerts and present them for one-tap review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                                } else {
                                    onToggleDetection(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                }
            }

            // Privacy Guarantee Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Zero-Cloud Privacy Guarantee",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SMS processing runs 100% locally on your device. OTPs, passwords, personal texts, and verification codes are never read, extracted, or stored.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Inbox Scanner Action
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Scan Existing Inbox",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scan past bank SMS from your messages inbox to discover unrecorded transactions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val readSmsLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            if (granted) {
                                isScanning = true
                                onScanInbox { count ->
                                    isScanning = false
                                    scanResultMessage = "Scan completed: $count bank transactions found!"
                                }
                            } else {
                                isScanning = false
                                scanResultMessage = "SMS read permission was not granted. Inbox scanning canceled."
                            }
                        }

                        Button(
                            onClick = {
                                readSmsLauncher.launch(Manifest.permission.READ_SMS)
                            },
                            enabled = !isScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning Inbox...", color = Color.Black)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Inbox Now", color = Color.Black)
                            }
                        }

                        if (scanResultMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = scanResultMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            // Pending Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pending Review (${pendingList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (pendingList.isEmpty()) {
                item {
                    Text(
                        text = "No pending SMS transactions waiting for review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(pendingList) { item ->
                    val isIncome = item.detectedType == "INCOME"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.detectedBank,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.detectedType} • ${DateHelper.formatDateTime(item.detectedTime, isPersian)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = CurrencyFormatter.format(item.detectedAmount, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isIncome) ColorIncome else ColorExpense
                                )
                            }

                            if (!item.maskedCard.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Card: •••• ${item.maskedCard}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onIgnoreSms(item) }) {
                                    Text("Ignore", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { confirmingDetection = item },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Text("Confirm Record", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Detection History
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detection History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (historyList.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) {
                            Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (historyList.isEmpty()) {
                item {
                    Text(
                        text = "No detection history yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(historyList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${item.detectedBank} (${item.detectedType})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Status: ${item.status} • ${DateHelper.formatDate(item.detectedTime, isPersian)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.format(item.detectedAmount, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to confirm SMS and select target Account & Category
    confirmingDetection?.let { detection ->
        AlertDialog(
            onDismissRequest = { confirmingDetection = null },
            title = { Text("Confirm SMS Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Amount: ${CurrencyFormatter.format(detection.detectedAmount, currency)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (detection.detectedType == "INCOME") ColorIncome else ColorExpense
                    )
                    Text("Bank: ${detection.detectedBank} • Type: ${detection.detectedType}")

                    Text("Select Account:", style = MaterialTheme.typography.labelMedium)
                    accounts.forEach { accItem ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountId = accItem.account.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedAccountId == accItem.account.id,
                                onClick = { selectedAccountId = accItem.account.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(accItem.account.name)
                        }
                    }

                    Text("Select Category:", style = MaterialTheme.typography.labelMedium)
                    categories.take(5).forEach { catItem ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryId = catItem.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == catItem.id,
                                onClick = { selectedCategoryId = catItem.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(catItem.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmSms(detection, selectedAccountId.ifBlank { accounts.first().account.id }, selectedCategoryId.ifBlank { categories.first().id })
                        confirmingDetection = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save to Records", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDetection = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
