package com.roadwaffle.xsnowwallpaper2

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlinx.coroutines.*
import kotlin.random.Random

class XSnowWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return XSnowEngine()
    }

    inner class XSnowEngine : Engine() {
        private var isVisible = false
        private val snowflakes = mutableListOf<Snowflake>()
        private val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        private val treePaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        private val snowPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 200
        }
        
        private var treeBitmap: Bitmap? = null
        private val snowBitmaps = mutableListOf<Bitmap>()
        private var screenWidth = 0
        private var screenHeight = 0
        private var animationJob: Job? = null
        private var lastTreeCount = 0
        private var lastSpeed = 0
        private var lastWind = 0
        
        // Battery optimization settings
        private var powerManager: PowerManager? = null
        private var isPowerSaveMode = false
        private var adaptiveFrameRate = true
        private var currentFrameDelay = 16L // Default 60 FPS
        private var lowPowerFrameDelay = 50L // 20 FPS for power saving
        private var normalFrameDelay = 16L // 60 FPS for normal mode
        
        // Wind storm system
        private var isStormActive = false
        private var stormDirection = 0f  // -1 for left, 1 for right
        private var stormIntensity = 0f
        private var stormDuration = 0
        private var maxStormDuration = 180  // 3 seconds at 60fps
        private var stormDecayRate = 0.02f  // How quickly storm intensity decreases
        private var stormPhaseInDuration = 60  // 1 second to phase in
        private var stormPhaseOutDuration = 90  // 1.5 seconds to phase out
        private var stormPhase = "none"  // "none", "phase_in", "active", "phase_out"
        
        // Animation settings - tweak these for different effects
        private val maxSnowflakes = 200  // Quadrupled from 50
        private val defaultSnowflakeSpeed = 12.0f  // Doubled from 6.0f
        private val defaultWindEffect = 0.5f
        private val spawnRate = 0.1f // Probability of spawning new snowflake per frame
        
        // Tree settings
        private val defaultNumberOfTrees = 12  // Doubled again from 6
        private val trees = mutableListOf<Tree>()

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            powerManager = getSystemService(POWER_SERVICE) as PowerManager
            loadBitmaps()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            screenWidth = holder.surfaceFrame.width()
            screenHeight = holder.surfaceFrame.height()
            initializeSnowflakes()
            initializeTrees()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            screenWidth = width
            screenHeight = height
            // Reinitialize snowflakes and trees when screen size changes
            snowflakes.clear()
            initializeSnowflakes()
            initializeTrees()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                startAnimation()
            } else {
                stopAnimation()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            stopAnimation()
            releaseBitmaps()
        }

        private fun checkPowerMode() {
            powerManager?.let { pm ->
                val wasPowerSaveMode = isPowerSaveMode
                isPowerSaveMode = pm.isPowerSaveMode || getPowerSaveModeSetting()
                
                // Adjust frame rate based on power mode
                if (getAdaptiveFrameRateSetting()) {
                    currentFrameDelay = if (isPowerSaveMode) lowPowerFrameDelay else normalFrameDelay
                    
                    // If power mode changed, restart animation with new frame rate
                    if (wasPowerSaveMode != isPowerSaveMode && isVisible) {
                        startAnimation()
                    }
                }
            }
        }

        private fun getAdaptiveSnowflakeCount(): Int {
            return if (isPowerSaveMode) {
                maxSnowflakes / 2 // Reduce snowflakes in power save mode
            } else {
                maxSnowflakes
            }
        }

        private fun getAdaptiveTreeCount(): Int {
            val baseTreeCount = getNumberOfTrees()
            return if (isPowerSaveMode) {
                maxOf(1, baseTreeCount / 2) // Reduce trees in power save mode
            } else {
                baseTreeCount
            }
        }

        private fun getAdaptiveSpawnRate(): Float {
            return if (isPowerSaveMode) {
                spawnRate * 0.5f // Reduce spawn rate in power save mode
            } else {
                spawnRate
            }
        }

        private fun getAdaptiveFrameRateSetting(): Boolean {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            return prefs.getBoolean("adaptiveFrameRate", true)
        }

        private fun getPowerSaveModeSetting(): Boolean {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            return prefs.getBoolean("powerSaveMode", false)
        }

        private fun loadBitmaps() {
            // Load the Christmas tree
            treeBitmap = BitmapFactory.decodeResource(resources, R.drawable.tannenbaum)
            
            // Load snowflake variations
            val snowflakeResources = listOf(
                R.drawable.snow00, R.drawable.snow01, R.drawable.snow02,
                R.drawable.snow03, R.drawable.snow04, R.drawable.snow05, R.drawable.snow06
            )
            
            snowflakeResources.forEach { resourceId ->
                BitmapFactory.decodeResource(resources, resourceId)?.let { bitmap ->
                    snowBitmaps.add(bitmap)
                }
            }
        }

        private fun releaseBitmaps() {
            treeBitmap?.recycle()
            treeBitmap = null
            snowBitmaps.forEach { it.recycle() }
            snowBitmaps.clear()
        }

        private fun initializeSnowflakes() {
            snowflakes.clear()
            repeat(getAdaptiveSnowflakeCount()) {
                snowflakes.add(createRandomSnowflake())
            }
        }
        
        private fun initializeTrees() {
            trees.clear()
            val numberOfTrees = getAdaptiveTreeCount()
            lastTreeCount = numberOfTrees
            repeat(numberOfTrees) {
                trees.add(createRandomTree())
            }
        }
        
        private fun createRandomTree(): Tree {
            return Tree(
                x = Random.nextFloat() * screenWidth, // Full screen width
                y = Random.nextFloat() * screenHeight, // Full screen height
                scale = Random.nextFloat() * 0.5f + 0.8f // Random scale between 0.8 and 1.3
            )
        }
        
        private fun getNumberOfTrees(): Int {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            return prefs.getInt("numberOfTrees", defaultNumberOfTrees)
        }
        
        private fun getSnowSpeed(): Float {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            val speedLevel = prefs.getInt("snowSpeed", 12)  // Doubled default
            return speedLevel.toFloat()
        }
        
        private fun getWindEffect(): Float {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            val windLevel = prefs.getInt("windEffect", 5)  // Default wind level
            return windLevel.toFloat() * 2.0f  // Much stronger wind effect multiplier
        }
        
        private fun getWindChance(): Float {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            val windChance = prefs.getInt("windChance", 20)  // Default 20% chance
            return windChance / 100.0f  // Convert percentage to decimal
        }
        
        private fun checkSettingsChanges() {
            val currentTreeCount = getNumberOfTrees()
            val currentSpeed = getSnowSpeed().toInt()
            val currentWind = getWindEffect().toInt()
            
            // Check if tree count has changed
            if (currentTreeCount != lastTreeCount) {
                initializeTrees()
            }
            
            // Update last known values
            lastTreeCount = currentTreeCount
            lastSpeed = currentSpeed
            lastWind = currentWind
        }
        
        private fun updateWindStorm() {
            // Skip storm updates in power save mode to save battery
            if (isPowerSaveMode) {
                return
            }
            
            val windLevel = getWindEffect()
            val windChance = getWindChance()
            
            // Randomly start a storm based on wind chance setting
            if (stormPhase == "none" && Random.nextFloat() < windChance * 0.01f) { // windChance% chance per frame
                startStorm(windLevel)
            }
            
            // Update existing storm based on phase
            when (stormPhase) {
                "phase_in" -> {
                    stormDuration++
                    // Gradually increase storm intensity during phase in
                    val phaseProgress = stormDuration.toFloat() / stormPhaseInDuration
                    stormIntensity = windLevel * 2.0f * phaseProgress
                    
                    if (stormDuration >= stormPhaseInDuration) {
                        stormPhase = "active"
                        stormDuration = 0
                    }
                }
                "active" -> {
                    stormDuration++
                    
                    // Check if storm should start phase out
                    if (stormDuration >= maxStormDuration) {
                        stormPhase = "phase_out"
                        stormDuration = 0
                    }
                }
                "phase_out" -> {
                    stormDuration++
                    // Gradually decrease storm intensity during phase out
                    val phaseProgress = 1.0f - (stormDuration.toFloat() / stormPhaseOutDuration)
                    stormIntensity = windLevel * 2.0f * phaseProgress
                    
                    if (stormDuration >= stormPhaseOutDuration || stormIntensity <= 0f) {
                        endStorm()
                    }
                }
            }
        }
        
        private fun startStorm(windLevel: Float) {
            isStormActive = true
            stormPhase = "phase_in"
            stormDirection = if (Random.nextBoolean()) 1f else -1f
            stormIntensity = 0f // Start at zero, will increase during phase in
            stormDuration = 0
        }
        
        private fun endStorm() {
            isStormActive = false
            stormPhase = "none"
            stormIntensity = 0f
            stormDuration = 0
        }

        private fun createRandomSnowflake(): Snowflake {
            val currentSpeed = getSnowSpeed()
            return Snowflake(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * screenHeight,
                speed = Random.nextFloat() * currentSpeed + 3.0f, // Tripled minimum speed from 1.0f to 3.0f
                wind = 0f, // Individual wind removed, now handled by storm system
                size = Random.nextFloat() * 0.5f + 0.5f,
                bitmapIndex = Random.nextInt(snowBitmaps.size)
            )
        }

        private fun startAnimation() {
            stopAnimation()
            animationJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isVisible) {
                    checkPowerMode() // Check power mode before updating
                    updateSnowflakes()
                    drawFrame()
                    delay(currentFrameDelay) // Use currentFrameDelay
                }
            }
        }

        private fun stopAnimation() {
            animationJob?.cancel()
            animationJob = null
        }

        private fun updateSnowflakes() {
            // Check if settings have changed
            checkSettingsChanges()
            
            // Update wind storm
            updateWindStorm()
            
            // Calculate current wind effect for all snowflakes
            val currentWindEffect = if (isStormActive) {
                stormDirection * stormIntensity
            } else {
                0f
            }
            
            // Update existing snowflakes with optimization for power save mode
            val snowflakesToUpdate = if (isPowerSaveMode) {
                snowflakes.take(maxOf(10, snowflakes.size / 2)) // Update fewer snowflakes in power save mode
            } else {
                snowflakes
            }
            
            snowflakesToUpdate.forEach { snowflake ->
                snowflake.y += snowflake.speed
                snowflake.x += currentWindEffect // Apply storm wind to all snowflakes
                
                // Wrap around horizontally
                if (snowflake.x < -50) snowflake.x = screenWidth + 50f
                if (snowflake.x > screenWidth + 50) snowflake.x = -50f
                
                // Reset if fallen off screen
                if (snowflake.y > screenHeight + 50) {
                    snowflake.y = -50f
                    snowflake.x = Random.nextFloat() * screenWidth
                }
            }
            
            // Randomly spawn new snowflakes (reduced frequency in power save mode)
            val spawnChance = if (isPowerSaveMode) {
                getAdaptiveSpawnRate() * 0.3f // Much lower spawn rate in power save mode
            } else {
                getAdaptiveSpawnRate()
            }
            
            if (Random.nextFloat() < spawnChance && snowflakes.size < getAdaptiveSnowflakeCount()) {
                snowflakes.add(createRandomSnowflake())
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas()
            
            if (canvas != null) {
                try {
                    // Clear the canvas
                    canvas.drawColor(Color.BLACK)
                    
                    // Draw multiple Christmas trees in random positions
                    treeBitmap?.let { tree ->
                        val treesToDraw = if (isPowerSaveMode) {
                            trees.take(maxOf(1, trees.size / 2)) // Draw fewer trees in power save mode
                        } else {
                            trees
                        }
                        
                        treesToDraw.forEach { treeData ->
                            val treeWidth = tree.width
                            val treeHeight = tree.height
                            val scaledWidth = treeWidth * treeData.scale
                            val scaledHeight = treeHeight * treeData.scale
                            
                            val srcRect = Rect(0, 0, treeWidth, treeHeight)
                            val dstRect = RectF(
                                treeData.x - scaledWidth / 2,
                                treeData.y - scaledHeight / 2,
                                treeData.x + scaledWidth / 2,
                                treeData.y + scaledHeight / 2
                            )
                            
                            canvas.drawBitmap(tree, srcRect, dstRect, treePaint)
                        }
                    }
                    
                    // Draw snowflakes with optimization for power save mode
                    val snowflakesToDraw = if (isPowerSaveMode) {
                        snowflakes.take(maxOf(10, snowflakes.size / 2)) // Draw fewer snowflakes in power save mode
                    } else {
                        snowflakes
                    }
                    
                    snowflakesToDraw.forEach { snowflake ->
                        if (snowflake.bitmapIndex < snowBitmaps.size) {
                            val snowBitmap = snowBitmaps[snowflake.bitmapIndex]
                            val size = snowBitmap.width * snowflake.size
                            
                            val srcRect = Rect(0, 0, snowBitmap.width, snowBitmap.height)
                            val dstRect = RectF(
                                snowflake.x - size / 2,
                                snowflake.y - size / 2,
                                snowflake.x + size / 2,
                                snowflake.y + size / 2
                            )
                            
                            canvas.drawBitmap(snowBitmap, srcRect, dstRect, paint)
                        }
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    data class Snowflake(
        var x: Float,
        var y: Float,
        val speed: Float,
        val wind: Float,
        val size: Float,
        val bitmapIndex: Int
    )
    
    data class Tree(
        val x: Float,
        val y: Float,
        val scale: Float
    )
} 