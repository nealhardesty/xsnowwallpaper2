package com.roadwaffle.xsnowwallpaper2

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Button

class PreferencesActivity : Activity() {

    private lateinit var treesSeekBar: SeekBar
    private lateinit var treesTextView: TextView
    private lateinit var speedSeekBar: SeekBar
    private lateinit var speedTextView: TextView
    private lateinit var windSeekBar: SeekBar
    private lateinit var windTextView: TextView
    private lateinit var windChanceSeekBar: SeekBar
    private lateinit var windChanceTextView: TextView
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize preferences
        prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
        
        // Create a simple layout with all settings
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        
        // Tree icon at the top
        val treeIcon = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.tannenbaum)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setMargins(0, 0, 0, 20)
            }
        }
        layout.addView(treeIcon)
        
        // Title
        val title = TextView(this).apply {
            text = "XSnow Wallpaper Settings"
            textSize = 24f
            setPadding(0, 0, 0, 30)
            setTextColor(android.graphics.Color.WHITE)
        }
        layout.addView(title)
        
        // Trees setting - consolidated line
        val treesLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val treesLabel = TextView(this).apply {
            text = "Trees: "
            textSize = 16f
            setPadding(0, 0, 20, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        treesLayout.addView(treesLabel)
        
        treesSeekBar = SeekBar(this)
        treesSeekBar.max = 36 // 0 to 36 trees
        treesSeekBar.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        treesLayout.addView(treesSeekBar)
        
        treesTextView = TextView(this).apply {
            textSize = 16f
            setPadding(20, 0, 0, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        treesLayout.addView(treesTextView)
        layout.addView(treesLayout)
        
        // Speed setting - consolidated line
        val speedLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val speedLabel = TextView(this).apply {
            text = "Snow Speed: "
            textSize = 16f
            setPadding(0, 0, 20, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        speedLayout.addView(speedLabel)
        
        speedSeekBar = SeekBar(this)
        speedSeekBar.max = 39 // 1 to 40 speed levels
        speedSeekBar.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        speedLayout.addView(speedSeekBar)
        
        speedTextView = TextView(this).apply {
            textSize = 16f
            setPadding(20, 0, 0, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        speedLayout.addView(speedTextView)
        layout.addView(speedLayout)
        
        // Wind setting - consolidated line
        val windLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val windLabel = TextView(this).apply {
            text = "Wind Intensity: "
            textSize = 16f
            setPadding(0, 0, 20, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        windLayout.addView(windLabel)
        
        windSeekBar = SeekBar(this)
        windSeekBar.max = 59 // 1 to 60 wind levels (tripled from 20)
        windSeekBar.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        windLayout.addView(windSeekBar)
        
        windTextView = TextView(this).apply {
            textSize = 16f
            setPadding(20, 0, 0, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        windLayout.addView(windTextView)
        layout.addView(windLayout)
        
        // Wind Chance setting - consolidated line
        val windChanceLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        
        val windChanceLabel = TextView(this).apply {
            text = "Wind Chance: "
            textSize = 16f
            setPadding(0, 0, 20, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        windChanceLayout.addView(windChanceLabel)
        
        windChanceSeekBar = SeekBar(this)
        windChanceSeekBar.max = 100 // 0 to 100 percent
        windChanceSeekBar.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        windChanceLayout.addView(windChanceSeekBar)
        
        windChanceTextView = TextView(this).apply {
            textSize = 16f
            setPadding(20, 0, 0, 0)
            setTextColor(android.graphics.Color.WHITE)
        }
        windChanceLayout.addView(windChanceTextView)
        layout.addView(windChanceLayout)
        
        // Save button
        val saveButton = Button(this).apply {
            text = "Save Settings"
            setPadding(0, 30, 0, 0)
        }
        layout.addView(saveButton)
        
        setContentView(layout)
        
        // Load current values
        val currentTrees = prefs.getInt("numberOfTrees", 12)
        val currentSpeed = prefs.getInt("snowSpeed", 12)
        val currentWind = prefs.getInt("windEffect", 5)
        val currentWindChance = prefs.getInt("windChance", 20)
        treesSeekBar.progress = currentTrees
        speedSeekBar.progress = currentSpeed - 1
        windSeekBar.progress = currentWind - 1
        windChanceSeekBar.progress = currentWindChance
        updateTreesText(currentTrees)
        updateSpeedText(currentSpeed)
        updateWindText(currentWind)
        updateWindChanceText(currentWindChance)
        
        // Set up listeners
        treesSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val trees = progress
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
        
        windChanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val windChance = progress
                updateWindChanceText(windChance)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        saveButton.setOnClickListener {
            // Save settings
            val trees = treesSeekBar.progress
            val speed = speedSeekBar.progress + 1
            val wind = windSeekBar.progress + 1
            val windChance = windChanceSeekBar.progress
            prefs.edit()
                .putInt("numberOfTrees", trees)
                .putInt("snowSpeed", speed)
                .putInt("windEffect", wind)
                .putInt("windChance", windChance)
                .apply()
            
            // Show confirmation and finish
            android.widget.Toast.makeText(this, "Settings saved!", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun updateTreesText(trees: Int) {
        treesTextView.text = "$trees"
    }
    
    private fun updateSpeedText(speed: Int) {
        speedTextView.text = "$speed"
    }
    
    private fun updateWindText(wind: Int) {
        windTextView.text = "$wind"
    }
    
    private fun updateWindChanceText(windChance: Int) {
        windChanceTextView.text = "$windChance%"
    }
} 