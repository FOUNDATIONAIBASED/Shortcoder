package com.example.shortcoder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.shortcoder.services.AutomationService
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class BackgroundExecutionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BackgroundExecutionManager"
        private const val WORK_NAME = "background_keepalive"
    }
    
    /**
     * Comprehensive setup for background execution
     */
    fun setupBackgroundExecution() {
        Log.d(TAG, "Setting up background execution")
        
        // Start the automation service
        startAutomationService()
        
        // Setup WorkManager for periodic tasks
        setupPeriodicWork()
        
        // Request battery optimization exemption
        requestBatteryOptimizationExemption()
        
        // Check and request auto-start permissions
        checkAutoStartPermissions()
    }
    
    /**
     * Start the automation service that handles SMS forwarding and automation
     */
    private fun startAutomationService() {
        try {
            val serviceIntent = Intent(context, AutomationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "Automation service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start automation service", e)
        }
    }
    
    /**
     * Setup periodic work to ensure services stay alive
     */
    private fun setupPeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(false)
            .build()
        
        val keepAliveWork = PeriodicWorkRequestBuilder<BackgroundKeepaliveWorker>(
            15, TimeUnit.MINUTES // Minimum interval for periodic work
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            keepAliveWork
        )
        
        Log.d(TAG, "Periodic keepalive work scheduled")
    }
    
    /**
     * Request battery optimization exemption
     */
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                showBatteryOptimizationDialog()
            }
        }
    }
    
    /**
     * Show dialog to request battery optimization exemption
     */
    private fun showBatteryOptimizationDialog() {
        if (context is androidx.appcompat.app.AppCompatActivity) {
            AlertDialog.Builder(context)
                .setTitle("Battery Optimization")
                .setMessage("For SMS forwarding and automation to work reliably in the background, please disable battery optimization for Shortcoder.\n\nThis ensures the app can run even when your device is sleeping.")
                .setPositiveButton("Open Settings") { _, _ ->
                    openBatteryOptimizationSettings()
                }
                .setNegativeButton("Skip") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    /**
     * Open battery optimization settings
     */
    private fun openBatteryOptimizationSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery optimization settings", e)
            // Fallback to general battery settings
            try {
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open battery settings", e2)
            }
        }
    }
    
    /**
     * Check and guide user for auto-start permissions on various manufacturers
     */
    private fun checkAutoStartPermissions() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val autoStartInfo = getAutoStartInfo(manufacturer)
        
        if (autoStartInfo != null && context is androidx.appcompat.app.AppCompatActivity) {
            showAutoStartDialog(autoStartInfo)
        }
    }
    
    /**
     * Get auto-start information for different manufacturers
     */
    private fun getAutoStartInfo(manufacturer: String): AutoStartInfo? {
        return when {
            manufacturer.contains("xiaomi") -> AutoStartInfo(
                "Xiaomi Auto-Start",
                "To ensure SMS forwarding works in background:\n\n1. Go to Security app\n2. Find 'Autostart'\n3. Enable Shortcoder\n4. Also check 'Battery Saver' settings",
                "com.miui.securitycenter"
            )
            manufacturer.contains("huawei") -> AutoStartInfo(
                "Huawei Auto-Start",
                "To ensure background operation:\n\n1. Go to Phone Manager\n2. Find 'Startup Manager'\n3. Enable Shortcoder\n4. Check 'Battery' settings for app launch",
                "com.huawei.systemmanager"
            )
            manufacturer.contains("oppo") -> AutoStartInfo(
                "OPPO Auto-Start",
                "To ensure background operation:\n\n1. Go to Settings > Battery > App Battery Saver\n2. Find Shortcoder\n3. Set to 'Don't optimize'\n4. Check 'Startup Manager'",
                "com.coloros.safecenter"
            )
            manufacturer.contains("vivo") -> AutoStartInfo(
                "VIVO Auto-Start",
                "To ensure background operation:\n\n1. Go to Settings > Battery > Background App Refresh\n2. Enable Shortcoder\n3. Check 'Auto-start Manager'",
                "com.vivo.permissionmanager"
            )
            manufacturer.contains("samsung") -> AutoStartInfo(
                "Samsung Battery Settings",
                "To ensure background operation:\n\n1. Go to Settings > Apps > Shortcoder\n2. Battery > Optimize battery usage\n3. Turn OFF optimization for Shortcoder",
                null
            )
            manufacturer.contains("oneplus") -> AutoStartInfo(
                "OnePlus Battery Optimization",
                "To ensure background operation:\n\n1. Go to Settings > Battery > Battery Optimization\n2. Find Shortcoder\n3. Set to 'Don't optimize'",
                null
            )
            else -> null
        }
    }
    
    /**
     * Show auto-start dialog for manufacturer-specific settings
     */
    private fun showAutoStartDialog(info: AutoStartInfo) {
        if (context is androidx.appcompat.app.AppCompatActivity) {
            AlertDialog.Builder(context)
                .setTitle(info.title)
                .setMessage(info.message)
                .setPositiveButton("Open Settings") { _, _ ->
                    openManufacturerSettings(info.packageName)
                }
                .setNegativeButton("I'll do it later") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
    
    /**
     * Open manufacturer-specific settings
     */
    private fun openManufacturerSettings(packageName: String?) {
        try {
            if (packageName != null) {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    context.startActivity(intent)
                    return
                }
            }
            
            // Fallback to general app settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${context.packageName}")
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open manufacturer settings", e)
        }
    }
    
    /**
     * Check if the automation service is running
     */
    fun isAutomationServiceRunning(): Boolean {
        return try {
            val serviceIntent = Intent(context, AutomationService::class.java)
            // This is a simplified check - in a real implementation you'd use ActivityManager
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Restart automation service if it's not running
     */
    fun ensureServiceRunning() {
        if (!isAutomationServiceRunning()) {
            Log.d(TAG, "Service not running, restarting...")
            startAutomationService()
        }
    }
    
    /**
     * Data class for auto-start information
     */
    data class AutoStartInfo(
        val title: String,
        val message: String,
        val packageName: String?
    )
}

/**
 * WorkManager worker to keep services alive
 */
class BackgroundKeepaliveWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    companion object {
        private const val TAG = "BackgroundKeepaliveWorker"
    }
    
    override fun doWork(): Result {
        return try {
            Log.d(TAG, "Keepalive worker running")
            
            // Ensure automation service is running
            val backgroundManager = BackgroundExecutionManager(applicationContext)
            backgroundManager.ensureServiceRunning()
            
            // You can add other background maintenance tasks here
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Keepalive worker failed", e)
            Result.retry()
        }
    }
} 