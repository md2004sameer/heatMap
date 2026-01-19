# HeatMap - Your Coding Journey on Your Wallpaper

HeatMap is an Android application designed for competitive programmers to stay motivated by keeping their progress right on their home screen. It fetches your LeetCode contribution heatmap and sets it as your wallpaper, along with tracking GeeksforGeeks POTD and other study plans.

## Features

- **LeetCode Heatmap Wallpaper**: Automatically syncs and updates your phone's wallpaper with your LeetCode heatmap.
- **Progress Tracking**: 
    - LeetCode profile statistics.
    - GeeksforGeeks Problem of the Day (POTD) status.
    - Striver's A2Z Sheet tracking.
- **Productivity Tools**:
    - Problem-specific note-taking.
    - Custom training plans and tasks.
    - Contest reminders to never miss a competition.
- **Background Sync**: Uses WorkManager for efficient background updates without draining battery.

## Technical Details

- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose with Material 3
- **Database**: Room (v2.6.1) for local persistence.
- **Networking**: Retrofit 2.11.0 & OkHttp
- **Dependency Injection/Processing**: KSP
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34/35

## Installation & Setup Guide

Follow these steps to get the project running on your local machine:

### 1. Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer is recommended.
- **JDK**: Java 17 or higher.
- **Android Device/Emulator**: Running Android 7.0 (API 24) or higher.

### 2. Clone the Repository
```bash
git clone git@github.com:md2004sameer/heatMap.git
cd heatMap
```

### 3. Open the Project
1. Launch Android Studio.
2. Select **Open** and navigate to the cloned `heatMap` directory.
3. Wait for the IDE to finish the Gradle sync.

### 4. Configure & Build
1. Ensure the `gradle.properties` file has `android.useAndroidX=true` and `android.nonTransitiveRClass=true`.
2. Connect your Android device via USB or start an Emulator.
3. Select the `app` configuration in the toolbar.
4. Click the **Run** button (green play icon) or press `Shift + F10`.

### 5. Using the App
1. On first launch, grant the necessary permissions (Wallpaper, Notifications).
2. Enter your **LeetCode Username** in the settings/profile section.
3. Tap on "Apply Wallpaper" to manually trigger the first update.
4. The app will now periodically update your wallpaper in the background (every 4 hours).

## Troubleshooting
- **Wallpaper not updating**: Ensure the app is not being killed by aggressive battery optimization. You might need to exclude it from battery optimization in System Settings.
- **Gradle Sync Fails**: Check if you have the correct version of the Kotlin/KSP plugins installed. The project uses Kotlin 2.1.0.

## Permissions Explained
- `INTERNET`: Required to fetch heatmap data and POTD.
- `SET_WALLPAPER`: Required to programmatically change the device wallpaper.
- `POST_NOTIFICATIONS`: For contest reminders and update status.
- `SCHEDULE_EXACT_ALARM`: Ensures contest reminders are triggered precisely.
