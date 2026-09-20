package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinancialProfileEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.util.DateHelper

@Composable
fun SettingsScreen(
    currentProfile: FinancialProfileEntity?,
    onSwitchProfile: (String) -> Unit,
    onOpenSmsSettings: () -> Unit,
    onBackupNow: (onResult: (Boolean, String) -> Unit) -> Unit,
    onRestoreBackup: (isReplace: Boolean, onResult: (Boolean, String) -> Unit) -> Unit,
    onDeleteCloudBackup: (onResult: (Boolean) -> Unit) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showProfileSwitchDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    fun sendEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Sina Finance Feedback")
            }
            context.startActivity(intent)
        } catch (e: Exception) {}
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
                text = "Settings & Security",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Country, cloud sync, privacy & developer credits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Active Country & Currency Card
        item {
            val isIran = currentProfile?.countryCode == "IR"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showProfileSwitchDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isIran) "🇮🇷" else "🇺🇸", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isIran) "Iran • Persian (تومان)" else "United States • English ($ USD)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to switch country profile safely",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Bank SMS Detection Link
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenSmsSettings),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GoldAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bank SMS Detection",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentProfile?.isSmsDetectionEnabled == true) "Active • Listening for bank transactions" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentProfile?.isSmsDetectionEnabled == true) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Google Drive Cloud Backup & Restore Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Cloud Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val backupDate = if (currentProfile != null && currentProfile.lastBackupTimestamp > 0) {
                                DateHelper.formatDateTime(currentProfile.lastBackupTimestamp, currentProfile.languageCode == "fa")
                            } else {
                                "Never"
                            }
                            Text(
                                text = "Last Backup: $backupDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isOperating = true
                                onBackupNow { success, msg ->
                                    isOperating = false
                                    statusMessage = msg
                                }
                            },
                            enabled = !isOperating,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Back Up Now", color = Color.Black)
                        }

                        FilledTonalButton(
                            onClick = { showRestoreDialog = true },
                            enabled = !isOperating,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore")
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }

        // Developer & Contact Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "About Developer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sina Naderi",
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sina Finance — Personal Finance Manager v1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact links
                    ContactRow(
                        icon = Icons.Default.Email,
                        label = "sinanaderi203@gmail.com",
                        onClick = { sendEmail("sinanaderi203@gmail.com") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ContactRow(
                        icon = Icons.Default.CameraAlt,
                        label = "Instagram: @sina__it",
                        onClick = { openUrl("https://instagram.com/sina__it") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ContactRow(
                        icon = Icons.Default.Send,
                        label = "Telegram: @sina_6",
                        onClick = { openUrl("https://t.me/sina_6") }
                    )
                }
            }
        }

        // Danger Zone: Clear Data
        item {
            OutlinedButton(
                onClick = { showClearDataDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Local Financial Data", color = Color(0xFFEF4444))
            }
        }
    }

    // Profile Switch Confirmation Dialog
    if (showProfileSwitchDialog) {
        val targetProfileId = if (currentProfile?.profileId == "IRAN_TOMAN") "US_USD" else "IRAN_TOMAN"
        val targetLabel = if (targetProfileId == "IRAN_TOMAN") "Iran 🇮🇷 (تومان)" else "United States 🇺🇸 ($ USD)"

        AlertDialog(
            onDismissRequest = { showProfileSwitchDialog = false },
            title = { Text("Switch Financial Profile") },
            text = {
                Text(
                    "Switch to $targetLabel?\n\n" +
                            "Notice: Sina Finance maintains distinct financial profiles for Iran and the United States. " +
                            "Your existing records, accounts, and amounts will NOT be silently converted or lost."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSwitchProfile(targetProfileId)
                        showProfileSwitchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Switch Profile", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileSwitchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Options Dialog (Replace vs Merge)
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Cloud Backup") },
            text = {
                Text("Restoring will read your cloud backup file. How would you like to restore?\n\n" +
                        "• Merge: Keep current records and import non-duplicate transactions.\n" +
                        "• Replace: Overwrite current records with the backup state.")
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showRestoreDialog = false
                        isOperating = true
                        onRestoreBackup(false) { success, msg ->
                            isOperating = false
                            statusMessage = msg
                        }
                    }) {
                        Text("Merge")
                    }
                    Button(
                        onClick = {
                            showRestoreDialog = false
                            isOperating = true
                            onRestoreBackup(true) { success, msg ->
                                isOperating = false
                                statusMessage = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Replace", color = Color.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Delete All Data?") },
            text = {
                Text("Are you sure you want to delete all transactions, accounts, and detection history for the current profile? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataDialog = false
                        statusMessage = "All local data erased."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
