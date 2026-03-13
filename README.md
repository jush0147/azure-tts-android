# Azure TTS Android

Android TextToSpeech engine backed by Azure Cognitive Services Speech SDK.

## CI APK build

This repository includes GitHub Actions workflow `.github/workflows/build-apk.yml`.

- Trigger manually from **Actions → Build Android APK → Run workflow**.
- Or trigger automatically on push / pull request.
- Download artifacts from workflow run:
  - `app-debug-apk` (debug build, always required)
  - `app-release-unsigned-apk` (release build, optional / non-blocking)

Install debug APK on phone:

```bash
adb install -r app-debug.apk
```

> Release APK from CI is unsigned by default and must be signed before normal installation/distribution.


If CI build fails, download artifact `gradle-build-logs` to inspect full Gradle output.


Build note: Azure Speech SDK dependency is resolved from `https://csspeechstorage.blob.core.windows.net/maven/` (configured in `settings.gradle.kts`).
