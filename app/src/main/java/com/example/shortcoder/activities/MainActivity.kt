package com.example.shortcoder.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.shortcoder.R
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.SmsForwardingSettings
import com.example.shortcoder.data.models.SmsForwardingRule
import com.example.shortcoder.data.models.ForwardingRuleType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateContainer: View
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        
        // Check forwarding settings on startup
        checkForwardingStatus()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewShortcuts)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        fab = findViewById(R.id.fabCreateShortcut)
        
        // Check if accessibility service is enabled and show setup button if not
        checkAccessibilityServiceAndShowSetup()
        
        setupRecyclerView()
        setupFab()
        loadShortcuts()
    }
    
    private fun checkAccessibilityServiceAndShowSetup() {
        if (!isAccessibilityServiceEnabled()) {
            // Add setup button to empty state or show notification
            showAccessibilitySetupOption()
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (e: Exception) {
            false
        }
        
        if (!accessibilityEnabled) return false
        
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        return enabledServices.contains("${packageName}/com.example.shortcoder.services.AiButtonDetectionService")
    }
    
    private fun showAccessibilitySetupOption() {
        // Show a subtle notification or add to menu
        Log.d("MainActivity", "Accessibility service not enabled - AI button detection unavailable")
        // Could add a notification or menu item here
    }

    private fun setupRecyclerView() {
        // Implementation of setupRecyclerView method
    }

    private fun setupFab() {
        // Implementation of setupFab method
    }

    private fun loadShortcuts() {
        // Implementation of loadShortcuts method
    }

    private fun checkForwardingStatus() {
        lifecycleScope.launch {
            try {
                val database = ShortcoderDatabase.getDatabase(this@MainActivity)
                val smsForwardingDao = database.smsForwardingDao()
                
                Log.d("MainActivity", "📊 Checking forwarding status...")
                
                // Check forwarding settings
                val settings = smsForwardingDao.getForwardingSettings()
                if (settings != null) {
                    Log.d("MainActivity", "⚙️ Forwarding Settings:")
                    Log.d("MainActivity", "   🌐 Global forwarding: ${if (settings.isGlobalForwardingEnabled) "✅ ENABLED" else "❌ DISABLED"}")
                    Log.d("MainActivity", "   📱 Global destination: '${settings.globalDestinationNumber}'")
                    Log.d("MainActivity", "   🏷️ Global prefix: '${settings.customGlobalPrefix}'")
                }
                
                // Check forwarding rules
                val rules = smsForwardingDao.getAllForwardingRules().first()
                val enabledRules = rules.filter { it.isEnabled }
                
                Log.d("MainActivity", "📋 Forwarding Rules: ${enabledRules.size} of ${rules.size} enabled")
                
                enabledRules.forEachIndexed { index, rule ->
                    Log.d("MainActivity", "   📋 Rule ${index + 1}: ${rule.ruleType} -> ${rule.destinationNumber}")
                    Log.d("MainActivity", "      📊 Forwarded: ${rule.forwardCount} messages")
                }
                
                if (settings?.isGlobalForwardingEnabled == true || enabledRules.isNotEmpty()) {
                    Log.d("MainActivity", "✅ FORWARDING SYSTEM IS ACTIVE AND READY!")
                    Log.d("MainActivity", "📱 SMS: Sends automatically")
                    Log.d("MainActivity", "📎 MMS: Opens messaging app with pre-filled content (tap SEND to complete)")
                } else {
                    Log.d("MainActivity", "⚠️ No forwarding rules enabled. Configure forwarding in SMS Forwarding settings.")
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "💥 Error checking forwarding status", e)
            }
        }
    }
} 