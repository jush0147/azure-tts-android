# Azure TTS Android

Android TextToSpeech engine backed by Azure Cognitive Services Speech SDK.

## CI APK build

This repository includes GitHub Actions workflow `.github/workflows/build-apk.yml`.

- Trigger manually from **Actions → Build Android APK → Run workflow**.
- Or trigger automatically on push / pull request.
- Download artifacts from workflow run:
  - `app-debug-apk` (debug build, always required)
  - `app-release-unsigned-apk` (raw release output)
  - `app-release-debug-signed-apk` (release variant signed in CI for direct install testing)

Install debug APK on phone:

```bash
adb install -r app-debug.apk
```

> Release APK from CI is unsigned by default and must be signed before normal installation/distribution.


If CI build fails, download artifact `gradle-build-logs` to inspect full Gradle output.

CI also prints the last 200 lines of the debug Gradle log directly in the workflow UI (Step Summary), so you can read key errors without downloading artifacts.


Build note: Azure Speech SDK dependency is resolved from `https://csspeechstorage.blob.core.windows.net/maven/` (configured in `settings.gradle.kts`).

CI failure summary includes key `error:` / `e:` compiler lines plus the last 200 log lines.


S24 checks (adb):
```bash
adb shell cmd package query-intent-services -a android.intent.action.TTS_SERVICE
adb shell pm list packages | grep azuretts
adb logcat -d | grep -i -e TextToSpeech -e tts -e AzureTTS
```
