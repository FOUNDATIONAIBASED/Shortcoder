package com.example.shortcoder.data.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.shortcoder.data.models.Shortcut
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    
    @Query("SELECT * FROM shortcuts ORDER BY lastModified DESC")
    fun getAllShortcuts(): Flow<List<Shortcut>>
    
    @Query("SELECT * FROM shortcuts ORDER BY lastModified DESC")
    suspend fun getAllShortcutsList(): List<Shortcut>
    
    @Query("SELECT * FROM shortcuts ORDER BY lastModified DESC")
    fun getAllShortcutsLiveData(): LiveData<List<Shortcut>>
    
    @Query("SELECT * FROM shortcuts WHERE id = :id")
    suspend fun getShortcutById(id: String): Shortcut?
    
    @Query("SELECT * FROM shortcuts WHERE isEnabled = 1 ORDER BY lastModified DESC")
    fun getEnabledShortcuts(): Flow<List<Shortcut>>
    
    @Query("SELECT * FROM shortcuts WHERE isInShareSheet = 1 AND isEnabled = 1")
    fun getShareSheetShortcuts(): Flow<List<Shortcut>>
    
    @Query("SELECT * FROM shortcuts WHERE name LIKE :query OR description LIKE :query")
    fun searchShortcuts(query: String): Flow<List<Shortcut>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: Shortcut)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcuts(shortcuts: List<Shortcut>)
    
    @Update
    suspend fun updateShortcut(shortcut: Shortcut)
    
    @Delete
    suspend fun deleteShortcut(shortcut: Shortcut)
    
    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteShortcutById(id: String)
    
    @Query("UPDATE shortcuts SET runCount = runCount + 1, lastRun = :timestamp WHERE id = :id")
    suspend fun incrementRunCount(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE shortcuts SET isEnabled = :enabled WHERE id = :id")
    suspend fun setShortcutEnabled(id: String, enabled: Boolean)
    
    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun getShortcutCount(): Int
    
    @Query("SELECT * FROM shortcuts ORDER BY runCount DESC LIMIT :limit")
    fun getMostUsedShortcuts(limit: Int = 5): Flow<List<Shortcut>>
    
    @Query("SELECT * FROM shortcuts WHERE lastRun > 0 ORDER BY lastRun DESC LIMIT :limit")
    fun getRecentlyUsedShortcuts(limit: Int = 10): Flow<List<Shortcut>>
} 