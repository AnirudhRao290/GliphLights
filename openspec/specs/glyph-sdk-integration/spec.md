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

### Requirement: SDK AAR dependency is included in the build
The project SHALL include the Nothing Glyph SDK AAR file in `app/libs/GlyphSDK.aar` and reference it as a local file dependency in `app/build.gradle.kts`. The build SHALL compile successfully with the SDK available on the classpath.

#### Scenario: AAR file exists and build succeeds
- **WHEN** the project is built with `./gradlew assembleDebug`
- **THEN** the build succeeds and `GlyphManager` class is resolvable on the classpath

### Requirement: GlyphManager initialization
The system SHALL initialize the Glyph SDK by calling `GlyphManager.getInstance(context)` followed by `glyphManager.init(callback)`. The initialization SHALL detect the device model using `Common.is24111()` and register with `glyphManager.register(Glyph.DEVICE_24111)`. The session state SHALL transition from `DISCONNECTED` → `INITIALIZING` → `CONNECTED` on success.

#### Scenario: Successful initialization on Nothing Phone (3a) Pro
- **WHEN** `init()` is called on a Nothing Phone (3a) Pro device
- **THEN** the session state transitions to `CONNECTED` and `DeviceInfo` reports the correct model with `isSupported = true`

#### Scenario: Initialization on unsupported device
- **WHEN** `init()` is called on a device where `Common.is24111()` returns false
- **THEN** the session state remains `DISCONNECTED` and an `SdkResult.Error` is returned with "Unsupported device"

### Requirement: Session management
The system SHALL manage Glyph SDK sessions with explicit `openSession()` and `closeSession()` calls. Commands (toggle, animate, progress, turnOff) SHALL only execute when a session is active. The session state SHALL track `SESSION_ACTIVE` vs `SESSION_CLOSED`.

#### Scenario: Open session after connection
- **WHEN** `openSession()` is called while session state is `CONNECTED`
- **THEN** `glyphManager.openSession()` is invoked and session state becomes `SESSION_ACTIVE`

#### Scenario: Command rejected without active session
- **WHEN** `toggleChannels()` is called while session state is not `SESSION_ACTIVE`
- **THEN** an `SdkResult.Error` is returned and no SDK command is sent

#### Scenario: Close session
- **WHEN** `closeSession()` is called while session state is `SESSION_ACTIVE`
- **THEN** `glyphManager.closeSession()` is invoked and session state becomes `SESSION_CLOSED`

### Requirement: Channel toggle via GlyphFrame
The system SHALL toggle specific Glyph channels by building a `GlyphFrame` using `GlyphManager.getGlyphFrameBuilder()`, calling `buildChannel(index)` for each target channel, and invoking `glyphManager.toggle(frame)`. The frame SHALL include ALL channels that should be active after the toggle (additive behavior). Active channel state SHALL be tracked internally.

#### Scenario: Toggle single channel on preserves others
- **WHEN** channels 0 and 1 are active and `toggleChannels(listOf(2))` is called
- **THEN** a GlyphFrame is built with channels 0, 1, and 2, and `glyphManager.toggle(frame)` is called

#### Scenario: Toggle single channel off preserves others
- **WHEN** channels 0, 1, and 2 are active and `toggleChannels(listOf(1))` is called
- **THEN** a GlyphFrame is built with channels 0 and 2, and `glyphManager.toggle(frame)` is called

#### Scenario: Toggle removes last channel
- **WHEN** only channel 0 is active and `toggleChannels(listOf(0))` is called
- **THEN** `glyphManager.turnOff()` is called and the active channel set is empty

### Requirement: Channel animation via GlyphFrame
The system SHALL animate Glyph channels by building a `GlyphFrame` with animation parameters (period, cycles, interval) and calling `glyphManager.animate(frame)`. The frame SHALL include `buildPeriod()`, `buildCycles()`, and `buildInterval()` calls before `build()`.

#### Scenario: Animate zone with custom parameters
- **WHEN** `animateChannels(channels, AnimationParams(period=2000, cycles=3, interval=200))` is called
- **THEN** a GlyphFrame is built with period=2000, cycles=3, interval=200 for the specified channels, and `glyphManager.animate(frame)` is called

