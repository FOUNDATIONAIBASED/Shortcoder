package com.example.shortcoder.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.shortcoder.R
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.*
import com.example.shortcoder.databinding.ActivityAutomationEditorBinding
import kotlinx.coroutines.launch
import java.util.UUID

class AutomationEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAutomationEditorBinding
    private lateinit var database: ShortcoderDatabase
    private var automationId: String? = null
    private var automation: Automation? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAutomationEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = ShortcoderDatabase.getDatabase(this)
        automationId = intent.getStringExtra("automation_id")
        
        setupUI()
        loadAutomation()
    }
    
    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (automationId != null) "Edit Automation" else "Create Automation"
        
        // Setup save button
        binding.buttonSave.setOnClickListener {
            saveAutomation()
        }
        
        // Setup trigger button
        binding.buttonSetTrigger.setOnClickListener {
            showTriggerOptions()
        }
        
        // Setup add action button
        binding.buttonAddAction.setOnClickListener {
            addSampleAction()
        }
    }
    
    private fun loadAutomation() {
        automationId?.let { id ->
            lifecycleScope.launch {
                automation = database.automationDao().getAutomationById(id)
                automation?.let { populateFields(it) }
            }
        }
    }
    
    private fun populateFields(automation: Automation) {
        binding.editTextName.setText(automation.name)
        binding.editTextDescription.setText(automation.description)
        binding.switchEnabled.isChecked = automation.isEnabled
        binding.switchRequireConfirmation.isChecked = automation.requiresConfirmation
        
        // Update trigger display
        binding.textViewTrigger.text = getTriggerDisplayText(automation.trigger)
        
        // Update action count display
        binding.textViewActionCount.text = "${automation.actions.size} actions configured"
    }
    
    private fun getTriggerDisplayText(trigger: AutomationTrigger): String {
        return when (trigger.type) {
            TriggerType.TIME_OF_DAY -> "Time of Day: ${trigger.parameters["time"] ?: "Not set"}"
            TriggerType.RECEIVE_MESSAGE -> "Receive Message"
            TriggerType.RECEIVE_MMS -> "Receive MMS"
            TriggerType.CONNECT_WIFI -> "Connect to WiFi"
            TriggerType.BATTERY_LEVEL -> "Battery Level: ${trigger.parameters["level"] ?: "Not set"}%"
            else -> trigger.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun showTriggerOptions() {
        val triggerTypes = arrayOf(
            "Time of Day",
            "Receive Message",
            "Receive MMS",
            "Connect to WiFi",
            "Battery Level",
            "Connect Charger",
            "Location - Arrive",
            "Location - Leave",
            "Bluetooth Connect",
            "App Opened",
            "Airplane Mode On"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Trigger")
            .setItems(triggerTypes) { _, which ->
                val triggerType = when (which) {
                    0 -> TriggerType.TIME_OF_DAY
                    1 -> TriggerType.RECEIVE_MESSAGE
                    2 -> TriggerType.RECEIVE_MMS
                    3 -> TriggerType.CONNECT_WIFI
                    4 -> TriggerType.BATTERY_LEVEL
                    5 -> TriggerType.CONNECT_CHARGER
                    6 -> TriggerType.ARRIVE_LOCATION
                    7 -> TriggerType.LEAVE_LOCATION
                    8 -> TriggerType.CONNECT_BLUETOOTH
                    9 -> TriggerType.OPEN_APP
                    else -> TriggerType.AIRPLANE_MODE_ON
                }
                
                showTriggerConfigurationDialog(triggerType, triggerTypes[which])
            }
            .show()
    }
    
    private fun showTriggerConfigurationDialog(triggerType: TriggerType, triggerName: String) {
        when (triggerType) {
            TriggerType.TIME_OF_DAY -> showTimeOfDayDialog()
            TriggerType.RECEIVE_MESSAGE -> showReceiveMessageDialog()
            TriggerType.RECEIVE_MMS -> showReceiveMmsDialog()
            TriggerType.BATTERY_LEVEL -> showBatteryLevelDialog()
            TriggerType.CONNECT_WIFI -> showWifiDialog()
            TriggerType.ARRIVE_LOCATION, TriggerType.LEAVE_LOCATION -> showLocationDialog(triggerType)
            TriggerType.OPEN_APP -> showAppDialog()
            else -> {
                // For simple triggers, just set with default parameters
                val trigger = AutomationTrigger(
                    type = triggerType,
                    parameters = emptyMap()
                )
                setTrigger(trigger)
                Toast.makeText(this, "Trigger set to: $triggerName", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showTimeOfDayDialog() {
        val timeInput = android.widget.EditText(this).apply {
            hint = "Time (HH:MM, e.g., 09:30)"
            inputType = android.text.InputType.TYPE_CLASS_DATETIME or android.text.InputType.TYPE_DATETIME_VARIATION_TIME
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(timeInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Time of Day")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val time = timeInput.text.toString()
                val trigger = AutomationTrigger(
                    type = TriggerType.TIME_OF_DAY,
                    parameters = mapOf("time" to time)
                )
                setTrigger(trigger)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showReceiveMessageDialog() {
        val senderInput = android.widget.EditText(this).apply {
            hint = "From Number (optional, leave empty for any)"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val keywordInput = android.widget.EditText(this).apply {
            hint = "Contains Text (optional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(senderInput)
            addView(keywordInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Receive Message")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val params = mutableMapOf<String, String>()
                if (senderInput.text.isNotEmpty()) {
                    params["sender"] = senderInput.text.toString()
                }
                if (keywordInput.text.isNotEmpty()) {
                    params["keyword"] = keywordInput.text.toString()
                }
                
                val trigger = AutomationTrigger(
                    type = TriggerType.RECEIVE_MESSAGE,
                    parameters = params
                )
                setTrigger(trigger)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showReceiveMmsDialog() {
        val senderInput = android.widget.EditText(this).apply {
            hint = "From Number (optional, leave empty for any)"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val keywordInput = android.widget.EditText(this).apply {
            hint = "Contains Text in subject/message (optional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val hasAttachmentsCheckbox = android.widget.CheckBox(this).apply {
            text = "Only MMS with attachments"
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(senderInput)
            addView(keywordInput)
            addView(hasAttachmentsCheckbox)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Receive MMS")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val params = mutableMapOf<String, String>()
                if (senderInput.text.isNotEmpty()) {
                    params["sender"] = senderInput.text.toString()
                }
                if (keywordInput.text.isNotEmpty()) {
                    params["keyword"] = keywordInput.text.toString()
                }
                if (hasAttachmentsCheckbox.isChecked) {
                    params["hasAttachments"] = "true"
                }
                
                val trigger = AutomationTrigger(
                    type = TriggerType.RECEIVE_MMS,
                    parameters = params
                )
                setTrigger(trigger)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showBatteryLevelDialog() {
        val levelInput = android.widget.EditText(this).apply {
            hint = "Battery Level (0-100)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val conditionSpinner = android.widget.Spinner(this)
        val adapter = android.widget.ArrayAdapter(
            this@AutomationEditorActivity,
            android.R.layout.simple_spinner_item,
            arrayOf("Below", "Above", "Equals")
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        conditionSpinner.adapter = adapter
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(android.widget.TextView(this@AutomationEditorActivity).apply {
                text = "Condition:"
                setPadding(0, 0, 0, 20)
            })
            addView(conditionSpinner)
            addView(android.widget.TextView(this@AutomationEditorActivity).apply {
                text = "Battery Level:"
                setPadding(0, 40, 0, 20)
            })
            addView(levelInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Battery Level")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val level = levelInput.text.toString()
                val condition = conditionSpinner.selectedItem.toString().lowercase()
                val trigger = AutomationTrigger(
                    type = TriggerType.BATTERY_LEVEL,
                    parameters = mapOf(
                        "level" to level,
                        "condition" to condition
                    )
                )
                setTrigger(trigger)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showWifiDialog() {
        val networkInput = android.widget.EditText(this).apply {
            hint = "WiFi Network Name (optional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(networkInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure WiFi Connection")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val params = if (networkInput.text.isNotEmpty()) {
                    mapOf("network" to networkInput.text.toString())
                } else {
                    emptyMap()
                }
                
                val trigger = AutomationTrigger(
                    type = TriggerType.CONNECT_WIFI,
                    parameters = params
                )
                setTrigger(trigger)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showLocationDialog(triggerType: TriggerType) {
        val addressInput = android.widget.EditText(this).apply {
            hint = "Address or Location Name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val radiusInput = android.widget.EditText(this).apply {
            hint = "Radius in meters (default: 100)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("100")
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(addressInput)
            addView(radiusInput)
        }
        
        val title = if (triggerType == TriggerType.ARRIVE_LOCATION) "Arrive at Location" else "Leave Location"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure $title")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val trigger = AutomationTrigger(
                    type = triggerType,
                    parameters = mapOf(
                        "address" to addressInput.text.toString(),
                        "radius" to radiusInput.text.toString()
                    )
                )
                setTrigger(trigger)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAppDialog() {
        val appInput = android.widget.EditText(this).apply {
            hint = "App Package Name (e.g., com.android.chrome)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(appInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure App Opened")
            .setView(layout)
            .setPositiveButton("Set") { _, _ ->
                val trigger = AutomationTrigger(
                    type = TriggerType.OPEN_APP,
                    parameters = mapOf("package" to appInput.text.toString())
                )
                setTrigger(trigger)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setTrigger(trigger: AutomationTrigger) {
        // Update the current automation with the new trigger
        automation = automation?.copy(trigger = trigger) ?: Automation(
            id = UUID.randomUUID().toString(),
            name = binding.editTextName.text.toString().ifEmpty { "New Automation" },
            trigger = trigger,
            actions = emptyList()
        )
        
        // Update UI
        binding.textViewTrigger.text = getTriggerDisplayText(trigger)
        
        Toast.makeText(this, "Trigger configured", Toast.LENGTH_SHORT).show()
    }
    
    private fun addSampleAction() {
        val actionTypes = arrayOf(
            "Run Shortcut",
            "Send Message",
            "Send Email", 
            "Make Call",
            "Open App",
            "Open URL",
            "Show Notification",
            "Show Alert",
            "Toggle WiFi",
            "Toggle Bluetooth",
            "Toggle Flashlight",
            "Set Volume",
            "Share Text",
            "Get Text Input",
            "Play Music",
            "Take Photo",
            "Get Location",
            "Create Event",
            "Get Contact",
            "Save to Files",
            "If Condition",
            "Repeat Action",
            "Set Variable",
            "Get Variable",
            "Custom Action"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Action Type")
            .setItems(actionTypes) { _, which ->
                if (which == 0) {
                    // Run Shortcut - show shortcut selection dialog
                    showShortcutSelectionDialog()
                } else {
                    val actionType = when (which) {
                        1 -> ActionType.SEND_MESSAGE
                        2 -> ActionType.SEND_EMAIL
                        3 -> ActionType.MAKE_CALL
                        4 -> ActionType.OPEN_APP
                        5 -> ActionType.OPEN_URL
                        6 -> ActionType.SHOW_NOTIFICATION
                        7 -> ActionType.SHOW_ALERT
                        8 -> ActionType.TOGGLE_WIFI
                        9 -> ActionType.TOGGLE_BLUETOOTH
                        10 -> ActionType.TOGGLE_FLASHLIGHT
                        11 -> ActionType.SET_VOLUME
                        12 -> ActionType.SHARE_TEXT
                        13 -> ActionType.GET_TEXT_FROM_INPUT
                        14 -> ActionType.PLAY_MUSIC
                        15 -> ActionType.TAKE_PHOTO
                        16 -> ActionType.GET_CURRENT_LOCATION
                        17 -> ActionType.CREATE_EVENT
                        18 -> ActionType.GET_CONTACT
                        19 -> ActionType.SAVE_TO_FILES
                        20 -> ActionType.IF_CONDITION
                        21 -> ActionType.REPEAT_ACTION
                        22 -> ActionType.SET_VARIABLE
                        23 -> ActionType.GET_VARIABLE
                        else -> ActionType.CUSTOM_ACTION
                    }
                    
                    addActionToAutomation(actionType, actionTypes[which])
                }
            }
            .show()
    }
    
    private fun showShortcutSelectionDialog() {
        lifecycleScope.launch {
            try {
                val shortcuts = database.shortcutDao().getAllShortcutsList()
                
                if (shortcuts.isEmpty()) {
                    Toast.makeText(this@AutomationEditorActivity, "No shortcuts available. Create a shortcut first.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                val shortcutNames = shortcuts.map { "${it.name} (${it.actions.size} actions)" }.toTypedArray()
                
                androidx.appcompat.app.AlertDialog.Builder(this@AutomationEditorActivity)
                    .setTitle("Select Shortcut to Run")
                    .setItems(shortcutNames) { _, which ->
                        val selectedShortcut = shortcuts[which]
                        addShortcutAsAction(selectedShortcut)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                    
            } catch (e: Exception) {
                Toast.makeText(this@AutomationEditorActivity, "Error loading shortcuts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun addShortcutAsAction(shortcut: com.example.shortcoder.data.models.Shortcut) {
        // Add all actions from the shortcut to current automation
        val currentActions = automation?.actions?.toMutableList() ?: mutableListOf()
        
        // Add each action from the shortcut
        shortcut.actions.forEach { shortcutAction ->
            val newAction = shortcutAction.copy(
                id = UUID.randomUUID().toString(),
                order = currentActions.size
            )
            currentActions.add(newAction)
        }
        
        // Update automation with new actions
        automation = automation?.copy(actions = currentActions) ?: Automation(
            id = UUID.randomUUID().toString(),
            name = binding.editTextName.text.toString().ifEmpty { "New Automation" },
            trigger = AutomationTrigger(type = TriggerType.TIME_OF_DAY, parameters = mapOf("time" to "09:00")),
            actions = currentActions
        )
        
        // Update UI
        binding.textViewActionCount.text = "${currentActions.size} actions configured"
        
        Toast.makeText(this, "Added ${shortcut.actions.size} actions from '${shortcut.name}'", Toast.LENGTH_SHORT).show()
    }
    
    private fun addActionToAutomation(actionType: ActionType, title: String) {
        // Add action to current automation
        val currentActions = automation?.actions?.toMutableList() ?: mutableListOf()
        val newAction = ShortcutAction(
            id = UUID.randomUUID().toString(),
            type = actionType,
            title = title,
            parameters = emptyMap(),
            order = currentActions.size
        )
        currentActions.add(newAction)
        
        // Update automation with new actions
        automation = automation?.copy(actions = currentActions) ?: Automation(
            id = UUID.randomUUID().toString(),
            name = binding.editTextName.text.toString().ifEmpty { "New Automation" },
            trigger = AutomationTrigger(type = TriggerType.TIME_OF_DAY, parameters = mapOf("time" to "09:00")),
            actions = currentActions
        )
        
        // Update UI
        binding.textViewActionCount.text = "${currentActions.size} actions configured"
        
        Toast.makeText(this, "Action added: $title", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveAutomation() {
        val name = binding.editTextName.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val isEnabled = binding.switchEnabled.isChecked
        val requiresConfirmation = binding.switchRequireConfirmation.isChecked
        
        if (name.isEmpty()) {
            binding.editTextName.error = "Name is required"
            return
        }
        
        lifecycleScope.launch {
            try {
                val automationToSave = automation?.copy(
                    name = name,
                    description = description,
                    isEnabled = isEnabled,
                    requiresConfirmation = requiresConfirmation,
                    lastModified = System.currentTimeMillis()
                ) ?: Automation(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    trigger = AutomationTrigger(
                        type = TriggerType.TIME_OF_DAY,
                        parameters = mapOf("time" to "09:00")
                    ),
                    isEnabled = isEnabled,
                    requiresConfirmation = requiresConfirmation,
                    actions = listOf(
                        // Add a sample action for demonstration
                        ShortcutAction(
                            id = UUID.randomUUID().toString(),
                            type = ActionType.SHOW_ALERT,
                            title = "Show Alert",
                            parameters = mapOf("message" to "Automation '$name' triggered!")
                        )
                    )
                )
                
                database.automationDao().insertAutomation(automationToSave)
                
                Toast.makeText(this@AutomationEditorActivity, "Automation saved", Toast.LENGTH_SHORT).show()
                finish()
                
            } catch (e: Exception) {
                Toast.makeText(this@AutomationEditorActivity, "Error saving automation: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 