## Why

The current ControlsScreen provides per-zone progress sliders for Zone A and Zone B, which simulate progress by toggling channels on/off proportionally. This is unintuitive and doesn't represent true brightness control. Users want a single global brightness setting that applies uniformly across all glyph zones, rather than managing brightness independently per zone.

## What Changes

- Remove Zone A progress bar (slider + ProgressSection) from ControlsScreen
- Remove Zone B progress bar (slider + ProgressSection) from ControlsScreen
- Add a global Glyph Brightness slider with four discrete levels: Off, Level 1, Level 2, Level 3
- Brightness setting applies to all glyph operations globally (toggle, animate, etc.)
- Persist brightness setting via DataStore so it survives app restarts
- Remove per-zone progress state from ControlsViewModel (progressZoneA, progressZoneB)

## Capabilities

### New Capabilities
- `global-brightness-control`: Global brightness slider with four discrete levels (Off, Level 1, Level 2, Level 3) applied across all glyph zones

### Modified Capabilities
- `glyph-controls`: Remove Zone A and Zone B progress bar requirements; add global brightness control requirement
- `glyph-sdk-integration`: Remove Zone A and Zone B progress display requirements; add brightness application to GlyphFrame building

## Impact

- **ControlsScreen.kt**: Remove ProgressSection for Zone A and Zone B; add global brightness slider UI
- **ControlsViewModel.kt**: Remove progressZoneA/progressZoneB state; add brightness state and update logic
- **Models.kt**: Add GlyphBrightness enum; add brightness field to AppSettings
- **SettingsRepository.kt**: Add brightness preference key and persistence
- **GlyphRepositoryImpl.kt**: Apply brightness to GlyphFrame building (buildBrightness call)
- **glyph-controls spec**: Update requirements to remove per-zone progress, add global brightness
- **glyph-sdk-integration spec**: Update requirements to remove Zone A/B progress, add brightness frame building
