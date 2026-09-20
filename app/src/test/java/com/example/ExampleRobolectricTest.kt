package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sms.BankSmsParser
import com.example.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Sina Finance", appName)
    }

    @Test
    fun `currency formatting works accurately`() {
        val tomanStr = CurrencyFormatter.format(150000.0, "TOMAN")
        assertTrue(tomanStr.contains("150,000"))
        assertTrue(tomanStr.contains("تومان"))

        val usdStr = CurrencyFormatter.format(1250.50, "USD")
        assertEquals("$1,250.50", usdStr)
    }

    @Test
    fun `bank sms parser parses transaction accurately`() {
        val rawSms = "واریز مبلغ 250,000 ریال به حساب بانک ملی. مانده: 1,500,000 ریال"
        val parsed = BankSmsParser.parse("BankMelli", rawSms, "IRAN_TOMAN")
        assertNotNull(parsed)
        assertEquals(25000.0, parsed!!.amount, 0.01) // 250,000 Rials = 25,000 Toman
        assertEquals("INCOME", parsed.transactionType)
    }
}
