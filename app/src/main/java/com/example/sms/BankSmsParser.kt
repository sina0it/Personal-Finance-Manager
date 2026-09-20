package com.example.sms

import com.example.data.model.SmsDetectionEntity
import com.example.data.model.SmsDetectionStatus
import com.example.data.model.TransactionType
import com.example.util.CurrencyFormatter
import java.security.MessageDigest
import java.util.regex.Pattern

data class ParsedSmsResult(
    val isValidFinancial: Boolean,
    val bankName: String,
    val transactionType: String,
    val amount: Double,
    val currency: String,
    val maskedCard: String?,
    val referenceNumber: String?,
    val balanceAfter: Double?,
    val confidence: Int
)

object BankSmsParser {

    // Filter out OTPs, verification codes, login tokens, ads, and personal messages
    private val securityOtpKeywords = listOf(
        "کد تایید", "رمز پویا", "رمز یکبار مصرف", "کد فعالسازی", "رمز دوم",
        "otp", "verification code", "security code", "one-time", "password", "passcode", "secret code"
    )

    fun isOtpOrSecurityMessage(body: String): Boolean {
        val lower = body.lowercase()
        return securityOtpKeywords.any { lower.contains(it) }
    }

    fun parse(sender: String, body: String, profileId: String): ParsedSmsResult? {
        if (isOtpOrSecurityMessage(body)) {
            return null
        }

        val normalizedBody = CurrencyFormatter.fromPersianDigits(body).replace("\n", " ").trim()

        return if (profileId == "IRAN_TOMAN") {
            parsePersianSms(sender, normalizedBody)
        } else {
            parseEnglishSms(sender, normalizedBody)
        }
    }

