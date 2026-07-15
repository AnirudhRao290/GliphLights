## MODIFIED Requirements

### Requirement: Channel toggle via GlyphFrame
The system SHALL toggle specific Glyph channels by building a `GlyphFrame` using `GlyphManager.getGlyphFrameBuilder()`, calling `buildChannel(index)` for each target channel, and invoking `glyphManager.toggle(frame)`. The frame SHALL include ALL channels that should be active after the toggle (additive behavior). Active channel state SHALL be tracked internally.

#### Scenario: Toggle single channel on preserves others
- **WHEN** channels 0 and 1 are active and `toggleChannels(listOf(2))` is called
- **THEN** a GlyphFrame is built with channels 0, 1, and 2, and `glyphManager.toggle(frame)` is called

#### Scenario: Toggle single channel off preserves others
- **WHEN** channels 0, 1, and 2 are active and `toggleChannels(listOf(1))` is called
- **THEN** a GlyphFrame is built with channels 0 and 2, and `glyphManager.toggle(frame)` is called

#### Scenario: Toggle removes last channel
- **WHEN** only channel 0 is active and `toggleChannels(listOf(0))` is called
- **THEN** `glyphManager.turnOff()` is called and the active channel set is empty

### Requirement: Channel animation via GlyphFrame
The system SHALL animate Glyph channels by building a `GlyphFrame` with animation parameters (period, cycles, interval) and calling `glyphManager.animate(frame)`. The frame SHALL include `buildPeriod()`, `buildCycles()`, and `buildInterval()` calls before `build()`.

#### Scenario: Animate zone with custom parameters
- **WHEN** `animateChannels(channels, AnimationParams(period=2000, cycles=3, interval=200))` is called
- **THEN** a GlyphFrame is built with period=2000, cycles=3, interval=200 for the specified channels, and `glyphManager.animate(frame)` is called

## ADDED Requirements

### Requirement: Progress display on Zone A
The system SHALL simulate a progress indicator on Zone A (vertical bar, 11 channels) by progressively toggling channels based on the progress percentage. At 0% all Zone A channels SHALL be off. At 100% all Zone A channels SHALL be on. Intermediate values SHALL turn on the first N channels where N = ceil(progress * 11 / 100).

#### Scenario: Display 50% progress on Zone A
- **WHEN** `displayProgressForZone(ZoneA, 50)` is called
- **THEN** approximately the first 6 channels of Zone A (A1-A6) SHALL be turned on and the rest off

#### Scenario: Display 0% progress on Zone A
- **WHEN** `displayProgressForZone(ZoneA, 0)` is called
- **THEN** all Zone A channels SHALL be turned off

### Requirement: Progress display on Zone B
The system SHALL simulate a progress indicator on Zone B (horizontal bar, 5 channels) by progressively toggling channels based on the progress percentage. At 0% all Zone B channels SHALL be off. At 100% all Zone B channels SHALL be on. Intermediate values SHALL turn on the first N channels where N = ceil(progress * 5 / 100).

#### Scenario: Display 50% progress on Zone B
- **WHEN** `displayProgressForZone(ZoneB, 50)` is called
- **THEN** approximately the first 3 channels of Zone B (B1-B3) SHALL be turned on and the rest off

#### Scenario: Display 100% progress on Zone B
- **WHEN** `displayProgressForZone(ZoneB, 100)` is called
- **THEN** all 5 Zone B channels SHALL be turned on
