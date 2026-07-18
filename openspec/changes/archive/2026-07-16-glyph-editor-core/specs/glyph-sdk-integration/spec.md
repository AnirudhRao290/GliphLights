## MODIFIED Requirements

### Requirement: Turn off all Glyphs
The system SHALL turn off all active Glyph channels by calling `glyphManager.turnOff()`. After turning off, the internal active channel set SHALL be cleared. The system SHALL also provide a `turnOffChannels(channels)` method that turns off specific channels without affecting other active channels.

#### Scenario: Turn off while channels are active
- **WHEN** `turnOff()` is called while some channels are active
- **THEN** `glyphManager.turnOff()` is called and the active channel set becomes empty

#### Scenario: Turn off specific channels
- **WHEN** `turnOffChannels(listOf(20, 25))` is called while channels 20, 25, and 31 are active
- **THEN** a GlyphFrame is built with only channel 31 (the remaining active channel) and `toggle(frame)` is called

#### Scenario: Turn off specific channels leaves none active
- **WHEN** `turnOffChannels(listOf(20, 25, 31))` is called while only those channels are active
- **THEN** `glyphManager.turnOff()` is called and the active channel set becomes empty
