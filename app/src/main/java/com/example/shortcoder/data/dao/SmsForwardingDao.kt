package com.example.shortcoder.data.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.shortcoder.data.models.SmsForwardingRule
import com.example.shortcoder.data.models.SmsForwardingSettings
import com.example.shortcoder.data.models.ForwardedMessageLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsForwardingDao {
    
    // SMS Forwarding Rules
    @Query("SELECT * FROM sms_forwarding_rules ORDER BY lastModified DESC")
    fun getAllForwardingRules(): Flow<List<SmsForwardingRule>>
    
    @Query("SELECT * FROM sms_forwarding_rules ORDER BY lastModified DESC")
    fun getAllForwardingRulesLiveData(): LiveData<List<SmsForwardingRule>>
    
    @Query("SELECT * FROM sms_forwarding_rules WHERE id = :id")
    suspend fun getForwardingRuleById(id: String): SmsForwardingRule?
    
    @Query("SELECT * FROM sms_forwarding_rules WHERE isEnabled = 1 ORDER BY lastModified DESC")
    suspend fun getEnabledForwardingRules(): List<SmsForwardingRule>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardingRule(rule: SmsForwardingRule)
    
    @Update
    suspend fun updateForwardingRule(rule: SmsForwardingRule)
    
    @Delete
    suspend fun deleteForwardingRule(rule: SmsForwardingRule)
    
    @Query("DELETE FROM sms_forwarding_rules WHERE id = :id")
    suspend fun deleteForwardingRuleById(id: String)
    
    @Query("UPDATE sms_forwarding_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setForwardingRuleEnabled(id: String, enabled: Boolean)
    
    @Query("UPDATE sms_forwarding_rules SET forwardCount = forwardCount + 1, lastForwarded = :timestamp WHERE id = :id")
    suspend fun incrementForwardCount(id: String, timestamp: Long = System.currentTimeMillis())
    
    // SMS Forwarding Settings
    @Query("SELECT * FROM sms_forwarding_settings WHERE id = 'default'")
    suspend fun getForwardingSettings(): SmsForwardingSettings?
    
    @Query("SELECT * FROM sms_forwarding_settings WHERE id = 'default'")
    fun getForwardingSettingsLiveData(): LiveData<SmsForwardingSettings?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardingSettings(settings: SmsForwardingSettings)
    
    @Update
    suspend fun updateForwardingSettings(settings: SmsForwardingSettings)
    
    // Forwarded Messages Log
    @Query("SELECT * FROM forwarded_messages_log ORDER BY timestamp DESC LIMIT :limit")
    fun getForwardedMessagesLog(limit: Int = 100): Flow<List<ForwardedMessageLog>>
    
    @Query("SELECT * FROM forwarded_messages_log WHERE ruleId = :ruleId ORDER BY timestamp DESC")
    fun getForwardedMessagesByRule(ruleId: String): Flow<List<ForwardedMessageLog>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardedMessageLog(log: ForwardedMessageLog)
    
    @Query("DELETE FROM forwarded_messages_log WHERE timestamp < :cutoffTime")
    suspend fun deleteOldForwardedMessages(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM forwarded_messages_log WHERE timestamp > :since")
    suspend fun getForwardedMessageCount(since: Long): Int
    
    @Query("DELETE FROM forwarded_messages_log")
    suspend fun clearForwardedMessagesLog()
} 