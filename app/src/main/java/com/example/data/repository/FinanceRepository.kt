package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.RoundingMode

data class AccountWithBalance(
    val account: AccountEntity,
    val currentBalance: Double
)

data class FinancialDashboardSummary(
    val totalBalance: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val netCashFlow: Double,
    val savingsRatePercent: Int,
    val totalBudgetLimit: Double,
    val totalBudgetSpent: Double,
    val upcomingPaymentsCount: Int
)

class FinanceRepository(
    private val database: AppDatabase,
    val preferenceManager: PreferenceManager
) {
    val activeProfileId: StateFlow<String> = preferenceManager.activeProfileId

    // Current profile
    val currentProfile: Flow<FinancialProfileEntity?> = activeProfileId.flatMapLatest { profileId ->
        database.financialProfileDao().getProfileFlow(profileId)
    }

    // Active accounts with calculated balances
    val accountsWithBalances: Flow<List<AccountWithBalance>> = activeProfileId.flatMapLatest { profileId ->
        combine(
            database.accountDao().getActiveAccounts(profileId),
            database.transactionDao().getAllTransactions(profileId)
        ) { accounts, transactions ->
            accounts.map { account ->
                var bal = BigDecimal.valueOf(account.initialBalance)
                for (tx in transactions) {
                    val amt = BigDecimal.valueOf(tx.amount)
                    when (tx.type) {
                        TransactionType.INCOME.name -> {
                            if (tx.accountId == account.id) bal = bal.add(amt)
                        }
                        TransactionType.EXPENSE.name -> {
                            if (tx.accountId == account.id) bal = bal.subtract(amt)
                        }
                        TransactionType.TRANSFER.name -> {
                            if (tx.accountId == account.id) bal = bal.subtract(amt)
                            if (tx.toAccountId == account.id) bal = bal.add(amt)
                        }
                    }
                }
                AccountWithBalance(account, bal.setScale(2, RoundingMode.HALF_UP).toDouble())
            }
        }
    }

    val allTransactions: Flow<List<TransactionEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.transactionDao().getAllTransactions(profileId)
    }

    val recentTransactions: Flow<List<TransactionEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.transactionDao().getRecentTransactions(profileId, 10)
    }

    val categories: Flow<List<CategoryEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.categoryDao().getCategories(profileId)
    }

    val debts: Flow<List<DebtEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.debtDao().getDebts(profileId)
    }

    val installments: Flow<List<InstallmentEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.installmentDao().getInstallments(profileId)
    }

    val recurringTransactions: Flow<List<RecurringTransactionEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.recurringTransactionDao().getRecurringTransactions(profileId)
    }

    val pendingSmsDetections: Flow<List<SmsDetectionEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.smsDetectionDao().getPendingDetections(profileId)
    }

    val smsHistory: Flow<List<SmsDetectionEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.smsDetectionDao().getDetectionHistory(profileId)
    }

    fun getBudgets(monthYear: String): Flow<List<BudgetEntity>> = activeProfileId.flatMapLatest { profileId ->
        database.budgetDao().getBudgetsForMonth(profileId, monthYear)
    }

    // Dashboard calculations
    fun getDashboardSummary(monthStartTime: Long, monthEndTime: Long): Flow<FinancialDashboardSummary> {
        return combine(
            accountsWithBalances,
            activeProfileId.flatMapLatest { database.transactionDao().getTransactionsInRange(it, monthStartTime, monthEndTime) },
            debts,
            installments
        ) { accounts, monthTx, debtsList, installmentsList ->
            val totalBal = accounts.fold(BigDecimal.ZERO) { acc, item -> acc.add(BigDecimal.valueOf(item.currentBalance)) }.toDouble()

            var income = BigDecimal.ZERO
            var expense = BigDecimal.ZERO
            for (tx in monthTx) {
                val amt = BigDecimal.valueOf(tx.amount)
                if (tx.type == TransactionType.INCOME.name) {
                    income = income.add(amt)
                } else if (tx.type == TransactionType.EXPENSE.name) {
                    expense = expense.add(amt)
                }
            }

            val incDouble = income.toDouble()
            val expDouble = expense.toDouble()
            val net = incDouble - expDouble
            val savingsRate = if (incDouble > 0) {
                (((incDouble - expDouble) / incDouble) * 100).toInt().coerceIn(0, 100)
            } else 0

            val upcomingDebts = debtsList.count { !it.isSettled && it.dueDate > System.currentTimeMillis() }
            val upcomingInstallments = installmentsList.count { it.paidInstallments < it.totalInstallments }

            FinancialDashboardSummary(
                totalBalance = totalBal,
                monthIncome = incDouble,
                monthExpense = expDouble,
                netCashFlow = net,
                savingsRatePercent = savingsRate,
                totalBudgetLimit = 0.0,
                totalBudgetSpent = expDouble,
                upcomingPaymentsCount = upcomingDebts + upcomingInstallments
            )
        }
    }

    suspend fun saveTransaction(tx: TransactionEntity) {
        database.transactionDao().insertOrUpdate(tx)
    }

    suspend fun deleteTransaction(tx: TransactionEntity) {
        database.transactionDao().delete(tx)
    }

    suspend fun saveAccount(account: AccountEntity) {
        database.accountDao().insertOrUpdate(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        database.accountDao().delete(account)
    }

    suspend fun saveCategory(category: CategoryEntity) {
        database.categoryDao().insertOrUpdate(category)
    }

    suspend fun deleteCategory(category: CategoryEntity, reassignToId: String? = null): Boolean {
        val count = database.categoryDao().getTransactionCountForCategory(category.id)
        if (count > 0) {
            if (reassignToId != null) {
                database.categoryDao().reassignTransactionsCategory(category.id, reassignToId)
            } else {
                return false
            }
        }
        database.categoryDao().delete(category)
        return true
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        database.budgetDao().insertOrUpdate(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        database.budgetDao().delete(budget)
    }

    suspend fun saveDebt(debt: DebtEntity) {
        database.debtDao().insertOrUpdate(debt)
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        database.debtDao().delete(debt)
    }

    suspend fun saveInstallment(installment: InstallmentEntity) {
        database.installmentDao().insertOrUpdate(installment)
    }

    suspend fun deleteInstallment(installment: InstallmentEntity) {
        database.installmentDao().delete(installment)
    }

    suspend fun saveRecurring(item: RecurringTransactionEntity) {
        database.recurringTransactionDao().insertOrUpdate(item)
    }

    suspend fun deleteRecurring(item: RecurringTransactionEntity) {
        database.recurringTransactionDao().delete(item)
    }

    suspend fun insertSmsDetection(detection: SmsDetectionEntity) {
        database.smsDetectionDao().insertIfNotExists(detection)
    }

    suspend fun updateSmsDetection(detection: SmsDetectionEntity) {
        database.smsDetectionDao().update(detection)
    }

    suspend fun clearSmsHistory(profileId: String) {
        database.smsDetectionDao().clearHistory(profileId)
    }

    suspend fun updateProfile(profile: FinancialProfileEntity) {
        database.financialProfileDao().insertOrUpdate(profile)
    }

    suspend fun switchProfile(profileId: String) {
        preferenceManager.setActiveProfileId(profileId)
    }

    suspend fun clearLocalData() {
        val profileId = activeProfileId.value
        database.transactionDao().deleteAllForProfile(profileId)
        database.accountDao().deleteAllForProfile(profileId)
        database.smsDetectionDao().clearHistory(profileId)
    }
}
