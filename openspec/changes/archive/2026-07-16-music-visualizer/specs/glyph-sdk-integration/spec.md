## ADDED Requirements

### Requirement: Toggle channels with brightness
The system SHALL provide a `toggleWithBrightness(channels, brightness)` method on GlyphRepository that toggles specific channels with a given brightness level in a single SDK command.

#### Scenario: Toggle with brightness applied
- **WHEN** `toggleWithBrightness(listOf(0,1,2), 0.5f)` is called
- **THEN** a GlyphFrame is built with channels 0, 1, 2 and `buildBrightness(0.5f)`, and `glyphManager.toggle(frame)` is called

#### Scenario: Turn off when brightness is zero
- **WHEN** `toggleWithBrightness(channels, 0.0f)` is called
- **THEN** `glyphManager.turnOff()` is called instead of sending a zero-brightness frame
