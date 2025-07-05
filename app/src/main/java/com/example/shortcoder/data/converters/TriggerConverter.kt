package com.example.shortcoder.data.converters

import androidx.room.TypeConverter
import com.example.shortcoder.data.models.AutomationTrigger
import com.google.gson.Gson

class TriggerConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromTrigger(trigger: AutomationTrigger): String {
        return gson.toJson(trigger)
    }

    @TypeConverter
    fun toTrigger(triggerString: String): AutomationTrigger {
        return gson.fromJson(triggerString, AutomationTrigger::class.java)
    }
} 