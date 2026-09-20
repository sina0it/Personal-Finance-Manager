package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FinancialProfileEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        DebtEntity::class,
        InstallmentEntity::class,
        SmsDetectionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financialProfileDao(): FinancialProfileDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun smsDetectionDao(): SmsDetectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sina_finance.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).populateInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun populateInitialData() {
        // Seed default profiles: Iran (Toman) and US (USD)
        val iranProfile = FinancialProfileEntity(
            profileId = "IRAN_TOMAN",
            countryCode = "IR",
            languageCode = "fa",
            currencyCode = "TOMAN",
            currencySymbol = "تومان",
            userName = "سینا نادری"
        )
        val usProfile = FinancialProfileEntity(
            profileId = "US_USD",
            countryCode = "US",
            languageCode = "en",
            currencyCode = "USD",
            currencySymbol = "$",
            userName = "Sina Naderi"
        )
        financialProfileDao().insertOrUpdate(iranProfile)
        financialProfileDao().insertOrUpdate(usProfile)

        // Seed default categories for Iran
        val defaultIranCategories = listOf(
            // Income
            CategoryEntity(id = "ir_cat_sal", profileId = "IRAN_TOMAN", name = "حقوق و دستمزد", type = "INCOME", iconName = "payments", colorHex = "#10B981", isDefault = true),
            CategoryEntity(id = "ir_cat_free", profileId = "IRAN_TOMAN", name = "پروژه و فریلنسری", type = "INCOME", iconName = "laptop", colorHex = "#3B82F6", isDefault = true),
            CategoryEntity(id = "ir_cat_biz", profileId = "IRAN_TOMAN", name = "کسب‌وکار", type = "INCOME", iconName = "storefront", colorHex = "#8B5CF6", isDefault = true),
            CategoryEntity(id = "ir_cat_inv", profileId = "IRAN_TOMAN", name = "سرمایه‌گذاری", type = "INCOME", iconName = "trending_up", colorHex = "#F59E0B", isDefault = true),
            CategoryEntity(id = "ir_cat_gift", profileId = "IRAN_TOMAN", name = "هدیه و پاداش", type = "INCOME", iconName = "card_giftcard", colorHex = "#EC4899", isDefault = true),
            CategoryEntity(id = "ir_cat_oth_inc", profileId = "IRAN_TOMAN", name = "سایر درآمدها", type = "INCOME", iconName = "account_balance_wallet", colorHex = "#14B8A6", isDefault = true),
            // Expense
            CategoryEntity(id = "ir_cat_food", profileId = "IRAN_TOMAN", name = "خوراک و رستوران", type = "EXPENSE", iconName = "restaurant", colorHex = "#EF4444", isDefault = true),
            CategoryEntity(id = "ir_cat_shop", profileId = "IRAN_TOMAN", name = "خرید و پوشاک", type = "EXPENSE", iconName = "shopping_bag", colorHex = "#F97316", isDefault = true),
            CategoryEntity(id = "ir_cat_trans", profileId = "IRAN_TOMAN", name = "حمل‌ونقل", type = "EXPENSE", iconName = "directions_car", colorHex = "#FBBF24", isDefault = true),
            CategoryEntity(id = "ir_cat_fuel", profileId = "IRAN_TOMAN", name = "سوخت و بنزین", type = "EXPENSE", iconName = "local_gas_station", colorHex = "#84CC16", isDefault = true),
            CategoryEntity(id = "ir_cat_bills", profileId = "IRAN_TOMAN", name = "قبوض و اینترنت", type = "EXPENSE", iconName = "receipt_long", colorHex = "#06B6D4", isDefault = true),
            CategoryEntity(id = "ir_cat_rent", profileId = "IRAN_TOMAN", name = "اجاره و مسکن", type = "EXPENSE", iconName = "home", colorHex = "#3B82F6", isDefault = true),
            CategoryEntity(id = "ir_cat_ent", profileId = "IRAN_TOMAN", name = "تفریح و سرگرمی", type = "EXPENSE", iconName = "sports_esports", colorHex = "#A855F7", isDefault = true),
            CategoryEntity(id = "ir_cat_health", profileId = "IRAN_TOMAN", name = "پزشکی و درمان", type = "EXPENSE", iconName = "local_hospital", colorHex = "#EC4899", isDefault = true),
            CategoryEntity(id = "ir_cat_edu", profileId = "IRAN_TOMAN", name = "آموزش و یادگیری", type = "EXPENSE", iconName = "school", colorHex = "#6366F1", isDefault = true),
            CategoryEntity(id = "ir_cat_travel", profileId = "IRAN_TOMAN", name = "سفر و گردشگری", type = "EXPENSE", iconName = "flight", colorHex = "#14B8A6", isDefault = true),
            CategoryEntity(id = "ir_cat_subs", profileId = "IRAN_TOMAN", name = "اشتراک‌ها", type = "EXPENSE", iconName = "subscriptions", colorHex = "#64748B", isDefault = true),
            CategoryEntity(id = "ir_cat_oth_exp", profileId = "IRAN_TOMAN", name = "سایر هزینه‌ها", type = "EXPENSE", iconName = "more_horiz", colorHex = "#94A3B8", isDefault = true)
        )

        // Seed default categories for US
        val defaultUsCategories = listOf(
            // Income
            CategoryEntity(id = "us_cat_sal", profileId = "US_USD", name = "Salary", type = "INCOME", iconName = "payments", colorHex = "#10B981", isDefault = true),
            CategoryEntity(id = "us_cat_free", profileId = "US_USD", name = "Freelance", type = "INCOME", iconName = "laptop", colorHex = "#3B82F6", isDefault = true),
            CategoryEntity(id = "us_cat_biz", profileId = "US_USD", name = "Business", type = "INCOME", iconName = "storefront", colorHex = "#8B5CF6", isDefault = true),
            CategoryEntity(id = "us_cat_inv", profileId = "US_USD", name = "Investment", type = "INCOME", iconName = "trending_up", colorHex = "#F59E0B", isDefault = true),
            CategoryEntity(id = "us_cat_gift", profileId = "US_USD", name = "Gift", type = "INCOME", iconName = "card_giftcard", colorHex = "#EC4899", isDefault = true),
            CategoryEntity(id = "us_cat_oth_inc", profileId = "US_USD", name = "Other Income", type = "INCOME", iconName = "account_balance_wallet", colorHex = "#14B8A6", isDefault = true),
            // Expense
            CategoryEntity(id = "us_cat_food", profileId = "US_USD", name = "Food & Dining", type = "EXPENSE", iconName = "restaurant", colorHex = "#EF4444", isDefault = true),
            CategoryEntity(id = "us_cat_shop", profileId = "US_USD", name = "Shopping", type = "EXPENSE", iconName = "shopping_bag", colorHex = "#F97316", isDefault = true),
            CategoryEntity(id = "us_cat_trans", profileId = "US_USD", name = "Transport", type = "EXPENSE", iconName = "directions_car", colorHex = "#FBBF24", isDefault = true),
            CategoryEntity(id = "us_cat_fuel", profileId = "US_USD", name = "Fuel & Gas", type = "EXPENSE", iconName = "local_gas_station", colorHex = "#84CC16", isDefault = true),
            CategoryEntity(id = "us_cat_bills", profileId = "US_USD", name = "Bills & Utilities", type = "EXPENSE", iconName = "receipt_long", colorHex = "#06B6D4", isDefault = true),
            CategoryEntity(id = "us_cat_rent", profileId = "US_USD", name = "Rent & Housing", type = "EXPENSE", iconName = "home", colorHex = "#3B82F6", isDefault = true),
            CategoryEntity(id = "us_cat_ent", profileId = "US_USD", name = "Entertainment", type = "EXPENSE", iconName = "sports_esports", colorHex = "#A855F7", isDefault = true),
            CategoryEntity(id = "us_cat_health", profileId = "US_USD", name = "Health & Medical", type = "EXPENSE", iconName = "local_hospital", colorHex = "#EC4899", isDefault = true),
            CategoryEntity(id = "us_cat_edu", profileId = "US_USD", name = "Education", type = "EXPENSE", iconName = "school", colorHex = "#6366F1", isDefault = true),
            CategoryEntity(id = "us_cat_travel", profileId = "US_USD", name = "Travel", type = "EXPENSE", iconName = "flight", colorHex = "#14B8A6", isDefault = true),
            CategoryEntity(id = "us_cat_subs", profileId = "US_USD", name = "Subscriptions", type = "EXPENSE", iconName = "subscriptions", colorHex = "#64748B", isDefault = true),
            CategoryEntity(id = "us_cat_oth_exp", profileId = "US_USD", name = "Other Expense", type = "EXPENSE", iconName = "more_horiz", colorHex = "#94A3B8", isDefault = true)
        )

        categoryDao().insertAll(defaultIranCategories)
        categoryDao().insertAll(defaultUsCategories)

        // Seed default starter accounts for Iran
        val iranDefaultAccounts = listOf(
            AccountEntity(id = "ir_acc_cash", profileId = "IRAN_TOMAN", name = "کیف پول نقدی", type = "CASH", initialBalance = 1500000.0, currency = "TOMAN", colorHex = "#10B981", iconName = "payments"),
            AccountEntity(id = "ir_acc_melli", profileId = "IRAN_TOMAN", name = "بانک ملی", type = "BANK", initialBalance = 24500000.0, currency = "TOMAN", bankName = "بانک ملی ایران", maskedCardNumber = "4912", colorHex = "#00C896", iconName = "account_balance")
        )
        for (acc in iranDefaultAccounts) {
            accountDao().insertOrUpdate(acc)
        }

        // Seed default starter accounts for US
        val usDefaultAccounts = listOf(
            AccountEntity(id = "us_acc_cash", profileId = "US_USD", name = "Cash Wallet", type = "CASH", initialBalance = 250.0, currency = "USD", colorHex = "#10B981", iconName = "payments"),
            AccountEntity(id = "us_acc_chase", profileId = "US_USD", name = "Chase Checking", type = "BANK", initialBalance = 3850.0, currency = "USD", bankName = "Chase", maskedCardNumber = "5821", colorHex = "#00C896", iconName = "account_balance")
        )
        for (acc in usDefaultAccounts) {
            accountDao().insertOrUpdate(acc)
        }
    }
}
