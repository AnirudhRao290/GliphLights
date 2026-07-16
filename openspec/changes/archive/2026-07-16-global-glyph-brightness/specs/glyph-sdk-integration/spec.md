## REMOVED Requirements

### Requirement: Progress display on Zone A
**Reason**: Replaced by global brightness control. The per-zone progress simulation via channel toggling is being removed.
**Migration**: Use the global brightness setting applied via GlyphFrame buildBrightness().

### Requirement: Progress display on Zone B
**Reason**: Replaced by global brightness control. The per-zone progress simulation via channel toggling is being removed.
**Migration**: Use the global brightness setting applied via GlyphFrame buildBrightness().

## ADDED Requirements

### Requirement: Brightness applied to GlyphFrame building
The system SHALL apply the global brightness setting to all GlyphFrame building operations by calling `glyphFrameBuilder.buildBrightness(level)` with the current brightness value (0.0, 0.33, 0.66, or 1.0).

#### Scenario: Toggle with brightness
- **WHEN** `toggleChannels()` is called while brightness is at Level 1 (0.33)
- **THEN** a GlyphFrame is built with `buildBrightness(0.33f)` before `build()`, and `glyphManager.toggle(frame)` is called

#### Scenario: Animate with brightness
- **WHEN** `animateChannels()` is called while brightness is at Level 2 (0.66)
- **THEN** a GlyphFrame is built with `buildBrightness(0.66f)` before `build()`, and `glyphManager.animate(frame)` is called

#### Scenario: Brightness at Off level
- **WHEN** brightness is set to Off (0.0)
- **THEN** GlyphFrame is built with `buildBrightness(0.0f)`, effectively disabling glyph output

### Requirement: Brightness state exposed via repository
The GlyphRepository SHALL expose a `brightness: StateFlow<GlyphBrightness>` that represents the current global brightness level. The initial value SHALL be read from SettingsRepository on initialization.

#### Scenario: Repository exposes brightness flow
- **WHEN** ViewModel subscribes to `glyphRepository.brightness`
- **THEN** it receives the current brightness level and updates when brightness changes

#### Scenario: Brightness initialized from settings
- **WHEN** GlyphRepository initializes
- **THEN** it reads the persisted brightness value from SettingsRepository and sets it as the initial state
