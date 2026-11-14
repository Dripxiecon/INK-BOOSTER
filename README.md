# INK BOOSTER

A native Android companion for preparing and monitoring a phone before playing Roblox, initially focused on the Samsung Galaxy A05 (4 GB RAM) and The Forge.

## Current prototype

- Native Android dashboard written in Java, with no third-party runtime dependencies.
- Available-memory reading through Android ActivityManager.
- Thermal status where the device exposes Android's thermal API.
- Battery temperature and active network transport.
- One-tap launch of the installed Roblox app.
- Practical graphics and session guidance for a lower-end device.
- Player-first graphics guidance explaining what Roblox's global quality slider can reduce and what requires game-developer support.

## Important limitations

INK BOOSTER does not inject into Roblox, modify game files, change Roblox's internal graphics settings, selectively hide distant player effects, kill background processes, clear memory caches, or guarantee higher FPS or lower ping. Android does not grant ordinary apps those powers. This app reports device information and provides safe actions and guidance.

Roblox's built-in manual graphics quality can reduce the overall rendering workload and may shorten view distance, but it is a broad setting—not a per-effect distance control. Selective player-first rendering (full nearby avatars/effects, simplified distant effects) must be implemented by the experience developer.

## Build

Requirements: JDK 17, Android SDK Platform 35, Android Gradle Plugin 8.9.2, and Gradle 8.11.1 or compatible wrapper.

Open the project in Android Studio and sync Gradle, or run `gradle assembleDebug` with an installed Gradle distribution.

The debug APK is generated at app/build/outputs/apk/debug/app-debug.apk.

## Device target

- Minimum Android: 8.0 (API 26)
- Compile / target SDK: 35
- Package: com.inkbooster

The app is designed to run on newer Android releases, including Android 17, subject to platform compatibility. This first build uses a stable SDK baseline rather than preview-only APIs.

## Roadmap

1. Verify the first build on a physical Galaxy A05.
2. Improve the dashboard with session history and clearer performance signals.
3. Add a guided game-start checklist and user-controlled settings shortcuts.
4. Explore a repeatable before/after session log for FPS, heat, memory, and ping where the platform exposes those readings.
5. Measure real gameplay results before making performance claims.
