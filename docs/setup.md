# Development setup

## Android Studio

Use an Android Studio version that supports **AGP 8.13.2**. The checked-in wrapper supplies **Gradle 8.13**. Set the IDE's Gradle JDK to **21**, which is also used by CI and the local Firestore emulator verification. Android Gradle Plugin 8.13 requires at least JDK 17. Install **Android SDK Platform 35**, **Build Tools 35.0.0**, Platform-Tools, and an Android Emulator image. API 36 is the verified emulator version. [AGP compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)

1. Open the repository root in Android Studio. Use **SDK Manager** to install the SDK and **Device Manager** to start a disposable AVD.
2. Sync Gradle. Android Studio can generate `local.properties` with the SDK location. See [local.properties.example](../local.properties.example) for the recognized settings.
3. Set the debug Firebase configuration for the intended environment below, then select the `app` run configuration and your emulator or device.

## Firebase

For a signed-in demo, use an approved staging Firebase project. Register Android package `com.example.vclass`, enable Firebase Authentication's email/password provider, and enable Cloud Firestore. The staging maintainer should verify that its deployed rules match the intended policy before exercising classroom operations.

Place the staging client config at `app/src/debug/google-services.json` for a git-ignored debug override. The base file at `app/google-services.json` is the repository's existing client configuration. Avoid using its mapped backend for destructive or fixture-based testing. This project uses the Android client SDK and does not use a service-account JSON file for app setup. [Firebase Android setup](https://firebase.google.com/docs/android/setup)

To compile or run signed-out smoke tests without a Firebase account, put [the synthetic Firebase client configuration](../tests/fixtures/ci-google-services.json) in the same ignored debug location if there is no existing override, or copy it to `app/google-services.json` **in a separate test checkout**. Disconnect the test emulator from the network for the Android suite. The fixture supports SDK initialization and fake-backed tests. Client operations against Firebase require a real staging project. The local Firestore rules suite runs independently of the Android configuration.

## Gemini, Cloudinary, and Realm

- Add `GEMINI_API_KEY` to ignored `local.properties` only for local live Gemini testing. Obtain a dedicated restricted test key from the project owner. Build and emulator regression tests can use an empty value. See [the APK exposure caveat](security.md).
- File uploads use the cloud name and unsigned preset referenced by [CloudinaryConfig](../app/src/main/java/com/example/vclass/core/config/CloudinaryConfig.kt). Review their intended environment with the owner. For isolated upload testing, use approved staging values in a disposable checkout. Use an unsigned preset restricted to the type and size of files needed for the test. Keep any Cloudinary API secret on a trusted server. [Cloudinary upload presets](https://cloudinary.com/documentation/upload_presets)
- Realm needs no setup for the existing screens. The legacy initializer is unregistered in the manifest.

## Build and troubleshoot

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug
```

- **SDK location not found** — Set the Android SDK in the IDE or correct `sdk.dir` in `local.properties`.
- **No matching client** — Confirm the staging config's Android application ID is `com.example.vclass` and sync again.
- **Unable to sign in or load data** — Confirm the app is using the intended staging config, email/password is enabled, and its rules are deployed and reviewed.
- **Gemini unavailable** — Verify the dedicated test key, network, and model access. Keep keys out of logs and screenshots.
- **Firestore tests fail to connect** — Run the npm script with Node 24 and JDK 21, and check that port 8088 is available. The script starts and stops the emulator.

See [Testing](testing.md) and [Manual verification](manual-verification.md).
