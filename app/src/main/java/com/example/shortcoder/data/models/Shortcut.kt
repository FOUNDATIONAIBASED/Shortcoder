package com.example.shortcoder.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.shortcoder.data.converters.ActionListConverter
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "shortcuts")
@TypeConverters(ActionListConverter::class)
@Parcelize
data class Shortcut(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val iconColor: String = "#2196F3", // Material Blue
    val iconName: String = "shortcut",
    val actions: List<ShortcutAction> = emptyList(),
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val runCount: Int = 0,
    val lastRun: Long = 0L,
    val tags: List<String> = emptyList(),
    val isInShareSheet: Boolean = false,
    val requiresConfirmation: Boolean = false
) : Parcelable

@Parcelize
data class ShortcutAction(
    val id: String,
    val type: ActionType,
    val title: String,
    val parameters: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    val order: Int = 0
) : Parcelable

enum class ActionType {
    // Communication Actions
    SEND_MESSAGE,
    SEND_EMAIL,
    MAKE_CALL,
    
    // Media Actions
    PLAY_MUSIC,
    TAKE_PHOTO,
    RECORD_VIDEO,
    
    // System Actions
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    SET_VOLUME,
    TOGGLE_FLASHLIGHT,
    
    // App Actions
    OPEN_APP,
    SHARE_TEXT,
    
    // Web Actions
    OPEN_URL,
    GET_WEB_PAGE,
    
    // Text Actions
    GET_TEXT_FROM_INPUT,
    SHOW_NOTIFICATION,
    SHOW_ALERT,
    
    // Location Actions
    GET_CURRENT_LOCATION,
    GET_DIRECTIONS,
    
    // Calendar Actions
    CREATE_EVENT,
    GET_UPCOMING_EVENTS,
    
    // Contacts Actions
    GET_CONTACT,
    CREATE_CONTACT,
    
    // File Actions
    SAVE_TO_FILES,
    GET_FILE,
    
    // Logic Actions
    IF_CONDITION,
    REPEAT_ACTION,
    CHOOSE_FROM_MENU,
    
    // Variables
    SET_VARIABLE,
    GET_VARIABLE,
    
    // Custom
    CUSTOM_ACTION
} 