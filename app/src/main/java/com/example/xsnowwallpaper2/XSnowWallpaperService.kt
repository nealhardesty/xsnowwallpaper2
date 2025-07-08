package com.example.xsnowwallpaper2

import android.graphics.*
import android.os.Handler
import android.os.Looper
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
        
        // Wind storm system
        private var isStormActive = false
        private var stormDirection = 0f  // -1 for left, 1 for right
        private var stormIntensity = 0f
        private var stormDuration = 0
        private var maxStormDuration = 180  // 3 seconds at 60fps
        private var stormDecayRate = 0.02f  // How quickly storm intensity decreases
        
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
            repeat(maxSnowflakes) {
                snowflakes.add(createRandomSnowflake())
            }
        }
        
        private fun initializeTrees() {
            trees.clear()
            val numberOfTrees = getNumberOfTrees()
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
            val windLevel = getWindEffect()
            
            // Randomly start a storm
            if (!isStormActive && Random.nextFloat() < 0.005f) { // 0.5% chance per frame
                startStorm(windLevel)
            }
            
            // Update existing storm
            if (isStormActive) {
                stormDuration++
                
                // Check if storm should end
                if (stormDuration >= maxStormDuration) {
                    endStorm()
                } else {
                    // Gradually decrease storm intensity
                    stormIntensity -= stormDecayRate
                    if (stormIntensity <= 0f) {
                        endStorm()
                    }
                }
            }
        }
        
        private fun startStorm(windLevel: Float) {
            isStormActive = true
            stormDirection = if (Random.nextBoolean()) 1f else -1f
            stormIntensity = windLevel * 2.0f // Strong initial intensity
            stormDuration = 0
        }
        
        private fun endStorm() {
            isStormActive = false
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
                    updateSnowflakes()
                    drawFrame()
                    delay(16) // ~60 FPS
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
            
            // Update existing snowflakes
            snowflakes.forEach { snowflake ->
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
            
            // Randomly spawn new snowflakes
            if (Random.nextFloat() < spawnRate && snowflakes.size < maxSnowflakes) {
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
                        trees.forEach { treeData ->
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
                    
                    // Draw snowflakes
                    snowflakes.forEach { snowflake ->
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