package com.example.shortcoder.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.shortcoder.data.models.*
import com.example.shortcoder.data.dao.*
import com.example.shortcoder.data.converters.*

@Database(
    entities = [
        Shortcut::class,
        Automation::class,
        SmsForwardingRule::class,
        SmsForwardingSettings::class,
        ForwardedMessageLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ActionListConverter::class, TriggerConverter::class)
abstract class ShortcoderDatabase : RoomDatabase() {
    
    abstract fun shortcutDao(): ShortcutDao
    abstract fun automationDao(): AutomationDao
    abstract fun smsForwardingDao(): SmsForwardingDao
    
    companion object {
        @Volatile
        private var INSTANCE: ShortcoderDatabase? = null
        
        fun getDatabase(context: Context): ShortcoderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShortcoderDatabase::class.java,
                    "shortcoder_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 