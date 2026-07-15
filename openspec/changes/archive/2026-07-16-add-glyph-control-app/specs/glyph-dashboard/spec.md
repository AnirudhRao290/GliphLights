## ADDED Requirements

### Requirement: Dashboard displays glyph status
The app SHALL display the current glyph state (active/inactive) prominently on the home screen.

#### Scenario: Glyph is active
- **WHEN** user opens the app and glyph channels are currently active
- **THEN** dashboard shows "Glyph Active" status with visual indicator

#### Scenario: Glyph is inactive
- **WHEN** user opens the app and no glyph channels are active
- **THEN** dashboard shows "Glyph Inactive" status with visual indicator

### Requirement: Dashboard displays device information
The app SHALL display connected device information including model detection and SDK connection status.

#### Scenario: Device is Phone (3a) Pro
- **WHEN** app detects device via Common.is24111()
- **THEN** dashboard shows "Nothing Phone (3a) Pro" as device model

#### Scenario: Device is unsupported Nothing model
- **WHEN** app detects a Nothing device that is not Phone (3a) Pro
- **THEN** dashboard shows the detected model name with limited feature notice

### Requirement: Dashboard displays active zones
The app SHALL show which glyph zones currently have active channels.

#### Scenario: Zones with active channels
- **WHEN** specific zones have active channels
- **THEN** dashboard highlights those zones (A, B, C) as active

### Requirement: Dashboard provides toggle all button
The app SHALL provide a button to toggle all glyph channels on or off.

#### Scenario: User taps toggle all
- **WHEN** user taps the "Toggle All" button
- **THEN** app builds a GlyphFrame with all channels and calls toggle()

### Requirement: Dashboard provides animate all button
The app SHALL provide a button to trigger a breathing animation on all channels.

#### Scenario: User taps animate all
- **WHEN** user taps the "Animate All" button
- **THEN** app builds a GlyphFrame with all channels and calls animate() with default parameters

### Requirement: Dashboard provides turn off button
The app SHALL provide a button to immediately turn off all active glyphs.

#### Scenario: User taps turn off
- **WHEN** user taps the "Turn Off" button
- **THEN** app calls GlyphManager.turnOff() and updates status display

### Requirement: Dashboard shows SDK connection state
The app SHALL display whether the Glyph SDK is connected and a session is active.

#### Scenario: SDK connected and session active
- **WHEN** GlyphManager service is connected and session is open
- **THEN** dashboard shows "SDK Ready" indicator

#### Scenario: SDK unavailable
- **WHEN** GlyphManager cannot connect to service
- **THEN** dashboard shows "SDK Unavailable" with explanation
