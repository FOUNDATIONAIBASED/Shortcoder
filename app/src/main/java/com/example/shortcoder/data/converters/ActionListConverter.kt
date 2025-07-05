package com.example.shortcoder.data.converters

import androidx.room.TypeConverter
import com.example.shortcoder.data.models.ShortcutAction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ActionListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromActionList(actions: List<ShortcutAction>): String {
        return gson.toJson(actions)
    }

    @TypeConverter
    fun toActionList(actionsString: String): List<ShortcutAction> {
        val type = object : TypeToken<List<ShortcutAction>>() {}.type
        return gson.fromJson(actionsString, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(strings: List<String>): String {
        return gson.toJson(strings)
    }

    @TypeConverter
    fun toStringList(stringsString: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(stringsString, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toStringMap(mapString: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(mapString, type) ?: emptyMap()
    }
} 