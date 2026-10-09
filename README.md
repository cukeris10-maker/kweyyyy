# 🎮 Floating Arrow Keys

A floating overlay keyboard for Android that injects **real physical KeyEvents** — perfect for games ported to Android that require keyboard input (arrow keys + space).

## Features
- Floating draggable keyboard overlay
- ↑ ↓ ← → arrow keys + Space bar
- Injects real `KeyEvent` objects (same as physical keyboard)
- Works on top of any app/game
- Dark semi-transparent UI that stays out of your way

## How It Works

The app uses `WindowManager.TYPE_APPLICATION_OVERLAY` to draw buttons on top of all other apps. When pressed, each button dispatches a real `KeyEvent` with `FLAG_FROM_SYSTEM | FLAG_SOFT_KEYBOARD` flags — this is what games detect as physical key input.

## Setup & Build

### Option A: GitHub Actions (Recommended)
1. Push this repo to GitHub
2. GitHub Actions auto-builds the APK on every push
3. Download the APK from **Actions → Your workflow run → Artifacts**
4. To make a release: `git tag v1.0 && git push --tags`

### Option B: Local Build
```bash
# Requires Android Studio or command-line SDK
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## Installation

1. Install the APK on your Android device
2. Open **Floating Arrow Keys** app
3. Grant **"Display over other apps"** permission when prompted
4. Press **"Show Floating Keys"**
5. Open your game — the floating keyboard appears on top!
6. Drag it anywhere using the handle at the top

## Permissions Required

| Permission | Reason |
|-----------|--------|
| `SYSTEM_ALERT_WINDOW` | Draw the floating overlay on top of other apps |
| `FOREGROUND_SERVICE` | Keep the overlay alive while gaming |

## Key Codes Sent

| Button | Android KeyCode |
|--------|----------------|
| ↑ | `KEYCODE_DPAD_UP` |
| ↓ | `KEYCODE_DPAD_DOWN` |
| ← | `KEYCODE_DPAD_LEFT` |
| → | `KEYCODE_DPAD_RIGHT` |
| SPACE | `KEYCODE_SPACE` |

## Notes

- Keys are sent as both `ACTION_DOWN` and `ACTION_UP` (triggered on finger press/release), supporting hold-to-move
- If a specific game still doesn't respond, it may use a custom input library — open an issue and we can try `AccessibilityService` injection as an alternative
