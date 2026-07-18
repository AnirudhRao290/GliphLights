## MODIFIED Requirements

### Requirement: SDK bridge converts model to toggle commands
The system SHALL convert the AnimationModel's active channels into SDK `toggle()` commands. The bridge SHALL build a `GlyphFrame` containing all currently active channels and call `glyphManager.toggle(frame)`.

#### Scenario: Multiple active channels sent in one frame
- **WHEN** channels 20, 25, and 31 are active
- **THEN** a single GlyphFrame is built with channels 20, 25, and 31, and `toggle(frame)` is called once

#### Scenario: No active channels turns off
- **WHEN** no channels are active
- **THEN** `glyphManager.turnOff()` is called instead of sending an empty frame

#### Scenario: SDK call timeout
- **WHEN** an SDK call (init, register, openSession, toggle) does not complete within 5 seconds
- **THEN** the call SHALL be cancelled and an `SdkResult.Error` returned with message indicating timeout

#### Scenario: Service connection timeout
- **WHEN** `GlyphManager.init(callback)` is called and the service connection callback does not fire within 5 seconds
- **THEN** the initialization SHALL fail with `SdkResult.Error` and message "Glyph service connection timed out"

#### Scenario: Timeout resets loading state
- **WHEN** an SDK call times out during `startSession()`
- **THEN** `isLoading` SHALL be set to `false` and `errorMessage` SHALL contain the timeout error message

## ADDED Requirements

### Requirement: SDK initialization timeout
The system SHALL enforce a maximum 5-second timeout on all SDK initialization calls (`init`, `register`, `openSession`). If any call exceeds this timeout, it SHALL fail gracefully with an error message.

#### Scenario: Init timeout on slow service
- **WHEN** `GlyphManagerWrapper.init()` is called and the Glyph service does not connect within 5 seconds
- **THEN** the init call SHALL throw `TimeoutCancellationException` which is caught and returned as `SdkResult.Error`

#### Scenario: Register timeout
- **WHEN** `glyphManager.register()` does not complete within 5 seconds
- **THEN** the register call SHALL fail with `SdkResult.Error` and message "Glyph registration timed out"

#### Scenario: Open session timeout
- **WHEN** `glyphManager.openSession()` does not complete within 5 seconds
- **THEN** the openSession call SHALL fail with `SdkResult.Error` and message "Glyph session open timed out"

### Requirement: Error recovery after timeout
The system SHALL ensure that after any SDK timeout, the UI returns to a state where the user can retry the operation.

#### Scenario: Retry after timeout
- **WHEN** `startSession()` fails due to timeout
- **THEN** `isLoading` SHALL be `false`, `isSessionActive` SHALL be `false`, and the Start button SHALL be enabled for retry

#### Scenario: Error message displayed
- **WHEN** an SDK timeout occurs during `startSession()`
- **THEN** a Snackbar SHALL display the timeout error message to the user
