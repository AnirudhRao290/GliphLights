## ADDED Requirements

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
The system SHALL toggle specific Glyph channels by building a `GlyphFrame` using `GlyphManager.getGlyphFrameBuilder()`, calling `buildChannel(index)` for each target channel, and invoking `glyphManager.toggle(frame)`. Active channel state SHALL be tracked internally.

#### Scenario: Toggle single channel on
- **WHEN** `toggleChannels(listOf(0))` is called with an active session
- **THEN** a GlyphFrame is built with channel 0 and `glyphManager.toggle(frame)` is called

#### Scenario: Toggle multiple channels
- **WHEN** `toggleChannels(listOf(0, 1, 20))` is called with an active session
- **THEN** a GlyphFrame is built with channels 0, 1, and 20, and `glyphManager.toggle(frame)` is called

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

### Requirement: Turn off all Glyphs
The system SHALL turn off all active Glyph channels by calling `glyphManager.turnOff()`. After turning off, the internal active channel set SHALL be cleared.

#### Scenario: Turn off while channels are active
- **WHEN** `turnOff()` is called while some channels are active
- **THEN** `glyphManager.turnOff()` is called and the active channel set becomes empty

### Requirement: Cleanup on app exit
The system SHALL release SDK resources on cleanup by calling `closeSession()` then `glyphManager.unInit()`. The session state SHALL return to `DISCONNECTED`.

#### Scenario: Cleanup releases resources
- **WHEN** `cleanup()` is called
- **THEN** the session is closed, `glyphManager.unInit()` is called, and session state is `DISCONNECTED`
