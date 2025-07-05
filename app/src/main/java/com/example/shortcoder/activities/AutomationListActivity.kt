package com.example.shortcoder.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shortcoder.R
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.Automation
import com.example.shortcoder.databinding.ActivityAutomationListBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class AutomationListActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAutomationListBinding
    private lateinit var database: ShortcoderDatabase
    private lateinit var automationAdapter: AutomationAdapter
    private val automations = mutableListOf<Automation>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutomationListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = ShortcoderDatabase.getDatabase(this)
        
        setupUI()
        setupRecyclerView()
        loadAutomations()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Automations"
        
        binding.fabAddAutomation.setOnClickListener {
            val intent = Intent(this, AutomationEditorActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupRecyclerView() {
        automationAdapter = AutomationAdapter(
            automations = automations,
            onToggleEnabled = { automation, enabled -> toggleAutomation(automation, enabled) },
            onEditAutomation = { automation -> editAutomation(automation) },
            onDeleteAutomation = { automation -> deleteAutomation(automation) },
            onRunAutomation = { automation -> runAutomation(automation) }
        )
        
        binding.recyclerViewAutomations.apply {
            layoutManager = LinearLayoutManager(this@AutomationListActivity)
            adapter = automationAdapter
        }
    }
    
    private fun loadAutomations() {
        lifecycleScope.launch {
            database.automationDao().getAllAutomations().collect { automationList ->
                automations.clear()
                automations.addAll(automationList)
                automationAdapter.notifyDataSetChanged()
                
                // Update empty state - use the correct container ID
                if (automations.isEmpty()) {
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    binding.recyclerViewAutomations.visibility = View.GONE
                } else {
                    binding.emptyStateContainer.visibility = View.GONE
                    binding.recyclerViewAutomations.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun toggleAutomation(automation: Automation, enabled: Boolean) {
        lifecycleScope.launch {
            try {
                database.automationDao().setAutomationEnabled(automation.id, enabled)
                Toast.makeText(
                    this@AutomationListActivity,
                    "${automation.name} ${if (enabled) "enabled" else "disabled"}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@AutomationListActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun editAutomation(automation: Automation) {
        val intent = Intent(this, AutomationEditorActivity::class.java).apply {
            putExtra("automation_id", automation.id)
        }
        startActivity(intent)
    }
    
    private fun deleteAutomation(automation: Automation) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Automation")
            .setMessage("Are you sure you want to delete '${automation.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        database.automationDao().deleteAutomation(automation)
                        Toast.makeText(this@AutomationListActivity, "Automation deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@AutomationListActivity, "Error deleting automation: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun runAutomation(automation: Automation) {
        lifecycleScope.launch {
            try {
                // Execute the automation manually
                val actionExecutor = com.example.shortcoder.engine.ActionExecutor(this@AutomationListActivity)
                val success = actionExecutor.executeActions(automation.actions)
                
                if (success) {
                    // Update run count
                    database.automationDao().incrementRunCount(automation.id)
                    Toast.makeText(this@AutomationListActivity, "Automation executed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AutomationListActivity, "Automation execution failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AutomationListActivity, "Error running automation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clear any pending tooltips to prevent window leaks
        binding.root.clearFocus()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh the list when returning from editor
        loadAutomations()
    }
}

class AutomationAdapter(
    private val automations: List<Automation>,
    private val onToggleEnabled: (Automation, Boolean) -> Unit,
    private val onEditAutomation: (Automation) -> Unit,
    private val onDeleteAutomation: (Automation) -> Unit,
    private val onRunAutomation: (Automation) -> Unit
) : RecyclerView.Adapter<AutomationAdapter.AutomationViewHolder>() {
    
    class AutomationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.textViewName)
        val descriptionText: TextView = itemView.findViewById(R.id.textViewDescription)
        val triggerText: TextView = itemView.findViewById(R.id.textViewTrigger)
        val actionCountText: TextView = itemView.findViewById(R.id.textViewActionCount)
        val runCountText: TextView = itemView.findViewById(R.id.textViewRunCount)
        val enabledSwitch: Switch = itemView.findViewById(R.id.switchEnabled)
        val runButton: View = itemView.findViewById(R.id.buttonRun)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutomationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_automation, parent, false)
        return AutomationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AutomationViewHolder, position: Int) {
        val automation = automations[position]
        
        holder.nameText.text = automation.name
        holder.descriptionText.text = automation.description.ifEmpty { "No description" }
        holder.triggerText.text = getTriggerDisplayText(automation.trigger)
        holder.actionCountText.text = "${automation.actions.size} actions"
        holder.runCountText.text = "Run ${automation.runCount} times"
        
        holder.enabledSwitch.isChecked = automation.isEnabled
        holder.enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggleEnabled(automation, isChecked)
        }
        
        holder.runButton.setOnClickListener {
            onRunAutomation(automation)
        }
        
        holder.itemView.setOnClickListener {
            onEditAutomation(automation)
        }
        
        holder.itemView.setOnLongClickListener {
            onDeleteAutomation(automation)
            true
        }
    }
    
    override fun getItemCount() = automations.size
    
    private fun getTriggerDisplayText(trigger: com.example.shortcoder.data.models.AutomationTrigger): String {
        return when (trigger.type) {
            com.example.shortcoder.data.models.TriggerType.TIME_OF_DAY -> "Time: ${trigger.parameters["time"] ?: "Not set"}"
            com.example.shortcoder.data.models.TriggerType.RECEIVE_MESSAGE -> "SMS received"
            com.example.shortcoder.data.models.TriggerType.RECEIVE_MMS -> "MMS received"
            com.example.shortcoder.data.models.TriggerType.CONNECT_WIFI -> "WiFi connected"
            com.example.shortcoder.data.models.TriggerType.BATTERY_LEVEL -> "Battery: ${trigger.parameters["level"] ?: "?"}%"
            else -> trigger.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }
} 