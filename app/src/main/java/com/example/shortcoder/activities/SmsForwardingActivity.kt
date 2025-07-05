package com.example.shortcoder.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shortcoder.R
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.*
import com.example.shortcoder.databinding.ActivitySmsForwardingBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.util.UUID

class SmsForwardingActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SmsForwardingActivity"
    }
    
    private lateinit var binding: ActivitySmsForwardingBinding
    private lateinit var database: ShortcoderDatabase
    private lateinit var rulesAdapter: SmsForwardingRulesAdapter
    private val forwardingRules = mutableListOf<SmsForwardingRule>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsForwardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = ShortcoderDatabase.getDatabase(this)
        
        setupUI()
        setupRecyclerView()
        loadSettings()
        loadForwardingRules()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "SMS/MMS Forwarding"
        
        binding.buttonSave.setOnClickListener {
            saveGlobalSettings()
        }
        
        binding.switchGlobalForwarding.setOnCheckedChangeListener { _, isChecked ->
            updateUIForGlobalForwarding(isChecked)
        }
        
        // Add new rule button
        binding.buttonAddRule?.setOnClickListener {
            showAddRuleDialog()
        }
    }
    
    private fun setupRecyclerView() {
        rulesAdapter = SmsForwardingRulesAdapter(
            rules = forwardingRules,
            onEditRule = { rule -> editRule(rule) },
            onDeleteRule = { rule -> deleteRule(rule) },
            onToggleRule = { rule, enabled -> toggleRule(rule, enabled) }
        )
        
        binding.recyclerViewRules?.apply {
            layoutManager = LinearLayoutManager(this@SmsForwardingActivity)
            adapter = rulesAdapter
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            val settings = database.smsForwardingDao().getForwardingSettings()
            settings?.let { populateFields(it) }
        }
    }
    
    private fun loadForwardingRules() {
        lifecycleScope.launch {
            database.smsForwardingDao().getAllForwardingRules().collect { rules ->
                forwardingRules.clear()
                forwardingRules.addAll(rules)
                rulesAdapter.notifyDataSetChanged()
            }
        }
    }
    
    private fun populateFields(settings: SmsForwardingSettings) {
        binding.switchGlobalForwarding.isChecked = settings.isGlobalForwardingEnabled
        binding.editTextDestinationNumber.setText(settings.globalDestinationNumber)
        binding.editTextCustomPrefix.setText(settings.customGlobalPrefix)
        binding.switchIncludeOriginalSender.isChecked = settings.includeOriginalSender
        binding.switchRequireConfirmation.isChecked = settings.requireConfirmationForNewRules
        updateUIForGlobalForwarding(settings.isGlobalForwardingEnabled)
    }
    
    private fun updateUIForGlobalForwarding(enabled: Boolean) {
        binding.editTextDestinationNumber.isEnabled = enabled
        binding.editTextCustomPrefix.isEnabled = enabled
        binding.switchIncludeOriginalSender.isEnabled = enabled
    }
    
    private fun showAddRuleDialog() {
        val ruleTypes = arrayOf(
            "Forward All Messages (SMS/MMS)",
            "Forward from Specific Numbers",
            "Block Specific Numbers", 
            "Forward Messages Containing Keywords",
            "Block Messages Containing Keywords"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Rule Type")
            .setItems(ruleTypes) { _, which ->
                val ruleType = when (which) {
                    0 -> ForwardingRuleType.FORWARD_ALL
                    1 -> ForwardingRuleType.FORWARD_FROM_SPECIFIC
                    2 -> ForwardingRuleType.FORWARD_EXCEPT_SPECIFIC
                    3 -> ForwardingRuleType.FORWARD_CONTAINING_KEYWORDS
                    else -> ForwardingRuleType.FORWARD_NOT_CONTAINING_KEYWORDS
                }
                
                showRuleConfigurationDialog(ruleType, ruleTypes[which])
            }
            .show()
    }
    
    private fun showRuleConfigurationDialog(ruleType: ForwardingRuleType, ruleTypeName: String) {
        val nameInput = android.widget.EditText(this).apply {
            hint = "Rule Name"
            setText(ruleTypeName)
        }
        
        val destinationInput = android.widget.EditText(this).apply {
            hint = "Destination Phone Number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        
        val prefixInput = android.widget.EditText(this).apply {
            hint = "Custom Prefix (optional)"
            setText("[Forwarded]")
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(nameInput)
            addView(destinationInput)
            addView(prefixInput)
        }
        
        // Add specific configuration based on rule type
        when (ruleType) {
            ForwardingRuleType.FORWARD_FROM_SPECIFIC, 
            ForwardingRuleType.FORWARD_EXCEPT_SPECIFIC -> {
                val numbersInput = android.widget.EditText(this).apply {
                    hint = "Phone Numbers (comma separated)"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                }
                layout.addView(numbersInput)
                
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Configure $ruleTypeName")
                    .setView(layout)
                    .setPositiveButton("Create Rule") { _, _ ->
                        val numbers = numbersInput.text.toString()
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        createForwardingRuleWithConfirmation(
                            name = nameInput.text.toString(),
                            ruleType = ruleType,
                            destinationNumber = destinationInput.text.toString(),
                            prefix = prefixInput.text.toString(),
                            sourceNumbers = numbers
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            
            ForwardingRuleType.FORWARD_CONTAINING_KEYWORDS,
            ForwardingRuleType.FORWARD_NOT_CONTAINING_KEYWORDS -> {
                val keywordsInput = android.widget.EditText(this).apply {
                    hint = "Keywords (comma separated)"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                }
                layout.addView(keywordsInput)
                
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Configure $ruleTypeName")
                    .setView(layout)
                    .setPositiveButton("Create Rule") { _, _ ->
                        val keywords = keywordsInput.text.toString()
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        createForwardingRuleWithConfirmation(
                            name = nameInput.text.toString(),
                            ruleType = ruleType,
                            destinationNumber = destinationInput.text.toString(),
                            prefix = prefixInput.text.toString(),
                            sourceNumbers = keywords // Using sourceNumbers field for keywords
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            
            else -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Configure $ruleTypeName")
                    .setView(layout)
                    .setPositiveButton("Create Rule") { _, _ ->
                        createForwardingRuleWithConfirmation(
                            name = nameInput.text.toString(),
                            ruleType = ruleType,
                            destinationNumber = destinationInput.text.toString(),
                            prefix = prefixInput.text.toString(),
                            sourceNumbers = emptyList()
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun createForwardingRuleWithConfirmation(
        name: String,
        ruleType: ForwardingRuleType,
        destinationNumber: String,
        prefix: String,
        sourceNumbers: List<String>
    ) {
        lifecycleScope.launch {
            val settings = database.smsForwardingDao().getForwardingSettings()
            
            if (settings?.requireConfirmationForNewRules == true) {
                // Show confirmation dialog
                MaterialAlertDialogBuilder(this@SmsForwardingActivity)
                    .setTitle("Confirm New Forwarding Rule")
                    .setMessage("Create forwarding rule '$name' that will forward messages to $destinationNumber?")
                    .setPositiveButton("Create") { _, _ ->
                        createForwardingRule(name, ruleType, destinationNumber, prefix, sourceNumbers)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                // Create directly without confirmation
                createForwardingRule(name, ruleType, destinationNumber, prefix, sourceNumbers)
            }
        }
    }
    
    private fun createForwardingRule(
        name: String,
        ruleType: ForwardingRuleType,
        destinationNumber: String,
        prefix: String,
        sourceNumbers: List<String>
    ) {
        if (name.isEmpty() || destinationNumber.isEmpty()) {
            Toast.makeText(this, "Name and destination number are required", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val rule = SmsForwardingRule(
                id = UUID.randomUUID().toString(),
                name = name,
                ruleType = ruleType,
                destinationNumber = destinationNumber,
                customPrefix = prefix,
                sourceNumbers = sourceNumbers,
                includeOriginalSender = true,
                isEnabled = true
            )
            
            database.smsForwardingDao().insertForwardingRule(rule)
            Toast.makeText(this@SmsForwardingActivity, "Rule created: $name", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun editRule(rule: SmsForwardingRule) {
        val nameInput = android.widget.EditText(this).apply {
            hint = "Rule Name"
            setText(rule.name)
        }
        
        val destinationInput = android.widget.EditText(this).apply {
            hint = "Destination Phone Number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(rule.destinationNumber)
        }
        
        val prefixInput = android.widget.EditText(this).apply {
            hint = "Custom Prefix (optional)"
            setText(rule.customPrefix)
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(nameInput)
            addView(destinationInput)
            addView(prefixInput)
        }
        
        // Add specific configuration based on rule type
        when (rule.ruleType) {
            ForwardingRuleType.FORWARD_FROM_SPECIFIC, 
            ForwardingRuleType.FORWARD_EXCEPT_SPECIFIC -> {
                val numbersInput = android.widget.EditText(this).apply {
                    hint = "Phone Numbers (comma separated)"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    setText(rule.sourceNumbers.joinToString(", "))
                }
                layout.addView(numbersInput)
                
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Edit Rule: ${rule.name}")
                    .setView(layout)
                    .setPositiveButton("Save Changes") { _, _ ->
                        val numbers = numbersInput.text.toString()
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        updateForwardingRule(
                            rule = rule,
                            name = nameInput.text.toString(),
                            destinationNumber = destinationInput.text.toString(),
                            prefix = prefixInput.text.toString(),
                            sourceNumbers = numbers
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            
            ForwardingRuleType.FORWARD_CONTAINING_KEYWORDS,
            ForwardingRuleType.FORWARD_NOT_CONTAINING_KEYWORDS -> {
                val keywordsInput = android.widget.EditText(this).apply {
                    hint = "Keywords (comma separated)"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    setText(rule.sourceNumbers.joinToString(", ")) // sourceNumbers used for keywords
                }
                layout.addView(keywordsInput)
                
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Edit Rule: ${rule.name}")
                    .setView(layout)
                    .setPositiveButton("Save Changes") { _, _ ->
                        val keywords = keywordsInput.text.toString()
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        updateForwardingRule(
                            rule = rule,
                            name = nameInput.text.toString(),
                            destinationNumber = destinationInput.text.toString(),
                            prefix = prefixInput.text.toString(),
                            sourceNumbers = keywords
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            
            else -> {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Edit Rule: ${rule.name}")
                    .setView(layout)
                    .setPositiveButton("Save Changes") { _, _ ->
                        updateForwardingRule(
                            rule = rule,
                            name = nameInput.text.toString(),
                            destinationNumber = destinationInput.text.toString(),
                            prefix = prefixInput.text.toString(),
                            sourceNumbers = rule.sourceNumbers
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun updateForwardingRule(
        rule: SmsForwardingRule,
        name: String,
        destinationNumber: String,
        prefix: String,
        sourceNumbers: List<String>
    ) {
        if (name.isEmpty() || destinationNumber.isEmpty()) {
            Toast.makeText(this, "Name and destination number are required", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val updatedRule = rule.copy(
                name = name,
                destinationNumber = destinationNumber,
                customPrefix = prefix,
                sourceNumbers = sourceNumbers,
                lastModified = System.currentTimeMillis()
            )
            
            database.smsForwardingDao().updateForwardingRule(updatedRule)
            Toast.makeText(this@SmsForwardingActivity, "Rule updated: $name", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteRule(rule: SmsForwardingRule) {
        showDeleteRuleConfirmationDialog(rule)
    }
    
    private fun showDeleteRuleConfirmationDialog(rule: SmsForwardingRule) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Forwarding Rule")
            .setMessage("Are you sure you want to delete \"${rule.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteForwardingRule(rule)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteForwardingRule(rule: SmsForwardingRule) {
        lifecycleScope.launch {
            try {
                database.smsForwardingDao().deleteForwardingRule(rule)
                Toast.makeText(this@SmsForwardingActivity, "Rule deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting rule", e)
                Toast.makeText(this@SmsForwardingActivity, "Error deleting rule", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleRule(rule: SmsForwardingRule, enabled: Boolean) {
        lifecycleScope.launch {
            database.smsForwardingDao().setForwardingRuleEnabled(rule.id, enabled)
            Toast.makeText(
                this@SmsForwardingActivity, 
                "Rule ${if (enabled) "enabled" else "disabled"}", 
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun saveGlobalSettings() {
        val isGlobalEnabled = binding.switchGlobalForwarding.isChecked
        val destinationNumber = binding.editTextDestinationNumber.text.toString().trim()
        val customPrefix = binding.editTextCustomPrefix.text.toString().trim()
        val includeOriginalSender = binding.switchIncludeOriginalSender.isChecked
        val requireConfirmation = binding.switchRequireConfirmation.isChecked
        
        if (isGlobalEnabled && destinationNumber.isEmpty()) {
            binding.editTextDestinationNumber.error = "Destination number required"
            return
        }
        
        lifecycleScope.launch {
            try {
                val settings = SmsForwardingSettings(
                    isGlobalForwardingEnabled = isGlobalEnabled,
                    globalDestinationNumber = destinationNumber,
                    customGlobalPrefix = customPrefix,
                    includeOriginalSender = includeOriginalSender,
                    logForwardedMessages = true,
                    requireConfirmationForNewRules = requireConfirmation,
                    lastModified = System.currentTimeMillis()
                )
                
                database.smsForwardingDao().insertForwardingSettings(settings)
                Toast.makeText(this@SmsForwardingActivity, "Settings saved", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@SmsForwardingActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

// Simple adapter for forwarding rules
class SmsForwardingRulesAdapter(
    private val rules: List<SmsForwardingRule>,
    private val onEditRule: (SmsForwardingRule) -> Unit,
    private val onDeleteRule: (SmsForwardingRule) -> Unit,
    private val onToggleRule: (SmsForwardingRule, Boolean) -> Unit
) : RecyclerView.Adapter<SmsForwardingRulesAdapter.RuleViewHolder>() {
    
    class RuleViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val titleText: android.widget.TextView = itemView.findViewById(android.R.id.text1)
        val subtitleText: android.widget.TextView = itemView.findViewById(android.R.id.text2)
        val enableSwitch: android.widget.Switch = android.widget.Switch(itemView.context)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RuleViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        
        val holder = RuleViewHolder(view)
        
        // Add switch to the layout
        val layout = view as android.widget.RelativeLayout
        val switchParams = android.widget.RelativeLayout.LayoutParams(
            android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
            android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        switchParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
        switchParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL)
        switchParams.marginEnd = 32
        
        holder.enableSwitch.layoutParams = switchParams
        layout.addView(holder.enableSwitch)
        
        return holder
    }
    
    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        
        holder.titleText.text = rule.name
        holder.subtitleText.text = "${rule.ruleType.name.replace("_", " ")} → ${rule.destinationNumber}"
        
        holder.enableSwitch.isChecked = rule.isEnabled
        holder.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggleRule(rule, isChecked)
        }
        
        holder.itemView.setOnClickListener {
            onEditRule(rule)
        }
        
        holder.itemView.setOnLongClickListener {
            onDeleteRule(rule)
            true
        }
    }
    
    override fun getItemCount() = rules.size
} 