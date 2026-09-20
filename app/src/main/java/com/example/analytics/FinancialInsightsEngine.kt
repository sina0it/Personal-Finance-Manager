package com.example.analytics

import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.util.CurrencyFormatter

data class FinancialInsight(
    val title: String,
    val message: String,
    val iconName: String,
    val isPositive: Boolean
)

object FinancialInsightsEngine {

    fun generateInsights(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        currency: String,
        isPersian: Boolean
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        val catMap = categories.associateBy { it.id }

        val now = System.currentTimeMillis()
        val oneMonthAgo = now - 30L * 24 * 60 * 60 * 1000
        val twoMonthsAgo = now - 60L * 24 * 60 * 60 * 1000

        val currentMonthTx = transactions.filter { it.timestamp in (oneMonthAgo + 1)..now }
        val previousMonthTx = transactions.filter { it.timestamp in (twoMonthsAgo + 1)..oneMonthAgo }

        val currentExpenses = currentMonthTx.filter { it.type == "EXPENSE" }
        val previousExpenses = previousMonthTx.filter { it.type == "EXPENSE" }

        val currentTotalExp = currentExpenses.sumOf { it.amount }
        val previousTotalExp = previousExpenses.sumOf { it.amount }

        // 1. Month-over-month overall expense trend
        if (previousTotalExp > 0 && currentTotalExp > 0) {
            val diffPercent = (((currentTotalExp - previousTotalExp) / previousTotalExp) * 100).toInt()
            if (diffPercent > 10) {
                insights.add(
                    FinancialInsight(
                        title = if (isPersian) "افزایش مخارج ماهانه" else "Monthly Spending Increased",
                        message = if (isPersian)
                            "شما در این ماه $diffPercent٪ بیشتر از ماه قبل خرج کرده‌اید."
                        else "You spent $diffPercent% more this month compared to last month.",
                        iconName = "trending_up",
                        isPositive = false
                    )
                )
            } else if (diffPercent < -10) {
                val savedPercent = -diffPercent
                insights.add(
                    FinancialInsight(
                        title = if (isPersian) "کاهش مخارج و صرفه‌جویی" else "Spending Reduced",
                        message = if (isPersian)
                            "شما در این ماه مخارج خود را $savedPercent٪ نسبت به ماه گذشته کاهش داده‌اید."
                        else "You reduced expenses by $savedPercent% compared to last month.",
                        iconName = "savings",
                        isPositive = true
                    )
                )
            }
        }

        // 2. Category spike detection
        val currentByCat = currentExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } }
        val prevByCat = previousExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } }

        for ((catId, curAmt) in currentByCat) {
            val prevAmt = prevByCat[catId] ?: 0.0
            val catName = catMap[catId]?.name ?: continue
            if (prevAmt > 0 && curAmt > prevAmt * 1.25) {
                val pct = (((curAmt - prevAmt) / prevAmt) * 100).toInt()
                insights.add(
                    FinancialInsight(
                        title = if (isPersian) "افزایش هزینه در دسته $catName" else "Higher Spending in $catName",
                        message = if (isPersian)
                            "هزینه شما در دسته «$catName» نسبت به ماه قبل $pct٪ رشد داشته است."
                        else "You spent $pct% more on $catName than last month.",
                        iconName = "shopping_cart",
                        isPositive = false
                    )
                )
                break // Add the top one
            }
        }

        // 3. Budget thresholds
        for (b in budgets) {
            val spent = currentExpenses.filter { it.categoryId == b.categoryId }.sumOf { it.amount }
            val catName = catMap[b.categoryId]?.name ?: "Budget"
            if (b.amount > 0) {
                val ratio = spent / b.amount
                if (ratio >= 1.0) {
                    val excess = spent - b.amount
                    insights.add(
                        FinancialInsight(
                            title = if (isPersian) "عبور از سقف بودجه $catName" else "$catName Budget Exceeded",
                            message = if (isPersian)
                                "بودجه $catName به میزان ${CurrencyFormatter.format(excess, currency)} پر شده است."
                            else "You exceeded your $catName budget by ${CurrencyFormatter.format(excess, currency)}.",
                            iconName = "warning",
                            isPositive = false
                        )
                    )
                } else if (ratio >= 0.8) {
                    val pct = (ratio * 100).toInt()
                    insights.add(
                        FinancialInsight(
                            title = if (isPersian) "نزدیک به سقف بودجه $catName" else "Near $catName Budget Limit",
                            message = if (isPersian)
                                "$pct٪ از سقف بودجه ماهانه $catName مصرف شده است."
                            else "$pct% of your $catName monthly budget has been used.",
                            iconName = "pie_chart",
                            isPositive = false
                        )
                    )
                }
            }
        }

        // 4. Savings rate
        val curIncome = currentMonthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        if (curIncome > 0) {
            val net = curIncome - currentTotalExp
            if (net > 0) {
                val rate = ((net / curIncome) * 100).toInt()
                if (rate >= 20) {
                    insights.add(
                        FinancialInsight(
                            title = if (isPersian) "نرخ پس‌انداز عالی" else "Strong Savings Rate",
                            message = if (isPersian)
                                "نرخ پس‌انداز شما در این دوره به $rate٪ رسیده است."
                            else "Your current savings rate is $rate%, which is above the 20% benchmark.",
                            iconName = "verified",
                            isPositive = true
                        )
                    )
                }
            }
        }

        // Fallback welcoming tip if no data yet
        if (insights.isEmpty()) {
            insights.add(
                FinancialInsight(
                    title = if (isPersian) "مدیریت هوشمند دارایی" else "Smart Wealth Management",
                    message = if (isPersian)
                        "با ثبت منظم تراکنش‌ها و بودجه‌ها، تحلیل‌های مقایسه‌ای دقیق در اینجا نمایش داده می‌شوند."
                    else "As you log transactions and budgets, real analytical insights will be calculated here automatically.",
                    iconName = "insights",
                    isPositive = true
                )
            )
        }

        return insights
    }
}
