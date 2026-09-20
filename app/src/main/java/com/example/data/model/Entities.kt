package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

enum class AccountType {
    CASH, BANK, CARD, SAVINGS, CREDIT
}

enum class TransactionSource {
    MANUAL, SMS, IMPORTED
}

enum class TransactionStatus {
    CONFIRMED, PENDING
}

enum class DebtType {
    I_OWE, OWED_TO_ME
}

enum class Frequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

enum class SmsDetectionStatus {
    PENDING, CONFIRMED, IGNORED
}

@Entity(tableName = "financial_profiles")
data class FinancialProfileEntity(
    @PrimaryKey val profileId: String, // "IRAN_TOMAN" or "US_USD"
    val countryCode: String, // "IR" or "US"
    val languageCode: String, // "fa" or "en"
    val currencyCode: String, // "TOMAN" or "USD"
    val currencySymbol: String, // "تومان" or "$"
    val userName: String = "Sina",
    val googleAccountEmail: String? = null,
    val isSmsDetectionEnabled: Boolean = false,
    val isAutoBackupEnabled: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val isAppLockEnabled: Boolean = false,
    val pinHash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val name: String,
    val type: String, // AccountType name
    val initialBalance: Double,
    val currency: String,
    val bankName: String? = null,
    val maskedCardNumber: String? = null,
    val colorHex: String = "#00C896",
    val iconName: String = "account_balance",
    val notes: String? = null,
    val isArchived: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val accountId: String,
    val toAccountId: String? = null, // for TRANSFER
    val categoryId: String,
    val type: String, // TransactionType name
    val amount: Double,
    val currency: String,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val notes: String? = null,
    val tags: String? = null,
    val attachmentUri: String? = null,
    val source: String = TransactionSource.MANUAL.name,
    val status: String = TransactionStatus.CONFIRMED.name,
    val smsRefId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val name: String,
    val type: String, // TransactionType name (INCOME or EXPENSE)
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val categoryId: String,
    val amount: Double,
    val period: String = "MONTHLY",
    val monthYear: String // e.g. "2026-09"
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val accountId: String,
    val categoryId: String,
    val type: String,
    val amount: Double,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val nextDueDate: Long,
    val description: String,
    val isPaused: Boolean = false
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val personName: String,
    val type: String, // I_OWE or OWED_TO_ME
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: Long,
    val description: String? = null,
    val isSettled: Boolean = false
)

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val title: String,
    val totalAmount: Double,
    val downPayment: Double = 0.0,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val installmentAmount: Double,
    val startDate: Long,
    val intervalMonths: Int = 1,
    val nextPaymentDate: Long
)

@Entity(tableName = "sms_detections")
data class SmsDetectionEntity(
    @PrimaryKey val id: String, // SHA-256 hash of sender + timestamp + amount
    val profileId: String,
    val sender: String,
    val snippet: String, // Cleaned non-sensitive snippet
    val detectedBank: String,
    val detectedType: String,
    val detectedAmount: Double,
    val detectedCurrency: String,
    val detectedTime: Long,
    val maskedCard: String? = null,
    val referenceNumber: String? = null,
    val balanceAfter: Double? = null,
    val confidence: Int,
    val status: String = SmsDetectionStatus.PENDING.name,
    val linkedTransactionId: String? = null
)
