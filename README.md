# MunawarAI — Android Voice Assistant

A real, buildable Android Studio project: voice-first assistant, Gemini-powered
understanding, onboarding with a live "Test API Key" check, personas/voice tuning,
and real device actions (calls, SMS, apps, torch, volume, alarms, settings panels,
YouTube/Maps/browser/email) — every action confirmed and verified, never faked.

## What's included

- **Onboarding** — welcome → paste your Gemini API key → **Test Key** button that
  calls Gemini live and shows a green check or the error → pick a personality →
  done. Key is stored only in local DataStore, never hard-coded.
- **Core AI pipeline** — speech → Gemini (structured-JSON-only prompt) → whitelist
  validator → `ActionExecutor` → real Android API call → verify → speak result.
  The model is never allowed to run arbitrary code; it can only request one of a
  fixed set of intents (`CALL_CONTACT`, `SEND_SMS`, `OPEN_APP`, `TORCH`,
  `SET_VOLUME`, `OPEN_SETTINGS_PANEL`, `SET_ALARM`, `PLAY_YOUTUBE`, `OPEN_MAPS`,
  `SEND_EMAIL`, `OPEN_BROWSER`, `DEVICE_INFO`, `CHAT`).
- **Controllers** — `CallController`, `SmsController`, `ContactResolver` (with
  Urdu relationship aliases like بھائی/امی), `AppController`, `TorchController`,
  `VolumeController`, `SettingsController` (opens the right system panel when
  Android blocks a direct toggle), `AlarmController`, `MediaController`
  (YouTube/Maps/browser/email via intents), `DeviceInfoController`.
- **Voice** — `VoiceEngine` wraps Android TTS (prefers a female voice if the
  device has one, adjustable pitch/rate, English/Urdu). `SpeechInputManager`
  wraps `SpeechRecognizer` with partial + final results.
- **Personas** — Friendly, Professional, Cute, Companion, Minimal, Energetic —
  each nudges pitch/rate and is fed into the Gemini system prompt.
- **Settings screen** — edit/test the API key any time, switch persona and
  language, tune pitch/speed sliders, toggle "Always Active" listening, and a
  plain-language privacy note.
- **Risk-based confirmation** — medium/high-risk actions (SMS, email, calls,
  alarms) always ask "Should I go ahead?" before doing anything; low-risk actions
  (torch, volume, opening an app) run immediately.
- **"Always Active" foreground service** — an explicit, user-toggled continuous
  listening loop with a persistent notification (Android requires this for any
  background microphone use) and a `BootReceiver` to resume it after reboot if
  the user left it on.
- Glowing dark-mode-first orb UI in Compose/Material 3.

## Important honesty note on "wake word"

Android has **no public always-on, low-power wake-word API**. A true "Hey
Munawar" that listens with minimal battery drain needs a dedicated engine such
as **Picovoice Porcupine** (free tier available) trained on a custom wake phrase.
What's implemented here is a fully disclosed, opt-in alternative: when "Always
Active" is on, the app restarts Android's regular `SpeechRecognizer` after every
utterance, backed by a foreground service with a visible notification. It costs
more battery than a real wake-word engine. If you want the "Hey Munawar" phrase
specifically (rather than tap-to-talk or continuous free-form listening), the
next step is dropping in the Porcupine Android SDK inside `SpeechInputManager`
and only starting `SpeechRecognizer` once Porcupine detects the phrase.

## Not included yet (flagged, not faked)

These were in the original spec but are large enough to be their own follow-up
so the app you get today actually compiles and runs:

- **NotificationListenerService** (reading/summarizing notifications) — needs
  its own permission screen and listener class.
- **AccessibilityService** automation (clicking UI elements Android otherwise
  blocks) — powerful and worth its own careful review before adding.
- **Calendar read/create** — permissions are declared in the manifest, but no
  `CalendarController` is wired up yet.
