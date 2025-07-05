package com.example.shortcoder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.shortcoder.services.AutomationService
import com.example.shortcoder.utils.BackgroundExecutionManager

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Device boot/restart completed or app updated, setting up background execution")
                
                // Use BackgroundExecutionManager for comprehensive setup
                val backgroundManager = BackgroundExecutionManager(context)
                backgroundManager.setupBackgroundExecution()
                
                Log.d(TAG, "Background execution setup completed")
            }
        }
    }
} 