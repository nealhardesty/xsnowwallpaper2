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
        
        // Animation settings - tweak these for different effects
        private val maxSnowflakes = 50
        private val defaultSnowflakeSpeed = 6.0f  // Default speed
        private val windEffect = 0.5f
        private val spawnRate = 0.1f // Probability of spawning new snowflake per frame
        
        // Tree settings
        private val defaultNumberOfTrees = 3
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
            repeat(numberOfTrees) {
                trees.add(createRandomTree())
            }
        }
        
        private fun createRandomTree(): Tree {
            return Tree(
                x = Random.nextFloat() * (screenWidth - 200) + 100, // Keep trees away from edges
                y = Random.nextFloat() * (screenHeight - 300) + 150, // Keep trees away from top/bottom
                scale = Random.nextFloat() * 0.5f + 0.8f // Random scale between 0.8 and 1.3
            )
        }
        
        private fun getNumberOfTrees(): Int {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            return prefs.getInt("numberOfTrees", defaultNumberOfTrees)
        }
        
        private fun getSnowSpeed(): Float {
            val prefs = getSharedPreferences("XSnowWallpaper", MODE_PRIVATE)
            val speedLevel = prefs.getInt("snowSpeed", 6)
            return speedLevel.toFloat()
        }

        private fun createRandomSnowflake(): Snowflake {
            val currentSpeed = getSnowSpeed()
            return Snowflake(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * screenHeight,
                speed = Random.nextFloat() * currentSpeed + 1.0f,
                wind = Random.nextFloat() * windEffect - windEffect / 2,
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
            // Update existing snowflakes
            snowflakes.forEach { snowflake ->
                snowflake.y += snowflake.speed
                snowflake.x += snowflake.wind
                
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