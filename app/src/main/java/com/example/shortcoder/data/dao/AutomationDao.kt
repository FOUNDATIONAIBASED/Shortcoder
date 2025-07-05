package com.example.shortcoder.data.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.shortcoder.data.models.Automation
import com.example.shortcoder.data.models.TriggerType
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    
    @Query("SELECT * FROM automations ORDER BY lastModified DESC")
    fun getAllAutomations(): Flow<List<Automation>>
    
    @Query("SELECT * FROM automations ORDER BY lastModified DESC")
    fun getAllAutomationsLiveData(): LiveData<List<Automation>>
    
    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getAutomationById(id: String): Automation?
    
    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    fun getEnabledAutomations(): Flow<List<Automation>>
    
    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    suspend fun getEnabledAutomationsList(): List<Automation>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: Automation)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomations(automations: List<Automation>)
    
    @Update
    suspend fun updateAutomation(automation: Automation)
    
    @Delete
    suspend fun deleteAutomation(automation: Automation)
    
    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun deleteAutomationById(id: String)
    
    @Query("UPDATE automations SET runCount = runCount + 1, lastRun = :timestamp WHERE id = :id")
    suspend fun incrementRunCount(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE automations SET isEnabled = :enabled WHERE id = :id")
    suspend fun setAutomationEnabled(id: String, enabled: Boolean)
    
    @Query("UPDATE automations SET isRunning = :running WHERE id = :id")
    suspend fun setAutomationRunning(id: String, running: Boolean)
    
    @Query("SELECT COUNT(*) FROM automations")
    suspend fun getAutomationCount(): Int
    
    @Query("SELECT * FROM automations WHERE isEnabled = 1 AND JSON_EXTRACT(trigger, '$.type') = :triggerType")
    suspend fun getAutomationsByTriggerType(triggerType: String): List<Automation>
    
    @Query("SELECT * FROM automations ORDER BY runCount DESC LIMIT :limit")
    fun getMostUsedAutomations(limit: Int = 5): Flow<List<Automation>>
} 