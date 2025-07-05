package com.example.shortcoder.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.shortcoder.R

class AccessibilitySetupActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_setup)
        
        setupViews()
    }
    
    private fun setupViews() {
        val titleText = findViewById<TextView>(R.id.titleText)
        val descriptionText = findViewById<TextView>(R.id.descriptionText)
        val instructionsText = findViewById<TextView>(R.id.instructionsText)
        val enableButton = findViewById<Button>(R.id.enableButton)
        val skipButton = findViewById<Button>(R.id.skipButton)
        
        titleText.text = "Enable AI Button Detection"
        descriptionText.text = "To automatically click Send buttons in messaging apps, Shortcoder needs accessibility permission."
        instructionsText.text = """
            1. Tap "Enable Accessibility Service" below
            2. Find "Shortcoder AI Button Detection" in the list
            3. Toggle it ON
            4. Confirm the permission
            5. Return to Shortcoder
        """.trimIndent()
        
        enableButton.setOnClickListener {
            openAccessibilitySettings()
        }
        
        skipButton.setOnClickListener {
            finish()
        }
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if accessibility service is now enabled
        if (isAccessibilityServiceEnabled()) {
            // Service is enabled, we can finish this activity
            setResult(RESULT_OK)
            finish()
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
} 