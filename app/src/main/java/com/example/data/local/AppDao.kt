package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialProfileDao {
    @Query("SELECT * FROM financial_profiles WHERE profileId = :profileId LIMIT 1")
    fun getProfileFlow(profileId: String): Flow<FinancialProfileEntity?>

    @Query("SELECT * FROM financial_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfile(profileId: String): FinancialProfileEntity?

    @Query("SELECT * FROM financial_profiles")
    fun getAllProfiles(): Flow<List<FinancialProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: FinancialProfileEntity)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE profileId = :profileId AND isArchived = 0 ORDER BY name ASC")
    fun getActiveAccounts(profileId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE profileId = :profileId ORDER BY isArchived ASC, name ASC")
    fun getAllAccounts(profileId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getAllTransactions(profileId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsInRange(profileId: String, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(profileId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY name ASC")
    fun getCategories(profileId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND type = :type ORDER BY name ASC")
    fun getCategoriesByType(profileId: String, type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountForCategory(categoryId: String): Int

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignTransactionsCategory(oldCategoryId: String, newCategoryId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND monthYear = :monthYear")
    fun getBudgetsForMonth(profileId: String, monthYear: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE profileId = :profileId ORDER BY nextDueDate ASC")
    fun getRecurringTransactions(profileId: String): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: RecurringTransactionEntity)

    @Delete
    suspend fun delete(item: RecurringTransactionEntity)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE profileId = :profileId ORDER BY isSettled ASC, dueDate ASC")
    fun getDebts(profileId: String): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE profileId = :profileId ORDER BY nextPaymentDate ASC")
    fun getInstallments(profileId: String): Flow<List<InstallmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(installment: InstallmentEntity)

    @Delete
    suspend fun delete(installment: InstallmentEntity)
}

@Dao
interface SmsDetectionDao {
    @Query("SELECT * FROM sms_detections WHERE profileId = :profileId AND status = 'PENDING' ORDER BY detectedTime DESC")
    fun getPendingDetections(profileId: String): Flow<List<SmsDetectionEntity>>

    @Query("SELECT * FROM sms_detections WHERE profileId = :profileId ORDER BY detectedTime DESC LIMIT 100")
    fun getDetectionHistory(profileId: String): Flow<List<SmsDetectionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(detection: SmsDetectionEntity): Long

    @Update
    suspend fun update(detection: SmsDetectionEntity)

    @Query("DELETE FROM sms_detections WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: String)
}
