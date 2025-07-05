package com.example.shortcoder.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.shortcoder.R
import com.example.shortcoder.data.database.ShortcoderDatabase
import com.example.shortcoder.data.models.Shortcut
import com.example.shortcoder.data.models.ShortcutAction
import com.example.shortcoder.data.models.ActionType
import com.example.shortcoder.databinding.ActivityShortcutEditorBinding
import kotlinx.coroutines.launch
import java.util.UUID

class ShortcutEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityShortcutEditorBinding
    private lateinit var database: ShortcoderDatabase
    private var shortcutId: String? = null
    private var shortcut: Shortcut? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShortcutEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        database = ShortcoderDatabase.getDatabase(this)
        shortcutId = intent.getStringExtra("shortcut_id")
        
        setupUI()
        loadShortcut()
    }
    
    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (shortcutId != null) "Edit Shortcut" else "Create Shortcut"
        
        // Setup save button
        binding.buttonSave.setOnClickListener {
            saveShortcut()
        }
        
        // Setup add action button
        binding.buttonAddAction.setOnClickListener {
            addSampleAction()
        }
    }
    
    private fun loadShortcut() {
        shortcutId?.let { id ->
            lifecycleScope.launch {
                shortcut = database.shortcutDao().getShortcutById(id)
                shortcut?.let { populateFields(it) }
            }
        }
    }
    
    private fun populateFields(shortcut: Shortcut) {
        binding.editTextName.setText(shortcut.name)
        binding.editTextDescription.setText(shortcut.description)
        binding.switchEnabled.isChecked = shortcut.isEnabled
        
        // Update action count display
        binding.textViewActionCount.text = "${shortcut.actions.size} actions configured"
    }
    
    private fun addSampleAction() {
        val actionTypes = arrayOf(
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
                val actionType = when (which) {
                    0 -> ActionType.SEND_MESSAGE
                    1 -> ActionType.SEND_EMAIL
                    2 -> ActionType.MAKE_CALL
                    3 -> ActionType.OPEN_APP
                    4 -> ActionType.OPEN_URL
                    5 -> ActionType.SHOW_NOTIFICATION
                    6 -> ActionType.SHOW_ALERT
                    7 -> ActionType.TOGGLE_WIFI
                    8 -> ActionType.TOGGLE_BLUETOOTH
                    9 -> ActionType.TOGGLE_FLASHLIGHT
                    10 -> ActionType.SET_VOLUME
                    11 -> ActionType.SHARE_TEXT
                    12 -> ActionType.GET_TEXT_FROM_INPUT
                    13 -> ActionType.PLAY_MUSIC
                    14 -> ActionType.TAKE_PHOTO
                    15 -> ActionType.GET_CURRENT_LOCATION
                    16 -> ActionType.CREATE_EVENT
                    17 -> ActionType.GET_CONTACT
                    18 -> ActionType.SAVE_TO_FILES
                    19 -> ActionType.IF_CONDITION
                    20 -> ActionType.REPEAT_ACTION
                    21 -> ActionType.SET_VARIABLE
                    22 -> ActionType.GET_VARIABLE
                    else -> ActionType.CUSTOM_ACTION
                }
                
                showActionConfigurationDialog(actionType, actionTypes[which])
            }
            .show()
    }
    
    private fun showActionConfigurationDialog(actionType: ActionType, actionName: String) {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        
        when (actionType) {
            ActionType.SEND_MESSAGE -> showSendMessageDialog()
            ActionType.SEND_EMAIL -> showSendEmailDialog()
            ActionType.SHOW_ALERT -> showAlertDialog()
            ActionType.OPEN_URL -> showOpenUrlDialog()
            ActionType.SHARE_TEXT -> showShareTextDialog()
            else -> {
                // For other actions, show a simple configuration
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Configure $actionName")
                    .setMessage("Action type: $actionName\n\nThis action will be added with default settings.")
                    .setPositiveButton("Add") { _, _ ->
                        addActionToShortcut(actionType, actionName, emptyMap())
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun showSendMessageDialog() {
        val view = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
        val phoneInput = android.widget.EditText(this).apply {
            hint = "Phone Number"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val messageInput = android.widget.EditText(this).apply {
            hint = "Message"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(phoneInput)
            addView(messageInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Send Message")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val params = mapOf(
                    "phoneNumber" to phoneInput.text.toString(),
                    "message" to messageInput.text.toString()
                )
                addActionToShortcut(ActionType.SEND_MESSAGE, "Send Message", params)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSendEmailDialog() {
        val emailInput = android.widget.EditText(this).apply {
            hint = "Email Address"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val subjectInput = android.widget.EditText(this).apply {
            hint = "Subject"
        }
        val bodyInput = android.widget.EditText(this).apply {
            hint = "Message Body"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(emailInput)
            addView(subjectInput)
            addView(bodyInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Send Email")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val params = mapOf(
                    "email" to emailInput.text.toString(),
                    "subject" to subjectInput.text.toString(),
                    "body" to bodyInput.text.toString()
                )
                addActionToShortcut(ActionType.SEND_EMAIL, "Send Email", params)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAlertDialog() {
        val messageInput = android.widget.EditText(this).apply {
            hint = "Alert Message"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(messageInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Show Alert")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val params = mapOf("message" to messageInput.text.toString())
                addActionToShortcut(ActionType.SHOW_ALERT, "Show Alert", params)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showOpenUrlDialog() {
        val urlInput = android.widget.EditText(this).apply {
            hint = "URL (e.g., https://www.example.com)"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(urlInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Open URL")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val params = mapOf("url" to urlInput.text.toString())
                addActionToShortcut(ActionType.OPEN_URL, "Open URL", params)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showShareTextDialog() {
        val textInput = android.widget.EditText(this).apply {
            hint = "Text to Share"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(textInput)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configure Share Text")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val params = mapOf("text" to textInput.text.toString())
                addActionToShortcut(ActionType.SHARE_TEXT, "Share Text", params)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addActionToShortcut(actionType: ActionType, title: String, parameters: Map<String, String>) {
        // Add action to current shortcut
        val currentActions = shortcut?.actions?.toMutableList() ?: mutableListOf()
        val newAction = ShortcutAction(
            id = UUID.randomUUID().toString(),
            type = actionType,
            title = title,
            parameters = parameters,
            order = currentActions.size
        )
        currentActions.add(newAction)
        
        // Update shortcut with new actions
        shortcut = shortcut?.copy(actions = currentActions) ?: Shortcut(
            id = UUID.randomUUID().toString(),
            name = binding.editTextName.text.toString().ifEmpty { "New Shortcut" },
            actions = currentActions
        )
        
        // Update UI
        binding.textViewActionCount.text = "${currentActions.size} actions configured"
        
        Toast.makeText(this, "Action added: $title", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveShortcut() {
        val name = binding.editTextName.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val isEnabled = binding.switchEnabled.isChecked
        
        if (name.isEmpty()) {
            binding.editTextName.error = "Name is required"
            return
        }
        
        lifecycleScope.launch {
            try {
                val shortcutToSave = shortcut?.copy(
                    name = name,
                    description = description,
                    isEnabled = isEnabled,
                    lastModified = System.currentTimeMillis()
                ) ?: Shortcut(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    isEnabled = isEnabled,
                    actions = listOf(
                        // Add a sample action for demonstration
                        ShortcutAction(
                            id = UUID.randomUUID().toString(),
                            type = ActionType.SHOW_ALERT,
                            title = "Show Alert",
                            parameters = mapOf("message" to "Hello from $name!")
                        )
                    )
                )
                
                database.shortcutDao().insertShortcut(shortcutToSave)
                
                Toast.makeText(this@ShortcutEditorActivity, "Shortcut saved", Toast.LENGTH_SHORT).show()
                finish()
                
            } catch (e: Exception) {
                Toast.makeText(this@ShortcutEditorActivity, "Error saving shortcut: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 