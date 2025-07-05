package com.example.shortcoder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.shortcoder.workers.SmsForwardingWorker
import com.example.shortcoder.workers.AutomationTriggerWorker

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received")
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val messageBody = message.messageBody ?: ""
                val timestamp = message.timestampMillis
                
                Log.d(TAG, "SMS from: $sender, Message: $messageBody")
                
                // Handle SMS forwarding
                handleSmsForwarding(context, sender, messageBody, timestamp)
                
                // Trigger SMS-based automations
                triggerSmsAutomations(context, sender, messageBody, timestamp)
            }
        }
    }
    
    private fun handleSmsForwarding(context: Context, sender: String, message: String, timestamp: Long) {
        val workRequest = OneTimeWorkRequestBuilder<SmsForwardingWorker>()
            .setInputData(workDataOf(
                "sender" to sender,
                "message" to message,
                "timestamp" to timestamp
            ))
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
    
    private fun triggerSmsAutomations(context: Context, sender: String, message: String, timestamp: Long) {
        val workRequest = OneTimeWorkRequestBuilder<AutomationTriggerWorker>()
            .setInputData(workDataOf(
                "triggerType" to "RECEIVE_MESSAGE",
                "sender" to sender,
                "message" to message,
                "timestamp" to timestamp
            ))
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
} 