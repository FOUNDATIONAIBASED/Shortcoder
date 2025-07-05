package com.example.shortcoder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "MmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // MMS functionality disabled - do nothing
        Log.d(TAG, "🔇 MMS functionality disabled - ignoring MMS broadcasts")
    }
} 