### Requirement: Progress display on Zone C
The system SHALL display a progress indicator on the Zone C camera ring by calling `glyphManager.displayProgress(frame, progress, reverse)`. The progress value SHALL range from 0 to 100.

#### Scenario: Display 50% progress
- **WHEN** `displayProgress(50, reverse=false)` is called
- **THEN** `glyphManager.displayProgress()` is invoked with progress=50 and reverse=false

### Requirement: Progress display on Zone A
The system SHALL simulate a progress indicator on Zone A (vertical bar, 11 channels) by progressively toggling channels based on the progress percentage. At 0% all Zone A channels SHALL be off. At 100% all Zone A channels SHALL be on. Intermediate values SHALL turn on the first N channels where N = ceil(progress * 11 / 100).

#### Scenario: Display 50% progress on Zone A
- **WHEN** `displayProgressForZone(ZoneA, 50)` is called
- **THEN** approximately the first 6 channels of Zone A (A1-A6) SHALL be turned on and the rest off

#### Scenario: Display 0% progress on Zone A
- **WHEN** `displayProgressForZone(ZoneA, 0)` is called
- **THEN** all Zone A channels SHALL be turned off

### Requirement: Progress display on Zone B
The system SHALL simulate a progress indicator on Zone B (horizontal bar, 5 channels) by progressively toggling channels based on the progress percentage. At 0% all Zone B channels SHALL be off. At 100% all Zone B channels SHALL be on. Intermediate values SHALL turn on the first N channels where N = ceil(progress * 5 / 100).

#### Scenario: Display 50% progress on Zone B
- **WHEN** `displayProgressForZone(ZoneB, 50)` is called
- **THEN** approximately the first 3 channels of Zone B (B1-B3) SHALL be turned on and the rest off

#### Scenario: Display 100% progress on Zone B
- **WHEN** `displayProgressForZone(ZoneB, 100)` is called
- **THEN** all 5 Zone B channels SHALL be turned on

### Requirement: Turn off all Glyphs
The system SHALL turn off all active Glyph channels by calling `glyphManager.turnOff()`. After turning off, the internal active channel set SHALL be cleared. The system SHALL also provide a `turnOffChannels(channels)` method that turns off specific channels without affecting other active channels.

#### Scenario: Turn off while channels are active
- **WHEN** `turnOff()` is called while some channels are active
- **THEN** `glyphManager.turnOff()` is called and the active channel set becomes empty

#### Scenario: Turn off specific channels
- **WHEN** `turnOffChannels(listOf(20, 25))` is called while channels 20, 25, and 31 are active
- **THEN** a GlyphFrame is built with only channel 31 (the remaining active channel) and `toggle(frame)` is called

#### Scenario: Turn off specific channels leaves none active
- **WHEN** `turnOffChannels(listOf(20, 25, 31))` is called while only those channels are active
- **THEN** `glyphManager.turnOff()` is called and the active channel set becomes empty

### Requirement: Cleanup on app exit
The system SHALL release SDK resources on cleanup by calling `closeSession()` then `glyphManager.unInit()`. The session state SHALL return to `DISCONNECTED`.

#### Scenario: Cleanup releases resources
- **WHEN** `cleanup()` is called
- **THEN** the session is closed, `glyphManager.unInit()` is called, and session state is `DISCONNECTED`

### Requirement: Toggle channels with brightness
The system SHALL provide a `toggleWithBrightness(channels, brightness)` method on GlyphRepository that toggles specific channels with a given brightness level in a single SDK command.

#### Scenario: Toggle with brightness applied
- **WHEN** `toggleWithBrightness(listOf(0,1,2), 0.5f)` is called
- **THEN** a GlyphFrame is built with channels 0, 1, 2 and `buildBrightness(0.5f)`, and `glyphManager.toggle(frame)` is called

#### Scenario: Turn off when brightness is zero
- **WHEN** `toggleWithBrightness(channels, 0.0f)` is called
- **THEN** `glyphManager.turnOff()` is called instead of sending a zero-brightness frame
