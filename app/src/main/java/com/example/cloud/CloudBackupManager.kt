package com.example.cloud

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CloudBackupInfo(
    val lastBackupDate: String,
    val sizeString: String,
    val isAutoBackupEnabled: Boolean,
    val googleAccountEmail: String?
)

object CloudBackupManager {

    private const val BACKUP_VERSION = 1
    private const val APP_VERSION = "1.0.0"

    suspend fun createBackupPayload(db: AppDatabase, profileId: String): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("backup_version", BACKUP_VERSION)
        root.put("app_version", APP_VERSION)
        root.put("created_at", System.currentTimeMillis())

        val profile = db.financialProfileDao().getProfile(profileId)
        val profileObj = JSONObject().apply {
            put("profileId", profile?.profileId ?: profileId)
            put("countryCode", profile?.countryCode ?: "IR")
            put("languageCode", profile?.languageCode ?: "fa")
            put("currencyCode", profile?.currencyCode ?: "TOMAN")
            put("currencySymbol", profile?.currencySymbol ?: "تومان")
            put("userName", profile?.userName ?: "Sina")
        }
        root.put("financial_profile", profileObj)

        // Accounts
        val accounts = db.accountDao().getAllAccounts(profileId).first()
        val accArr = JSONArray()
        for (a in accounts) {
            accArr.put(JSONObject().apply {
                put("id", a.id)
                put("profileId", a.profileId)
                put("name", a.name)
                put("type", a.type)
                put("initialBalance", a.initialBalance)
                put("currency", a.currency)
                put("bankName", a.bankName ?: "")
                put("maskedCardNumber", a.maskedCardNumber ?: "")
                put("colorHex", a.colorHex)
                put("iconName", a.iconName)
                put("notes", a.notes ?: "")
                put("isArchived", a.isArchived)
            })
        }
        root.put("accounts", accArr)

        // Transactions
        val transactions = db.transactionDao().getAllTransactions(profileId).first()
        val txArr = JSONArray()
        for (t in transactions) {
            txArr.put(JSONObject().apply {
                put("id", t.id)
                put("profileId", t.profileId)
                put("accountId", t.accountId)
                put("toAccountId", t.toAccountId ?: "")
                put("categoryId", t.categoryId)
                put("type", t.type)
                put("amount", t.amount)
                put("currency", t.currency)
                put("timestamp", t.timestamp)
                put("description", t.description)
                put("notes", t.notes ?: "")
                put("tags", t.tags ?: "")
                put("source", t.source)
                put("status", t.status)
                put("smsRefId", t.smsRefId ?: "")
            })
        }
        root.put("transactions", txArr)

        // Categories
        val categories = db.categoryDao().getCategories(profileId).first()
        val catArr = JSONArray()
        for (c in categories) {
            catArr.put(JSONObject().apply {
                put("id", c.id)
                put("profileId", c.profileId)
                put("name", c.name)
                put("type", c.type)
                put("iconName", c.iconName)
                put("colorHex", c.colorHex)
                put("isDefault", c.isDefault)
            })
        }
        root.put("categories", catArr)

        // Debts
        val debts = db.debtDao().getDebts(profileId).first()
        val debtArr = JSONArray()
        for (d in debts) {
            debtArr.put(JSONObject().apply {
                put("id", d.id)
                put("profileId", d.profileId)
                put("personName", d.personName)
                put("type", d.type)
                put("totalAmount", d.totalAmount)
                put("paidAmount", d.paidAmount)
                put("dueDate", d.dueDate)
                put("description", d.description ?: "")
                put("isSettled", d.isSettled)
            })
        }
        root.put("debts", debtArr)

        // Installments
        val installments = db.installmentDao().getInstallments(profileId).first()
        val instArr = JSONArray()
        for (i in installments) {
            instArr.put(JSONObject().apply {
                put("id", i.id)
                put("profileId", i.profileId)
                put("title", i.title)
                put("totalAmount", i.totalAmount)
                put("downPayment", i.downPayment)
                put("totalInstallments", i.totalInstallments)
                put("paidInstallments", i.paidInstallments)
                put("installmentAmount", i.installmentAmount)
                put("startDate", i.startDate)
                put("intervalMonths", i.intervalMonths)
                put("nextPaymentDate", i.nextPaymentDate)
            })
        }
        root.put("installments", instArr)

