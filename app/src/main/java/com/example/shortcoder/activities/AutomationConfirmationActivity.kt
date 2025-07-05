package com.example.shortcoder.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.engine.ActionExecutor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AutomationConfirmationActivity : AppCompatActivity() {
    
    private lateinit var database: ShortcoderDatabase
    private lateinit var actionExecutor: ActionExecutor
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = ShortcoderDatabase.getDatabase(this)
        actionExecutor = ActionExecutor(this)
        
        val automationId = intent.getStringExtra("automation_id")
        if (automationId != null) {
            showConfirmationDialog(automationId)
        } else {
            finish()
        }
    }
    
    private fun showConfirmationDialog(automationId: String) {
        lifecycleScope.launch {
            try {
                val automation = database.automationDao().getAutomationById(automationId)
                if (automation == null) {
                    Toast.makeText(this@AutomationConfirmationActivity, "Automation not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                
                MaterialAlertDialogBuilder(this@AutomationConfirmationActivity)
                    .setTitle("Run Automation")
                    .setMessage("${automation.name}\n\nThis automation will execute ${automation.actions.size} action${if (automation.actions.size != 1) "s" else ""}. Do you want to continue?")
                    .setPositiveButton("Run") { _, _ ->
                        executeAutomation(automation)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        finish()
                    }
                    .setOnDismissListener {
                        finish()
                    }
                    .show()
                    
            } catch (e: Exception) {
                Toast.makeText(this@AutomationConfirmationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun executeAutomation(automation: com.example.shortcoder.data.models.Automation) {
        lifecycleScope.launch {
            try {
                val success = actionExecutor.executeActions(automation.actions)
                
                if (success) {
                    // Update run count and last run time
                    database.automationDao().incrementRunCount(automation.id)
                    Toast.makeText(this@AutomationConfirmationActivity, "Automation executed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AutomationConfirmationActivity, "Automation execution failed", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@AutomationConfirmationActivity, "Error executing automation: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }
} 