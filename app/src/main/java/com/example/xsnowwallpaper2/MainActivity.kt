package com.example.xsnowwallpaper2

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.widget.TextView
import android.widget.Button

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple layout with options
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "XSnow Wallpaper"
            textSize = 24f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(title)
        
        // Set Wallpaper button
        val setWallpaperButton = Button(this).apply {
            text = "Set as Wallpaper"
            setPadding(0, 20, 0, 20)
        }
        layout.addView(setWallpaperButton)
        
        // Settings button
        val settingsButton = Button(this).apply {
            text = "Settings"
            setPadding(0, 20, 0, 0)
        }
        layout.addView(settingsButton)
        
        setContentView(layout)
        
        // Set up button listeners
        setWallpaperButton.setOnClickListener {
            openWallpaperSettings()
            finish()
        }
        
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun openWallpaperSettings() {
        try {
            // Try to open live wallpaper picker directly
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, 
                android.content.ComponentName(this, XSnowWallpaperService::class.java))
            startActivity(intent)
                    } catch (e: Exception) {
                try {
                    // Fallback: open general wallpaper settings
                    val intent = Intent("android.settings.WALLPAPER_SETTINGS")
                    startActivity(intent)
                    
                    // Show toast with instructions
                    Toast.makeText(this, 
                        "Please select 'Live Wallpapers' and choose 'XSnow Wallpaper'", 
                        Toast.LENGTH_LONG).show()
                } catch (e2: Exception) {
                    // Final fallback: open display settings
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                    startActivity(intent)
                    
                    Toast.makeText(this, 
                        "Navigate to Wallpaper settings and select 'XSnow Wallpaper'", 
                        Toast.LENGTH_LONG).show()
                }
            }
    }
} 