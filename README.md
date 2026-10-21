# Floating Note

A lightweight Android floating note overlay app. Create persistent, draggable, resizable notes that stay on top of other apps.

## Features

- **Floating Overlay** — System alert window that stays on top of all apps
- **Drag to Move** — Tap and drag anywhere on the note to reposition
- **Pinch to Resize** — Two-finger pinch gesture to scale the note
- **Resize Handle** — Drag the bottom-right corner to resize precisely
- **Minimize** — Double-tap to collapse into a small dot; double-tap dot to expand
- **Markdown Preview** — Toggle between edit and rendered Markdown view (≡ button)
- **Auto-save** — Text, position, and size persist across reboots
- **Boot Auto-start** — Note reappears automatically after device restart
- **Foreground Service** — Runs reliably with ongoing notification

## Supported Markdown

| Syntax | Example |
|--------|---------|
| Headers | `# H1`, `## H2`, `### H3` |
| Bold | `**text**` or `__text__` |
| Italic | `*text*` or `_text_` |
| Strikethrough | `~~text~~` |
| Inline Code | `` `code` `` |
| Code Blocks | ```` ```code``` ```` |
| Links | `[text](url)` |
| Blockquotes | `> quote` |
| Bullet Lists | `- item` or `* item` |
| Numbered Lists | `1. item` |
| Horizontal Rule | `---` |

## Permissions

| Permission | Purpose |
|------------|---------|
| `SYSTEM_ALERT_WINDOW` | Draw floating overlay on top of other apps |
| `FOREGROUND_SERVICE` | Keep note alive with persistent notification |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14+ foreground service type |
| `RECEIVE_BOOT_COMPLETED` | Restore note on device boot |

## Installation

### From Source
```bash
git clone https://github.com/sherin-nishara/Android-Notes.git
cd Android-Notes
./gradlew installDebug
```

### Requirements
- Android 7.0 (API 24) or higher
- "Display over other apps" permission (granted on first launch)

## Usage

1. **Launch the app** — Opens permission screen for overlay access
2. **Grant permission** — Enable "Display over other apps" for Floating Note
3. **Note appears** — A floating note window shows on screen
4. **Interact**:
   - Type directly in the note
   - Drag header or body to move
   - Pinch with two fingers to resize
   - Drag bottom-right handle for precise resize
   - Tap ≡ to toggle Markdown preview
   - Tap − to minimize to a dot
   - Double-tap dot to restore

## Architecture

```
FloatingNoteService  → Foreground service managing the overlay window
FloatingNoteView     → Custom View (EditText + preview + drag/resize handlers)
NoteRepository       → SharedPreferences persistence (text, position, size)
MarkdownRenderer     → Lightweight Markdown → SpannableString parser
BootReceiver         → Restarts service on BOOT_COMPLETED
MainActivity         → Entry point; requests overlay permission then starts service
```

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Run lint
./gradlew lint
```

## Tech Stack

- **Language**: Kotlin
- **Build**: Gradle (Kotlin DSL)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Service + Custom View (no Jetpack Compose)

## License

MIT License — feel free to use, modify, and distribute.