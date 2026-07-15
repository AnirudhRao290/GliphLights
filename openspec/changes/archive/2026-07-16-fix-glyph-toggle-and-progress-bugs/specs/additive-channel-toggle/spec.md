## ADDED Requirements

### Requirement: Individual channel toggle is additive
When a user toggles an individual channel, the system SHALL preserve all other currently active channels. The toggled channel SHALL be added to the active set if it was off, or removed if it was on. The GlyphFrame sent to the SDK SHALL contain all channels that should remain active.

#### Scenario: Toggle on preserves existing channels
- **WHEN** channels A1 and A2 are active and the user toggles A3 on
- **THEN** the GlyphFrame SHALL contain channels A1, A2, and A3, and all three SHALL be lit

#### Scenario: Toggle off preserves other channels
- **WHEN** channels A1, A2, and A3 are active and the user toggles A2 off
- **THEN** the GlyphFrame SHALL contain channels A1 and A3, and only those SHALL remain lit

#### Scenario: Toggle last channel off
- **WHEN** only channel A1 is active and the user toggles A1 off
- **THEN** the active channel set SHALL be empty and `turnOff()` SHALL be called

### Requirement: Toggle All checks current state
When the user presses Toggle All, the system SHALL turn OFF all lights if they are currently active, or turn ON all lights if they are currently inactive.

#### Scenario: Toggle All when lights are off
- **WHEN** no channels are active and the user presses Toggle All
- **THEN** all channels across all zones SHALL be toggled ON

#### Scenario: Toggle All when lights are on
- **WHEN** all channels are active and the user presses Toggle All
- **THEN** all channels SHALL be turned OFF

#### Scenario: Toggle All when some lights are on
- **WHEN** some channels are active (not all) and the user presses Toggle All
- **THEN** all channels SHALL be turned ON (completing the set)

### Requirement: Zone toggle is additive
When a user toggles an entire zone, the system SHALL preserve channels from other zones. Only the target zone's channels SHALL be affected.

#### Scenario: Toggle Zone A preserves Zone B and C
- **WHEN** Zone B channels are active and the user toggles Zone A on
- **THEN** Zone B channels SHALL remain active and Zone A channels SHALL also become active

#### Scenario: Toggle Zone A off preserves Zone B
- **WHEN** both Zone A and Zone B channels are active and the user toggles Zone A off
- **THEN** Zone B channels SHALL remain active and Zone A channels SHALL become inactive
