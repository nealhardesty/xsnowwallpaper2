# XSnow Wallpaper

A modern Android live wallpaper featuring animated Christmas trees with falling snow and dynamic wind storms. Built with Kotlin and optimized for performance with advanced battery management features.

## Features

- **Live Wallpaper Service**: Uses `WallpaperService.Engine` for smooth animation
- **Canvas-based Rendering**: Draws using Canvas and SurfaceHolder for optimal performance
- **Coroutine-powered Animation**: Uses Kotlin coroutines instead of traditional Threads for efficient animation loops
- **Responsive Design**: Adapts to different screen sizes and orientations with automatic surface handling
- **Memory Efficient**: Properly manages bitmap resources with automatic recycling
- **Battery Optimization**: Advanced power management with adaptive frame rates and reduced effects in power save mode
- **Dynamic Wind System**: Realistic wind storms with smooth phase transitions and configurable intensity
- **Multiple Tree Support**: Random placement and scaling of multiple Christmas trees
- **Seven Snowflake Variations**: Different snowflake designs for visual variety

## Customization

### User-Configurable Settings

All major visual effects can be customized through the intuitive in-app settings interface:

#### **Trees (0-36, Default: 12)**
- Controls the number of Christmas trees displayed
- Trees are randomly positioned across the full screen with random scaling (0.8x to 1.3x)
- Set to 0 for a snow-only wallpaper experience

#### **Snow Speed (1-40, Default: 12)**
- Controls the base falling speed of snowflakes
- Each snowflake gets a randomized speed within a calculated range based on this setting
- Higher values create more dramatic snowfall

#### **Wind Intensity (1-60, Default: 5)**
- Controls the strength of wind storms when they occur
- Affects horizontal movement during active storm phases
- Higher values create more dramatic sideways snow movement

#### **Wind Chance (0-100%, Default: 20%)**
- Controls how frequently wind storms occur
- Percentage chance per frame that a storm will begin
- 0% disables wind effects entirely, 100% creates almost constant storms

#### **Battery Optimization Settings**
- **Adaptive Frame Rate**: Automatically reduces frame rate (60fps to 20fps) in power save mode
- **Power Save Mode**: Manual override to enable battery-saving optimizations

### Developer Customization

Advanced users can modify these constants in `XSnowWallpaperService.kt`:

```kotlin
// Core animation settings
private val maxSnowflakes = 200          // Maximum number of snowflakes on screen
private val defaultSnowflakeSpeed = 12.0f // Base speed multiplier for falling snow
private val defaultWindEffect = 0.5f     // Base wind effect strength
private val spawnRate = 0.1f             // Probability of spawning new snowflake per frame

// Wind storm system
private var maxStormDuration = 180       // Storm duration at 60fps (3 seconds)
private var stormPhaseInDuration = 60    // Phase-in duration (1 second)
private var stormPhaseOutDuration = 90   // Phase-out duration (1.5 seconds)

// Battery optimization
private var normalFrameDelay = 16L       // 60 FPS for normal mode
private var lowPowerFrameDelay = 50L     // 20 FPS for power saving
```

### Background Art

The Christmas tree is loaded from `R.drawable.tannenbaum`. Multiple trees are displayed with:
- **Random Positioning**: Full screen coverage with random X and Y coordinates
- **Random Scaling**: Each tree scaled between 0.8x and 1.3x for variety
- **Dynamic Count**: Number of trees (0-36) configurable through settings

### Snowflake Variations

The wallpaper uses seven different snowflake images (`snow00.png` through `snow06.png`) for visual variety. You can:
- Add more snowflake images to the `drawable` folder
- Update the `snowflakeResources` list in `loadBitmaps()` to include new images
- Modify the `bitmapIndex` assignment in `createRandomSnowflake()` for different distribution

## Snow Simulation Mechanics

This section provides a detailed technical breakdown of how the snow simulation works, including all calculations and parameters that control the realistic snowfall behavior.

### Core Physics Engine

The snow simulation operates on a frame-based physics system running at 60 FPS (or 20 FPS in power save mode). Each frame, the system:

