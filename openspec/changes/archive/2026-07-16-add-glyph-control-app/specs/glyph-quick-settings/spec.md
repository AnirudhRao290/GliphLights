## ADDED Requirements

### Requirement: Quick Settings tile displays current state
The Quick Settings Tile SHALL display the current glyph state with an appropriate icon.

#### Scenario: Tile shows active icon
- **WHEN** glyph is currently active
- **THEN** tile displays the active/on icon variant

#### Scenario: Tile shows inactive icon
- **WHEN** glyph is currently inactive
- **THEN** tile displays the inactive/off icon variant

### Requirement: Quick Settings tile toggles glyph on tap
The Quick Settings Tile SHALL toggle all glyph channels when tapped.

#### Scenario: User taps tile when glyph is off
- **WHEN** user taps the Quick Settings tile and glyph is inactive
- **THEN** tile activates all channels via toggle() and updates its icon

#### Scenario: User taps tile when glyph is on
- **WHEN** user taps the Quick Settings tile and glyph is active
- **THEN** tile deactivates all channels via toggle() and updates its icon

### Requirement: Quick Settings tile updates dynamically
The tile SHALL update its appearance when glyph state changes from any source.

#### Scenario: State change from app
- **WHEN** glyph state changes via the main app
- **THEN** Quick Settings tile icon updates to reflect new state

#### Scenario: State change from widget
- **WHEN** glyph state changes via the widget
- **THEN** Quick Settings tile icon updates to reflect new state
