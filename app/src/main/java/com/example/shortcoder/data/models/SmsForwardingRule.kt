package com.example.shortcoder.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "sms_forwarding_rules")
@Parcelize
data class SmsForwardingRule(
    @PrimaryKey
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,
    val ruleType: ForwardingRuleType,
    val sourceNumbers: List<String> = emptyList(), // Empty means all numbers
    val destinationNumber: String,
    val includeOriginalSender: Boolean = true,
    val customPrefix: String = "",
    val onlyWhenScreenOff: Boolean = false,
    val quietHoursStart: String = "", // Format: "HH:mm"
    val quietHoursEnd: String = "", // Format: "HH:mm"
    val isQuietHoursEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val forwardCount: Int = 0,
    val lastForwarded: Long = 0L
) : Parcelable

enum class ForwardingRuleType {
    FORWARD_ALL, // Forward all SMS messages
    FORWARD_FROM_SPECIFIC, // Forward only from specific numbers
    FORWARD_EXCEPT_SPECIFIC, // Forward all except from specific numbers (blocking)
    FORWARD_CONTAINING_KEYWORDS, // Forward messages containing specific keywords
    FORWARD_NOT_CONTAINING_KEYWORDS // Forward messages not containing specific keywords
}

@Entity(tableName = "sms_forwarding_settings")
@Parcelize
data class SmsForwardingSettings(
    @PrimaryKey
    val id: String = "default",
    val isGlobalForwardingEnabled: Boolean = false,
    val globalDestinationNumber: String = "",
    val includeOriginalSender: Boolean = true,
    val customGlobalPrefix: String = "[Forwarded]",
    val logForwardedMessages: Boolean = true,
    val requireConfirmationForNewRules: Boolean = true,
    val lastModified: Long = System.currentTimeMillis()
) : Parcelable

@Entity(tableName = "forwarded_messages_log")
@Parcelize
data class ForwardedMessageLog(
    @PrimaryKey
    val id: String,
    val originalSender: String,
    val originalMessage: String,
    val destinationNumber: String,
    val forwardedMessage: String,
    val ruleId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val wasSuccessful: Boolean = true,
    val errorMessage: String = ""
) : Parcelable 