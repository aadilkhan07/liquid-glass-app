# Liquid Glass Wallpapers

A modern Android app built with Jetpack Compose to easily preview, edit, and apply glass style effects to high-quality wallpapers.

## Features

- **Gallery Grid**: Browse a curated collection of wallpapers.
- **Liquid Glass Editor**: Drag, move, and resize a glass-effect region with real-time blur, opacity, refraction, and corner adjustments.
- **Instant Wallpaper Apply**: Crop & apply your customized wallpaper instantly with a tap.
- **Smooth, Jetpack Compose UI**: Enjoy responsive, material-based design.

## Screenshots

_(Add your screenshots here)_

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device or emulator (min SDK 33)

### Build & Run

1. Clone this repository
2. Open in Android Studio
3. Sync Gradle
4. Run on your device/emulator

### Dependencies

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
- [Capturable](https://github.com/PatilShreyas/Capturable)

## File Structure

- `app/src/main/java/com.aadil.liquidglass/`
  - `ui/screens/MainScreen.kt` - Main UI and editor logic
  - `viewmodel/EditorViewModel.kt` - State management
  - `model/Wallpaper.kt` - Data model
  - `MainActivity.kt` - App entry
- `app/src/main/res/drawable/` - Wallpaper images

## Customizing

- Add your own images in `res/drawable` and update `EditorViewModel.kt` to include them.
- Tweak glass effect parameters in the editor for unique styles.

## Credits

- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) by Kyant0
- [Capturable](https://github.com/PatilShreyas/Capturable) by Shreyas Patil

---

MIT License
