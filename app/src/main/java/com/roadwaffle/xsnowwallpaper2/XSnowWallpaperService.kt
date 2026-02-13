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
        
        // Layered snow system
        private val layerCount = 5  // Number of snow layers for depth effect
        private val snowLayers = mutableListOf<MutableList<Snowflake>>()
        
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
        
        // Layered wind storm system with linked propagation effects
        private var layeredWindSystem: LayeredWindSystem? = null
        private var maxStormDuration = 180  // 3 seconds at 60fps
        private var stormPhaseInDuration = 120  // 2 seconds to phase in (doubled from original)
        private var stormPhaseOutDuration = 180  // 3 seconds to phase out (doubled from original)
        
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
            initializeSnowLayers()
            initializeTrees()
            initializeWindSystem()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            screenWidth = width
            screenHeight = height
            // Reinitialize snowflakes and trees when screen size changes
            snowLayers.forEach { it.clear() }
            initializeSnowLayers()
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

        private fun initializeSnowLayers() {
            snowLayers.clear()
            // Create empty lists for each layer
            repeat(layerCount) {
                snowLayers.add(mutableListOf())
            }
            
            // Distribute snowflakes across layers
            val totalSnowflakes = getAdaptiveSnowflakeCount()
            repeat(totalSnowflakes) {
                // Randomly assign to a layer
                val layerIndex = Random.nextInt(layerCount)
                snowLayers[layerIndex].add(createRandomSnowflake(layerIndex))
            }
        }
        
        private fun initializeWindSystem() {
            val windIntensity = getWindEffect()
            val windChance = getWindChance()
            
            layeredWindSystem = LayeredWindSystem(
                layerCount = layerCount,
                windIntensity = windIntensity,
                windChance = windChance,
                windDuration = maxStormDuration,
                windPhaseInDuration = stormPhaseInDuration,
                windPhaseOutDuration = stormPhaseOutDuration
            )
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
        

        private fun createRandomSnowflake(layerIndex: Int = 0): Snowflake {
            val currentSpeed = getSnowSpeed()
            
            // Layer-based properties
            val layerDepth = (layerIndex + 1).toFloat() / layerCount
            val layerSpeed = currentSpeed * layerDepth
            
            val baseSpeed = run {
                val maxSpeed = layerSpeed + 3.0f
                val minSpeed = (maxSpeed * 0.8f).coerceAtLeast(3.0f)
                val r = Random.nextFloat()
                val skewed = kotlin.math.sqrt(r)
                minSpeed + skewed * (maxSpeed - minSpeed)
            }
            
            // Background layers are more transparent, foreground layers more opaque
            val alpha = 0.3f + (layerDepth * 0.7f) // Range from 0.3 to 1.0
            
            return Snowflake(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * screenHeight,
                speed = baseSpeed,
                wind = 0f, // Individual wind removed, now handled by storm system
                size = Random.nextFloat() * 0.5f + 0.5f,
                bitmapIndex = Random.nextInt(snowBitmaps.size),
                layer = layerIndex,
                layerDepth = layerDepth,
                alpha = alpha,
                rotation = Random.nextFloat() * Math.PI.toFloat() * 2f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 0.02f,
                baseSpeed = baseSpeed,
                windSpeedX = 0f
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
            
            // Skip wind updates in power save mode to save battery
            if (!isPowerSaveMode) {
                // Update layered wind system
                layeredWindSystem?.update()
            }
            
            // Update snowflakes in each layer
            snowLayers.forEachIndexed { layerIndex, layer ->
                val windEffect = if (isPowerSaveMode) {
                    0f // No wind in power save mode
                } else {
                    layeredWindSystem?.getWindEffectForLayer(layerIndex) ?: 0f
                }
                
                // Determine how many snowflakes to update based on power mode
                val snowflakesToUpdate = if (isPowerSaveMode) {
                    layer.take(maxOf(5, layer.size / 2)) // Update fewer snowflakes in power save mode
                } else {
                    layer
                }
                
                snowflakesToUpdate.forEach { snowflake ->
                    // Apply base vertical movement
                    snowflake.y += snowflake.speed
                    
                    // Apply wind effect (scaled by layer depth)
                    snowflake.windSpeedX = windEffect * snowflake.layerDepth
                    snowflake.x += snowflake.windSpeedX
                    
                    // Update rotation
                    snowflake.rotation += snowflake.rotationSpeed
                    
                    // Wrap around horizontally
                    if (snowflake.x < -50) snowflake.x = screenWidth + 50f
                    if (snowflake.x > screenWidth + 50) snowflake.x = -50f
                    
                    // Reset if fallen off screen
                    if (snowflake.y > screenHeight + 50) {
                        snowflake.y = -50f
                        snowflake.x = Random.nextFloat() * screenWidth
                    }
                }
            }
            
            // Randomly spawn new snowflakes (reduced frequency in power save mode)
            val spawnChance = if (isPowerSaveMode) {
                getAdaptiveSpawnRate() * 0.3f // Much lower spawn rate in power save mode
            } else {
                getAdaptiveSpawnRate()
            }
            
            val totalSnowflakes = snowLayers.sumOf { it.size }
            if (Random.nextFloat() < spawnChance && totalSnowflakes < getAdaptiveSnowflakeCount()) {
                // Randomly select a layer to spawn snowflake in
                val layerIndex = Random.nextInt(layerCount)
                snowLayers[layerIndex].add(createRandomSnowflake(layerIndex))
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
                    
                    // Draw snowflakes by layer (background to foreground) with proper depth effects
                    for (layerIndex in 0 until layerCount) {
                        val layer = snowLayers.getOrNull(layerIndex) ?: continue
                        
                        val snowflakesToDraw = if (isPowerSaveMode) {
                            layer.take(maxOf(5, layer.size / 2)) // Draw fewer snowflakes in power save mode
                        } else {
                            layer
                        }
                        
                        snowflakesToDraw.forEach { snowflake ->
                            if (snowflake.bitmapIndex < snowBitmaps.size) {
                                val snowBitmap = snowBitmaps[snowflake.bitmapIndex]
                                
                                // Apply layer depth to size
                                val depthScale = snowflake.size * snowflake.layerDepth
                                val size = snowBitmap.width * depthScale
                                
                                // Save canvas state for rotation and alpha
                                canvas.save()
                                
                                // Apply alpha based on layer depth
                                paint.alpha = (snowflake.alpha * 255).toInt()
                                
                                // Translate to snowflake position
                                canvas.translate(snowflake.x, snowflake.y)
                                
                                // Apply rotation
                                canvas.rotate(Math.toDegrees(snowflake.rotation.toDouble()).toFloat())
                                
                                // Draw snowflake centered at origin
                                val srcRect = Rect(0, 0, snowBitmap.width, snowBitmap.height)
                                val dstRect = RectF(
                                    -size / 2,
                                    -size / 2,
                                    size / 2,
                                    size / 2
                                )
                                
                                canvas.drawBitmap(snowBitmap, srcRect, dstRect, paint)
                                
                                // Restore canvas state
                                canvas.restore()
                            }
                        }
                    }
                    
                    // Reset paint alpha for next frame
                    paint.alpha = 255
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
        val bitmapIndex: Int,
        val layer: Int = 0,
        val layerDepth: Float = 1.0f,
        val alpha: Float = 1.0f,
        var rotation: Float = 0f,
        val rotationSpeed: Float = 0f,
        val baseSpeed: Float = speed,
        var windSpeedX: Float = 0f
    )
    
    data class Tree(
        val x: Float,
        val y: Float,
        val scale: Float
    )
    
    /**
     * Wind system for a single layer with phase-in and phase-out effects
     */
    inner class WindSystem(
        private val layerIndex: Int,
        private val totalLayers: Int,
        private val windIntensity: Float,
        private val windChance: Float,
        private val windDuration: Int,
        private val windPhaseInDuration: Int,
        private val windPhaseOutDuration: Int
    ) {
        var isActive = false
            private set
        var direction = 1f // 1 for right, -1 for left
            private set
        var intensity = 0f
            private set
        private var duration = 0
        
        // Layer-specific properties
        val layerDepth = (layerIndex + 1).toFloat() / totalLayers
        private val baseIntensity = windIntensity * layerDepth
        private val layerWindChance = windChance * (0.5f + layerDepth * 0.5f) // Deeper layers have more frequent wind
        
        fun update() {
            // Check if we should start a new wind event
            if (!isActive && Random.nextFloat() < layerWindChance) {
                startWind()
            }
            
            // Update active wind
            if (isActive) {
                duration++
                
                // Calculate wind intensity based on phase
                intensity = when {
                    duration <= windPhaseInDuration -> {
                        // Phase in: ramp up
                        val progress = duration.toFloat() / windPhaseInDuration
                        baseIntensity * progress
                    }
                    duration >= windDuration - windPhaseOutDuration -> {
                        // Phase out: ramp down
                        val remainingDuration = windDuration - duration
                        val progress = remainingDuration.toFloat() / windPhaseOutDuration
                        baseIntensity * progress
                    }
                    else -> {
                        // Full intensity
                        baseIntensity
                    }
                }
                
                // End wind event
                if (duration >= windDuration) {
                    stopWind()
                }
            }
        }
        
        private fun startWind() {
            isActive = true
            direction = if (Random.nextBoolean()) -1f else 1f
            duration = 0
            intensity = 0f
        }
        
        private fun stopWind() {
            isActive = false
            intensity = 0f
            duration = 0
        }
        
        fun getWindEffect(): Float = if (isActive) direction * intensity else 0f
    }
    
    /**
     * Layered wind system manager with linked effects between layers
     * Wind in one layer can propagate to neighboring layers with diminishing intensity
     */
    inner class LayeredWindSystem(
        private val layerCount: Int,
        private val windIntensity: Float,
        private val windChance: Float,
        private val windDuration: Int,
        private val windPhaseInDuration: Int,
        private val windPhaseOutDuration: Int
    ) {
        private val windSystems = mutableListOf<WindSystem>()
        private val linkedEffects = FloatArray(layerCount) { 0f }
        
        init {
            // Create a wind system for each layer
            for (i in 0 until layerCount) {
                windSystems.add(
                    WindSystem(
                        i, layerCount, windIntensity, windChance,
                        windDuration, windPhaseInDuration, windPhaseOutDuration
                    )
                )
            }
        }
        
        fun update() {
            // Update all individual wind systems
            windSystems.forEach { it.update() }
            
            // Calculate linked effects between layers
            calculateLinkedEffects()
        }
        
        private fun calculateLinkedEffects() {
            // Reset linked effects
            linkedEffects.fill(0f)
            
            // For each layer, check if it has active wind and propagate to neighbors
            for (i in 0 until layerCount) {
                val windSystem = windSystems[i]
                if (windSystem.isActive) {
                    val sourceEffect = windSystem.getWindEffect()
                    val sourceDirection = windSystem.direction
                    // Propagate to adjacent layers with diminishing intensity
                    propagateWindToNeighbors(i, sourceDirection, kotlin.math.abs(sourceEffect))
                }
            }
        }
        
        private fun propagateWindToNeighbors(sourceLayerIndex: Int, direction: Float, intensity: Float) {
            // Define propagation falloff - how much wind effect carries to adjacent layers
            val propagationFalloff = floatArrayOf(0.6f, 0.3f, 0.1f) // 60%, 30%, 10% for distance 1, 2, 3
            
            for (distance in 1 until propagationFalloff.size.coerceAtMost(layerCount)) {
                val falloffFactor = propagationFalloff[distance - 1]
                val propagatedIntensity = intensity * falloffFactor
                
                // Propagate to both directions from source layer
                val lowerLayerIndex = sourceLayerIndex - distance
                val upperLayerIndex = sourceLayerIndex + distance
                
                // Add linked effect to lower layer (if valid)
                if (lowerLayerIndex >= 0) {
                    linkedEffects[lowerLayerIndex] += direction * propagatedIntensity
                }
                
                // Add linked effect to upper layer (if valid)
                if (upperLayerIndex < layerCount) {
                    linkedEffects[upperLayerIndex] += direction * propagatedIntensity
                }
            }
        }
        
        fun getWindEffectForLayer(layerIndex: Int): Float {
            return if (layerIndex in 0 until windSystems.size) {
                // Combine the layer's own wind effect with linked effects from neighbors
                val ownEffect = windSystems[layerIndex].getWindEffect()
                val linkedEffect = linkedEffects[layerIndex]
                ownEffect + linkedEffect
            } else {
                0f
            }
        }
    }
} 