- **Authenticated background email sending** (vs. today's "open composer
  pre-filled") — needs an OAuth flow.
- Multi-contact disambiguation currently answers in text/voice ("which one do
  you mean?") rather than a tappable picker UI.

## Getting a Gemini API key

1. Go to **https://aistudio.google.com/app/apikey**.
2. Sign in, click **Create API key**, copy it.
3. Paste it into the app's onboarding screen (or Settings later) and tap
   **Test API Key** — you should see a green check and "Key is valid."

Free tier is enough for this app's usage pattern (Gemini 2.0 Flash).

## Building it

1. Install **Android Studio** (Ladybug/2024.2 or newer).
2. `File → Open` and select the `MunawarAI` folder (the one containing
   `settings.gradle.kts`).
3. Let Gradle sync — it will download the Gradle 8.7 distribution and all
   dependencies (Compose BOM, DataStore, OkHttp, kotlinx-serialization,
   Navigation Compose) automatically.
4. Plug in a phone (Developer Options → USB debugging on) or start an emulator
   running **API 26+**.
5. Click **Run ▶**.
6. On first launch: grant the permission prompts (mic, calls, contacts, SMS,
   camera), then walk through onboarding and paste your Gemini key.

### If Gradle sync complains about the wrapper

Android Studio can regenerate it: `File → Sync Project with Gradle Files`, or
run `gradle wrapper --gradle-version 8.7` once you have any Gradle installed
locally. The `gradle/wrapper/gradle-wrapper.properties` file is already
pointing at 8.7.

## Trying it out

Tap the mic and try:

- "Turn on the flashlight" → torch on, verified, confirmed by voice.
- "Set volume to 30 percent" → media volume changes.
- "Open YouTube" → launches the app if installed.
- "Turn on Wi-Fi" → on most modern Android versions this can't be toggled
  directly by a third-party app, so it opens the Wi-Fi settings panel and
  says so explicitly, instead of pretending it succeeded.
- "Call Ali" / "Text Mom I'm on my way" → resolves the contact, asks "Should
  I go ahead?", then calls/sends only after you confirm.
- "What's my battery percentage?" → reads it directly.

## Where to extend it

- **New action type**: add it to `ActionType` in `ActionModels.kt`, add it to
  the whitelist description in `buildSystemPrompt`, add a branch in
  `ActionExecutor.execute`, and (if it needs one) a new controller in
  `core/executor/`.
- **New persona**: add an enum entry to `Persona` in `PreferencesManager.kt`.
- **Swap AI provider**: implement the `AIProvider` interface (see
  `GeminiProvider`) with e.g. Groq or OpenAI, and construct that instead in
  `MunawarApplication`.
- **Real wake word**: see the honesty note above.

## Pushing to GitHub

```bash
cd MunawarAI
git init
git add .
git commit -m "MunawarAI: initial Android voice assistant"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

`.gitignore` already excludes `build/`, `.gradle/`, `.idea/`, `local.properties`
and any `*.jks`/`*.keystore` files, so you won't accidentally commit build
output or signing keys.

## GitHub Actions — automatic APK builds

`.github/workflows/build-apk.yml` builds a debug APK on every push/PR to
`main` (or on demand via **Actions → Build APK → Run workflow**). It:

1. Checks out the repo.
2. Installs JDK 17 and the Android SDK.
3. Installs Gradle 8.7 directly (via `gradle/actions/setup-gradle`) and runs
   `gradle assembleDebug` — this **does not require a committed Gradle
   wrapper jar**, which is a binary file this project intentionally doesn't
   ship, so there's nothing stale or mismatched to break the CI build.
4. Uploads the resulting APK as a workflow artifact.

**To get the APK:** after a run finishes, open the run under the repo's
**Actions** tab → scroll to **Artifacts** → download `MunawarAI-debug-apk`.

### If you also want a signed release APK

Add a `keystore.jks` as a GitHub **Actions secret** (base64-encoded) plus its
passwords, decode it in a workflow step, add a `signingConfigs { release { ... } }`
block to `app/build.gradle.kts`, and change the build step to
`gradle assembleRelease`. Left out here since it needs your own keystore.

### Local Gradle wrapper note

This project ships `gradle/wrapper/gradle-wrapper.properties` (pointing at
Gradle 8.7) but not the wrapper's binary jar, so `./gradlew` won't run until
that jar exists. Opening the project in Android Studio and syncing will
generate it automatically; from a terminal with Gradle installed you can also
run `gradle wrapper --gradle-version 8.7` once. CI never needs this — the
workflow installs Gradle itself.
