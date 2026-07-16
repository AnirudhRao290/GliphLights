## MODIFIED Requirements

### Requirement: AnimationModel as shared data contract
The system SHALL define an immutable `AnimationModel` data class containing a list of `NodeState` objects. Each `NodeState` SHALL contain a channel index, on/off state, and timestamp. Both the preview renderer and the SDK bridge SHALL consume the same AnimationModel.

#### Scenario: Model contains current state
- **WHEN** nodes A3, A7, and C12 are ON
- **THEN** the AnimationModel contains exactly 3 NodeState entries with those channel indices and isOn=true

#### Scenario: Model is immutable
- **WHEN** the AnimationModel is created
- **THEN** it cannot be modified after creation; new states produce a new model instance

### Requirement: SDK bridge converts model to toggle commands
The system SHALL convert the AnimationModel's active channels into SDK `toggle()` commands. The bridge SHALL build a `GlyphFrame` containing all currently active channels and call `glyphManager.toggle(frame)`. The bridge SHALL NOT send commands unless a session is active.

#### Scenario: Multiple active channels sent in one frame
- **WHEN** channels 20, 25, and 31 are active and session is active
- **THEN** a single GlyphFrame is built with channels 20, 25, and 31, and `toggle(frame)` is called once

#### Scenario: No active channels turns off
- **WHEN** no channels are active and session is active
- **THEN** `glyphManager.turnOff()` is called instead of sending an empty frame

#### Scenario: Command skipped when session inactive
- **WHEN** channels are active but no session is active
- **THEN** no SDK command is sent and no error is thrown

### Requirement: SDK bridge rate limiting
The system SHALL enforce a minimum 33ms interval between SDK commands. If the bridge is called faster than this interval, it SHALL coalesce — only the most recent state is sent when the interval elapses.

#### Scenario: Rapid state changes coalesced
- **WHEN** the bridge receives 5 state updates within 100ms
- **THEN** only 3 SDK commands are sent (at 0ms, 33ms, 66ms), using the latest state at each interval

#### Scenario: State updates at normal rate
- **WHEN** the bridge receives state updates every 50ms
- **THEN** each update results in one SDK command

### Requirement: SDK-agnostic editor
The editor components (GlyphMapView, GestureSampler, PreviewRenderer, ViewModel) SHALL NOT import or reference any Nothing Glyph SDK classes. Only the GlyphSdkRenderer SHALL depend on the SDK.

#### Scenario: Editor compiles without SDK
- **WHEN** the editor packages are compiled
- **THEN** they produce no compilation errors even if the Glyph SDK AAR is removed from the classpath

#### Scenario: SDK bridge is the only SDK touchpoint
- **WHEN** the codebase is analyzed for SDK imports
- **THEN** only GlyphSdkRenderer and GlyphRepository contain Glyph SDK imports
