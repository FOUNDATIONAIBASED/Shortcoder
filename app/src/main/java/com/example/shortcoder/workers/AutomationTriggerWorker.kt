package com.example.shortcoder.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shortcoder.R
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.TriggerType
import com.example.shortcoder.data.models.AutomationTrigger
import com.example.shortcoder.data.models.ShortcutAction
import com.example.shortcoder.engine.ActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class AutomationTriggerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "AutomationTriggerWorker"
    }
    
    private val database = ShortcoderDatabase.getDatabase(context)
    private val automationDao = database.automationDao()
    private val actionExecutor = ActionExecutor(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val triggerTypeString = inputData.getString("triggerType") ?: return@withContext Result.failure()
            
            Log.d(TAG, "Processing automation trigger: $triggerTypeString")
            
            // Check if this trigger matches any automation
            val automations = database.automationDao().getEnabledAutomationsList()
            
            for (automation in automations) {
                val trigger = automation.trigger
                var shouldTrigger = false
                
                when (trigger.type) {
                    TriggerType.RECEIVE_MESSAGE -> {
                        if (triggerTypeString == "RECEIVE_MESSAGE") {
                            shouldTrigger = checkSmsMatch(trigger, inputData.getString("message"), inputData.getString("sender"))
                        }
                    }
                    TriggerType.BATTERY_LEVEL -> {
                        if (triggerTypeString == "battery") {
                            val targetLevel = trigger.parameters["level"]?.toIntOrNull() ?: 0
                            val currentLevel = inputData.getInt("battery_level", 0)
                            shouldTrigger = currentLevel <= targetLevel
                        }
                    }
                    else -> {
                        // Handle other trigger types
                        shouldTrigger = triggerTypeString == trigger.type.name
                    }
                }
                
                if (shouldTrigger) {
                    Log.d(TAG, "Executing automation: ${automation.name}")
                    executeAutomationWithConfirmation(automation)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in automation trigger processing", e)
            Result.failure()
        }
    }

    private fun checkSmsMatch(trigger: AutomationTrigger, message: String?, sender: String?): Boolean {
        val senderFilter = trigger.parameters["sender"]
        val keywordFilter = trigger.parameters["keyword"]
        
        // Check sender filter
        if (!senderFilter.isNullOrEmpty() && sender != senderFilter) {
            return false
        }
        
        // Check keyword filter
        if (!keywordFilter.isNullOrEmpty() && (message == null || !message.contains(keywordFilter, ignoreCase = true))) {
            return false
        }
        
        return true
    }

    private suspend fun executeAutomationActions(actions: List<ShortcutAction>) {
        try {
            if (actions.isEmpty()) return
            
            // Execute actions - for background execution, we skip confirmation
            val success = actionExecutor.executeActions(actions)
            
            if (success) {
                Log.d(TAG, "Automation executed successfully")
            } else {
                Log.w(TAG, "Automation execution failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing automation", e)
        }
    }

    // Add method to handle confirmation-required automations
    private suspend fun executeAutomationWithConfirmation(automation: com.example.shortcoder.data.models.Automation) {
        try {
            if (automation.actions.isEmpty()) return
            
            // For confirmation-required automations, show notification instead of executing directly
            if (automation.requiresConfirmation) {
                showConfirmationNotification(automation)
                return
            }
            
            // Execute actions directly for non-confirmation automations
            val success = actionExecutor.executeActions(automation.actions)
            
            if (success) {
                Log.d(TAG, "Automation executed successfully: ${automation.name}")
                // Update run count
                database.automationDao().incrementRunCount(automation.id)
            } else {
                Log.w(TAG, "Automation execution failed: ${automation.name}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing automation: ${automation.name}", e)
        }
    }

    private suspend fun showConfirmationNotification(automation: com.example.shortcoder.data.models.Automation) {
        // Create a notification asking user to confirm automation execution
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(applicationContext, com.example.shortcoder.activities.AutomationConfirmationActivity::class.java).apply {
            putExtra("automation_id", automation.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            automation.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, "automation")
            .setSmallIcon(R.drawable.ic_automation)
            .setContentTitle("Automation Ready")
            .setContentText("${automation.name} is ready to run. Tap to confirm.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(automation.id.hashCode(), notification)
    }
} 