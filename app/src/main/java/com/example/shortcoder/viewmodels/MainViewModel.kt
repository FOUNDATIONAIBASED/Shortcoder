package com.example.shortcoder.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.Shortcut
import com.example.shortcoder.engine.ActionExecutor
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = ShortcoderDatabase.getDatabase(application)
    private val shortcutDao = database.shortcutDao()
    private val actionExecutor = ActionExecutor(application)
    
    val shortcuts: LiveData<List<Shortcut>> = shortcutDao.getAllShortcuts().asLiveData()
    
    fun runShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            try {
                // Execute the shortcut actions
                val success = actionExecutor.executeActions(shortcut.actions)
                
                if (success) {
                    // Increment run count
                    shortcutDao.incrementRunCount(shortcut.id)
                }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }
    
    fun toggleShortcutEnabled(shortcut: Shortcut) {
        viewModelScope.launch {
            shortcutDao.setShortcutEnabled(shortcut.id, !shortcut.isEnabled)
        }
    }
    
    fun deleteShortcut(shortcut: Shortcut) {
        viewModelScope.launch {
            shortcutDao.deleteShortcut(shortcut)
        }
    }
} 