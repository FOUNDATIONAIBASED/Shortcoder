package com.example.shortcoder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shortcoder.adapters.ShortcutAdapter
import com.example.shortcoder.databinding.ActivityMainBinding
import com.example.shortcoder.services.AutomationService
import com.example.shortcoder.viewmodels.MainViewModel
import com.example.shortcoder.activities.ShortcutEditorActivity
import com.example.shortcoder.activities.AutomationEditorActivity
import com.example.shortcoder.activities.SmsForwardingActivity
import com.example.shortcoder.utils.BackgroundExecutionManager
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.Context
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.Shortcut
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var shortcutAdapter: ShortcutAdapter
    private lateinit var backgroundExecutionManager: BackgroundExecutionManager
    private lateinit var database: ShortcoderDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = ShortcoderDatabase.getDatabase(this)
        
        // Initialize background execution manager
        backgroundExecutionManager = BackgroundExecutionManager(this)
        
        // Debug: Check forwarding settings
        debugForwardingSettings()
        
        setupViewModel()
        setupUI()
        setupObservers()
        requestPermissions()
        createNotificationChannels()
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }
    
    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Shortcoder"
        
        // Setup RecyclerView
        shortcutAdapter = ShortcutAdapter(
            onShortcutClick = { shortcut ->
                viewModel.runShortcut(shortcut)
            },
            onShortcutLongClick = { shortcut ->
                showShortcutOptionsDialog(shortcut)
            },
            onShortcutDelete = { shortcut ->
                showDeleteShortcutDialog(shortcut)
            }
        )
        
        binding.recyclerViewShortcuts.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = shortcutAdapter
        }
        
        // Setup FAB
        binding.fabCreateShortcut.setOnClickListener {
            showCreateOptions()
        }
        
        // Setup bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_shortcuts -> {
                    // Already on shortcuts screen
                    true
                }
                R.id.nav_automations -> {
                    startActivity(Intent(this, com.example.shortcoder.activities.AutomationListActivity::class.java))
                    true
                }
                R.id.nav_sms_forwarding -> {
                    startActivity(Intent(this, SmsForwardingActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Setup empty state button
        binding.buttonCreateFirstShortcut.setOnClickListener {
            val intent = Intent(this, ShortcutEditorActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun showCreateOptions() {
        val options = arrayOf(
            "Create Shortcut",
            "Create Automation",
            "SMS Forwarding Rule"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Create New")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ShortcutEditorActivity::class.java))
                    1 -> startActivity(Intent(this, AutomationEditorActivity::class.java))
                    2 -> startActivity(Intent(this, SmsForwardingActivity::class.java))
                }
            }
            .show()
    }
    
    private fun requestPermissions() {
        val requiredPermissions = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        Dexter.withContext(this)
            .withPermissions(requiredPermissions)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        Toast.makeText(this@MainActivity, "All permissions granted", Toast.LENGTH_SHORT).show()
                        // Setup background execution after permissions are granted
                        setupBackgroundExecution()
                    } else {
                        Toast.makeText(this@MainActivity, "Some permissions were denied. App functionality may be limited.", Toast.LENGTH_LONG).show()
                        // Still try to setup background execution with available permissions
                        setupBackgroundExecution()
                    }
                }
                
                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Permissions Required")
                        .setMessage("Shortcoder needs these permissions to provide automation and SMS forwarding functionality.")
                        .setPositiveButton("Grant") { _, _ -> token.continuePermissionRequest() }
                        .setNegativeButton("Deny") { _, _ -> token.cancelPermissionRequest() }
                        .show()
                }
            })
            .check()
    }
    
    /**
     * Setup comprehensive background execution
     * This ensures SMS forwarding and automation work even when app is closed
     */
    private fun setupBackgroundExecution() {
        backgroundExecutionManager.setupBackgroundExecution()
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure service is still running when app comes to foreground
        backgroundExecutionManager.ensureServiceRunning()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // SMS/MMS Forwarding Channel
            val smsForwardingChannel = NotificationChannel(
                "sms_forwarding",
                "SMS Forwarding",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for SMS forwarding status"
            }
            
            // MMS Forwarding Channel
            val mmsForwardingChannel = NotificationChannel(
                "mms_forwarding",
                "MMS Forwarding",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for MMS forwarding with attachments"
            }
            
            // Automation Channel
            val automationChannel = NotificationChannel(
                "automation",
                "Automation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from automation actions"
            }
            
            notificationManager.createNotificationChannels(listOf(
                smsForwardingChannel,
                mmsForwardingChannel,
                automationChannel
            ))
        }
    }
    
    private fun setupObservers() {
        // Observe shortcuts
        viewModel.shortcuts.observe(this) { shortcuts ->
            shortcutAdapter.submitList(shortcuts)
            
            // Update empty state - use the correct container ID
            if (shortcuts.isEmpty()) {
                binding.emptyStateContainer.visibility = android.view.View.VISIBLE
                binding.recyclerViewShortcuts.visibility = android.view.View.GONE
            } else {
                binding.emptyStateContainer.visibility = android.view.View.GONE
                binding.recyclerViewShortcuts.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun showShortcutOptionsDialog(shortcut: Shortcut) {
        val options = arrayOf(
            "Edit",
            "Toggle Enable/Disable",
            "Delete"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(shortcut.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Edit shortcut
                        val intent = Intent(this, ShortcutEditorActivity::class.java).apply {
                            putExtra("shortcut_id", shortcut.id)
                        }
                        startActivity(intent)
                    }
                    1 -> {
                        // Toggle enabled
                        viewModel.toggleShortcutEnabled(shortcut)
                        Toast.makeText(
                            this,
                            if (shortcut.isEnabled) "Shortcut disabled" else "Shortcut enabled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        // Delete shortcut
                        showDeleteShortcutDialog(shortcut)
                    }
                }
            }
            .show()
    }

    private fun showDeleteShortcutDialog(shortcut: Shortcut) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Shortcut")
            .setMessage("Are you sure you want to delete \"${shortcut.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteShortcut(shortcut)
                Toast.makeText(this, "Shortcut deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun debugForwardingSettings() {
        lifecycleScope.launch {
            try {
                val database = ShortcoderDatabase.getDatabase(this@MainActivity)
                val smsForwardingDao = database.smsForwardingDao()
                
                Log.d("MainActivity", "🔍 DEBUG: Checking forwarding settings...")
                
                // Check forwarding settings
                val settings = smsForwardingDao.getForwardingSettings()
                if (settings != null) {
                    Log.d("MainActivity", "⚙️ Forwarding Settings Found:")
                    Log.d("MainActivity", "🌐 Global forwarding enabled: ${settings.isGlobalForwardingEnabled}")
                    Log.d("MainActivity", "📱 Global destination: '${settings.globalDestinationNumber}'")
                    Log.d("MainActivity", "🏷️ Global prefix: '${settings.customGlobalPrefix}'")
                    Log.d("MainActivity", "👤 Include original sender: ${settings.includeOriginalSender}")
                } else {
                    Log.e("MainActivity", "❌ NO FORWARDING SETTINGS FOUND!")
                }
                
                // Check forwarding rules
                val rules = smsForwardingDao.getAllForwardingRules().first()
                Log.d("MainActivity", "📋 Found ${rules.size} total forwarding rules")
                
                val enabledRules = rules.filter { it.isEnabled }
                Log.d("MainActivity", "✅ Found ${enabledRules.size} ENABLED forwarding rules")
                
                if (enabledRules.isEmpty()) {
                    Log.e("MainActivity", "❌ NO ENABLED FORWARDING RULES FOUND!")
                    Log.e("MainActivity", "⚠️ This is likely why MMS forwarding is not working!")
                } else {
                    enabledRules.forEachIndexed { index, rule ->
                        Log.d("MainActivity", "📋 Enabled Rule $index:")
                        Log.d("MainActivity", "   📱 Destination: ${rule.destinationNumber}")
                        Log.d("MainActivity", "   📝 Type: ${rule.ruleType}")
                        Log.d("MainActivity", "   🏷️ Prefix: '${rule.customPrefix}'")
                        Log.d("MainActivity", "   ✅ Enabled: ${rule.isEnabled}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainActivity", "💥 Error checking forwarding settings", e)
            }
        }
    }
} 