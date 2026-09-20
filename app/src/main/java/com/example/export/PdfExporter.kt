package com.example.export

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.data.model.CategoryEntity
import com.example.data.model.FinancialProfileEntity
import com.example.data.model.TransactionEntity
import com.example.util.CurrencyFormatter
import com.example.util.DateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    suspend fun generatePdf(
        context: Context,
        profile: FinancialProfileEntity,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        dateRangeLabel: String
    ): File = withContext(Dispatchers.IO) {
        val fileName = "SinaFinance_Report_${profile.currencyCode}_${System.currentTimeMillis()}.pdf"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        val isPersian = profile.languageCode == "fa"
        val currency = profile.currencyCode

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Header Banner (Emerald Gradient)
        val bannerPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 595f, 100f, Color.parseColor("#00C896"), Color.parseColor("#008F6B"), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, 595f, 90f, bannerPaint)

        // Header Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = if (isPersian) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val appTitle = if (isPersian) "سینا فایننس — گزارش مالی هوشمند" else "Sina Finance — Financial Report"
        val titleX = if (isPersian) 560f else 35f
        canvas.drawText(appTitle, titleX, 42f, titlePaint)

        // Subtitle / Date
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#D1FAE5")
            textSize = 11f
            isAntiAlias = true
            textAlign = if (isPersian) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val subText = if (isPersian) "بازه زمانی: $dateRangeLabel  •  واحد پولی: تومان" else "Period: $dateRangeLabel  •  Currency: $currency"
        canvas.drawText(subText, titleX, 64f, subTitlePaint)

        // Calculate Totals
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val net = totalIncome - totalExpense

        // 3 Summary Cards
        val cardPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        val cardBorderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cardTextTitle = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 10f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val cardTextVal = Paint().apply {
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // Card 1: Income
        val r1 = RectF(35f, 105f, 195f, 160f)
        canvas.drawRoundRect(r1, 8f, 8f, cardPaint)
        canvas.drawRoundRect(r1, 8f, 8f, cardBorderPaint)
        canvas.drawText(if (isPersian) "مجموع درآمد" else "Total Income", 115f, 125f, cardTextTitle)
        cardTextVal.color = Color.parseColor("#10B981")
        canvas.drawText(CurrencyFormatter.format(totalIncome, currency), 115f, 147f, cardTextVal)

        // Card 2: Expense
        val r2 = RectF(217f, 105f, 377f, 160f)
        canvas.drawRoundRect(r2, 8f, 8f, cardPaint)
        canvas.drawRoundRect(r2, 8f, 8f, cardBorderPaint)
        canvas.drawText(if (isPersian) "مجموع هزینه" else "Total Expenses", 297f, 125f, cardTextTitle)
        cardTextVal.color = Color.parseColor("#EF4444")
        canvas.drawText(CurrencyFormatter.format(totalExpense, currency), 297f, 147f, cardTextVal)

        // Card 3: Net Cash Flow
        val r3 = RectF(400f, 105f, 560f, 160f)
        canvas.drawRoundRect(r3, 8f, 8f, cardPaint)
        canvas.drawRoundRect(r3, 8f, 8f, cardBorderPaint)
        canvas.drawText(if (isPersian) "گردش خالص مالی" else "Net Balance", 480f, 125f, cardTextTitle)
        cardTextVal.color = if (net >= 0) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
        canvas.drawText(CurrencyFormatter.format(net, currency), 480f, 147f, cardTextVal)

        // Section Title: Category Breakdown
        val secTitlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = if (isPersian) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val secX = if (isPersian) 560f else 35f
        canvas.drawText(if (isPersian) "تفکیک بر اساس دسته‌بندی" else "Category Breakdown", secX, 190f, secTitlePaint)

        // Top categories
        val catMap = categories.associateBy { it.id }
        val expensesByCategory = transactions.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)

        var curY = 210f
        val barBgPaint = Paint().apply { color = Color.parseColor("#E2E8F0") }
        val barFillPaint = Paint().apply { color = Color.parseColor("#00C896") }
        val catNamePaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10f
            isAntiAlias = true
            textAlign = if (isPersian) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val catValPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = if (isPersian) Paint.Align.LEFT else Paint.Align.RIGHT
        }

        for ((catId, amount) in expensesByCategory) {
            val name = catMap[catId]?.name ?: "Other"
            val pct = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
            val amtStr = CurrencyFormatter.format(amount, currency)

            if (isPersian) {
                canvas.drawText(name, 560f, curY, catNamePaint)
                canvas.drawText(amtStr, 35f, curY, catValPaint)
            } else {
                canvas.drawText(name, 35f, curY, catNamePaint)
                canvas.drawText(amtStr, 560f, curY, catValPaint)
            }

            // Draw progress bar
            val barRect = RectF(35f, curY + 4f, 560f, curY + 10f)
            canvas.drawRoundRect(barRect, 3f, 3f, barBgPaint)
            val fillWidth = (525f * pct).coerceIn(4f, 525f)
            val fillRect = if (isPersian) {
                RectF(560f - fillWidth, curY + 4f, 560f, curY + 10f)
            } else {
                RectF(35f, curY + 4f, 35f + fillWidth, curY + 10f)
            }
            canvas.drawRoundRect(fillRect, 3f, 3f, barFillPaint)

            curY += 24f
        }

        // Section Title: Recent Transactions
        curY += 15f
        canvas.drawText(if (isPersian) "فهرست تراکنش‌ها" else "Transaction Records", secX, curY, secTitlePaint)
        curY += 15f

        // Table Header
        val thPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        canvas.drawLine(35f, curY, 560f, curY, linePaint)
        curY += 14f

        if (isPersian) {
            thPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("تاریخ", 560f, curY, thPaint)
            canvas.drawText("دسته‌بندی", 460f, curY, thPaint)
            canvas.drawText("شرح تراکنش", 340f, curY, thPaint)
            thPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("مبلغ", 35f, curY, thPaint)
        } else {
            thPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Date", 35f, curY, thPaint)
            canvas.drawText("Category", 130f, curY, thPaint)
            canvas.drawText("Description", 250f, curY, thPaint)
            thPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount", 560f, curY, thPaint)
        }
        curY += 6f
        canvas.drawLine(35f, curY, 560f, curY, linePaint)
        curY += 14f

        val trPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
            isAntiAlias = true
        }
        val trAmtPaint = Paint().apply {
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        for (tx in transactions.take(15)) {
            val dateStr = DateHelper.formatDate(tx.timestamp, isPersian)
            val catName = catMap[tx.categoryId]?.name ?: "—"
            val desc = tx.description.take(22)
            val amtStr = (if (tx.type == "INCOME") "+ " else if (tx.type == "EXPENSE") "- " else "") +
                    CurrencyFormatter.format(tx.amount, currency)
            trAmtPaint.color = if (tx.type == "INCOME") Color.parseColor("#10B981") else Color.parseColor("#EF4444")

            if (isPersian) {
                trPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(dateStr, 560f, curY, trPaint)
                canvas.drawText(catName, 460f, curY, trPaint)
                canvas.drawText(desc, 340f, curY, trPaint)
                trAmtPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(amtStr, 35f, curY, trAmtPaint)
            } else {
                trPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(dateStr, 35f, curY, trPaint)
                canvas.drawText(catName, 130f, curY, trPaint)
                canvas.drawText(desc, 250f, curY, trPaint)
                trAmtPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(amtStr, 560f, curY, trAmtPaint)
            }

            curY += 16f
            if (curY > 800f) break
        }

        // Footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Sina Finance • Developer: Sina Naderi (sinanaderi203@gmail.com) • Generated on Android", 297f, 825f, footerPaint)

        doc.finishPage(page)
        doc.writeTo(FileOutputStream(file))
        doc.close()

        file
    }
}
