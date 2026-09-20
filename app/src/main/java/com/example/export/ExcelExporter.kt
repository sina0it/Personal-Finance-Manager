package com.example.export

import android.content.Context
import com.example.data.model.*
import com.example.util.CurrencyFormatter
import com.example.util.DateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    suspend fun generateExcel(
        context: Context,
        profile: FinancialProfileEntity,
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        debts: List<DebtEntity>,
        installments: List<InstallmentEntity>
    ): File = withContext(Dispatchers.IO) {
        val fileName = "SinaFinance_${profile.currencyCode}_${System.currentTimeMillis()}.xlsx"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        val zip = ZipOutputStream(FileOutputStream(file))

        val isPersian = profile.languageCode == "fa"
        val currency = profile.currencyCode

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet5.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet6.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet7.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray())
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray())
        zip.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
    <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
    <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet5.xml"/>
    <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet6.xml"/>
    <Relationship Id="rId7" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet7.xml"/>
    <Relationship Id="rId8" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".toByteArray())
        zip.closeEntry()

        // 4. xl/workbook.xml
        val sheet1Name = if (isPersian) "تراکنش‌ها" else "Transactions"
        val sheet2Name = if (isPersian) "خلاصه مالی" else "Summary"
        val sheet3Name = if (isPersian) "دسته‌بندی‌ها" else "Categories"
        val sheet4Name = if (isPersian) "حساب‌ها" else "Accounts"
        val sheet5Name = if (isPersian) "بودجه‌ها" else "Budgets"
        val sheet6Name = if (isPersian) "بدهی و طلب" else "Debts"
        val sheet7Name = if (isPersian) "اقساط" else "Installments"

        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="$sheet1Name" sheetId="1" r:id="rId1"/>
        <sheet name="$sheet2Name" sheetId="2" r:id="rId2"/>
        <sheet name="$sheet3Name" sheetId="3" r:id="rId3"/>
        <sheet name="$sheet4Name" sheetId="4" r:id="rId4"/>
        <sheet name="$sheet5Name" sheetId="5" r:id="rId5"/>
        <sheet name="$sheet6Name" sheetId="6" r:id="rId6"/>
        <sheet name="$sheet7Name" sheetId="7" r:id="rId7"/>
    </sheets>
