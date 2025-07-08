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
    private lateinit var windSeekBar: SeekBar
    private lateinit var windTextView: TextView
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
            max = 35 // 1 to 36 trees (quadrupled from 9)
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
            max = 39 // 1 to 40 speed levels (doubled)
            setPadding(0, 0, 0, 20)
        }
        layout.addView(speedSeekBar)
        
        // Speed Value display
        speedTextView = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(speedTextView)
        
        // Wind Description
        val windDescription = TextView(this).apply {
            text = "Adjust the wind effect strength:"
            textSize = 16f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(windDescription)
        
        // Wind SeekBar
        windSeekBar = SeekBar(this).apply {
            max = 19 // 1 to 20 wind levels
            setPadding(0, 0, 0, 20)
        }
        layout.addView(windSeekBar)
        
        // Wind Value display
        windTextView = TextView(this).apply {
            textSize = 18f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(windTextView)
        
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
        val currentTrees = prefs.getInt("numberOfTrees", 12) // Doubled again from 6
        val currentSpeed = prefs.getInt("snowSpeed", 12)     // Doubled default
        val currentWind = prefs.getInt("windEffect", 5)      // Default wind level
        treesSeekBar.progress = currentTrees - 1 // SeekBar is 0-based
        speedSeekBar.progress = currentSpeed - 1 // SeekBar is 0-based
        windSeekBar.progress = currentWind - 1   // SeekBar is 0-based
        updateTreesText(currentTrees)
        updateSpeedText(currentSpeed)
        updateWindText(currentWind)
        
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
        
        windSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val wind = progress + 1
                updateWindText(wind)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        applyButton.setOnClickListener {
            val trees = treesSeekBar.progress + 1
            val speed = speedSeekBar.progress + 1
            val wind = windSeekBar.progress + 1
            prefs.edit()
                .putInt("numberOfTrees", trees)
                .putInt("snowSpeed", speed)
                .putInt("windEffect", wind)
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
    
    private fun updateWindText(wind: Int) {
        windTextView.text = "Wind effect: $wind"
    }
} 