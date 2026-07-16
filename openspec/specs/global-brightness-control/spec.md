## ADDED Requirements

### Requirement: Global brightness control with four discrete levels
The app SHALL provide a global Glyph Brightness slider with four discrete levels: Off, Level 1, Level 2, and Level 3. The brightness setting SHALL apply globally across all glyph zones rather than being configured per zone.

#### Scenario: User selects Off level
- **WHEN** user selects the Off brightness level
- **THEN** brightness is set to 0.0 and all glyph output is disabled

#### Scenario: User selects Level 1
- **WHEN** user selects Level 1 brightness
- **THEN** brightness is set to 0.33 and glyphs emit at low brightness

#### Scenario: User selects Level 2
- **WHEN** user selects Level 2 brightness
- **THEN** brightness is set to 0.66 and glyphs emit at medium brightness

#### Scenario: User selects Level 3
- **WHEN** user selects Level 3 brightness
- **THEN** brightness is set to 1.0 and glyphs emit at full brightness

### Requirement: Brightness persists across sessions
The brightness setting SHALL be persisted using DataStore Preferences and restored on app launch.

#### Scenario: Brightness survives app restart
- **WHEN** user sets brightness to Level 2 and restarts the app
- **THEN** brightness is restored to Level 2

#### Scenario: Brightness survives device reboot
- **WHEN** device is rebooted
- **THEN** brightness retains its previously configured value

### Requirement: Brightness applies to all glyph operations
The global brightness setting SHALL be applied to all glyph frame building operations including toggle, animate, and any other SDK commands.

#### Scenario: Toggle with brightness applied
- **WHEN** user toggles channels while brightness is at Level 1
- **THEN** the GlyphFrame is built with brightness=0.33 and sent to the SDK

#### Scenario: Animate with brightness applied
- **WHEN** user triggers animation while brightness is at Level 2
- **THEN** the GlyphFrame is built with brightness=0.66 and sent to the SDK

### Requirement: Brightness slider displayed on Controls screen
The Controls screen SHALL display the global brightness slider prominently, replacing the removed Zone A and Zone B progress bars.

#### Scenario: Brightness slider visible
- **WHEN** user navigates to the Controls screen
- **THEN** a brightness slider with four discrete positions (Off, Level 1, Level 2, Level 3) is displayed

#### Scenario: Current brightness reflected in slider
- **WHEN** brightness is set to Level 3
- **THEN** the slider shows Level 3 as selected
