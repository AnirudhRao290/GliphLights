## ADDED Requirements

### Requirement: Display all three zones
The app SHALL display controls for all 3 glyph zones: A (A1-A11), B (B1-B5), and C (C1-C20).

#### Scenario: Zones displayed
- **WHEN** user navigates to controls screen
- **THEN** app shows Zone A, Zone B, and Zone C sections with channel counts

### Requirement: Toggle individual zone
The app SHALL allow users to toggle an entire zone on/off using SDK zone shortcuts.

#### Scenario: User toggles Zone A
- **WHEN** user taps Zone A toggle button
- **THEN** app calls buildChannelA().build() and toggle()

#### Scenario: User toggles Zone B
- **WHEN** user taps Zone B toggle button
- **THEN** app calls buildChannelB().build() and toggle()

#### Scenario: User toggles Zone C
- **WHEN** user taps Zone C toggle button
- **THEN** app calls buildChannelC().build() and toggle()

### Requirement: Toggle individual channel
The app SHALL allow users to toggle individual channels within each zone.

#### Scenario: User toggles specific channel
- **WHEN** user taps a specific channel toggle (e.g., C5)
- **THEN** app builds GlyphFrame with that channel index and calls toggle()

### Requirement: Animate zone with parameters
The app SHALL allow users to trigger breathing animation on a zone with configurable period, cycles, and interval.

#### Scenario: User triggers animation on Zone C
- **WHEN** user taps animate on Zone C with period=3000ms, cycles=3, interval=500ms
- **THEN** app builds GlyphFrame with those parameters and calls animate()

#### Scenario: User triggers animation with defaults
- **WHEN** user taps animate without configuring parameters
- **THEN** app uses default values (period=2000ms, cycles=2, interval=200ms)

### Requirement: Display progress on C zone
The app SHALL allow users to display a progress value on the C zone (camera ring).

#### Scenario: User sets progress value
- **WHEN** user adjusts progress slider for Zone C
- **THEN** app calls displayProgress() with the C zone frame and progress value

#### Scenario: User sets reverse progress
- **WHEN** user toggles reverse option and adjusts progress
- **THEN** app calls displayProgress() with reverse=true

### Requirement: Combine channels in single frame
The app SHALL allow users to select multiple channels across zones and control them simultaneously.

#### Scenario: User selects multiple channels
- **WHEN** user selects channels A3, B2, and C10
- **THEN** app builds GlyphFrame with all three channels and applies toggle/animate

### Requirement: Turn off all glyphs
The app SHALL provide a button to immediately turn off all active glyphs.

#### Scenario: User taps turn off
- **WHEN** user taps the "Turn Off All" button
- **THEN** app calls GlyphManager.turnOff()

### Requirement: Display active effects list
The app SHALL show which channels and zones are currently active.

#### Scenario: Active channels displayed
- **WHEN** channels are toggled on
- **THEN** controls screen highlights active channels with visual indicators
