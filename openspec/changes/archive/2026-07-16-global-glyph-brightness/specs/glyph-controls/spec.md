## REMOVED Requirements

### Requirement: Display progress on Zone A
**Reason**: Replaced by global brightness control. Per-zone progress simulation via channel toggling is being removed in favor of a unified brightness setting.
**Migration**: Use the global Glyph Brightness slider (Off, Level 1, Level 2, Level 3) to control all zone brightness.

### Requirement: Display progress on Zone B
**Reason**: Replaced by global brightness control. Per-zone progress simulation via channel toggling is being removed in favor of a unified brightness setting.
**Migration**: Use the global Glyph Brightness slider (Off, Level 1, Level 2, Level 3) to control all zone brightness.

## ADDED Requirements

### Requirement: Global brightness control on controls screen
The app SHALL display a global Glyph Brightness slider on the Controls screen with four discrete levels: Off, Level 1, Level 2, and Level 3. The brightness setting SHALL apply globally across all glyph zones.

#### Scenario: User adjusts brightness slider
- **WHEN** user moves the brightness slider to Level 2
- **THEN** brightness is set to 0.66 and all subsequent glyph operations use this brightness

#### Scenario: Brightness slider reflects current state
- **WHEN** user navigates to Controls screen with brightness at Level 1
- **THEN** the slider shows Level 1 as the selected position
