package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analytics.FinancialInsight
import com.example.analytics.FinancialInsightsEngine
import com.example.cloud.CloudBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.model.*
import com.example.data.repository.AccountWithBalance
import com.example.data.repository.FinanceRepository
import com.example.data.repository.FinancialDashboardSummary
import com.example.export.ExcelExporter
import com.example.export.PdfExporter
import com.example.sms.SmsInboxScanner
import com.example.util.DateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val preferenceManager = PreferenceManager(application)
    val repository = FinanceRepository(db, preferenceManager)

    val activeProfileId: StateFlow<String> = repository.activeProfileId
    val currentProfile: StateFlow<FinancialProfileEntity?> = repository.currentProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<AccountWithBalance>> = repository.accountsWithBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<DebtEntity>> = repository.debts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installments: StateFlow<List<InstallmentEntity>> = repository.installments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSmsList: StateFlow<List<SmsDetectionEntity>> = repository.pendingSmsDetections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smsHistoryList: StateFlow<List<SmsDetectionEntity>> = repository.smsHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthKey = DateHelper.getCurrentMonthYearKey()
    val currentBudgets: StateFlow<List<BudgetEntity>> = repository.getBudgets(currentMonthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month timestamps
    private val monthRange = DateHelper.getMonthStartEndTimestamps()
    val dashboardSummary: StateFlow<FinancialDashboardSummary> = repository.getDashboardSummary(monthRange.first, monthRange.second)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialDashboardSummary(0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0))

    // Insights
    val financialInsights: StateFlow<List<FinancialInsight>> = combine(
        allTransactions,
        categories,
        currentBudgets,
        currentProfile
    ) { txs, cats, bgts, prof ->
        val currency = prof?.currencyCode ?: "TOMAN"
        val isPersian = prof?.languageCode == "fa"
        FinancialInsightsEngine.generateInsights(txs, cats, bgts, currency, isPersian)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and filter in transactions
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<String?>("ALL")
    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()

    private val _selectedAccountFilter = MutableStateFlow<String?>("ALL")
    val selectedAccountFilter: StateFlow<String?> = _selectedAccountFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _searchQuery,
        _selectedTypeFilter,
        _selectedAccountFilter
    ) { txs, query, typeFilter, accountFilter ->
        txs.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    (tx.notes?.contains(query, ignoreCase = true) == true) ||
                    (tx.tags?.contains(query, ignoreCase = true) == true)
            val matchesType = typeFilter == null || typeFilter == "ALL" || tx.type == typeFilter
            val matchesAccount = accountFilter == null || accountFilter == "ALL" || tx.accountId == accountFilter
            matchesQuery && matchesType && matchesAccount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state messaging
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String?) {
        _selectedTypeFilter.value = type
    }

    fun setAccountFilter(accId: String?) {
        _selectedAccountFilter.value = accId
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // CRUD Transactions
    fun addTransaction(
        accountId: String,
        toAccountId: String? = null,
        categoryId: String,
        type: String,
        amount: Double,
        description: String,
        notes: String? = null,
        tags: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val prof = currentProfile.value
            val tx = TransactionEntity(
                profileId = activeProfileId.value,
                accountId = accountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                type = type,
                amount = amount,
                currency = prof?.currencyCode ?: "TOMAN",
                timestamp = timestamp,
                description = description,
                notes = notes,
                tags = tags
            )
            repository.saveTransaction(tx)
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.saveTransaction(tx.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    // CRUD Accounts
    fun addAccount(
        name: String,
        type: String,
        initialBalance: Double,
        bankName: String? = null,
        maskedCard: String? = null,
        colorHex: String = "#00C896",
        iconName: String = "account_balance",
        notes: String? = null
    ) {
        viewModelScope.launch {
            val prof = currentProfile.value
            val account = AccountEntity(
                profileId = activeProfileId.value,
                name = name,
                type = type,
                initialBalance = initialBalance,
                currency = prof?.currencyCode ?: "TOMAN",
                bankName = bankName,
                maskedCardNumber = maskedCard,
                colorHex = colorHex,
                iconName = iconName,
                notes = notes
            )
            repository.saveAccount(account)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.saveAccount(account.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    // CRUD Categories
    fun addCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                profileId = activeProfileId.value,
                name = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex
            )
            repository.saveCategory(cat)
        }
    }

    fun deleteCategory(category: CategoryEntity, reassignToId: String? = null, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteCategory(category, reassignToId)
            onResult(success)
        }
    }

    // CRUD Budgets
    fun setBudget(categoryId: String, amount: Double) {
        viewModelScope.launch {
            val b = BudgetEntity(
                profileId = activeProfileId.value,
                categoryId = categoryId,
                amount = amount,
                monthYear = currentMonthKey
            )
            repository.saveBudget(b)
        }
    }

    // CRUD Debts
    fun addDebt(personName: String, type: String, totalAmount: Double, dueDate: Long, desc: String?) {
        viewModelScope.launch {
            val debt = DebtEntity(
                profileId = activeProfileId.value,
                personName = personName,
                type = type,
                totalAmount = totalAmount,
                dueDate = dueDate,
                description = desc
            )
            repository.saveDebt(debt)
        }
    }

    fun settleDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.saveDebt(debt.copy(isSettled = true, paidAmount = debt.totalAmount))
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    // CRUD Installments
    fun addInstallment(title: String, totalAmount: Double, downPayment: Double, totalInstallments: Int, installmentAmount: Double, nextPaymentDate: Long) {
        viewModelScope.launch {
            val inst = InstallmentEntity(
                profileId = activeProfileId.value,
                title = title,
                totalAmount = totalAmount,
                downPayment = downPayment,
                totalInstallments = totalInstallments,
                installmentAmount = installmentAmount,
                startDate = System.currentTimeMillis(),
                nextPaymentDate = nextPaymentDate
            )
            repository.saveInstallment(inst)
        }
    }

    fun payInstallment(inst: InstallmentEntity) {
        viewModelScope.launch {
            val newPaid = (inst.paidInstallments + 1).coerceAtMost(inst.totalInstallments)
            // Advance next payment date by 30 days
            val nextDate = inst.nextPaymentDate + (30L * 24 * 60 * 60 * 1000)
            repository.saveInstallment(inst.copy(paidInstallments = newPaid, nextPaymentDate = nextDate))
        }
    }

    // SMS Actions
    fun confirmSmsTransaction(detection: SmsDetectionEntity, accountId: String, categoryId: String) {
        viewModelScope.launch {
            // Add as confirmed transaction
            val tx = TransactionEntity(
                profileId = detection.profileId,
                accountId = accountId,
                categoryId = categoryId,
                type = detection.detectedType,
                amount = detection.detectedAmount,
                currency = detection.detectedCurrency,
                timestamp = detection.detectedTime,
                description = "${detection.detectedBank} — ${detection.detectedType}",
                notes = "Auto-detected from SMS (${detection.sender})",
                source = TransactionSource.SMS.name,
                status = TransactionStatus.CONFIRMED.name,
                smsRefId = detection.id
            )
            repository.saveTransaction(tx)
            repository.updateSmsDetection(detection.copy(status = SmsDetectionStatus.CONFIRMED.name, linkedTransactionId = tx.id))
            showMessage("Transaction added successfully")
        }
    }

    fun ignoreSmsDetection(detection: SmsDetectionEntity) {
        viewModelScope.launch {
            repository.updateSmsDetection(detection.copy(status = SmsDetectionStatus.IGNORED.name))
        }
    }

    fun scanPreviousSms(context: Context, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = SmsInboxScanner.scanInbox(context, activeProfileId.value)
            onResult(count)
        }
    }

    fun clearSmsHistory() {
        viewModelScope.launch {
            repository.clearSmsHistory(activeProfileId.value)
        }
    }

    // Profile & Settings
    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            repository.switchProfile(profileId)
        }
    }

    fun updateProfile(profile: FinancialProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun setSmsDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val prof = currentProfile.value ?: return@launch
            repository.updateProfile(prof.copy(isSmsDetectionEnabled = enabled))
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val prof = currentProfile.value ?: return@launch
            repository.updateProfile(prof.copy(isAutoBackupEnabled = enabled))
        }
    }

    // Google Sign-In & Backup
    fun setGoogleAccount(email: String?) {
        viewModelScope.launch {
            val prof = currentProfile.value ?: return@launch
            repository.updateProfile(prof.copy(googleAccountEmail = email))
        }
    }

    fun backupNow(context: Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val timestamp = CloudBackupManager.saveBackupToCloud(context, db, activeProfileId.value)
                val prof = currentProfile.value
                if (prof != null) {
                    repository.updateProfile(prof.copy(lastBackupTimestamp = timestamp))
                }
                onComplete(true, "Cloud backup created successfully")
            } catch (e: Exception) {
                onComplete(false, "Backup failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun restoreBackup(context: Context, isReplace: Boolean, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = CloudBackupManager.restoreFromCloud(context, db, activeProfileId.value, isReplace)
                if (success) {
                    onComplete(true, "Backup restored successfully")
                } else {
                    onComplete(false, "No cloud backup found for current profile")
                }
            } catch (e: Exception) {
                onComplete(false, "Restore failed: ${e.localizedMessage ?: "Corrupted backup"}")
            }
        }
    }

    fun deleteCloudBackup(context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = CloudBackupManager.deleteCloudBackup(context, activeProfileId.value)
            val prof = currentProfile.value
            if (prof != null) {
                repository.updateProfile(prof.copy(lastBackupTimestamp = 0L))
            }
            onComplete(success)
        }
    }

    fun exportExcel(context: Context, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val prof = currentProfile.value ?: return@launch
                val file = ExcelExporter.generateExcel(
                    context = context,
                    profile = prof,
                    transactions = allTransactions.value,
                    accounts = accounts.value.map { it.account },
                    categories = categories.value,
                    budgets = currentBudgets.value,
                    debts = debts.value,
                    installments = installments.value
                )
                onComplete(file)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }

    fun exportPdf(context: Context, dateRangeLabel: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val prof = currentProfile.value ?: return@launch
                val file = PdfExporter.generatePdf(
                    context = context,
                    profile = prof,
                    transactions = allTransactions.value,
                    categories = categories.value,
                    dateRangeLabel = dateRangeLabel
                )
                onComplete(file)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }

    fun clearAllLocalData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearLocalData()
            onComplete()
        }
    }
}
