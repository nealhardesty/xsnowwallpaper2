package com.example.xsnowwallpaper2

import android.app.Activity
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Button
import android.content.SharedPreferences

class SettingsActivity : Activity() {

    private lateinit var treesSeekBar: SeekBar
    private lateinit var treesTextView: TextView
    private lateinit var speedSeekBar: SeekBar
    private lateinit var speedTextView: TextView
    private lateinit var applyButton: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple layout programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "XSnow Wallpaper Settings"
            textSize = 24f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(title)
        
        // Trees Description
        val treesDescription = TextView(this).apply {
            text = "Adjust the number of trees displayed:"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(treesDescription)
        
        // Trees SeekBar
        treesSeekBar = SeekBar(this).apply {
            max = 8 // 1 to 9 trees
            setPadding(0, 0, 0, 20)
        }
        layout.addView(treesSeekBar)
        
        // Trees Value display
        treesTextView = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(treesTextView)
        
        // Speed Description
        val speedDescription = TextView(this).apply {
            text = "Adjust the speed of falling snow:"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(speedDescription)
        
        // Speed SeekBar
        speedSeekBar = SeekBar(this).apply {
            max = 19 // 1 to 20 speed levels
            setPadding(0, 0, 0, 20)
        }
        layout.addView(speedSeekBar)
        
        // Speed Value display
        speedTextView = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(speedTextView)
        
        // Apply button
        applyButton = Button(this).apply {
            text = "Apply Settings"
            setPadding(0, 20, 0, 0)
        }
        layout.addView(applyButton)
        
        setContentView(layout)
        
        // Initialize preferences
        prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
        
        // Load current values
        val currentTrees = prefs.getInt("numberOfTrees", 3)
        val currentSpeed = prefs.getInt("snowSpeed", 6)
        treesSeekBar.progress = currentTrees - 1 // SeekBar is 0-based
        speedSeekBar.progress = currentSpeed - 1 // SeekBar is 0-based
        updateTreesText(currentTrees)
        updateSpeedText(currentSpeed)
        
        // Set up listeners
        treesSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val trees = progress + 1
                updateTreesText(trees)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress + 1
                updateSpeedText(speed)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        applyButton.setOnClickListener {
            val trees = treesSeekBar.progress + 1
            val speed = speedSeekBar.progress + 1
            prefs.edit()
                .putInt("numberOfTrees", trees)
                .putInt("snowSpeed", speed)
                .apply()
            finish()
        }
    }
    
    private fun updateTreesText(trees: Int) {
        treesTextView.text = "Number of trees: $trees"
    }
    
    private fun updateSpeedText(speed: Int) {
        speedTextView.text = "Snow speed: $speed"
    }
} 