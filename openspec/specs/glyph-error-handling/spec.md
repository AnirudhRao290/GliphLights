## ADDED Requirements

### Requirement: SDK unavailable error handling
The app SHALL gracefully handle cases where the Glyph SDK is not available.

#### Scenario: Non-Nothing device
- **WHEN** app is installed on a device without Glyph hardware
- **THEN** app displays friendly message explaining device incompatibility and disables all glyph controls

#### Scenario: SDK service not found
- **WHEN** device has Glyph hardware but SDK service is not available
- **THEN** app displays message suggesting device compatibility check

### Requirement: Unsupported device handling
The app SHALL detect and handle unsupported Nothing Phone models.

#### Scenario: Unsupported Nothing model
- **WHEN** app runs on a Nothing device not recognized by Common.is*() methods
- **THEN** app displays which features may be available and which are not

### Requirement: Missing permissions error handling
The app SHALL handle missing required permissions gracefully.

#### Scenario: Permission denied
- **WHEN** user denies the com.nothing.ketchum.permission.ENABLE permission
- **THEN** app explains which permission is needed and why, provides retry option

#### Scenario: Permission permanently denied
- **WHEN** user permanently denies a required permission
- **THEN** app directs user to system settings to enable the permission

### Requirement: GlyphException handling
The app SHALL catch and handle GlyphException from SDK operations.

#### Scenario: GlyphException on session open
- **WHEN** openSession() throws GlyphException
- **THEN** app catches exception, logs it, and displays "Session error" message with retry

#### Scenario: GlyphException on toggle/animate
- **WHEN** toggle() or animate() throws GlyphException
- **THEN** app catches exception and displays "Control error" message

#### Scenario: GlyphException on register
- **WHEN** register() returns false or throws exception
- **THEN** app displays "Registration failed" message explaining authorization issue

### Requirement: Service connection error handling
The app SHALL handle GlyphManager service connection failures.

#### Scenario: Service disconnected
- **WHEN** onServiceDisconnected callback fires
- **THEN** app updates state to disconnected, closes session, and notifies user

#### Scenario: Service never connects
- **WHEN** GlyphManager.init() callback does not fire within timeout
- **THEN** app displays "Service unavailable" message

### Requirement: Error messages are user-friendly
All error messages displayed to users SHALL be clear, actionable, and non-technical.

#### Scenario: Error message displayed
- **WHEN** any error condition occurs
- **THEN** app shows a message explaining what happened and what the user can do

### Requirement: Error state recovery
The app SHALL allow users to recover from error states.

#### Scenario: Retry after error
- **WHEN** an error occurs and user taps retry
- **THEN** app attempts the failed operation again (re-init, re-register, re-open session)

#### Scenario: Graceful degradation
- **WHEN** a non-critical feature fails
- **THEN** app continues to function with remaining features while showing partial availability
