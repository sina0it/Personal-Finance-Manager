package com.example.sms

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsInboxScanner {

    suspend fun scanInbox(context: Context, profileId: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("_id", "address", "body", "date"),
                null,
                null,
                "date DESC LIMIT 150"
            )

            val db = AppDatabase.getInstance(context)

            cursor?.use {
                val addressCol = it.getColumnIndex("address")
                val bodyCol = it.getColumnIndex("body")
                val dateCol = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = if (addressCol >= 0) it.getString(addressCol) ?: "" else ""
                    val body = if (bodyCol >= 0) it.getString(bodyCol) ?: "" else ""
                    val date = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()

                    val parsed = BankSmsParser.parse(address, body, profileId)
                    if (parsed != null && parsed.isValidFinancial) {
                        val hashId = BankSmsParser.generateSmsHash(address, date, parsed.amount)
                        val entity = BankSmsParser.toSmsDetectionEntity(
                            id = hashId,
                            profileId = profileId,
                            sender = address,
                            bodySnippet = body,
                            timestamp = date,
                            result = parsed
                        )
                        val inserted = db.smsDetectionDao().insertIfNotExists(entity)
                        if (inserted != -1L) {
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Permission or content resolver issue handled safely
        }
        count
    }
}
