# XSnow Wallpaper

A festive Android live wallpaper featuring animated Christmas trees, falling snow, and dynamic wind storms. Customize your winter scene and set it as your home screen background.

## Features

- **Falling Snow** — Up to 200 snowflakes with seven unique designs, varying sizes, and layered depth for a realistic parallax effect
- **Christmas Trees** — Display up to 36 randomly placed and scaled trees, or set to 0 for a snow-only look
- **Dynamic Wind** — Periodic wind storms sweep snow sideways with smooth transitions between calm and gusty conditions
- **Battery Smart** — Adaptive frame rate and a manual power save mode reduce effects and lower FPS to conserve battery
- **Fully Customizable** — Fine-tune every aspect of the scene through an intuitive settings interface

## Settings

| Setting | Range | Default | Description |
|---|---|---|---|
| Trees | 0–36 | 12 | Number of Christmas trees on screen |
| Snow Speed | 1–160 | 24 | How fast snowflakes fall |
| Snow Scale | 0.1x–8.0x | 1.0x | Size multiplier for snowflakes |
| Wind Intensity | 1–60 | 5 | Strength of wind gusts |
| Wind Chance | 0–100% | 20% | How often wind storms kick in |
| Adaptive Frame Rate | On / Off | On | Automatically lowers FPS in system power save mode |
| Power Save Mode | On / Off | Off | Manual override for battery optimization |

## Getting Started

### Install

Build and install via Android Studio (requires Android 8.0+ / API 26+). After installation, two launcher icons appear:

- **XSnow Wallpaper** — Quick settings and wallpaper activation
- **Preferences** — Full settings including snow scale and battery options

### Set as Wallpaper

Tap **Set Wallpaper** in the app to jump directly to the live wallpaper picker. Alternatively, long-press your home screen, choose **Wallpapers** > **Live Wallpapers**, and select **XSnow Wallpaper**.

### Tips for Older or Low-End Devices

- Reduce tree count to 6–12
- Lower snow speed
- Enable Power Save Mode
- Reduce wind intensity and chance

## Requirements

- Android 8.0 or newer (API 26+)

## Building from Source

Open the project in Android Studio, sync Gradle, connect a device or emulator running API 26+, and run. The app is written in Kotlin, uses coroutines for animation, and targets compile SDK 36.

## License

This project is a modernization of the original xsnowwallpaper project.