1. **Updates existing snowflake positions**
2. **Processes wind storm dynamics**
3. **Spawns new snowflakes probabilistically**
4. **Handles boundary conditions and recycling**

### Snowflake Creation and Properties

Each snowflake is a `data class` with the following properties:

```kotlin
data class Snowflake(
    var x: Float,        // Current horizontal position
    var y: Float,        // Current vertical position
    val speed: Float,    // Individual falling speed (pixels per frame)
    val wind: Float,     // Individual wind susceptibility (unused in current implementation)
    val size: Float,     // Scale multiplier for rendering (0.5-1.0)
    val bitmapIndex: Int // Index into snowflake texture array (0-6)
)
```

#### **Speed Calculation Algorithm**

When a snowflake is created, its speed is calculated using a sophisticated distribution system:

```kotlin
private fun createRandomSnowflake(): Snowflake {
    val currentSpeed = getSnowSpeed() // User setting (1-40)
    return Snowflake(
        speed = run {
            val maxSpeed = currentSpeed + 3.0f
            val minSpeed = (maxSpeed * 0.8f).coerceAtLeast(3.0f)
            val r = Random.nextFloat()
            val skewed = kotlin.math.sqrt(r)
            minSpeed + skewed * (maxSpeed - minSpeed)
        }
        // ... other properties
    )
}
```

**Mathematical Breakdown:**
1. **Base Speed Range**: `maxSpeed = userSetting + 3.0f`
2. **Minimum Speed**: `minSpeed = max(maxSpeed * 0.8, 3.0)` (ensures minimum falling speed)
3. **Distribution Skewing**: Uses `sqrt(random)` to create a bias toward slower speeds
4. **Final Speed**: `minSpeed + skewed * (maxSpeed - minSpeed)`

**Examples:**
- User Setting 12 (default): Speed range 11.6-15.0 pixels/frame
- User Setting 1 (minimum): Speed range 3.0-4.0 pixels/frame  
- User Setting 40 (maximum): Speed range 34.4-43.0 pixels/frame

#### **Size Randomization**

Each snowflake gets a random size multiplier:
```kotlin
size = Random.nextFloat() * 0.5f + 0.5f  // Range: 0.5x to 1.0x
```

#### **Visual Variety**

Snowflakes are randomly assigned one of seven different bitmap textures:
```kotlin
bitmapIndex = Random.nextInt(snowBitmaps.size)  // 0-6 for snow00.png through snow06.png
```

### Dynamic Wind Storm System

The wind system creates realistic weather patterns with smooth transitions between calm and stormy conditions.

#### **Storm State Machine**

The wind system operates as a finite state machine with four distinct phases:

1. **"none"**: No wind effects active
2. **"phase_in"**: Gradually increasing wind intensity (1 second)
3. **"active"**: Full storm intensity (3 seconds)
4. **"phase_out"**: Gradually decreasing wind intensity (1.5 seconds)

#### **Storm Initiation**

Each frame, the system checks whether to start a new storm:

```kotlin
if (stormPhase == "none" && Random.nextFloat() < windChance * 0.01f) {
    startStorm(windLevel)
}
```

**Probability Calculation:**
- User sets Wind Chance (0-100%)
- Per-frame probability = `windChance * 0.01f`
- At 60 FPS with 20% chance: ~12 storms per minute on average

#### **Storm Intensity Calculations**

**Phase In (60 frames = 1 second):**
```kotlin
val phaseProgress = stormDuration.toFloat() / stormPhaseInDuration
stormIntensity = windLevel * 2.0f * phaseProgress
```

**Active Phase (180 frames = 3 seconds):**
```kotlin
stormIntensity = windLevel * 2.0f  // Full intensity
```

**Phase Out (90 frames = 1.5 seconds):**
```kotlin
val phaseProgress = 1.0f - (stormDuration.toFloat() / stormPhaseOutDuration)
stormIntensity = windLevel * 2.0f * phaseProgress
```

#### **Wind Direction**

Storm direction is randomly chosen at storm start:
```kotlin
stormDirection = if (Random.nextBoolean()) 1f else -1f  // Left (-1) or Right (+1)
```

