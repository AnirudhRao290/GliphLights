## ADDED Requirements

### Requirement: SDK availability check
The app SHALL check for Nothing Glyph SDK availability at startup.

#### Scenario: SDK available on Nothing device
- **WHEN** app launches on a Nothing device with Glyph SDK available
- **THEN** app initializes GlyphManager, registers with correct device constant, and opens session

#### Scenario: SDK unavailable on non-Nothing device
- **WHEN** app launches on a non-Nothing device
- **THEN** app displays unavailable state, disables controls, shows explanation message

### Requirement: GlyphRepository abstraction layer
The app SHALL implement a GlyphRepository interface that abstracts the Nothing Glyph SDK.

#### Scenario: Repository provides toggle API
- **WHEN** ViewModel calls repository toggle with a GlyphFrame
- **THEN** repository forwards command to GlyphManager.toggle() and returns result

#### Scenario: Repository provides animate API
- **WHEN** ViewModel calls repository animate with GlyphFrame parameters
- **THEN** repository builds GlyphFrame and calls GlyphManager.animate()

#### Scenario: Repository provides turnOff API
- **WHEN** ViewModel calls repository turnOff
- **THEN** repository calls GlyphManager.turnOff()

#### Scenario: Repository provides status API
- **WHEN** ViewModel requests glyph status
- **THEN** repository returns current session state and active channels

### Requirement: SDK initialization lifecycle
The app SHALL properly initialize and release SDK resources following Android lifecycle.

#### Scenario: App in foreground
- **WHEN** app is in foreground
- **THEN** GlyphManager is initialized, registered, and session is open

#### Scenario: App goes to background
- **WHEN** app moves to background
- **THEN** session is closed and GlyphManager resources are released

#### Scenario: App returns to foreground
- **WHEN** app returns to foreground
- **THEN** session is re-opened if service is still connected

### Requirement: Device detection
The app SHALL detect the specific Nothing device model and register accordingly.

#### Scenario: Phone (3a) Pro detected
- **WHEN** Common.is24111() returns true
- **THEN** app registers with Glyph.DEVICE_24111 and enables all 36 channels

#### Scenario: Other Nothing device detected
- **WHEN** a different Nothing model is detected
- **THEN** app registers with appropriate device constant and adjusts available channels

### Requirement: Query supported features
The app SHALL query the SDK for supported features and capabilities based on device model.

#### Scenario: Features queried on init
- **WHEN** SDK initializes successfully for Phone (3a) Pro
- **THEN** app determines available zones (A: 11 channels, B: 5 channels, C: 20 channels)

#### Scenario: Unsupported feature encountered
- **WHEN** user tries to use a feature not supported by the current device
- **THEN** app displays "Not supported on this device" message

### Requirement: API key configuration
The app SHALL support API key configuration for SDK access.

#### Scenario: Debug mode
- **WHEN** app is in debug build variant
- **THEN** app uses "test" as the NothingKey meta-data value

#### Scenario: Release mode
- **WHEN** app is in release build variant
- **THEN** app requires a valid API key configured in AndroidManifest

### Requirement: Session management state machine
The app SHALL manage SDK sessions using a state machine to prevent misuse.

#### Scenario: Valid session sequence
- **WHEN** app starts and SDK is available
- **THEN** state machine transitions: DISCONNECTED → INITIALIZING → CONNECTED → SESSION_ACTIVE

#### Scenario: Session closed properly
- **WHEN** app goes to background
- **THEN** state machine transitions: SESSION_ACTIVE → SESSION_CLOSED → CONNECTED

#### Scenario: Invalid session attempt
- **WHEN** app tries to use SDK without active session
- **THEN** app catches GlyphException and displays error message
