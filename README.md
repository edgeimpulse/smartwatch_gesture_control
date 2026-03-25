# Gesture Control Demo

A Wear OS app for Galaxy Watch 4 that uses on-device gesture recognition (via Edge Impulse) to control a WiZ smart bulb over Wi-Fi.

## Gestures

| Gesture | Action |
|---|---|
| Double clap | Toggle bulb on/off |
| Brighten | Increase brightness |
| Dim | Decrease brightness |
| Color cycle | Cycle to next colour |

## Requirements

- Galaxy Watch 4 (Wear OS, API 30+)
- WiZ smart bulb on the same Wi-Fi network as the watch
- Android Studio Meerkat or later
- NDK r25+ and CMake 3.22.1

## Build

1. Clone the repo
2. Open the project in Android Studio
3. Build > Make Project

The Edge Impulse SDK and trained model are included in `app/src/main/cpp/ei-deployment/` — no additional download is needed.

## First-run configuration

On first launch, open the **Settings** page (swipe left twice) and enter your bulb's IP address and port, then tap **SAVE**. Use **TEST** to confirm the watch can reach the bulb before enabling gesture control.

Default values: `192.168.1.103 : 38899`

## Updating the model

When you retrain the model in Edge Impulse Studio:

1. Go to **Deployment** in Edge Impulse Studio
2. Select **C++ Android library** and click **Build**
3. Extract the downloaded `.zip` — you will get a folder like `imu-test-light-cpp-android-v3/`
4. Run the update script from the project root:

```bat
update_ei_deployment.bat C:\path\to\imu-test-light-cpp-android-v3
```

5. In Android Studio: **Build > Clean Project**, then **Build > Rebuild Project**

The script validates the source folder, removes the old deployment, and copies the new one into `app/src/main/cpp/ei-deployment/`.

### Manual update (without the script)

Delete `app/src/main/cpp/ei-deployment/` and replace it with the contents of the extracted export folder, then rebuild.