#### **Snowflake Wind Physics**

During active wind, all snowflakes receive horizontal displacement:
```kotlin
snowflake.x += currentWindEffect  // Applied every frame
```

Where `currentWindEffect = stormDirection * stormIntensity`

**Example Calculations:**
- User Wind Intensity: 30
- Active Storm Intensity: `30 * 2.0f = 60.0f pixels/frame`
- Left Storm: `currentWindEffect = -1 * 60.0f = -60.0f`
- During phase-in at 50% progress: `currentWindEffect = -1 * 30.0f = -30.0f`

### Spawning Mechanics

New snowflakes are spawned probabilistically each frame:

```kotlin
if (Random.nextFloat() < spawnRate && snowflakes.size < maxSnowflakes) {
    snowflakes.add(createRandomSnowflake())
}
```

**Spawn Rate Adjustments:**
- **Normal Mode**: `spawnRate = 0.1f` (10% chance per frame)
- **Power Save Mode**: `spawnRate * 0.3f` (3% chance per frame)

At 60 FPS with normal spawn rate: ~6 new snowflakes per second

### Boundary Handling and Recycling

#### **Vertical Recycling**
When snowflakes fall off screen:
```kotlin
if (snowflake.y > screenHeight + 50) {
    snowflake.y = -50f
    snowflake.x = Random.nextFloat() * screenWidth
}
```

#### **Horizontal Wrapping**
Wind can push snowflakes off-screen horizontally:
```kotlin
if (snowflake.x < -50) snowflake.x = screenWidth + 50f
if (snowflake.x > screenWidth + 50) snowflake.x = -50f
```

### Battery Optimization Mechanics

The system implements sophisticated power management:

#### **Adaptive Snowflake Count**
```kotlin
private fun getAdaptiveSnowflakeCount(): Int {
    return if (isPowerSaveMode) {
        maxSnowflakes / 2  // 100 instead of 200
    } else {
        maxSnowflakes      // 200 snowflakes
    }
}
```

#### **Selective Rendering**
In power save mode, only a subset of elements are updated and rendered:
```kotlin
val snowflakesToUpdate = if (isPowerSaveMode) {
    snowflakes.take(maxOf(10, snowflakes.size / 2))
} else {
    snowflakes
}
```

#### **Frame Rate Adjustment**
```kotlin
currentFrameDelay = if (isPowerSaveMode) lowPowerFrameDelay else normalFrameDelay
// 50ms (20 FPS) vs 16ms (60 FPS)
```

### Performance Characteristics

**Normal Mode (60 FPS):**
- Up to 200 active snowflakes
- Full wind storm effects
- All trees rendered with full scaling
- ~6 new snowflakes spawned per second

**Power Save Mode (20 FPS):**
- Up to 100 active snowflakes  
- Reduced spawn rate (1.8 per second)
- Simplified rendering pipeline
- No wind storm processing
- 50% fewer trees rendered

## Technical Details

- **Target API**: 26+ (Android 8.0+)
- **Language**: Kotlin
- **Animation Framework**: Kotlin Coroutines with configurable frame rates
- **Rendering**: Android Canvas API with hardware acceleration
- **Memory Management**: Automatic bitmap recycling and resource cleanup
- **Surface Handling**: Proper canvas locking/unlocking with error handling
- **Persistence**: SharedPreferences for user settings storage
- **Permissions**: SET_WALLPAPER, WAKE_LOCK for power management

## Building and Debugging

### Android Studio Debug Configuration

A debug configuration has been set up for easy testing:

1. **Open the project** in Android Studio
2. **Sync Gradle files** (File → Sync Project with Gradle Files)
3. **Select the debug configuration**: In the toolbar, you should see "Debug XSnow Wallpaper" in the run configurations dropdown
4. **Connect a device or start an emulator** (API 26+ required)
5. **Run the debug configuration**: Click the green play button or press Shift+F10

### Testing the Live Wallpaper

After the app installs successfully, you'll see two launcher icons:

#### **1. Main App Icon (XSnow Wallpaper)**
Opens the primary settings interface with wallpaper activation:
- **Trees (0-36)**: Number of Christmas trees displayed
- **Snow Speed (1-40)**: Base falling speed of snowflakes  
- **Wind Intensity (1-60)**: Strength of wind storms
- **Wind Chance (0-100%)**: Frequency of storm occurrence
- **Set Wallpaper**: Saves settings and opens the system wallpaper picker
- **OK**: Saves settings and returns to launcher

#### **2. Settings Icon (Preferences)**
Opens the advanced preferences interface:
- All visual settings from the main app
- **Adaptive Frame Rate**: Toggle automatic FPS adjustment in power save mode
- **Power Save Mode**: Manual override for battery optimization
- **Save Settings**: Saves all preferences and returns to launcher

### Wallpaper Installation Process

#### **Automatic Method (Recommended):**
1. **Use the "Set Wallpaper" button** in the main app
2. **System will attempt** to open the live wallpaper picker directly
3. **Preview and confirm** the wallpaper selection

#### **Manual Method (Fallback):**
1. **Navigate to device wallpaper settings:**
   - Long press on home screen → "Wallpapers" or
   - Settings → Display → Wallpaper or  
   - Settings → Wallpaper & style
2. **Select "Live Wallpapers"** from the available options
3. **Find "XSnow Wallpaper"** in the list and tap it
4. **Preview the animation** and tap "Set wallpaper"

#### **Troubleshooting Wallpaper Selection:**
The app includes intelligent fallback handling:
- **Primary**: Direct live wallpaper component activation
- **Secondary**: Generic wallpaper settings intent
- **Tertiary**: Display settings with user guidance

### Debug Features

- **Logcat Monitoring**: View detailed logs in Android Studio's Logcat window (View → Tool Windows → Logcat)
- **Breakpoint Debugging**: Set breakpoints in `XSnowWallpaperService.kt` to debug animation logic
- **Performance Profiling**: Use Android Studio's Profiler to monitor CPU, memory, and battery usage
- **Hot Reload**: Make code changes and redeploy without reinstalling
- **Settings Persistence**: SharedPreferences are immediately saved and applied

### Troubleshooting

#### **Installation Issues**
- **App not appearing**: Ensure the device supports live wallpapers (API 26+ required)
- **Installation fails**: Verify device/emulator runs Android 8.0+ (API level 26 or higher)
- **Multiple icons missing**: Check AndroidManifest.xml for proper activity declarations

#### **Runtime Issues**
- **Wallpaper not animating**: Check Logcat for error messages; verify WallpaperService is running
- **Settings not saving**: Ensure SharedPreferences write permissions; check for storage errors
- **Performance stuttering**: Enable Power Save Mode or reduce settings values
- **Battery drain**: Activate Adaptive Frame Rate and reduce tree count and snow speed

#### **Wind Effects Not Working**
- **No storms occurring**: Increase Wind Chance setting above 0%
- **Storms too frequent**: Reduce Wind Chance percentage
- **Wind too weak**: Increase Wind Intensity setting

#### **Optimization for Older Devices**
- **Reduce Trees**: Set to 6-12 trees instead of default 36
- **Lower Snow Speed**: Use values 1-10 for gentler animation
- **Enable Power Save**: Use manual Power Save Mode override
- **Reduce Wind Effects**: Set Wind Intensity to 1-20 and Wind Chance to 5-10%

## Application Structure

### **Core Components**

- **`XSnowWallpaperService`**: Main wallpaper service with rendering engine
- **`MainActivity`**: Primary user interface with settings and wallpaper activation
- **`PreferencesActivity`**: Advanced settings interface with battery optimization options
- **Resource Assets**: Seven snowflake textures and Christmas tree bitmap

### **Permissions and Manifest**

- **`SET_WALLPAPER`**: Required for live wallpaper functionality
- **`WAKE_LOCK`**: Used for power management optimization
- **Live Wallpaper Service**: Properly declared with wallpaper intent filters

## Usage

After installation, the wallpaper appears in the system's live wallpaper picker as "XSnow Wallpaper". The application provides intuitive settings control and automatic wallpaper activation through the main interface.

## License

This project is a modernization of the original xsnowwallpaper project. 