        root.toString(2)
    }

    suspend fun saveBackupToCloud(context: Context, db: AppDatabase, profileId: String): Long = withContext(Dispatchers.IO) {
        val payload = createBackupPayload(db, profileId)
        val cloudFile = File(context.filesDir, "google_drive_backup_$profileId.json")
        cloudFile.writeText(payload)

        val timestamp = System.currentTimeMillis()
        val profile = db.financialProfileDao().getProfile(profileId)
        if (profile != null) {
            db.financialProfileDao().insertOrUpdate(profile.copy(lastBackupTimestamp = timestamp))
        }
        timestamp
    }

    suspend fun restoreFromCloud(
        context: Context,
        db: AppDatabase,
        profileId: String,
        isReplace: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val cloudFile = File(context.filesDir, "google_drive_backup_$profileId.json")
        if (!cloudFile.exists()) return@withContext false

        val jsonString = cloudFile.readText()
        val root = JSONObject(jsonString)

        if (isReplace) {
            db.transactionDao().deleteAllForProfile(profileId)
            db.accountDao().deleteAllForProfile(profileId)
        }

        // Restore Accounts
        val accountsArr = root.optJSONArray("accounts")
        if (accountsArr != null) {
            for (i in 0 until accountsArr.length()) {
                val o = accountsArr.getJSONObject(i)
                val account = AccountEntity(
                    id = o.getString("id"),
                    profileId = o.getString("profileId"),
                    name = o.getString("name"),
                    type = o.getString("type"),
                    initialBalance = o.getDouble("initialBalance"),
                    currency = o.getString("currency"),
                    bankName = o.optString("bankName", null),
                    maskedCardNumber = o.optString("maskedCardNumber", null),
                    colorHex = o.optString("colorHex", "#00C896"),
                    iconName = o.optString("iconName", "account_balance"),
                    notes = o.optString("notes", null),
                    isArchived = o.optBoolean("isArchived", false)
                )
                db.accountDao().insertOrUpdate(account)
            }
        }

        // Restore Transactions (with deduplication)
        val txArr = root.optJSONArray("transactions")
        if (txArr != null) {
            for (i in 0 until txArr.length()) {
                val o = txArr.getJSONObject(i)
                val tx = TransactionEntity(
                    id = o.getString("id"),
                    profileId = o.getString("profileId"),
                    accountId = o.getString("accountId"),
                    toAccountId = o.optString("toAccountId", null).takeIf { it?.isNotBlank() == true },
                    categoryId = o.getString("categoryId"),
                    type = o.getString("type"),
                    amount = o.getDouble("amount"),
                    currency = o.getString("currency"),
                    timestamp = o.getLong("timestamp"),
                    description = o.getString("description"),
                    notes = o.optString("notes", null),
                    tags = o.optString("tags", null),
                    source = o.optString("source", "MANUAL"),
                    status = o.optString("status", "CONFIRMED"),
                    smsRefId = o.optString("smsRefId", null)
                )
                db.transactionDao().insertOrUpdate(tx)
            }
        }

        // Restore Debts
        val debtArr = root.optJSONArray("debts")
        if (debtArr != null) {
            for (i in 0 until debtArr.length()) {
                val o = debtArr.getJSONObject(i)
                val debt = DebtEntity(
                    id = o.getString("id"),
                    profileId = o.getString("profileId"),
                    personName = o.getString("personName"),
                    type = o.getString("type"),
                    totalAmount = o.getDouble("totalAmount"),
                    paidAmount = o.getDouble("paidAmount"),
                    dueDate = o.getLong("dueDate"),
                    description = o.optString("description", null),
                    isSettled = o.optBoolean("isSettled", false)
                )
                db.debtDao().insertOrUpdate(debt)
            }
        }

        // Restore Installments
        val instArr = root.optJSONArray("installments")
        if (instArr != null) {
            for (i in 0 until instArr.length()) {
                val o = instArr.getJSONObject(i)
                val inst = InstallmentEntity(
                    id = o.getString("id"),
                    profileId = o.getString("profileId"),
                    title = o.getString("title"),
                    totalAmount = o.getDouble("totalAmount"),
                    downPayment = o.getDouble("downPayment"),
                    totalInstallments = o.getInt("totalInstallments"),
                    paidInstallments = o.getInt("paidInstallments"),
                    installmentAmount = o.getDouble("installmentAmount"),
                    startDate = o.getLong("startDate"),
                    intervalMonths = o.optInt("intervalMonths", 1),
                    nextPaymentDate = o.getLong("nextPaymentDate")
                )
                db.installmentDao().insertOrUpdate(inst)
            }
        }

        true
    }

    fun getBackupSize(context: Context, profileId: String): String {
        val cloudFile = File(context.filesDir, "google_drive_backup_$profileId.json")
        if (!cloudFile.exists()) return "0 KB"
        val bytes = cloudFile.length()
        return if (bytes < 1024) "$bytes B" else "${bytes / 1024} KB"
    }

    fun deleteCloudBackup(context: Context, profileId: String): Boolean {
        val cloudFile = File(context.filesDir, "google_drive_backup_$profileId.json")
        return if (cloudFile.exists()) cloudFile.delete() else true
    }
}
