package com.example.shortcoder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.shortcoder.MainActivity
import com.example.shortcoder.R
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.engine.AutomationEngine
import kotlinx.coroutines.*

class AutomationService : Service() {
    
    companion object {
        private const val TAG = "AutomationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "automation_service_channel"
        private const val CHANNEL_NAME = "Automation Service"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var automationEngine: AutomationEngine
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AutomationService created")
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Initialize automation engine
        val database = ShortcoderDatabase.getDatabase(this)
        automationEngine = AutomationEngine(this, database)
        
        // Start as foreground service immediately
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AutomationService started")
        
        // Ensure we're running as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start automation engine
        serviceScope.launch {
            automationEngine.start()
        }
        
        // Return START_STICKY to ensure service restarts if killed
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "AutomationService destroyed")
        
        // Stop automation engine
        serviceScope.launch {
            automationEngine.stop()
        }
        
        // Cancel all coroutines
        serviceScope.cancel()
        
        super.onDestroy()
        
        // Restart the service if it was killed
        restartService()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed, ensuring service continues")
        
        // Update notification to show service is running in background
        val notification = createNotification(isTaskRemoved = true)
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Don't call super.onTaskRemoved() to prevent service from being killed
        // The service should continue running even when task is removed
    }
    
    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Shortcoder running in background for SMS forwarding and automation"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create persistent notification for foreground service
     */
    private fun createNotification(isTaskRemoved: Boolean = false): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = "Shortcoder Active"
        val text = if (isTaskRemoved) {
            "Running in background • SMS forwarding and automation active"
        } else {
            "SMS forwarding and automation ready"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shortcuts)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setLocalOnly(true)
            .build()
    }
    
    /**
     * Restart the service if it gets killed
     */
    private fun restartService() {
        serviceScope.launch {
            delay(1000) // Wait a second before restarting
            
            try {
                val restartIntent = Intent(this@AutomationService, AutomationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restartIntent)
                } else {
                    startService(restartIntent)
                }
                Log.d(TAG, "Service restart initiated")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart service", e)
            }
        }
    }
    
    /**
     * Update notification with current status
     */
    fun updateNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shortcoder Active")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_shortcuts)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setLocalOnly(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
} 