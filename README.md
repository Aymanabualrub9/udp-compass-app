# UdpCompassSender - Android source (ready to build)

This repository contains a simple Android Studio project that:
- uses Fused Location Provider + Rotation Vector sensor
- sends JSON UDP packets (latitude, longitude, accuracy, heading) to a configured IP and port
- runs as a foreground service to keep sending in background

**Important:** I cannot build an APK in this environment because there is no Android SDK / build tools here.
You can build the APK locally by following these steps:

1. Install Android Studio (Chip: Windows/Mac/Linux) and necessary SDKs (API 34 recommended).
2. Open this folder in Android Studio: `File -> Open -> /path/to/project`
3. Let Gradle sync. If prompted, install missing SDK components.
4. Connect your phone (enable USB debugging) or use an emulator, then build & Run or Build -> Build Bundle(s) / APK(s) -> Build APK(s).
5. The generated APK will be under `app/build/outputs/apk/`.

If you want, I can:
- walk you through building step-by-step,
- modify code (change default port, add UI features),
- or help you produce a release-signed APK (I'll provide the commands and gradle changes).

**Default settings in app:**
- default destination IP: 192.168.1.2
- default destination port: 5005
- default send rate: 5 Hz

**Note about permissions:** On modern Android you will be asked for location permission at runtime. For background sending you may also need `ACCESS_BACKGROUND_LOCATION`.
