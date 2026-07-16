## ADDED Requirements

### Requirement: Visualizer engine with mode-based rendering
The system SHALL implement a visualizer engine that accepts processed audio amplitude and renders Glyph commands based on the active visualization mode.

#### Scenario: Engine renders at target frame rate
- **WHEN** the visualizer is running
- **THEN** Glyph commands are dispatched at 20-30 FPS with a minimum 33ms interval between SDK commands

#### Scenario: Engine stops rendering when stopped
- **WHEN** the user stops the visualizer
- **THEN** no further Glyph commands are sent and all active channels are turned off

### Requirement: Pulse mode
The system SHALL implement a Pulse mode that maps audio amplitude to global Glyph brightness, causing all active channels to pulse with the sound.

#### Scenario: Loud sound produces bright pulse
- **WHEN** amplitude is 0.9 and Pulse mode is active
- **THEN** all Glyph channels are toggled on with brightness 0.9

#### Scenario: Quiet sound produces dim pulse
- **WHEN** amplitude is 0.2 and Pulse mode is active
- **THEN** all Glyph channels are toggled on with brightness 0.2

#### Scenario: Silence turns off Glyph
- **WHEN** amplitude is 0.0 and Pulse mode is active
- **THEN** all Glyph channels are turned off

### Requirement: Wave mode
The system SHALL implement a Wave mode that activates Glyph zones sequentially (A then B then C) based on amplitude, creating a wave-like effect across the phone.

#### Scenario: High amplitude activates all zones
- **WHEN** amplitude is above 0.7 and Wave mode is active
- **THEN** zones A, B, and C are all activated with brightness proportional to amplitude

#### Scenario: Medium amplitude activates partial zones
- **WHEN** amplitude is 0.4 and Wave mode is active
- **THEN** only Zone A is activated with brightness 0.4

#### Scenario: Low amplitude activates no zones
- **WHEN** amplitude is below 0.15 and Wave mode is active
- **THEN** no zones are activated

### Requirement: Beat mode
The system SHALL implement a Beat mode that detects amplitude peaks and flashes the Glyph briefly on each beat.

#### Scenario: Beat detected triggers flash
- **WHEN** amplitude crosses above 0.6 after being below 0.3 (peak detected)
- **THEN** all Glyph channels flash on at full brightness for 100ms then turn off

#### Scenario: No beat means no activity
- **WHEN** amplitude remains steady without peaks
- **THEN** no Glyph activity occurs

### Requirement: Glow mode
The system SHALL implement a Glow mode where brightness smoothly follows the audio level with additional smoothing for a organic, breathing-like effect.

#### Scenario: Brightness tracks audio smoothly
- **WHEN** amplitude varies between 0.2 and 0.8
- **THEN** Glyph brightness follows with additional EMA smoothing (alpha=0.15), creating gentle transitions

#### Scenario: Glow mode uses all zones
- **WHEN** Glow mode is active
- **THEN** all Glyph zones (A, B, C) are activated with the same smoothed brightness value

### Requirement: Automatic mode disabling on SDK limitation
The system SHALL automatically disable visualization modes that cannot be implemented due to SDK limitations, and inform the user.

#### Scenario: Mode unavailable on unsupported device
- **WHEN** a visualization mode requires SDK features not available on the current device
- **THEN** the mode is marked as unavailable in the UI and cannot be selected
