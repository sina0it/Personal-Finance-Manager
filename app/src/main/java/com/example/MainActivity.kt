package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.SinaBottomBar
import com.example.ui.components.SinaNavScreen
import com.example.ui.components.SinaTopBar
import com.example.ui.screens.*
import com.example.ui.theme.SinaFinanceTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateExtra = intent.getStringExtra("navigate_to")

        setContent {
            SinaFinanceTheme {
                val hasCompletedOnboarding by viewModel.preferenceManager.hasCompletedOnboarding.collectAsStateWithLifecycle()
                val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
                val isPersian = currentProfile?.languageCode == "fa"
                val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    if (!hasCompletedOnboarding) {
                        OnboardingScreen(
                            onFinish = { country, language, currency ->
                                val profileId = if (country == "IR") "IRAN_TOMAN" else "US_USD"
                                viewModel.switchProfile(profileId)
                                viewModel.preferenceManager.setOnboardingCompleted(true)
                            }
                        )
                    } else {
                        MainContent(viewModel = viewModel, initialRoute = if (navigateExtra == "sms_pending") "sms_detection" else "home")
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(
    viewModel: FinanceViewModel,
    initialRoute: String = "home"
) {
    var currentRoute by remember { mutableStateOf(initialRoute) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var addTxInitialType by remember { mutableStateOf("EXPENSE") }

    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val insights by viewModel.financialInsights.collectAsStateWithLifecycle()
    val pendingSmsList by viewModel.pendingSmsList.collectAsStateWithLifecycle()
    val smsHistoryList by viewModel.smsHistoryList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()

    val currency = currentProfile?.currencyCode ?: "TOMAN"

    var showProfileSwitchDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentRoute != "sms_detection") {
                SinaTopBar(
                    currentProfile = currentProfile,
                    pendingSmsCount = pendingSmsList.size,
                    onProfileClick = { showProfileSwitchDialog = true },
                    onPendingSmsClick = { currentRoute = "sms_detection" }
                )
            }
        },
        bottomBar = {
            if (currentRoute != "sms_detection") {
                SinaBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { currentRoute = it }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentRoute) {
                SinaNavScreen.HOME.route -> {
                    HomeScreen(
                        currentProfile = currentProfile,
                        summary = summary,
                        insights = insights,
                        recentTransactions = recentTransactions,
                        categories = categories,
                        pendingSmsList = pendingSmsList,
                        onAddIncome = {
                            addTxInitialType = "INCOME"
                            showAddTxDialog = true
                        },
                        onAddExpense = {
                            addTxInitialType = "EXPENSE"
                            showAddTxDialog = true
                        },
                        onAddTransfer = {
                            addTxInitialType = "TRANSFER"
                            showAddTxDialog = true
                        },
                        onOpenAnalytics = { currentRoute = SinaNavScreen.ANALYTICS.route },
                        onViewAllTransactions = { currentRoute = SinaNavScreen.TRANSACTIONS.route },
                        onOpenSmsReview = { currentRoute = "sms_detection" }
                    )
                }

                SinaNavScreen.ACCOUNTS.route -> {
                    AccountsScreen(
                        currentProfile = currentProfile,
                        accounts = accounts,
                        onAddAccount = { name, type, bal, bank, card ->
                            viewModel.addAccount(name, type, bal, bank, card)
                        },
                        onDeleteAccount = { viewModel.deleteAccount(it) }
                    )
                }

                SinaNavScreen.ANALYTICS.route -> {
                    AnalyticsScreen(
                        currentProfile = currentProfile,
                        transactions = allTransactions,
                        categories = categories,
                        insights = insights,
                        onExportExcel = { onResult ->
                            viewModel.exportExcel(viewModel.getApplication(), onResult)
                        },
                        onExportPdf = { period, onResult ->
                            viewModel.exportPdf(viewModel.getApplication(), period, onResult)
                        }
                    )
                }

                SinaNavScreen.TRANSACTIONS.route -> {
                    TransactionsScreen(
                        currentProfile = currentProfile,
                        transactions = filteredTransactions,
                        categories = categories,
                        accounts = accounts,
                        searchQuery = searchQuery,
                        selectedTypeFilter = selectedTypeFilter,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onTypeFilterChange = { viewModel.setTypeFilter(it) },
                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                        onOpenAddTransaction = {
                            addTxInitialType = "EXPENSE"
                            showAddTxDialog = true
                        }
                    )
                }

                SinaNavScreen.SETTINGS.route -> {
                    SettingsScreen(
                        currentProfile = currentProfile,
                        onSwitchProfile = { targetId ->
                            viewModel.switchProfile(targetId)
                        },
                        onOpenSmsSettings = { currentRoute = "sms_detection" },
                        onBackupNow = { onResult ->
                            viewModel.backupNow(viewModel.getApplication(), onResult)
                        },
                        onRestoreBackup = { isReplace, onResult ->
                            viewModel.restoreBackup(viewModel.getApplication(), isReplace, onResult)
                        },
                        onDeleteCloudBackup = { onResult ->
                            viewModel.deleteCloudBackup(viewModel.getApplication(), onResult)
                        },
                        onClearAllData = {
                            viewModel.clearAllLocalData {}
                        }
                    )
                }

                "sms_detection" -> {
                    SmsDetectionScreen(
                        currentProfile = currentProfile,
                        pendingList = pendingSmsList,
                        historyList = smsHistoryList,
                        accounts = accounts,
                        categories = categories,
                        onToggleDetection = { viewModel.setSmsDetectionEnabled(it) },
                        onConfirmSms = { detection, accId, catId ->
                            viewModel.confirmSmsTransaction(detection, accId, catId)
                        },
                        onIgnoreSms = { viewModel.ignoreSmsDetection(it) },
                        onScanInbox = { onCount ->
                            viewModel.scanPreviousSms(viewModel.getApplication(), onCount)
                        },
                        onClearHistory = { viewModel.clearSmsHistory() },
                        onBack = { currentRoute = SinaNavScreen.HOME.route }
                    )
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddTxDialog) {
        AddTransactionDialog(
            initialType = addTxInitialType,
            accounts = accounts,
            categories = categories,
            currency = currency,
            onDismiss = { showAddTxDialog = false },
            onConfirm = { accountId, toAccountId, categoryId, type, amount, desc, notes ->
                viewModel.addTransaction(
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    type = type,
                    amount = amount,
                    description = desc,
                    notes = notes
                )
            }
        )
    }

    // Profile Switch Confirmation from Top Bar
    if (showProfileSwitchDialog) {
        val targetProfileId = if (currentProfile?.profileId == "IRAN_TOMAN") "US_USD" else "IRAN_TOMAN"
        val targetLabel = if (targetProfileId == "IRAN_TOMAN") "Iran 🇮🇷 (تومان)" else "United States 🇺🇸 ($ USD)"

        AlertDialog(
            onDismissRequest = { showProfileSwitchDialog = false },
            title = { Text("Switch Financial Profile") },
            text = {
                Text(
                    "Switch to $targetLabel?\n\n" +
                            "Notice: Existing amounts and accounts are kept separate and will NEVER be silently converted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.switchProfile(targetProfileId)
                        showProfileSwitchDialog = false
                    }
                ) {
                    Text("Switch Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileSwitchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
