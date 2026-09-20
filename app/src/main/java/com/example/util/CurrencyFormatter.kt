package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

object CurrencyFormatter {

    fun format(amount: Double, currency: String, usePersianDigits: Boolean = false): String {
        val bd = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)

        return when (currency.uppercase()) {
            "TOMAN", "IRR" -> {
                // Toman uses integers without decimals
                val intVal = bd.setScale(0, RoundingMode.HALF_UP).toLong()
                val symbols = DecimalFormatSymbols(Locale.US)
                val formatter = DecimalFormat("#,###", symbols)
                val formatted = formatter.format(intVal)
                val output = if (usePersianDigits) toPersianDigits(formatted) else formatted
                "$output تومان"
            }
            "USD" -> {
                val symbols = DecimalFormatSymbols(Locale.US)
                val formatter = DecimalFormat("$#,##0.00", symbols)
                formatter.format(bd.toDouble())
            }
            else -> {
                val symbols = DecimalFormatSymbols(Locale.US)
                val formatter = DecimalFormat("#,##0.00", symbols)
                "${formatter.format(bd.toDouble())} $currency"
            }
        }
    }

    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (c in input) {
            if (c in '0'..'9') {
                sb.append(persianDigits[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun fromPersianDigits(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when (c) {
                '۰' -> sb.append('0')
                '۱' -> sb.append('1')
                '۲' -> sb.append('2')
                '۳' -> sb.append('3')
                '۴' -> sb.append('4')
                '۵' -> sb.append('5')
                '۶' -> sb.append('6')
                '۷' -> sb.append('7')
                '۸' -> sb.append('8')
                '۹' -> sb.append('9')
                '٫' -> sb.append('.')
                '٬' -> sb.append(',')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}

object DateHelper {
    private val standardDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val standardTimeFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val friendlyDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    fun formatDate(timestamp: Long, isPersian: Boolean): String {
        if (isPersian) {
            val jalali = JalaliCalendar.gregorianToJalali(timestamp)
            return "${jalali.year}/${jalali.month}/${jalali.day}"
        }
        return friendlyDateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return standardTimeFormat.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long, isPersian: Boolean): String {
        return "${formatDate(timestamp, isPersian)} ${formatTime(timestamp)}"
    }

    fun getCurrentMonthYearKey(): String {
        val cal = Calendar.getInstance()
        return String.format(Locale.US, "%d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun getMonthStartEndTimestamps(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        return Pair(start, end)
    }
}

// Lightweight, precise Jalali / Solar calendar conversion algorithm
object JalaliCalendar {
    data class JalaliDate(val year: Int, val month: Int, val day: Int)

    fun gregorianToJalali(timestamp: Long): JalaliDate {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1
        val gDay = cal.get(Calendar.DAY_OF_MONTH)

        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = gYear - 1600
        var gm = gMonth - 1
        var gd = gDay - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400

        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..10) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i
                break
            }
            jDayNo -= jDaysInMonth[i]
        }
        val jd = jDayNo + 1
        return JalaliDate(jy, jm + 1, jd)
    }
}
