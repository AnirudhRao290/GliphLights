## Context

The GliphLights app controls Nothing Phone (3a) Pro glyph LEDs across three zones: Zone A (vertical bar, 11 channels), Zone B (horizontal bar, 5 channels), and Zone C (camera ring, 20 channels). The current ControlsScreen provides per-zone progress sliders for Zone A and Zone B that simulate progress by toggling channels on/off proportionally. This approach is unintuitive and doesn't represent true brightness control. The Nothing Glyph SDK provides a `buildBrightness()` method on `GlyphFrameBuilder` that accepts a float value (0.0–1.0) for native brightness control.

## Goals / Non-Goals

**Goals:**
- Replace Zone A and Zone B progress sliders with a single global brightness control
- Provide four discrete brightness levels: Off (0%), Level 1 (33%), Level 2 (66%), Level 3 (100%)
- Apply brightness globally to all glyph operations (toggle, animate, etc.)
- Persist brightness setting across app restarts via DataStore
- Simplify the ControlsScreen UI by removing per-zone progress complexity

**Non-Goals:**
- Changing per-zone toggle or animate functionality
- Adding continuous brightness slider (sticking to 4 discrete levels)
- Modifying the DashboardScreen layout
- Adding brightness control to Zone C (it already uses SDK's native displayProgress)

## Decisions

### Decision 1: Use GlyphBrightness enum with 4 levels

**Choice:** Define a `GlyphBrightness` enum with `OFF`, `LEVEL_1`, `LEVEL_2`, `LEVEL_3` values mapping to SDK brightness floats 0.0, 0.33, 0.66, 1.0.

**Alternative considered:** Continuous float slider (0.0–1.0). Rejected because the user explicitly requested four discrete levels, and discrete steps are simpler to persist and reason about.

### Decision 2: Apply brightness in GlyphRepositoryImpl via buildBrightness()

**Choice:** Modify `GlyphRepositoryImpl` to call `glyphFrameBuilder.buildBrightness(level)` when building frames for toggle/animate operations. The brightness value is read from a `StateFlow` exposed by the repository.

**Alternative considered:** Apply brightness at the ViewModel layer by modifying channel selection. Rejected because it doesn't use the SDK's native brightness capability and would be semantically incorrect.

### Decision 3: Persist brightness in DataStore via SettingsRepository

**Choice:** Add a `GLYPH_BRIGHTNESS` key to `SettingsKeys` and a `glyphBrightness` field to `AppSettings`. The `SettingsRepository` interface gets an `updateGlyphBrightness()` method.

**Rationale:** Follows the existing pattern for persisting settings (period, cycles, interval, theme). DataStore is already in use and handles persistence correctly.

### Decision 4: Expose brightness state from GlyphRepository

**Choice:** Add `val brightness: StateFlow<GlyphBrightness>` to `GlyphRepository` interface. ViewModels read from this flow. The repository reads the initial value from `SettingsRepository` on initialization.

**Rationale:** Centralizes brightness state in the repository layer, consistent with how `glyphState` and `deviceInfo` are managed.

## Risks / Trade-offs

- **[Risk] SDK buildBrightness() may not be available on all devices** → Mitigation: Check SDK version/feature support before calling; fall back to no-op if unavailable. The `DeviceInfo.isSupported` flag already gates SDK usage.
- **[Risk] Brightness state sync between SettingsRepository and GlyphRepository** → Mitigation: GlyphRepository reads from SettingsRepository on init and exposes a single Source of Truth via StateFlow. Settings changes flow through the repository.
- **[Trade-off] Removing Zone A/B progress bars** → Users who relied on those sliders lose that functionality. Mitigation: The global brightness control is a more intuitive replacement that applies to all zones.
