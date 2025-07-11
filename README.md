# XSnow Wallpaper

A modern Android live wallpaper featuring an animated Christmas tree with falling snow. Built with Kotlin and Jetpack libraries.

## Features

- **Live Wallpaper Service**: Uses `WallpaperService.Engine` for smooth animation
- **Canvas-based Rendering**: Draws using Canvas and SurfaceHolder for optimal performance
- **Coroutine-powered Animation**: Uses Kotlin coroutines instead of traditional Threads
- **Responsive Design**: Adapts to different screen sizes and orientations
- **Memory Efficient**: Properly manages bitmap resources

## Customization

### Snowflake Behavior

You can tweak the snowflake animation by modifying these constants in `XSnowWallpaperService.kt`:

```kotlin
// Animation settings - tweak these for different effects
private val maxSnowflakes = 200         // Maximum number of snowflakes (quadrupled)
private val defaultSnowflakeSpeed = 12.0f // Default speed of falling snow (doubled)
private val defaultWindEffect = 0.5f    // Default wind effect strength
private val spawnRate = 0.1f            // Probability of spawning new snowflake per frame
```

**Note:** Snow speed (1-40 levels, minimum speed tripled), wind effect (1-60 levels, creates periodic storms with smooth phase-in/phase-out transitions), and wind chance (0-100%, controls storm frequency) are now configurable in the app settings. Trees can be set to 0 for a snow-only wallpaper.

### Background Art

The Christmas tree is loaded from `R.drawable.tannenbaum`. Multiple trees are displayed in random positions with random scaling (0.8x to 1.3x). The number of trees (0-36, default: 12) can be configured in the app settings. Set to 0 for a snow-only wallpaper.

### Snowflake Variations

The wallpaper uses multiple snowflake images (`snow00.png` through `snow06.png`) for variety. You can:
- Add more snowflake images to the `drawable` folder
- Update the `snowflakeResources` list in `loadBitmaps()` to include new images
- Modify the `bitmapIndex` assignment in `createRandomSnowflake()` for different distribution

## Technical Details

- **Target API**: 26+ (Android 8.0+)
- **Language**: Kotlin
- **Animation**: Coroutines with 60 FPS target
- **Memory Management**: Automatic bitmap recycling
- **Surface Handling**: Proper canvas locking/unlocking

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

1. **Main App Icon** - Opens settings and wallpaper picker:
   - **Trees**: Adjust number of trees (0-36)
   - **Snow Speed**: Adjust snow speed (1-40)
   - **Wind Intensity**: Adjust wind effect (1-60)
   - **Wind Chance**: Adjust storm frequency (0-100%)
   - **Set Wallpaper**: Save settings and open wallpaper picker
   - **OK**: Save settings and return to launcher

2. **Settings Icon** - Opens preferences only:
   - Same settings as above but without opening wallpaper picker
   - **Save Settings**: Saves and returns to launcher

To set the wallpaper:
1. **Select "Live Wallpapers"** from the wallpaper options
2. **Find "XSnow Wallpaper"** in the list and tap it
3. **Preview and set** the wallpaper

**Alternative manual method:**
- Go to your device's wallpaper settings:
  - Long press on home screen → "Wallpapers" or
  - Settings → Display → Wallpaper or
  - Settings → Wallpaper & style
- Select "Live Wallpapers" and choose "XSnow Wallpaper"

### Debug Features

- **Logcat**: Monitor logs in Android Studio's Logcat window (View → Tool Windows → Logcat)
- **Breakpoints**: Set breakpoints in `XSnowWallpaperService.kt` to debug animation logic
- **Performance**: Use Android Studio's Profiler to monitor CPU, memory, and battery usage
- **Hot reload**: Make code changes and redeploy without reinstalling

### Troubleshooting

- **App not appearing**: Ensure the device supports live wallpapers (most modern devices do)
- **Installation fails**: Check that your device/emulator has API 26+ (Android 8.0+)
- **Wallpaper not animating**: Check Logcat for any error messages
- **Performance issues**: Reduce `maxSnowflakes` in the service for better performance on older devices

## Usage

After installation, the wallpaper will appear in the system's live wallpaper picker as "XSnow Wallpaper".

## License

This project is a modernization of the original xsnowwallpaper project. 