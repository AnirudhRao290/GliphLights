## 1. Data Model Changes

- [x] 1.1 Add `GlyphBrightness` enum to Models.kt with OFF, LEVEL_1, LEVEL_2, LEVEL_3 values mapping to SDK brightness floats (0.0, 0.33, 0.66, 1.0)
- [x] 1.2 Add `glyphBrightness: GlyphBrightness` field to `AppSettings` data class with default OFF

## 2. Settings Persistence

- [x] 2.1 Add `GLYPH_BRIGHTNESS` key to `SettingsKeys` in SettingsRepository.kt
- [x] 2.2 Add `updateGlyphBrightness(brightness: GlyphBrightness)` method to `SettingsRepository` interface
- [x] 2.3 Implement `updateGlyphBrightness()` in `SettingsRepositoryImpl` using DataStore

## 3. Repository Layer

- [x] 3.1 Add `val brightness: StateFlow<GlyphBrightness>` to `GlyphRepository` interface
- [x] 3.2 Add `_brightness` MutableStateFlow to `GlyphRepositoryImpl` and expose via `brightness`
- [x] 3.3 Read persisted brightness from SettingsRepository on init and set initial state
- [x] 3.4 Add `updateBrightness(brightness: GlyphBrightness)` method to `GlyphRepository` interface and implementation
- [x] 3.5 Apply brightness to GlyphFrame building in `toggleChannels()` via `buildBrightness(level)`
- [x] 3.6 Apply brightness to GlyphFrame building in `animateChannels()` via `buildBrightness(level)`
- [x] 3.7 Apply brightness to GlyphFrame building in `toggleAll()` and `animateAll()` via `buildBrightness(level)`

## 4. ViewModel Changes

- [x] 4.1 Remove `progressZoneA` and `progressZoneB` MutableStateFlows from `ControlsViewModel`
- [x] 4.2 Remove `updateProgressForZone()` method from `ControlsViewModel`
- [x] 4.3 Add brightness StateFlow exposure from `ControlsViewModel` (reading from `glyphRepository.brightness`)
- [x] 4.4 Add `updateBrightness(brightness: GlyphBrightness)` method to `ControlsViewModel` that calls `glyphRepository.updateBrightness()`

## 5. UI Changes

- [x] 5.1 Remove `ProgressSection` composable for Zone A and Zone B from ControlsScreen.kt
- [x] 5.2 Remove `progressZoneA` and `progressZoneB` state collection from ControlsScreen
- [x] 5.3 Remove zone-to-progress mapping logic from ControlsScreen
- [x] 5.4 Add global brightness slider UI with four discrete positions (Off, Level 1, Level 2, Level 3) to ControlsScreen
- [x] 5.5 Connect brightness slider to ControlsViewModel.updateBrightness()

## 6. Spec Updates

- [x] 6.1 Sync delta specs to main specs using openspec sync-specs
