package com.example.shortcoder.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shortcoder.data.models.Shortcut
import com.example.shortcoder.databinding.ItemShortcutBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

class ShortcutAdapter(
    private val onShortcutClick: (Shortcut) -> Unit,
    private val onShortcutLongClick: (Shortcut) -> Unit,
    private val onShortcutDelete: ((Shortcut) -> Unit)? = null
) : ListAdapter<Shortcut, ShortcutAdapter.ShortcutViewHolder>(ShortcutDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val binding = ItemShortcutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShortcutViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ShortcutViewHolder(
        private val binding: ItemShortcutBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(shortcut: Shortcut) {
            binding.apply {
                textViewShortcutName.text = shortcut.name
                textViewShortcutDescription.text = shortcut.description.ifEmpty { 
                    "Automation with ${shortcut.actions.size} action${if (shortcut.actions.size != 1) "s" else ""}"
                }
                
                // Set action count chip with better formatting
                chipActionCount.text = "${shortcut.actions.size} action${if (shortcut.actions.size != 1) "s" else ""}"
                
                // Use vibrant iOS Shortcuts-style gradient colors
                val colors = listOf(
                    "#FF6B6B", // Red
                    "#4ECDC4", // Teal
                    "#45B7D1", // Blue
                    "#96CEB4", // Green
                    "#FFEAA7", // Yellow
                    "#DDA0DD", // Purple
                    "#98D8C8", // Mint
                    "#FFB347", // Orange
                    "#87CEEB", // Sky Blue
                    "#F0E68C"  // Khaki
                )
                
                try {
                    if (shortcut.iconColor.isNotEmpty()) {
                        cardViewShortcut.setCardBackgroundColor(Color.parseColor(shortcut.iconColor))
                    } else {
                        val colorIndex = shortcut.id.hashCode().absoluteValue % colors.size
                        cardViewShortcut.setCardBackgroundColor(Color.parseColor(colors[colorIndex]))
                    }
                } catch (e: Exception) {
                    // Fallback to default color from list
                    val colorIndex = shortcut.id.hashCode().absoluteValue % colors.size
                    cardViewShortcut.setCardBackgroundColor(Color.parseColor(colors[colorIndex]))
                }
                
                // Handle stats visibility with better formatting
                val hasRunCount = shortcut.runCount > 0
                val hasLastRun = shortcut.lastRun > 0
                
                if (hasRunCount) {
                    textViewRunCount.text = "${shortcut.runCount} run${if (shortcut.runCount != 1) "s" else ""}"
                    textViewRunCount.visibility = android.view.View.VISIBLE
                } else {
                    textViewRunCount.visibility = android.view.View.GONE
                }
                
                if (hasLastRun) {
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    textViewLastRun.text = dateFormat.format(Date(shortcut.lastRun))
                    textViewLastRun.visibility = android.view.View.VISIBLE
                } else {
                    textViewLastRun.visibility = android.view.View.GONE
                }
                
                // Show separator dot only if both stats are visible
                separatorDot.visibility = if (hasRunCount && hasLastRun) android.view.View.VISIBLE else android.view.View.GONE
                
                // Set enabled/disabled state with visual feedback
                root.alpha = if (shortcut.isEnabled) 1.0f else 0.6f
                
                // Add iOS-style press animation
                root.setOnClickListener {
                    if (shortcut.isEnabled) {
                        // Smooth press animation
                        root.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(100)
                            .withEndAction {
                                root.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(100)
                                    .start()
                            }
                            .start()
                        
                        onShortcutClick(shortcut)
                    }
                }
                
                root.setOnLongClickListener {
                    // Haptic feedback for long press
                    root.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    onShortcutLongClick(shortcut)
                    true
                }
            }
        }
    }
    
    class ShortcutDiffCallback : DiffUtil.ItemCallback<Shortcut>() {
        override fun areItemsTheSame(oldItem: Shortcut, newItem: Shortcut): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Shortcut, newItem: Shortcut): Boolean {
            return oldItem == newItem
        }
    }
} 