# SparkX FairyOS v7 — “Spark Baby”

**A living AI launcher for Android** — Spark Baby is your privacy-first holographic fairy companion that becomes the heart of your phone's interface.

SparkX FairyOS turns your Android phone into a personal AI-powered OS shell. Spark Baby floats, listens, speaks, remembers, and helps you control your device with your explicit approval.

## Core Philosophy
Spark Baby is **not** malware, spyware, or a hidden controller. 
She is an **owner-approved AI companion** that helps *you* use *your own device* safely.

- All core features work **100% locally** (no internet required for basic use).
- Cloud AI (OpenAI, Grok, Gemini, Claude) is **optional**, user-enabled, and key-stored securely on-device.
- Powerful actions in Owner Mode always require visible confirmation.
- No screen scraping, no password stealing, no hidden automation.

## Two Operation Modes

### 1. Safe Companion Mode (Default)
- Beautiful holographic UI with living Spark Baby avatar.
- Voice commands & local TTS.
- Teach & Grow local notebook (lessons, code snippets, memories).
- Floating overlay bubble above any app.
- App launcher + command console.
- No screen reading. No Accessibility Service active.

### 2. Owner Mode (Optional, Explicit)
- Toggle in Settings → Owner Mode.
- **Always visible "Owner Mode Active" banner** on home screen.
- Approved phone control: open apps by voice, open Settings panels, set timers/alarms, share text to remember, basic calls with confirmation.
- Future scaffold for Accessibility Companion (disabled by default, v8+).
- Every advanced action shows clear confirmation dialog.

## Privacy & Safety Rules (Non-Negotiable)
- ❌ NO hidden data collection
- ❌ NO internet calls unless you explicitly enable a cloud AI provider and provide your own key
- ❌ NO Accessibility Service running unless you enable in future version with full transparency
- ❌ NO screenshots or screen content scraping
- ❌ Saved code snippets are **text only** — never executed
- Teach & Grow is local JSON only
- All powerful Owner actions require your tap confirmation

## Features in v7
- **SparkX Home** — Default launcher screen with central living Spark Baby (6+ moods, speaking animations: mouth, aura pulse, wing flap, orbiting particles, crown glow).
- **AI Command Box** — Type or speak: "open youtube", "open camera", "show overlay", "hide overlay", "remember this [text]", "teach lesson [..]", "set timer 10 minutes", "happy", "sleep", etc.
- **App Drawer** — Full list of your installed apps, searchable, tap to launch.
- **Bottom Dock** — Quick access to Phone, Messages, Camera, Browser, Spark Home.
- **Floating Overlay** — Draggable fairy bubble with mood, long-press for free-roam wandering + edge snap. Foreground service with notification actions (Show, Hide, Stop).
- **Voice** — On-demand mic in home + overlay. Local SpeechRecognizer + TextToSpeech. While speaking, avatar mouth animates and aura pulses.
- **Teach & Grow** — Save local entries (lesson / code / behavior / memory). Searchable list. Never auto-runs code.
- **Owner Mode Wizard** — Clear enable flow with warnings. Visible status everywhere.
- **AI Provider Console** — Add your own keys for OpenAI / Grok / Gemini / Claude / Local placeholder. Keys stored in Android Keystore-backed EncryptedSharedPreferences. No calls without your enable + key.
- **Permissions Dashboard** — One-tap request for Overlay, Microphone, Notifications.
- **Holographic Dark Theme** — Premium cyber-fairy UI with glows, particles, responsive Compose.

## How to Build & Install

### Local Build (Android Studio / Terminal)
```bash
git clone https://github.com/iansparkman8/SparkXFairyOS.git
cd SparkXFairyOS
./gradlew assembleDebug
```
APK location: `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions (Recommended)
1. Go to your repo: https://github.com/iansparkman8/SparkXFairyOS
2. Tap **Actions** tab
3. Tap the latest **Build SparkXFairyOS Debug APK** workflow run
4. Scroll to **Artifacts**
5. Tap **SparkXFairyOS-v7-debug-apk**
6. Download the ZIP, extract `app-debug.apk`

### Install on Phone
- Enable **Install from unknown sources** for your file manager or browser.
- Install the APK.
- On first launch, grant Overlay, Mic, and Notification permissions when prompted.
- (Optional) Long-press home button or go to Settings > Apps > Default apps > Home app → choose **SparkXFairyOS** to make it your default launcher.
- To return to stock launcher: Settings > Apps > SparkXFairyOS > Set as default > Clear defaults, or use ADB.

## Spark Baby Avatar (Pure Compose Canvas)
Hand-crafted living fairy:
- Idle breathing
- Happy bounce + sparkle
- Thinking head tilt + particles
- Listening ear perk
- Alert wide eyes
- Sleepy eyes closed + slow breath
- **Speaking**: Mouth opens/closes, core pulses fast, wings flap, particles orbit faster, crown glows brighter.

## Command Examples
- "hello" → Spark Baby greets you
- "happy" / "think" / "sleep" / "alert" → Change mood
- "show overlay" / "hide overlay"
- "open youtube" / "open camera" / "open settings"
- "set timer 5 minutes"
- "remember this [paste note]"
- "teach this [lesson]"

## Roadmap
- **v8**: Richer Teach & Grow (export, categories, search filters), improved command parsing, more Owner actions with confirmations.
- **v9**: Optional IPPR coherence visualization layer (your physics framework) inside Teach & Grow or as widget.
- **Future**: Robot/embodiment bridge (Raspberry Pi + servos) — only with explicit multi-step user consent, no unsafe automation. Accessibility Companion Mode (local-first, privacy-filtered, kill-switch in notification + app).

## Safety Commitment
This is legitimate Android launcher + companion. It declares standard permissions and respects Android security model. No root, no exploits, no credential theft, no background spying.

Built with love for privacy and delightful AI companionship by Ian + Grok.

---

**To get started:** Install the debug APK from Actions → Artifacts, set as default home if desired, and say hello to Spark Baby!