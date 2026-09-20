package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.sms.BankSmsParser
import com.example.sms.SmsNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefManager = PreferenceManager(context)
        val activeProfileId = prefManager.activeProfileId.value

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val profile = db.financialProfileDao().getProfile(activeProfileId)

                // Only process if user explicitly enabled SMS detection
                if (profile?.isSmsDetectionEnabled != true) {
                    return@launch
                }

                for (sms in messages) {
                    val sender = sms.displayOriginatingAddress ?: ""
                    val body = sms.displayMessageBody ?: ""
                    val timestamp = sms.timestampMillis

                    val parsed = BankSmsParser.parse(sender, body, activeProfileId)
                    if (parsed != null && parsed.isValidFinancial) {
                        val id = BankSmsParser.generateSmsHash(sender, timestamp, parsed.amount)
                        val entity = BankSmsParser.toSmsDetectionEntity(
                            id = id,
                            profileId = activeProfileId,
                            sender = sender,
                            bodySnippet = body,
                            timestamp = timestamp,
                            result = parsed
                        )

                        val rowId = db.smsDetectionDao().insertIfNotExists(entity)
                        if (rowId != -1L) {
                            // Non-intrusive notification for user review
                            SmsNotificationHelper.showDetectedNotification(
                                context = context,
                                bankName = parsed.bankName,
                                amount = parsed.amount,
                                currency = parsed.currency,
                                type = parsed.transactionType
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Safeguard against any runtime exception
            } finally {
                pendingResult.finish()
            }
        }
    }
}