</workbook>""".toByteArray())
        zip.closeEntry()

        // 5. xl/styles.xml
        zip.putNextEntry(ZipEntry("xl/styles.xml"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="2">
        <font><sz val="11"/><name val="Segoe UI"/></font>
        <font><b/><sz val="11"/><name val="Segoe UI"/></font>
    </fonts>
    <fills count="2">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
    </fills>
    <borders count="1">
        <border><left/><right/><top/><bottom/><diagonal/></border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="2">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/>
    </cellXfs>
</styleSheet>""".toByteArray())
        zip.closeEntry()

        // Helper to write a simple sheet XML
        fun writeSheet(zip: ZipOutputStream, entryName: String, rows: List<List<String>>) {
            zip.putNextEntry(ZipEntry(entryName))
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetData>""")
            for ((rIdx, row) in rows.withIndex()) {
                val rowNum = rIdx + 1
                sb.append("""<row r="$rowNum">""")
                for ((cIdx, cellVal) in row.withIndex()) {
                    val colLetter = ('A'.code + cIdx).toChar()
                    val cellRef = "$colLetter$rowNum"
                    val styleAttr = if (rowNum == 1) """ s="1"""" else ""
                    val escaped = cellVal.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                    sb.append("""<c r="$cellRef" t="inlineStr"$styleAttr><is><t>$escaped</t></is></c>""")
                }
                sb.append("""</row>""")
            }
            sb.append("""</sheetData>
</worksheet>""")
            zip.write(sb.toString().toByteArray())
            zip.closeEntry()
        }

        // Sheet 1: Transactions
        val catMap = categories.associateBy { it.id }
        val accMap = accounts.associateBy { it.id }

        val txHeader = if (isPersian) listOf("شناسه", "نوع", "مبلغ ($currency)", "دسته‌بندی", "حساب", "تاریخ", "توضیحات", "منبع")
        else listOf("ID", "Type", "Amount ($currency)", "Category", "Account", "Date", "Description", "Source")

        val txRows = mutableListOf<List<String>>()
        txRows.add(txHeader)
        for (tx in transactions) {
            val catName = catMap[tx.categoryId]?.name ?: "—"
            val accName = accMap[tx.accountId]?.name ?: "—"
            val dateStr = DateHelper.formatDate(tx.timestamp, isPersian)
            val amtStr = CurrencyFormatter.format(tx.amount, currency)
            txRows.add(listOf(tx.id.take(8), tx.type, amtStr, catName, accName, dateStr, tx.description, tx.source))
        }
        writeSheet(zip, "xl/worksheets/sheet1.xml", txRows)

        // Sheet 2: Summary
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netFlow = totalIncome - totalExpense
        val sumRows = listOf(
            if (isPersian) listOf("شاخص مالی", "مقدار ($currency)") else listOf("Financial Metric", "Value ($currency)"),
            listOf(if (isPersian) "مجموع درآمد" else "Total Income", CurrencyFormatter.format(totalIncome, currency)),
            listOf(if (isPersian) "مجموع هزینه" else "Total Expenses", CurrencyFormatter.format(totalExpense, currency)),
            listOf(if (isPersian) "گردش خالص مالی" else "Net Cash Flow", CurrencyFormatter.format(netFlow, currency)),
            listOf(if (isPersian) "تعداد کل تراکنش‌ها" else "Total Transactions", transactions.size.toString()),
            listOf(if (isPersian) "تعداد حساب‌ها" else "Total Accounts", accounts.size.toString())
        )
        writeSheet(zip, "xl/worksheets/sheet2.xml", sumRows)

        // Sheet 3: Categories
        val catHeader = if (isPersian) listOf("نام دسته", "نوع", "رنگ") else listOf("Name", "Type", "Color")
        val catRows = mutableListOf(catHeader)
        for (c in categories) {
            catRows.add(listOf(c.name, c.type, c.colorHex))
        }
        writeSheet(zip, "xl/worksheets/sheet3.xml", catRows)

        // Sheet 4: Accounts
        val accHeader = if (isPersian) listOf("نام حساب", "نوع", "موجودی اولیه", "واحد پول", "بانک") else listOf("Account Name", "Type", "Initial Balance", "Currency", "Bank")
        val accRows = mutableListOf(accHeader)
        for (a in accounts) {
            accRows.add(listOf(a.name, a.type, CurrencyFormatter.format(a.initialBalance, currency), a.currency, a.bankName ?: "—"))
        }
        writeSheet(zip, "xl/worksheets/sheet4.xml", accRows)

        // Sheet 5: Budgets
        val bgtHeader = if (isPersian) listOf("دسته", "سقف بودجه", "دوره") else listOf("Category", "Limit", "Period")
        val bgtRows = mutableListOf(bgtHeader)
        for (b in budgets) {
            val cName = catMap[b.categoryId]?.name ?: "—"
            bgtRows.add(listOf(cName, CurrencyFormatter.format(b.amount, currency), b.period))
        }
        writeSheet(zip, "xl/worksheets/sheet5.xml", bgtRows)

        // Sheet 6: Debts
        val debtHeader = if (isPersian) listOf("طرف حساب", "نوع", "کل مبلغ", "پرداخت شده", "وضعیت") else listOf("Person/Company", "Type", "Total Amount", "Paid Amount", "Status")
        val debtRows = mutableListOf(debtHeader)
        for (d in debts) {
            val status = if (d.isSettled) (if (isPersian) "تسویه شده" else "Settled") else (if (isPersian) "جاری" else "Active")
            debtRows.add(listOf(d.personName, d.type, CurrencyFormatter.format(d.totalAmount, currency), CurrencyFormatter.format(d.paidAmount, currency), status))
        }
        writeSheet(zip, "xl/worksheets/sheet6.xml", debtRows)

        // Sheet 7: Installments
        val instHeader = if (isPersian) listOf("عنوان", "مبلغ کل", "تعداد اقساط", "اقساط پرداخت شده", "مبلغ هر قسط") else listOf("Title", "Total Amount", "Total Installments", "Paid Installments", "Amount Per Installment")
        val instRows = mutableListOf(instHeader)
        for (i in installments) {
            instRows.add(listOf(i.title, CurrencyFormatter.format(i.totalAmount, currency), i.totalInstallments.toString(), i.paidInstallments.toString(), CurrencyFormatter.format(i.installmentAmount, currency)))
        }
        writeSheet(zip, "xl/worksheets/sheet7.xml", instRows)

        zip.finish()
        zip.close()

        file
    }
}
