package com.example.shortcoder.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.shortcoder.data.converters.ActionListConverter
import com.example.shortcoder.data.converters.TriggerConverter
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "automations")
@TypeConverters(ActionListConverter::class, TriggerConverter::class)
@Parcelize
data class Automation(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val trigger: AutomationTrigger,
    val actions: List<ShortcutAction> = emptyList(),
    val isEnabled: Boolean = true,
    val requiresConfirmation: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val runCount: Int = 0,
    val lastRun: Long = 0L,
    val isRunning: Boolean = false
) : Parcelable

@Parcelize
data class AutomationTrigger(
    val type: TriggerType,
    val parameters: Map<String, String> = emptyMap()
) : Parcelable

enum class TriggerType {
    // Time-based triggers
    TIME_OF_DAY,
    ALARM,
    
    // Location-based triggers
    ARRIVE_LOCATION,
    LEAVE_LOCATION,
    
    // Communication triggers
    RECEIVE_MESSAGE,
    RECEIVE_MMS,
    RECEIVE_EMAIL,
    
    // App triggers
    OPEN_APP,
    CLOSE_APP,
    
    // Device triggers
    CONNECT_BLUETOOTH,
    DISCONNECT_BLUETOOTH,
    CONNECT_WIFI,
    DISCONNECT_WIFI,
    CONNECT_CHARGER,
    DISCONNECT_CHARGER,
    BATTERY_LEVEL,
    
    // System triggers
    AIRPLANE_MODE_ON,
    AIRPLANE_MODE_OFF,
    DO_NOT_DISTURB_ON,
    DO_NOT_DISTURB_OFF,
    LOW_POWER_MODE_ON,
    LOW_POWER_MODE_OFF,
    
    // Communication specific triggers
    CALL_RECEIVED,
    CALL_MISSED,
    
    // Custom triggers
    CUSTOM_TRIGGER
} 