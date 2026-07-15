# Glyph Control

A native Android app for controlling the **Nothing Phone (3a) Pro** Glyph LED interface. Built with Jetpack Compose, Material 3, and the official Nothing Glyph SDK.

## Features

- **Dashboard** — Real-time glyph status, device info, and one-tap controls
- **Glyph Controls** — Toggle 36 individual channels across 3 zones (A: 11, B: 5, C: 20)
- **Zone Toggle** — Turn entire zones on/off while preserving other zones
- **Additive Toggle** — Multiple channels stay on simultaneously
- **Animation** — Breathing effects with configurable period, cycles, and interval
- **Progress Display** — Visual progress bars for all 3 zones (Zone C uses native `displayProgress`, Zones A/B simulate via channel stepping)
- **Home Screen Widget** — Glance widget with toggle and turn off buttons
- **Quick Settings Tile** — Toggle glyphs from the notification shade
- **Settings** — Customize default animation parameters, theme, and startup behavior
- **Splash Screen** — Glyph logo on launch

## Architecture

```
app/src/main/java/com/example/gliphlights/
├── di/                  # Hilt dependency injection modules
├── models/              # Data classes, enums, sealed classes
├── repository/          # GlyphRepository interface + impl, SettingsRepository
├── sdk/                 # GlyphManagerWrapper (real SDK calls)
├── services/            # GlyphTileService (Quick Settings)
├── ui/
│   ├── screens/         # Dashboard, Controls, Settings composables
│   └── theme/           # Material 3 theme (monochrome Nothing OS-inspired)
├── utils/               # ErrorHandler, AppLifecycleObserver
├── viewmodel/           # DashboardViewModel, ControlsViewModel, SettingsViewModel
└── widgets/             # GlyphWidget (Glance), GlyphWidgetReceiver
```

**Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, KSP, Navigation Compose, Glance, DataStore, Coroutines

## Requirements

- **Device:** Nothing Phone (3a) Pro (model `is24111`)
- **OS:** Android 14+ (API 34)
- **SDK:** Nothing Glyph SDK (`com.nothing.ketchum`)

## Build

```bash
./gradlew assembleDebug
```

> **Note:** The Glyph SDK AAR (`app/libs/GlyphSDK.aar`) is not included in the repo. Download it from the [Nothing Developer Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) and place it in `app/libs/`.

## Glyph Zones

| Zone | Channels | Index Range | Hardware |
|------|----------|-------------|----------|
| A    | A1–A11   | 20–30       | Vertical bar |
| B    | B1–B5    | 31–35       | Horizontal bar |
| C    | C1–C20   | 0–19        | Camera ring |

## SDK Integration

The app uses the official Nothing Glyph SDK APIs:

- `toggle(GlyphFrame)` — Turn specific channels on/off
- `animate(GlyphFrame)` — Breathing animation with parameters
- `displayProgress(GlyphFrame, progress, reverse)` — Progress indicator on Zone C
- `turnOff()` — Immediately turn off all glyphs

All SDK calls run on `Dispatchers.IO` with full error handling via `SdkResult`.

## OpenSpec

Change tracking and specs are maintained in `openspec/`:

- **Main specs:** `openspec/specs/` — 8 capability specs
- **Archived changes:** `openspec/changes/archive/` — 3 completed changes

## License

MIT
