package com.example.shortcoder.workers

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SmsForwardingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SmsForwardingWorker"
    }
    
    private val database = ShortcoderDatabase.getDatabase(context)
    private val smsForwardingDao = database.smsForwardingDao()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sender = inputData.getString("sender") ?: return@withContext Result.failure()
            val message = inputData.getString("message") ?: return@withContext Result.failure()
            val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())
            
            Log.d(TAG, "Processing SMS forwarding for: $sender")
            
            // Get forwarding settings and enabled rules
            val settings = smsForwardingDao.getForwardingSettings()
            val rules = smsForwardingDao.getEnabledForwardingRules()
            
            var anyForwardingDone = false
            
            // Process global forwarding if enabled
            if (settings?.isGlobalForwardingEnabled == true && settings.globalDestinationNumber.isNotEmpty()) {
                forwardMessage(
                    sender = sender,
                    message = message,
                    destinationNumber = settings.globalDestinationNumber,
                    prefix = settings.customGlobalPrefix,
                    includeOriginalSender = settings.includeOriginalSender,
                    ruleId = "global",
                    timestamp = timestamp
                )
                anyForwardingDone = true
            }
            
            // Process specific rules (these work independently of global forwarding)
            for (rule in rules) {
                if (shouldForwardMessage(rule, sender, message)) {
                    forwardMessage(
                        sender = sender,
                        message = message,
                        destinationNumber = rule.destinationNumber,
                        prefix = rule.customPrefix,
                        includeOriginalSender = rule.includeOriginalSender,
                        ruleId = rule.id,
                        timestamp = timestamp
                    )
                    
                    // Increment forward count for this rule
                    smsForwardingDao.incrementForwardCount(rule.id, timestamp)
                    anyForwardingDone = true
                }
            }
            
            if (anyForwardingDone) {
                Log.d(TAG, "SMS forwarding completed for: $sender")
            } else {
                Log.d(TAG, "No forwarding rules matched for: $sender")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS forwarding", e)
            Result.failure()
        }
    }
    
    private fun shouldForwardMessage(rule: SmsForwardingRule, sender: String, message: String): Boolean {
        // Check quiet hours
        if (rule.isQuietHoursEnabled && isInQuietHours(rule.quietHoursStart, rule.quietHoursEnd)) {
            return false
        }
        
        // Check screen state if needed
        if (rule.onlyWhenScreenOff && isScreenOn()) {
            return false
        }
        
        return when (rule.ruleType) {
            ForwardingRuleType.FORWARD_ALL -> true
            ForwardingRuleType.FORWARD_FROM_SPECIFIC -> rule.sourceNumbers.contains(sender)
            ForwardingRuleType.FORWARD_EXCEPT_SPECIFIC -> !rule.sourceNumbers.contains(sender)
            ForwardingRuleType.FORWARD_CONTAINING_KEYWORDS -> {
                rule.sourceNumbers.any { keyword -> message.contains(keyword, ignoreCase = true) }
            }
            ForwardingRuleType.FORWARD_NOT_CONTAINING_KEYWORDS -> {
                !rule.sourceNumbers.any { keyword -> message.contains(keyword, ignoreCase = true) }
            }
        }
    }
    
    private suspend fun forwardMessage(
        sender: String,
        message: String,
        destinationNumber: String,
        prefix: String,
        includeOriginalSender: Boolean,
        ruleId: String,
        timestamp: Long
    ) {
        try {
            val forwardedMessage = buildForwardedMessage(sender, message, prefix, includeOriginalSender)
            
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(destinationNumber, null, forwardedMessage, null, null)
            
            // Log the forwarded message
            val log = ForwardedMessageLog(
                id = UUID.randomUUID().toString(),
                originalSender = sender,
                originalMessage = message,
                destinationNumber = destinationNumber,
                forwardedMessage = forwardedMessage,
                ruleId = ruleId,
                timestamp = timestamp,
                wasSuccessful = true
            )
            
            smsForwardingDao.insertForwardedMessageLog(log)
            
            Log.d(TAG, "SMS forwarded successfully to: $destinationNumber")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward SMS to: $destinationNumber", e)
            
            // Log the failed attempt
            val log = ForwardedMessageLog(
                id = UUID.randomUUID().toString(),
                originalSender = sender,
                originalMessage = message,
                destinationNumber = destinationNumber,
                forwardedMessage = "",
                ruleId = ruleId,
                timestamp = timestamp,
                wasSuccessful = false,
                errorMessage = e.message ?: "Unknown error"
            )
            
            smsForwardingDao.insertForwardedMessageLog(log)
        }
    }
    
    private fun buildForwardedMessage(
        sender: String,
        message: String,
        prefix: String,
        includeOriginalSender: Boolean
    ): String {
        return buildString {
            if (prefix.isNotEmpty()) {
                append(prefix)
                append(" ")
            }
            
            if (includeOriginalSender) {
                append("From: $sender - ")
            }
            
            append(message)
        }
    }
    
    private fun isInQuietHours(startTime: String, endTime: String): Boolean {
        if (startTime.isEmpty() || endTime.isEmpty()) return false
        
        try {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentTimeInMinutes = currentHour * 60 + currentMinute
            
            val startParts = startTime.split(":")
            val startTimeInMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            
            val endParts = endTime.split(":")
            val endTimeInMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            
            return if (startTimeInMinutes <= endTimeInMinutes) {
                currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
            } else {
                // Quiet hours span midnight
                currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes <= endTimeInMinutes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing quiet hours", e)
            return false
        }
    }
    
    private fun isScreenOn(): Boolean {
        // This would require additional implementation to check screen state
        // For now, we'll assume screen is on
        return true
    }
} 