    private fun parsePersianSms(sender: String, text: String): ParsedSmsResult? {
        // Persian financial keywords
        val hasFinancialKeyword = text.contains("واریز") || text.contains("برداشت") ||
                text.contains("خرید") || text.contains("انتقال") ||
                text.contains("مانده") || text.contains("مبلغ") ||
                text.contains("پایا") || text.contains("ساتنا") ||
                text.contains("کارمزد")

        if (!hasFinancialKeyword) {
            return null
        }

        // Determine transaction type
        val type = when {
            text.contains("واریز") -> TransactionType.INCOME.name
            text.contains("برداشت") || text.contains("خرید") || text.contains("کارمزد") -> TransactionType.EXPENSE.name
            text.contains("انتقال") || text.contains("پایا") || text.contains("ساتنا") -> TransactionType.TRANSFER.name
            else -> TransactionType.EXPENSE.name
        }

        // Detect Bank Name
        val bankName = when {
            text.contains("ملت") || sender.contains("mellat", true) -> "بانک ملت"
            text.contains("ملی") || sender.contains("melli", true) -> "بانک ملی ایران"
            text.contains("صادرات") || sender.contains("saderat", true) -> "بانک صادرات"
            text.contains("تجارت") || sender.contains("tejarat", true) -> "بانک تجارت"
            text.contains("سامان") || sender.contains("saman", true) -> "بانک سامان"
            text.contains("پاسارگاد") || sender.contains("pasargad", true) -> "بانک پاسارگاد"
            text.contains("پارسیان") || sender.contains("parsian", true) -> "بانک پارسیان"
            text.contains("سپه") || sender.contains("sepah", true) -> "بانک سپه"
            text.contains("کشاورزی") || sender.contains("keshavarzi", true) -> "بانک کشاورزی"
            text.contains("بلو") || sender.contains("blubank", true) -> "بلو بانک"
            text.contains("آینده") || sender.contains("ayandeh", true) -> "بانک آینده"
            text.contains("شهر") || sender.contains("shahr", true) -> "بانک شهر"
            text.contains("رفاه") || sender.contains("refah", true) -> "بانک رفاه"
            else -> "بانک"
        }

        // Extract Amount (e.g. مبلغ: 150,000 ریال / تومان or 150000+ or 150,000-)
        var amount: Double? = null
        var isRial = text.contains("ریال") || text.contains("Rial", true)

        val amountPatterns = listOf(
            Pattern.compile("(?:مبلغ|مبلغ تراکنش)\\s*[:\\-]?\\s*([\\d,]+)"),
            Pattern.compile("([\\d,]+)\\s*(?:ریال|تومان)"),
            Pattern.compile("([\\d,]+)[\\+\\-]")
        )

        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawAmt = matcher.group(1)?.replace(",", "")
                val parsed = rawAmt?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    amount = parsed
                    break
                }
            }
        }

        if (amount == null) {
            return null
        }

        // If amount was in Rials, convert to Toman (divide by 10)
        val finalAmountInToman = if (isRial && amount >= 10) amount / 10.0 else amount

        // Extract Masked Card (e.g. کارت: 4912 or کارت*4912 or 6037...4912)
        var maskedCard: String? = null
        val cardMatcher = Pattern.compile("(?:کارت|حساب)[\\s:*]*([\\d]{4})").matcher(text)
        if (cardMatcher.find()) {
            maskedCard = cardMatcher.group(1)
        }

        // Extract Reference Number (پیگیری: 123456 or ارجاع: 123456)
        var refNumber: String? = null
        val refMatcher = Pattern.compile("(?:پیگیری|ارجاع|شماره پیگیری)[\\s:]*([\\d]+)").matcher(text)
        if (refMatcher.find()) {
            refNumber = refMatcher.group(1)
        }

        // Extract Balance After (مانده: 1,500,000)
        var balanceAfter: Double? = null
        val balMatcher = Pattern.compile("(?:مانده|موجودی)[\\s:]*([\\d,]+)").matcher(text)
        if (balMatcher.find()) {
            val rawBal = balMatcher.group(1)?.replace(",", "")
            val parsedBal = rawBal?.toDoubleOrNull()
            if (parsedBal != null) {
                balanceAfter = if (isRial && parsedBal >= 10) parsedBal / 10.0 else parsedBal
            }
        }

        var confidence = 75
        if (bankName != "بانک") confidence += 10
        if (maskedCard != null) confidence += 5
        if (refNumber != null) confidence += 10

        return ParsedSmsResult(
            isValidFinancial = true,
            bankName = bankName,
            transactionType = type,
            amount = finalAmountInToman,
            currency = "TOMAN",
            maskedCard = maskedCard,
            referenceNumber = refNumber,
            balanceAfter = balanceAfter,
            confidence = confidence.coerceIn(50, 99)
        )
    }

    private fun parseEnglishSms(sender: String, text: String): ParsedSmsResult? {
        val lower = text.lowercase()
        val hasKeyword = lower.contains("debit") || lower.contains("credit") ||
                lower.contains("deposit") || lower.contains("withdrawal") ||
                lower.contains("transaction") || lower.contains("purchase") ||
                lower.contains("sent") || lower.contains("received") ||
                lower.contains("spent") || lower.contains("balance")

        if (!hasKeyword) return null

        val type = when {
            lower.contains("deposit") || lower.contains("received") || lower.contains("refund") -> TransactionType.INCOME.name
            lower.contains("transfer") -> TransactionType.TRANSFER.name
            else -> TransactionType.EXPENSE.name
        }

        val bankName = when {
            lower.contains("chase") || sender.contains("chase", true) -> "Chase"
            lower.contains("bank of america") || lower.contains("bofa") || sender.contains("bofa", true) -> "Bank of America"
            lower.contains("wells fargo") || sender.contains("wells", true) -> "Wells Fargo"
            lower.contains("citi") || sender.contains("citi", true) -> "Citibank"
            lower.contains("capital one") || sender.contains("capitalone", true) -> "Capital One"
            else -> "Bank"
        }

        var amount: Double? = null
        val amountMatcher = Pattern.compile("\\$([\\d,]+\\.?\\d{0,2})").matcher(text)
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        }

        if (amount == null) {
            val numMatcher = Pattern.compile("(?:amount|for|of)\\s*([\\d,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE).matcher(text)
            if (numMatcher.find()) {
                amount = numMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
            }
        }

        if (amount == null) return null

        var maskedCard: String? = null
        val cardMatcher = Pattern.compile("(?:ending in|card|acct|account)\\s*(?:\\.\\.\\.|#|x+)?([0-9]{4})", Pattern.CASE_INSENSITIVE).matcher(text)
        if (cardMatcher.find()) {
            maskedCard = cardMatcher.group(1)
        }

        var confidence = 80
        if (bankName != "Bank") confidence += 10
        if (maskedCard != null) confidence += 9

        return ParsedSmsResult(
            isValidFinancial = true,
            bankName = bankName,
            transactionType = type,
            amount = amount,
            currency = "USD",
            maskedCard = maskedCard,
            referenceNumber = null,
            balanceAfter = null,
            confidence = confidence.coerceIn(50, 99)
        )
    }

    fun generateSmsHash(sender: String, timestamp: Long, amount: Double): String {
        val raw = "$sender-$timestamp-$amount"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(24)
    }

    fun toSmsDetectionEntity(
        id: String,
        profileId: String,
        sender: String,
        bodySnippet: String,
        timestamp: Long,
        result: ParsedSmsResult
    ): SmsDetectionEntity {
        return SmsDetectionEntity(
            id = id,
            profileId = profileId,
            sender = sender,
            snippet = bodySnippet.take(120),
            detectedBank = result.bankName,
            detectedType = result.transactionType,
            detectedAmount = result.amount,
            detectedCurrency = result.currency,
            detectedTime = timestamp,
            maskedCard = result.maskedCard,
            referenceNumber = result.referenceNumber,
            balanceAfter = result.balanceAfter,
            confidence = result.confidence,
            status = SmsDetectionStatus.PENDING.name
        )
    }
}
