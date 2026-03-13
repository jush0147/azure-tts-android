# Azure TTS Android

Android TextToSpeech engine backed by Azure Cognitive Services Speech SDK.

## CI APK build

This repository includes GitHub Actions workflow `.github/workflows/build-apk.yml`.

- Trigger manually from **Actions → Build Android APK → Run workflow**.
- Or trigger automatically on push / pull request.
- Download artifact `app-debug-apk` from the workflow run.
- Install on phone:

```bash
adb install -r app-debug.apk
```

> Note: this is a debug APK intended for testing.
