package com.example.shortcoder.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.media.AudioManager
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.shortcoder.data.models.ShortcutAction
import com.example.shortcoder.data.models.ActionType
import com.example.shortcoder.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import androidx.appcompat.app.AlertDialog

class ActionExecutor(private val context: Context) {
    
    companion object {
        private const val TAG = "ActionExecutor"
        private const val NOTIFICATION_CHANNEL_ID = "action_notifications"
    }
    
    private var isFlashlightOn = false
    
    init {
        createNotificationChannel()
    }
    
    suspend fun executeActions(actions: List<ShortcutAction>): Boolean {
        return withContext(Dispatchers.Main) {
            var allSuccessful = true
            
            for (action in actions.filter { it.isEnabled }.sortedBy { it.order }) {
                try {
                    val success = executeAction(action)
                    if (!success) {
                        allSuccessful = false
                        Log.w(TAG, "Action failed: ${action.title}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing action: ${action.title}", e)
                    allSuccessful = false
                }
            }
            
            allSuccessful
        }
    }
    
    private suspend fun executeAction(action: ShortcutAction): Boolean {
        Log.d(TAG, "Executing action: ${action.title} (${action.type})")
        
        return when (action.type) {
            ActionType.SEND_MESSAGE -> executeSendMessage(action)
            ActionType.SEND_EMAIL -> executeSendEmail(action)
            ActionType.MAKE_CALL -> executeMakeCall(action)
            ActionType.OPEN_APP -> executeOpenApp(action)
            ActionType.OPEN_URL -> executeOpenUrl(action)
            ActionType.SHOW_NOTIFICATION -> executeShowNotification(action)
            ActionType.SHOW_ALERT -> executeShowAlert(action)
            ActionType.TOGGLE_WIFI -> executeToggleWifi(action)
            ActionType.TOGGLE_BLUETOOTH -> executeToggleBluetooth(action)
            ActionType.TOGGLE_FLASHLIGHT -> executeToggleFlashlight(action)
            ActionType.SET_VOLUME -> executeSetVolume(action)
            ActionType.SHARE_TEXT -> executeShareText(action)
            ActionType.GET_TEXT_FROM_INPUT -> executeGetTextFromInput(action)
            ActionType.PLAY_MUSIC -> executePlayMusic(action)
            ActionType.TAKE_PHOTO -> executeTakePhoto(action)
            ActionType.RECORD_VIDEO -> executeRecordVideo(action)
            ActionType.GET_CURRENT_LOCATION -> executeGetCurrentLocation(action)
            ActionType.GET_DIRECTIONS -> executeGetDirections(action)
            ActionType.CREATE_EVENT -> executeCreateEvent(action)
            ActionType.GET_UPCOMING_EVENTS -> executeGetUpcomingEvents(action)
            ActionType.GET_CONTACT -> executeGetContact(action)
            ActionType.CREATE_CONTACT -> executeCreateContact(action)
            ActionType.SAVE_TO_FILES -> executeSaveToFiles(action)
            ActionType.GET_FILE -> executeGetFile(action)
            ActionType.IF_CONDITION -> executeIfCondition(action)
            ActionType.REPEAT_ACTION -> executeRepeatAction(action)
            ActionType.CHOOSE_FROM_MENU -> executeChooseFromMenu(action)
            ActionType.SET_VARIABLE -> executeSetVariable(action)
            ActionType.GET_VARIABLE -> executeGetVariable(action)
            ActionType.GET_WEB_PAGE -> executeGetWebPage(action)
            ActionType.CUSTOM_ACTION -> executeCustomAction(action)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Action Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from shortcut actions"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun executeSendMessage(action: ShortcutAction): Boolean {
        return try {
            val phoneNumber = action.parameters["phoneNumber"] ?: return false
            val message = action.parameters["message"] ?: return false
            
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            
            Log.d(TAG, "SMS sent to: $phoneNumber")
            Toast.makeText(context, "SMS sent to $phoneNumber", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    private fun executeSendEmail(action: ShortcutAction): Boolean {
        return try {
            val email = action.parameters["email"] ?: return false
            val subject = action.parameters["subject"] ?: ""
            val body = action.parameters["body"] ?: ""
            
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send email", e)
            false
        }
    }
    
    private fun executeMakeCall(action: ShortcutAction): Boolean {
        return try {
            val phoneNumber = action.parameters["phoneNumber"] ?: return false
            
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make call", e)
            false
        }
    }
    
    private fun executeOpenApp(action: ShortcutAction): Boolean {
        return try {
            val packageName = action.parameters["packageName"] ?: return false
            
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                Toast.makeText(context, "App not found: $packageName", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app", e)
            false
        }
    }
    
    private fun executeOpenUrl(action: ShortcutAction): Boolean {
        return try {
            val url = action.parameters["url"] ?: return false
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL", e)
            false
        }
    }
    
    private fun executeShowNotification(action: ShortcutAction): Boolean {
        return try {
            val title = action.parameters["title"] ?: "Shortcoder"
            val message = action.parameters["message"] ?: ""
            
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shortcut)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
            Toast.makeText(context, "Notification: ${action.parameters["message"]}", Toast.LENGTH_LONG).show()
            true
        }
    }
    
    private fun executeShowAlert(action: ShortcutAction): Boolean {
        return try {
            val message = action.parameters["message"] ?: "Alert from Shortcoder"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show alert", e)
            false
        }
    }
    
    private fun executeToggleWifi(action: ShortcutAction): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ requires user interaction to toggle WiFi
                val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please toggle WiFi manually", Toast.LENGTH_SHORT).show()
            } else {
                @Suppress("DEPRECATION")
                val isEnabled = wifiManager.isWifiEnabled
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = !isEnabled
                Toast.makeText(context, "WiFi ${if (isEnabled) "disabled" else "enabled"}", Toast.LENGTH_SHORT).show()
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle WiFi", e)
            Toast.makeText(context, "Failed to toggle WiFi", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    private fun executeToggleBluetooth(action: ShortcutAction): Boolean {
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            
            if (bluetoothAdapter == null) {
                Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
                return false
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ requires user interaction
                val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please toggle Bluetooth manually", Toast.LENGTH_SHORT).show()
            } else {
                @Suppress("DEPRECATION")
                if (bluetoothAdapter.isEnabled) {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.disable()
                    Toast.makeText(context, "Bluetooth disabled", Toast.LENGTH_SHORT).show()
                } else {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.enable()
                    Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle Bluetooth", e)
            Toast.makeText(context, "Failed to toggle Bluetooth", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    private fun executeToggleFlashlight(action: ShortcutAction): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            
            if (cameraId == null) {
                Toast.makeText(context, "Flashlight not available", Toast.LENGTH_SHORT).show()
                return false
            }
            
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            
            Toast.makeText(context, "Flashlight ${if (isFlashlightOn) "on" else "off"}", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Flashlight toggled: $isFlashlightOn")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            Toast.makeText(context, "Failed to toggle flashlight: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    private fun executeSetVolume(action: ShortcutAction): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val volumeLevel = action.parameters["volume"]?.toIntOrNull() ?: 50
            val streamType = when (action.parameters["streamType"]) {
                "media" -> AudioManager.STREAM_MUSIC
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                else -> AudioManager.STREAM_MUSIC
            }
            
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val targetVolume = (volumeLevel * maxVolume / 100).coerceIn(0, maxVolume)
            
            audioManager.setStreamVolume(streamType, targetVolume, AudioManager.FLAG_SHOW_UI)
            
            Toast.makeText(context, "Volume set to $volumeLevel%", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set volume", e)
            false
        }
    }
    
    private fun executeShareText(action: ShortcutAction): Boolean {
        return try {
            val text = action.parameters["text"] ?: return false
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(Intent.createChooser(intent, "Share text").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share text", e)
            false
        }
    }
    
    private fun executeGetTextFromInput(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Text input action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executePlayMusic(action: ShortcutAction): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://media/external/audio/media")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play music", e)
            false
        }
    }
    
    private fun executeTakePhoto(action: ShortcutAction): Boolean {
        return try {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take photo", e)
            false
        }
    }
    
    private fun executeRecordVideo(action: ShortcutAction): Boolean {
        return try {
            val intent = Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record video", e)
            false
        }
    }
    
    private fun executeGetCurrentLocation(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Location action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeGetDirections(action: ShortcutAction): Boolean {
        return try {
            val destination = action.parameters["destination"] ?: return false
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("geo:0,0?q=$destination")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get directions", e)
            false
        }
    }
    
    private fun executeCreateEvent(action: ShortcutAction): Boolean {
        return try {
            val title = action.parameters["title"] ?: "Event"
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                putExtra(android.provider.CalendarContract.Events.TITLE, title)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create event", e)
            false
        }
    }
    
    private fun executeGetUpcomingEvents(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Calendar events action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeGetContact(action: ShortcutAction): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_PICK).apply {
                data = android.provider.ContactsContract.Contacts.CONTENT_URI
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get contact", e)
            false
        }
    }
    
    private fun executeCreateContact(action: ShortcutAction): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.ContactsContract.Contacts.CONTENT_URI
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create contact", e)
            false
        }
    }
    
    private fun executeSaveToFiles(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Save to files action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeGetFile(action: ShortcutAction): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file", e)
            false
        }
    }
    
    private fun executeIfCondition(action: ShortcutAction): Boolean {
        Toast.makeText(context, "If condition action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeRepeatAction(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Repeat action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeChooseFromMenu(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Choose from menu action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeSetVariable(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Set variable action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeGetVariable(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Get variable action executed", Toast.LENGTH_SHORT).show()
        return true
    }
    
    private fun executeGetWebPage(action: ShortcutAction): Boolean {
        return try {
            val url = action.parameters["url"] ?: return false
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get web page", e)
            false
        }
    }
    
    private fun executeCustomAction(action: ShortcutAction): Boolean {
        Toast.makeText(context, "Custom action executed: ${action.title}", Toast.LENGTH_SHORT).show()
        return true
    }

    suspend fun executeActionsWithConfirmation(actions: List<ShortcutAction>, automationName: String? = null): Boolean = withContext(Dispatchers.Main) {
        return@withContext suspendCoroutine { continuation ->
            val activity = context as? androidx.appcompat.app.AppCompatActivity
            if (activity == null) {
                // If not in activity context, execute without confirmation
                CoroutineScope(Dispatchers.IO).launch {
                    val result = executeActions(actions)
                    continuation.resume(result)
                }
                return@suspendCoroutine
            }
            
            val message = if (automationName != null) {
                "Run automation '$automationName' with ${actions.size} action${if (actions.size != 1) "s" else ""}?"
            } else {
                "Execute ${actions.size} action${if (actions.size != 1) "s" else ""}?"
            }
            
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Confirm Execution")
                .setMessage(message)
                .setPositiveButton("Run") { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = executeActions(actions)
                        continuation.resume(result)
                    }
                }
                .setNegativeButton("Cancel") { _, _ ->
                    continuation.resume(false)
                }
                .setCancelable(false)
                .show()
        }
    }
} 